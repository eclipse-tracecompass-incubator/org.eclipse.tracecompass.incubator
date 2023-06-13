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

import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Handles GPU operations from HIP calls.
 *
 * It uses state system information from the ApiEventHandler to access the
 * operation name
 *
 * @author Arnaud Fiorini
 */
public class OperationEventHandler implements IRocmEventHandler {

    private static final String QUEUES = "Queues"; //$NON-NLS-1$
    private static final String ROCM_AGENT = "ROCm Agent "; //$NON-NLS-1$
    private static final String QUEUE = "Queue "; //$NON-NLS-1$
    private static final String UNKNOWN = "Unknown Operation"; //$NON-NLS-1$

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) {
        Long timestamp = event.getTimestamp().toNanos();
        boolean isHipOperationBegin = event.getName().equals(layout.getHipOperationBegin());
        boolean isHipOperationEnd = event.getName().equals(layout.getHipOperationEnd());
        if (isHipOperationBegin || isHipOperationEnd) {
            String operationName = ""; //$NON-NLS-1$
            if (isHipOperationBegin) {
                try {
                    operationName = getCorrespondingHipCall(event, ssb, layout);
                    if (operationName.equals("")) { //$NON-NLS-1$
                        operationName = event.getContent().getFieldValue(String.class, layout.fieldOperationName());
                    }
                } catch (AttributeNotFoundException e) {
                    Activator.getInstance().logError(e.getMessage());
                }
            }
            // Create or find call stack quark
            Integer agentId = event.getContent().getFieldValue(Integer.class, layout.fieldAgentId());
            Integer queueId = event.getContent().getFieldValue(Integer.class, layout.fieldQueueId());
            Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
            if (agentId == null || queueId == null || correlationId == null) {
                return;
            }
            int rootQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.ROOT, QUEUES);
            int agentQuark = ssb.getQuarkRelativeAndAdd(rootQuark, ROCM_AGENT + agentId.toString());
            int queueQuark = ssb.getQuarkRelativeAndAdd(agentQuark, QUEUE + queueId.toString());
            int callStackQuark = ssb.getQuarkRelativeAndAdd(queueQuark, InstrumentedCallStackAnalysis.CALL_STACK);

            // Add the operation to the queue if we are treating a begin event
            if (isHipOperationBegin) {
                int depth = 1;
                int subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                // While there is already activity on the quark
                while (!ssb.queryOngoingState(subQuark).isNull()) {
                    depth += 1;
                    subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                }
                // Register event name in the call stack
                ssb.modifyAttribute(timestamp, operationName, subQuark);
                // Set call stack depth
                ssb.modifyAttribute(timestamp, depth, callStackQuark);
                // Set correlation id
                int correlationIdQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.CORRELATION_ID);
                ssb.modifyAttribute(timestamp, correlationId, correlationIdQuark);
                // Else if we have an end event, move all operations after the
                // one we received up.
            } else {
                int depth = 1;
                int maxDepth = ssb.queryOngoingState(callStackQuark).unboxInt();
                int subQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(depth));
                try {
                    // While there is already activity on the quark, go through
                    // each level
                    while (ssb.queryOngoingState(ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.CORRELATION_ID)).unboxLong() != correlationId) {
                        depth += 1;
                        subQuark = ssb.getQuarkRelative(callStackQuark, String.valueOf(depth));
                    }

                    int previousQuark = -1;
                    while (!ssb.queryOngoingState(subQuark).isNull()) {
                        if (depth >= maxDepth) {
                            ssb.modifyAttribute(timestamp, null, subQuark);
                            ssb.modifyAttribute(timestamp, depth - 1, callStackQuark);
                            break;
                        }
                        // Getting previous quark
                        previousQuark = subQuark;
                        // Getting next level quark
                        depth += 1;
                        subQuark = ssb.getQuarkRelative(callStackQuark, String.valueOf(depth));
                        int nextCorrelationIdQuark = ssb.getQuarkRelativeAndAdd(subQuark, RocmCallStackStateProvider.CORRELATION_ID);
                        Long nextCorrelationId = ssb.queryOngoingState(nextCorrelationIdQuark).unboxLong();
                        // Move operation down 1 level (inserting null to force the creation of a new interval
                        ssb.modifyAttribute(timestamp, null, previousQuark);
                        ssb.modifyAttribute(timestamp, ssb.queryOngoingState(subQuark).unboxValue(), previousQuark);
                        ssb.modifyAttribute(timestamp, nextCorrelationId, ssb.getQuarkRelativeAndAdd(previousQuark, RocmCallStackStateProvider.CORRELATION_ID));
                    }
                } catch (AttributeNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getCorrespondingHipCall(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout) throws AttributeNotFoundException {
        Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
        if (correlationId == null) {
            return UNKNOWN;
        }
        int operationsQuark = ssb.getQuarkAbsoluteAndAdd(RocmCallStackStateProvider.HIP_OPERATION_QUEUES);
        long ts = event.getTimestamp().getValue();
        int depth = 1;
        int subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
        // While there is already activity on the quark
        while (correlationId != ssb.queryOngoingState(subQuark).unboxLong()) {
            depth += 1;
            subQuark = ssb.getQuarkRelative(operationsQuark, String.valueOf(depth));
        }
        ssb.modifyAttribute(ts, null, subQuark);
        int nameQuark = ssb.getQuarkRelative(subQuark, RocmCallStackStateProvider.NAME);
        String hipOperationName = ssb.queryOngoingState(nameQuark).unboxStr();
        ssb.modifyAttribute(ts, null, nameQuark);
        return hipOperationName;
    }
}
