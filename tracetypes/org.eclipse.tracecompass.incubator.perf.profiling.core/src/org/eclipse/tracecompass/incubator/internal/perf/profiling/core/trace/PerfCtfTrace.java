/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.trace;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.perf.profiling.core.Activator;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.trace.layout.PerfEventLayout;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.event.aspect.CtfChannelAspect;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableList;

/**
 * A perf trace converted to CTF using the "perf data convert --to-ctf" command
 * line
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class PerfCtfTrace extends CtfTmfTrace implements IKernelTrace {

    private static final Collection<@NonNull ITmfEventAspect<?>> PERF_CTF_ASPECTS = ImmutableList.of(
            TmfBaseAspects.getTimestampAspect(),
            new CtfChannelAspect(),
            new PerfCpuAspect(),
            TmfBaseAspects.getEventTypeAspect(),
            TmfBaseAspects.getContentsAspect());

    /**
     * CTF metadata should mention the tracer as perf, so confidence is pretty high
     */
    private static final int CONFIDENCE = 101;
    private static final String PERF_DOMAIN = "\"perf\""; //$NON-NLS-1$

    /**
     * Constructor
     */
    public PerfCtfTrace() {
        super();
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return PERF_CTF_ASPECTS;
    }

    @Override
    public @Nullable IStatus validate(final @Nullable IProject project, final @Nullable String path) {
        IStatus status = super.validate(project, path);
        // The trace is a kernel trace, let's see if it's also a perf trace, otherwise, return an error for this type
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = Objects.requireNonNull(((CtfTraceValidationStatus) status).getEnvironment());
            /* Make sure the domain is "kernel" in the trace's env vars */
            String domain = environment.get("tracer_name"); //$NON-NLS-1$
            if (!PERF_DOMAIN.equals(domain)) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "This trace is not a perf trace"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public IKernelAnalysisEventLayout getKernelEventLayout() {
        return PerfEventLayout.getInstance();
    }

//    FIXME: Does not work yet, it causes errors in the state systems since the events at 0
//    @Override
//    protected synchronized void updateAttributes(@Nullable ITmfContext context, ITmfEvent event) {
//        super.updateAttributes(context, event);
//        // Do not update start and end time if timestamp of event is 0
//        ITmfTimestamp timestamp = event.getTimestamp();
//        if (timestamp.getValue() == 0 && (getStartTime().getValue() == 0)) {
//            setStartTime(TmfTimestamp.BIG_BANG);
//            setEndTime(TmfTimestamp.BIG_CRUNCH);
//        }
//    }

}
