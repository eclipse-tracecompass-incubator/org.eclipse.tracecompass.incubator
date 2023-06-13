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
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.ApiEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.IRocmEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.OperationEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * This state provider creates callstacks and apply different event handlers.
 * There are multiple types of events, each described in their respective
 * handler.
 *
 * Attribute tree:
 *
 * <pre>
 * |- OperationQueues
 * |  |- <Queue>
 * |  |  |- <Depth> -> Each depth represents one operation that has been queued,
 * |  |  |             other depths are for other queued operations.
 * |- Root
 * |  |- Processes
 * |  |  |- <Thread>
 * |  |  |  |- <API> -> Each API is a callstack showing which API call is executed.
 * |  |- Queues
 * |  |  |  |- <ROCm Agent / GPU>
 * |  |  |  |  |- <Queue> -> Each queue implemented as a callstack with corresponding
 * |  |  |  |  |             compute kernel activity.
 * </pre>
 *
 * @author Arnaud Fiorini
 */
public class RocmCallStackStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.rocm.core.stateprovider.atomic"; //$NON-NLS-1$
    /** Name of the root attribute */
    public static final String ROOT = "root"; //$NON-NLS-1$
    /** Name of the sub-attribute where the name of the operation is stored */
    public static final String NAME = "name"; //$NON-NLS-1$
    /** Name of the sub-attribute where the correlation id is stored */
    public static final String CORRELATION_ID = "correlation_id"; //$NON-NLS-1$
    /** Name of the sub-attribute where the operations are stored */
    public static final String HIP_OPERATION_QUEUES = "hip_operation_queues"; //$NON-NLS-1$

    private final RocmEventLayout fLayout;
    private IRocmEventHandler fApiEventHandler;
    private IRocmEventHandler fOperationEventHandler;

    /**
     * Constructor
     *
     * @param trace
     *            Trace to follow
     * @param layout
     *            The layout of the events
     */
    public RocmCallStackStateProvider(ITmfTrace trace, RocmEventLayout layout) {
        super(trace, ID);
        fLayout = layout;
        fApiEventHandler = new ApiEventHandler();
        fOperationEventHandler = new OperationEventHandler();
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new RocmCallStackStateProvider(getTrace(), fLayout);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        if (event.getName().equals(fLayout.getHsaOperationBegin()) || event.getName().equals(fLayout.getHsaOperationEnd())) {
            fOperationEventHandler.handleEvent(event, ssb, fLayout);
        } else if (event.getName().equals(fLayout.getHipOperationBegin()) || event.getName().equals(fLayout.getHipOperationEnd())) {
            fOperationEventHandler.handleEvent(event, ssb, fLayout);
        } else if (event.getName().startsWith(fLayout.getHipPrefix()) || event.getName().startsWith(fLayout.getHsaPrefix())) {
            fApiEventHandler.handleEvent(event, ssb, fLayout);
        }
    }
}
