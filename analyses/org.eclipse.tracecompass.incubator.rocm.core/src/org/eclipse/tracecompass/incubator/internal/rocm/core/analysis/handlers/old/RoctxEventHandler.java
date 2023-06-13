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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmCallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmStrings;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * This event handler is used for ROCtx events which are custom tracepoints
 * defined by the user.
 *
 * @author Arnaud Fiorini
 */
public class RoctxEventHandler extends AbstractGpuEventHandler {

    /**
     * @param stateProvider
     *            The state provider that is using this event handler
     */
    public RoctxEventHandler(RocmCallStackStateProvider stateProvider) {
        super(stateProvider);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) throws AttributeNotFoundException {
        // Select the correct quark
        int systemQuark = ssb.getQuarkAbsoluteAndAdd(CallStackStateProvider.PROCESSES, RocmStrings.SYSTEM);
        Long threadId = event.getContent().getFieldValue(Long.class, RocmStrings.TID);
        if (threadId == null) {
            threadId = 0l;
        }
        int threadQuark = ssb.getQuarkRelativeAndAdd(systemQuark, RocmStrings.THREAD + threadId.toString());
        int apiQuark = ssb.getQuarkRelativeAndAdd(threadQuark, "RocTX"); //$NON-NLS-1$
        int callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        // Push message
        Long timestamp = event.getTimestamp().getValue();
        String message = event.getContent().getFieldValue(String.class, RocmStrings.MESSAGE);
        if (message == null || message.equals(StringUtils.EMPTY)) {
            ssb.popAttribute(timestamp, callStackQuark);
            return;
        }
        ssb.pushAttribute(timestamp, message, callStackQuark);
    }
}
