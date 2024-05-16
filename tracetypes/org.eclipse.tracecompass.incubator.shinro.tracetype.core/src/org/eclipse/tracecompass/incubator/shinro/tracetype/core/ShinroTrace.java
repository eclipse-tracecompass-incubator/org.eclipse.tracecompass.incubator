/*******************************************************************************
 * Copyright (c) 2024 MIPS Tech LLC
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.shinro.tracetype.core;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.counters.core.aspects.ITmfCounterAspect;
import org.eclipse.tracecompass.incubator.internal.shinro.tracetype.core.Activator;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoBinaryAspect;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoFunctionAspect;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoSourceAspect;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableSet;

/**
 * Class to contain MIPS Shinro traces
 *
 */
public class ShinroTrace extends CtfTmfTrace {

    private static final int CONFIDENCE = 100;

    private static final @NonNull Collection<ITmfEventAspect<?>> SHINRO_ASPECTS;

    static {
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.addAll(CtfTmfTrace.CTF_ASPECTS);
        // It's not clear yet whether the following 3 aspects will work for us.  We might need to
        // create distinct but analogous classes
        builder.add(UstDebugInfoBinaryAspect.INSTANCE);
        builder.add(UstDebugInfoFunctionAspect.INSTANCE);
        builder.add(UstDebugInfoSourceAspect.INSTANCE);

        SHINRO_ASPECTS = builder.build();
    }

    /** Default collections of aspects */
    private @NonNull Collection<ITmfEventAspect<?>> fShinroTraceAspects = ImmutableSet.copyOf(SHINRO_ASPECTS);


    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.addAll(SHINRO_ASPECTS);
        builder.addAll(createCounterAspects(this));
        fShinroTraceAspects = builder.build();
    }

    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "bare" in the trace's env vars */
            String domain = environment.get("domain"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"bare\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.ShinroTrace_DomainError);
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    Collection<ITmfCounterAspect> createCounterAspects(ITmfTraceWithPreDefinedEvents trace) {
        ImmutableSet.Builder<ITmfCounterAspect> perfBuilder = new ImmutableSet.Builder<>();
        // This next if statement is dead code, just here to avoid unused parameter error
        if (trace == null) {

        }
        /*  For reference, this is from LttngUstTrace, we might need something analogous:
        for (ITmfEventType eventType : trace.getContainedEventTypes()) {
            for (String fieldName : eventType.getFieldNames()) {
                if (fieldName != null) {
                    if (CONTEXT_PERF_CPU.matcher(fieldName).matches()) {
                        perfBuilder.add(new CounterAspect(fieldName, fieldName.substring(CONTEXT_PERF_PREFIX.length()), TmfCpuAspect.class));
                    } else if (CONTEXT_PERF_THREAD.matcher(fieldName).matches()) {
                        perfBuilder.add(new CounterAspect(fieldName, fieldName.substring(CONTEXT_PERF_PREFIX.length()), LinuxTidAspect.class));
                    } else if (CONTEXT_PERF_UNKNOWN.matcher(fieldName).matches()) {
                        perfBuilder.add(new CounterAspect(fieldName, fieldName.substring(CONTEXT_PERF_PREFIX.length())));
                    }
                }
            }
        }
        */
        return perfBuilder.build();
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fShinroTraceAspects;
    }

}
