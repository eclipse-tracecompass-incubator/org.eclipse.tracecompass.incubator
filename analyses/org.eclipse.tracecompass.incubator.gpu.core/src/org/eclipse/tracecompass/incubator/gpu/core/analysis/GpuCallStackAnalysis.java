/*******************************************************************************
 * Copyright (c) 2024 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.analysis;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.gpu.core.handlers.ApiEventHandler;
import org.eclipse.tracecompass.incubator.gpu.core.handlers.IGpuEventHandler;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * This class defines an analysis module to generate a timeline for traces from
 * GPU runtimes. It will organize calls to the API, memory copies and GPU kernel
 * launches in separated callstacks for each CPU thread.
 */
public class GpuCallStackAnalysis extends InstrumentedCallStackAnalysis {

    private static final String ID = "org.eclipse.tracecompass.incubator.gpu.core.analysis.callstack"; //$NON-NLS-1$
    private static final String EDGES = "EDGES"; //$NON-NLS-1$
    /** Name of the root attribute */
    public static final String ROOT = "root"; //$NON-NLS-1$
    /** Name of the sub-attribute where the name of the operation is stored */
    public static final String NAME = "name"; //$NON-NLS-1$
    /** Name of the sub-attribute where the correlation id is stored */
    public static final String CORRELATION_ID = "correlation_id"; //$NON-NLS-1$
    /** Name of the sub-attribute where the operations are stored */
    public static final String HIP_OPERATION_QUEUES = "hip_operation_queues"; //$NON-NLS-1$

    @Override
    protected Collection<Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new GpuCallStackStateProvider(Objects.requireNonNull(getTrace()));
    }

    private class GpuCallStackStateProvider extends AbstractTmfStateProvider {

        private final IGpuTraceEventLayout fLayout;
        private IGpuEventHandler fApiEventHandler;

        public GpuCallStackStateProvider(ITmfTrace trace) {
            super(trace, ID);
            fLayout = ((IGpuTrace) trace).getGpuTraceEventLayout();
            fApiEventHandler = new ApiEventHandler();
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public ITmfStateProvider getNewInstance() {
            return new GpuCallStackStateProvider(getTrace());
        }

        @Override
        protected void eventHandle(ITmfEvent event) {
            ITmfStateSystemBuilder ssb = getStateSystemBuilder();
            if (ssb == null) {
                return;
            }
            if (fLayout.isApiEvent(event)) {
                fApiEventHandler.handleEvent(event, ssb, fLayout, this);
            }
        }
    }
}
