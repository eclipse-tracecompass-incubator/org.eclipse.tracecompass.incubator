/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.fieldcount.core;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiGenericAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiTableEntryAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiAnalysis;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableClass;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiLongNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimestamp;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

/**
 * Event count analysis, an on-demand analysis that generates Lami Tables while
 * not being an LTTng Analysis report. This will help with textual logs in order
 * to de-duplicate fields.
 *
 * @author Matthew Khouzam
 *
 */
public class FieldCountAnalysis extends LamiAnalysis {

    private static final long MASK = (1 << 10) - 1L;
    /**
     * This is a simple way to remove unique elements. There should be a smarter
     * way to do this.
     */
    private static final int MEMORY_SANITY_LIMIT = 10000;

    /**
     * Constructor
     */
    public FieldCountAnalysis() {
        super("Event Fields Count", false, trace -> true, Collections.emptyList());
    }

    @Override
    protected synchronized void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        return true;
    }

    private static int workRemaining(ITmfTrace trace) {
        return (int) Math.min(trace.getNbEvents() / (MASK + 1), Integer.MAX_VALUE);
    }

    @Override
    public List<LamiResultTable> execute(ITmfTrace trace, @Nullable TmfTimeRange timeRange, String extraParamsString, IProgressMonitor monitor) throws CoreException {
        List<LamiResultTable> results = new ArrayList<>();
        List<ITmfEventAspect<?>> aspects = new ArrayList<>();
        TmfTimeRange tr = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;
        Set<String> forbidden = TmfBaseAspects.getBaseAspects().stream().map(aspect -> aspect.getName()).collect(Collectors.toSet());
        Iterable<ITmfEventAspect<?>> eventAspects = getTraceAspects(trace);
        for (ITmfEventAspect<?> aspect : eventAspects) {
            Type[] genericInterfaces = aspect.getClass().getGenericInterfaces();
            if (genericInterfaces.length > 0) {
                Type type = genericInterfaces[0];
                if (!type.getClass().isAssignableFrom(Number.class) && !(forbidden.contains(aspect.getName()))) {
                    aspects.add(aspect);
                }
            }
        }
        SubMonitor mon = SubMonitor.convert(monitor, "Event Count Analysis", workRemaining(trace));
        AtomicLong done = new AtomicLong();
        Map<String, Multiset<String>> eventAspectCounts = new HashMap<>();

        TmfEventRequest req = new TmfEventRequest(ITmfEvent.class, tr, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                }
                for (ITmfEventAspect<?> aspect : aspects) {
                    Object resolved = aspect.resolve(event);
                    if (resolved != null) {
                        Multiset<String> dataSet = eventAspectCounts.computeIfAbsent(aspect.getName(), unused -> HashMultiset.create());
                        if (dataSet.size() < MEMORY_SANITY_LIMIT || dataSet.contains(resolved)) {
                            dataSet.add(String.valueOf(resolved));
                        }
                    }
                }
                if ((done.incrementAndGet() & MASK) == 0) {
                    mon.setWorkRemaining(workRemaining(trace));
                    mon.worked(1);
                    monitor.setTaskName("Event Count Analysis (" + done.get() + ")");
                }
            }

        };
        trace.sendRequest(req);
        try {
            req.waitForCompletion();
            for (Entry<String, Multiset<String>> entry : eventAspectCounts.entrySet()) {
                Multiset<String> dataSet = entry.getValue();
                List<LamiTableEntry> entries = new ArrayList<>();
                for (String element : dataSet.elementSet()) {
                    /* A row is an array of cells */
                    List<LamiData> data = Arrays.asList(new LamiString(element), new LamiLongNumber((long) dataSet.count(element)));
                    entries.add(new LamiTableEntry(data));
                }
                List<LamiTableEntryAspect> tableAspects = Arrays.asList(new LamiCategoryAspect(entry.getKey(), 0), new LamiCountAspect("count", 1));
                LamiTableClass tableClass = new LamiTableClass(entry.getKey(), entry.getKey(), tableAspects, Collections.emptySet());
                LamiResultTable lrt = new LamiResultTable(createTimeRange(tr), tableClass, entries);
                results.add(lrt);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return results;
    }

    // copied from TmfEventsEditor
    /**
     * Get the event table for the given trace. It will be of the type defined
     * by the extension point if applicable, else it will be a default table
     * with the extension-point-defined columns (if any).
     *
     * @param trace
     *            The event table is for this trace
     * @param parent
     *            The parent composite of the table
     * @param cacheSize
     *            The cache size to use
     * @return The event table for the trace
     */
    private static Iterable<ITmfEventAspect<?>> getTraceAspects(ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            return getExperimentAspects((TmfExperiment) trace);
        }
        return trace.getEventAspects();
    }

    /**
     * Get the events table for an experiment. If all traces in the experiment
     * are of the same type, use the same behavior as if it was one trace of
     * that type.
     *
     * @param experiment
     *            the experiment
     * @param parent
     *            the parent Composite
     * @param cacheSize
     *            the event table cache size
     * @return An event table of the appropriate type
     */
    private static Iterable<ITmfEventAspect<?>> getExperimentAspects(
            final TmfExperiment experiment) {
        List<ITmfTrace> traces = experiment.getTraces();
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = new ImmutableSet.Builder<>();

        /* For experiments, we'll add a "trace name" aspect/column */
        builder.add(TmfBaseAspects.getTraceNameAspect());

        String commonTraceType = getCommonTraceType(experiment);
        if (commonTraceType != null) {
            /*
             * All the traces in this experiment are of the same type, let's
             * just use the normal table for that type.
             */
            builder.addAll(traces.get(0).getEventAspects());

        } else {
            /*
             * There are different trace types in the experiment, so we are
             * definitely using a TmfEventsTable. Aggregate the columns from all
             * trace types.
             */
            for (ITmfTrace trace : traces) {
                Iterable<ITmfEventAspect<?>> traceAspects = trace.getEventAspects();
                builder.addAll(traceAspects);
            }
        }
        return builder.build();
    }

    /**
     * Check if an experiment contains traces of all the same type. If so,
     * returns this type as a String. If not, returns null.
     *
     * @param experiment
     *            The experiment
     * @return The common trace type if there is one, or 'null' if there are
     *         different types.
     */
    private static @Nullable String getCommonTraceType(TmfExperiment experiment) {
        String commonTraceType = null;
        try {
            for (final ITmfTrace trace : experiment.getTraces()) {
                final IResource resource = trace.getResource();
                if (resource == null) {
                    return null;
                }

                final String traceType = resource.getPersistentProperty(TmfCommonConstants.TRACETYPE);
                if ((commonTraceType != null) && !commonTraceType.equals(traceType)) {
                    return null;
                }
                commonTraceType = traceType;
            }
        } catch (CoreException e) {
            /*
             * One of the traces didn't advertise its type, we can't infer
             * anything.
             */
            return null;
        }
        return commonTraceType;
    }
    // end of copied from TmfEventsEditor

    /**
     * Todo, move to LAMI
     */
    private static LamiTimeRange createTimeRange(TmfTimeRange timeRange) {
        return new LamiTimeRange(new LamiTimestamp(timeRange.getStartTime().toNanos()), new LamiTimestamp(timeRange.getStartTime().toNanos()));
    }

    /**
     * Todo, move to LAMI
     */
    private final class LamiString extends LamiData {
        private final String fElement;

        private LamiString(String element) {
            fElement = element;
        }

        @Override
        public @NonNull String toString() {
            return fElement;
        }
    }

    /**
     * Count aspect, generic
     *
     * TODO: move to LAMI
     *
     * @author Matthew Khouzam
     *
     */
    private final class LamiCountAspect extends LamiGenericAspect {

        private LamiCountAspect(String name, int column) {
            super(name, null, column, true, false);
        }
    }

    /**
     * Category aspect, generic
     *
     * TODO: move to LAMI
     *
     * @author Matthew Khouzam
     *
     */
    private final class LamiCategoryAspect extends LamiGenericAspect {

        private LamiCategoryAspect(String name, int column) {
            super(name, null, column, false, false);
        }
    }

}
