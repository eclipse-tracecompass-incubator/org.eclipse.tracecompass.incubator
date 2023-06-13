/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers;

import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.incubator.rocm.core.trace.RocmTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Handler for All APIs (HSA and HIP).
 *
 * This handler handles all API calls that are written to call stacks and are
 * also used to provide other information for the GPU operations.
 *
 * @author Arnaud Fiorini
 */
public class ApiEventHandler implements IRocmEventHandler {

    private static final String HIP = "HIP"; //$NON-NLS-1$
    private static final String HSA = "HSA"; //$NON-NLS-1$

    private boolean fIsThreadIdProvidedHSA = false;
    private boolean fIsThreadIdProvidedHIP = false;

    private static void provideThreadId(ITmfEvent event, ITmfStateSystemBuilder ssb, int quark, RocmEventLayout layout) {
        Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
        ssb.modifyAttribute(event.getTimestamp().getValue(), tid, quark);
    }

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
        if (tid == null) {
            return;
        }
        int rootQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.ROOT, CallStackStateProvider.PROCESSES);
        int processQuark = ssb.getQuarkRelativeAndAdd(rootQuark, tid.toString());
        addEventToOperationQueue(event, ssb, layout);
        boolean isEndEvent = false;

        int callStackQuark = ITmfStateSystem.INVALID_ATTRIBUTE;
        if (event.getName().startsWith(layout.getHipPrefix())) {
            int apiQuark = ssb.getQuarkRelativeAndAdd(processQuark, HIP);
            callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            isEndEvent = event.getName().endsWith(layout.getHipEndSuffix());
            if (!fIsThreadIdProvidedHIP) {
                provideThreadId(event, ssb, processQuark, layout);
                provideThreadId(event, ssb, apiQuark, layout);
                fIsThreadIdProvidedHIP = true;
            }
        } else if (event.getName().startsWith(layout.getHsaPrefix())) {
            if (event.getName().equals(layout.getHsaHandleType())) {
                return;
            }
            int apiQuark = ssb.getQuarkRelativeAndAdd(processQuark, HSA);
            callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            isEndEvent = event.getName().endsWith(layout.getHsaEndSuffix());
            if (!fIsThreadIdProvidedHSA) {
                provideThreadId(event, ssb, processQuark, layout);
                provideThreadId(event, ssb, apiQuark, layout);
                fIsThreadIdProvidedHSA = true;
            }
        }
        if (isEndEvent) {
            ssb.popAttribute(event.getTimestamp().getValue(), callStackQuark);
            return;
        }
        // Trimming the begin out of the event name
        String eventName = event.getName().startsWith(layout.getHipPrefix()) ? event.getName().substring(0, event.getName().length() - layout.getHipBeginSuffix().length())
                : event.getName().substring(0, event.getName().length() - layout.getHsaBeginSuffix().length());
        ssb.pushAttribute(event.getTimestamp().getValue(), eventName, callStackQuark);
    }

    private static void addEventToOperationQueue(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
        if (correlationId == null) {
            return;
        }
        int operationsQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.HIP_OPERATION_QUEUES);
        long ts = event.getTimestamp().getValue();

        if (layout.isMemcpyBegin(event.getName()) || (event.getName().equals(layout.hipLaunchKernelBegin()) && ((RocmTrace) event.getTrace()).isContainingKernelGpuActivity())
                || event.getName().equals(layout.hipStreamSynchronizeBegin())) {
            int depth = 1;
            int subQuark = ssb.getQuarkRelativeAndAdd(operationsQuark, String.valueOf(depth));
            // While there is already activity on the quark
            while (!ssb.queryOngoingState(subQuark).isNull()) {
                depth += 1;
                subQuark = ssb.getQuarkRelativeAndAdd(operationsQuark, String.valueOf(depth));
            }
            // Register event name in the call stack
            ssb.modifyAttribute(ts, correlationId, subQuark);
            int nameQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.NAME);
            if (layout.isMemcpyBegin(event.getName())) {
                ssb.modifyAttribute(ts, event.getContent().getFieldValue(String.class, layout.fieldMemcpyKind()), nameQuark);
            } else if (event.getName().equals(layout.hipLaunchKernelBegin())) {
                ssb.modifyAttribute(ts, event.getContent().getFieldValue(String.class, layout.fieldKernelName()), nameQuark);
            } else if (event.getName().equals(layout.hipStreamSynchronizeBegin())) {
                ssb.modifyAttribute(ts, event.getName().substring(0, event.getName().length() - layout.getHipBeginSuffix().length()), nameQuark);
            }
        }
        if (event.getName().equals(layout.hipStreamSynchronizeEnd())) {
            int depth = 1;
            int subQuark;
            try {
                subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
                // While there is already activity on the quark
                while (correlationId != ssb.queryOngoingState(subQuark).unboxLong()) {
                    depth += 1;
                    subQuark = ssb.optQuarkRelative(operationsQuark, String.valueOf(depth));
                    if (subQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        return;
                    }
                }
                ssb.modifyAttribute(ts, null, subQuark);
                int nameQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.NAME);
                ssb.modifyAttribute(ts, null, nameQuark);
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
