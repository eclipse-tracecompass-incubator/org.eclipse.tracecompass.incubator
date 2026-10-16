/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.ProcessNameAspect;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * Event aspects for the TraceX trace type.
 *
 * <p>
 * Two kinds of aspects are exposed:
 * <ul>
 * <li>Typed columns for every stable {@link TraceXEvent} field (context,
 * priority, event id, raw words, resolved object names, ...), so the Events
 * table is fully populated.</li>
 * <li>OS-model aspects ({@link LinuxTidAspect}, {@link ProcessNameAspect}, and a
 * thread-priority aspect) that let the shared Trace Compass tooling and the
 * kernel-shaped execution analysis correlate TraceX contexts to threads.</li>
 * </ul>buildAspects
 */
public final class TraceXAspects {

    private TraceXAspects() {
        // utility
    }

    private static final List<@NonNull ITmfEventAspect<?>> ASPECTS = buildAspects();

    /**
     * Get the immutable list of TraceX event aspects.
     *
     * @return the aspects
     */
    public static @NonNull Iterable<@NonNull ITmfEventAspect<?>> getAspects() {
        return ASPECTS;
    }

    private static @Nullable TraceXEvent event(ITmfEvent event) {
        Object value = event.getContent().getValue();
        return value instanceof TraceXEvent ? (TraceXEvent) value : null;
    }

    private static List<@NonNull ITmfEventAspect<?>> buildAspects() {
        ImmutableList.Builder<@NonNull ITmfEventAspect<?>> builder = ImmutableList.builder();

        // Base timestamp and type columns.
        builder.add(TmfBaseAspects.getTimestampAspect());
        builder.add(TmfBaseAspects.getEventTypeAspect());

        // OS-model aspects: TID, process (thread) name, and priority. These are
        // what let the shared views and analyses treat TraceX contexts as
        // threads.
        builder.add(new TraceXTidAspect());
        builder.add(new TraceXProcessNameAspect());
        builder.add(new TraceXPriorityAspect());
        builder.add(new TraceXPreemptionThresholdAspect());
        // Single-core CPU aspect: lets the stock kernel CPU-usage analysis
        // resolve a CPU for every event so it can accumulate per-thread time.
        builder.add(new TraceXCpuAspect());
        // One typed column per stable TraceXEvent field.
        builder.add(stringField("Context", TraceXEvent.FIELD_CONTEXT)); //$NON-NLS-1$
        builder.add(stringField("Context address", TraceXEvent.FIELD_CONTEXT_ADDRESS)); //$NON-NLS-1$
        builder.add(longField("Event ID", TraceXEvent.FIELD_EVENT_ID)); //$NON-NLS-1$
        builder.add(longField("Raw event ID", TraceXEvent.FIELD_RAW_EVENT_ID)); //$NON-NLS-1$
        builder.add(longField("Raw priority", TraceXEvent.FIELD_RAW_PRIORITY)); //$NON-NLS-1$
        builder.add(longField("Raw timestamp", TraceXEvent.FIELD_RAW_TIMESTAMP)); //$NON-NLS-1$
        builder.add(longField("Relative ticks", TraceXEvent.FIELD_RELATIVE_TICKS)); //$NON-NLS-1$
        builder.add(longField("Logical rank", TraceXEvent.FIELD_LOGICAL_RANK)); //$NON-NLS-1$
        builder.add(longField("Physical offset", TraceXEvent.FIELD_PHYSICAL_OFFSET)); //$NON-NLS-1$

        // Raw information words (always present, four of them).
        for (int word = 1; word <= 4; word++) {
            builder.add(longField("Info word " + word, "raw_info_" + word)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return builder.build();
    }

    /** A generic aspect reading a named value from the event field map. */
    private static @NonNull ITmfEventAspect<Object> field(String name, String key) {
        return new ITmfEventAspect<Object>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getHelpText() {
                return ""; //$NON-NLS-1$
            }

            @Override
            public @Nullable Object resolve(ITmfEvent event) {
                TraceXEvent traceXEvent = event(event);
                return traceXEvent == null ? null : traceXEvent.getFields().get(key);
            }
        };
    }

    private static @NonNull ITmfEventAspect<Object> stringField(String name, String key) {
        return field(name, key);
    }

    /** A field aspect that resolves to a {@link Long} when the value is numeric. */
    private static @NonNull ITmfEventAspect<Long> longField(String name, String key) {
        return new ITmfEventAspect<Long>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getHelpText() {
                return ""; //$NON-NLS-1$
            }

            @Override
            public @Nullable Long resolve(ITmfEvent event) {
                TraceXEvent traceXEvent = event(event);
                if (traceXEvent == null) {
                    return null;
                }
                Object value = traceXEvent.getFields().get(key);
                if (value instanceof Number) {
                    return Long.valueOf(((Number) value).longValue());
                }
                return null;
            }
        };
    }

    /** TID aspect: ThreadX thread object address resolved to a dense TID. */
    private static final class TraceXTidAspect extends LinuxTidAspect {
        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            TraceXEvent traceXEvent = event(event);
            ITmfTrace trace = event.getTrace();
            if (traceXEvent == null || !(trace instanceof TraceXTrace)) {
                return null;
            }
            TraceXContext context = traceXEvent.getContext();
            if (context.getKind() != TraceXContext.Kind.THREAD
                    && context.getKind() != TraceXContext.Kind.IDLE) {
                return null;
            }
            int tid = ((TraceXTrace) trace).getThreadId(context.getRawAddress());
            return tid < 0 ? null : Integer.valueOf(tid);
        }
    }

    /** Single-core CPU aspect: TraceX runs on a single logical CPU (0). */
    private static final class TraceXCpuAspect extends TmfCpuAspect {
        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event(event) == null ? null : Integer.valueOf(0);
        }
    }

    /** Process-name aspect: the resolved ThreadX thread name. */
    private static final class TraceXProcessNameAspect extends ProcessNameAspect {        @Override
        public @Nullable String resolve(@NonNull ITmfEvent event) {
            TraceXEvent traceXEvent = event(event);
            if (traceXEvent == null) {
                return null;
            }
            return traceXEvent.getContext().getName();
        }
    }

    /** Thread-priority aspect reading the decoded 10-bit ThreadX priority. */
    private static final class TraceXPriorityAspect implements ITmfEventAspect<Integer> {
        @Override
        public String getName() {
            return "Priority"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return "ThreadX thread priority (lower is higher priority)"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            TraceXEvent traceXEvent = event(event);
            return traceXEvent == null ? null : traceXEvent.getContext().getPriority();
        }
    }

    /** Preemption-threshold aspect reading the decoded 10-bit threshold. */
    private static final class TraceXPreemptionThresholdAspect implements ITmfEventAspect<Integer> {
        @Override
        public String getName() {
            return "Preemption threshold"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return ""; //$NON-NLS-1$
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            TraceXEvent traceXEvent = event(event);
            return traceXEvent == null ? null
                    : traceXEvent.getContext().getPreemptionThreshold();
        }
    }
}
