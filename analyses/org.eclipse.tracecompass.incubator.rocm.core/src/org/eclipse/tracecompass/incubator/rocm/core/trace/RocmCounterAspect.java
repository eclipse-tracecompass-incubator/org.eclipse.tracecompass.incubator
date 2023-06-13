/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.rocm.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmStrings;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Counter aspects to make use of the Counter analysis
 *
 * @author Arnaud Fiorini
 */
public class RocmCounterAspect extends CounterAspect {

    private Integer fCounterId;

    /**
     * Rocm Counter Aspect constructor
     *
     * @param fieldName
     *            The field of the performance counter
     * @param label
     *            Label describing the performance counter
     * @param class1
     *            The Gpu group to which the counter is associated
     * @param counterId
     *            the counter id which allows to retrieve the label after
     *            initialization
     */
    public RocmCounterAspect(@NonNull String fieldName, @NonNull String label, Class<@NonNull GpuAspect> class1, Integer counterId) {
        super(fieldName, label, class1);
        fCounterId = counterId;
    }

    /**
     * Rocm Counter Aspect constructor
     *
     * @param fieldName
     *            The field of the performance counter
     * @param label
     *            Label describing the performance counter
     * @param counterId
     *            the counter id which allows to retrieve the label after
     *            initialization
     */
    public RocmCounterAspect(@NonNull String fieldName, @NonNull String label, Integer counterId) {
        super(fieldName, label);
        fCounterId = counterId;
    }

    @Override
    public boolean isCumulative() {
        return false;
    }

    @Override
    public @Nullable Number resolve(@NonNull ITmfEvent event) {
        Integer counterId = event.getContent().getFieldValue(Integer.class, RocmStrings.ID);
        if (fCounterId.equals(counterId)) {
            if (event.getName().equals(RocmStrings.METRIC_DOUBLE)) {
                return event.getContent().getFieldValue(Double.class, RocmStrings.VALUE);
            } else if (event.getName().equals(RocmStrings.METRIC_INTEGER)) {
                return event.getContent().getFieldValue(Long.class, RocmStrings.VALUE);
            }
        }
        if (event.getName().equals(RocmStrings.HIP_ACTIVITY)) {
            CounterAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(event.getTrace(),
                    CounterAnalysis.class, CounterAnalysis.ID);
            if (module == null) {
                return null;
            }
            ITmfStateSystem ss = module.getStateSystem();
            if (ss == null) {
                return null;
            }
            try {
                String gpuId = event.getContent().getFieldValue(String.class, RocmStrings.DEVICE_ID);
                int groupQuark = ss.getQuarkAbsolute(CounterAnalysis.GROUPED_COUNTER_ASPECTS_ATTRIB);
                int gpuGroupQuark = ss.getQuarkRelative(groupQuark, GpuAspect.INSTANCE.getName());
                int gpuQuark = ss.getQuarkRelative(gpuGroupQuark, gpuId);
                int counterQuark = ss.getQuarkRelative(gpuQuark, this.getName());
                Long timestampEnd = event.getContent().getFieldValue(Long.class, RocmStrings.END);
                if (timestampEnd != null) {
                    long valueBefore = ss.querySingleState(event.getTimestamp().getValue(), counterQuark).getValueLong();
                    long valueAfter = ss.querySingleState(((CtfTmfTrace) event.getTrace()).timestampCyclesToNanos(timestampEnd), counterQuark).getValueLong();
                    return valueAfter - valueBefore;
                }
            } catch (AttributeNotFoundException | StateSystemDisposedException e) {
                // Either the analysis is not available or the state system has
                // not been written yet.
                return null;
            }
        }
        return null;
    }
}
