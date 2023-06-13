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

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old;

import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HostThreadIdentifier.KERNEL_CATEGORY;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmStrings;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Handles compute kernel events generated with ROCProfiler that are not GPU
 * activity events.
 *
 * @author Arnaud Fiorini
 */
public class HsaKernelEventHandler extends AbstractGpuEventHandler {

    /**
     * @param stateProvider
     *            The state provider that is using this event handler
     */
    public HsaKernelEventHandler(RocmCallStackStateProvider stateProvider) {
        super(stateProvider);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) throws AttributeNotFoundException {
        String kernelName = event.getContent().getFieldValue(String.class, RocmStrings.KERNEL_NAME);
        Long queueId = event.getContent().getFieldValue(Long.class, RocmStrings.QUEUE_ID);
        Long gpuId = event.getContent().getFieldValue(Long.class, RocmStrings.GPU_ID);
        if (queueId == null || gpuId == null) {
            return;
        }
        int gpuQuark = ssb.getQuarkAbsoluteAndAdd(CallStackStateProvider.PROCESSES, RocmStrings.GPU + gpuId.toString());
        int queuesQuark = ssb.getQuarkRelativeAndAdd(gpuQuark, RocmStrings.QUEUES);
        int queueQuark = ssb.getQuarkRelativeAndAdd(queuesQuark, RocmStrings.QUEUE + Long.toString(queueId));
        int callStackQuark = ssb.getQuarkRelativeAndAdd(queueQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        Long timestamp = event.getTimestamp().toNanos();
        Long timestampEnd = event.getContent().getFieldValue(Long.class, "complete_time"); //$NON-NLS-1$
        if (timestampEnd != null) {
            timestampEnd = ((CtfTmfTrace) event.getTrace()).timestampCyclesToNanos(timestampEnd);
            pushParallelActivityOnCallStack(ssb, callStackQuark, kernelName, timestamp, timestampEnd);
        }
        // Add Host Thread Identifier for dependency arrows
        HostThreadIdentifier queueHostThreadIdentifier = new HostThreadIdentifier(queueId.intValue(), KERNEL_CATEGORY.QUEUE, gpuId.intValue());
        addHostIdToStateSystemIfNotDefined(ssb, event.getTrace(), queueHostThreadIdentifier, callStackQuark);
    }
}
