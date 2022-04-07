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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * {@link Ros2MessageFlowDataProvider} factory
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowDataProviderFactory implements IDataProviderFactory {

    private final Map<ITmfTrace, Ros2MessageFlowAnalysis> fMap = new HashMap<>();

    /**
     * Constructor, registers the module with the {@link TmfSignalManager}
     */
    public Ros2MessageFlowDataProviderFactory() {
        TmfSignalManager.register(this);
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        Ros2MessageFlowAnalysis module = fMap.remove(trace);
        if (null == module || null == module.getModel()) {
            return null;
        }
        return new Ros2MessageFlowDataProvider(trace, module);
    }

    /**
     * {@link TmfSignalHandler} for when {@link Ros2MessageFlowAnalysis} is
     * started, as the analysis is not registered with the trace, we use this to
     * know to associate a {@link Ros2MessageFlowAnalysis} to a trace.
     *
     * @param startAnalysisSignal
     *            analysis started signal
     */
    @TmfSignalHandler
    public synchronized void analysisStarted(TmfStartAnalysisSignal startAnalysisSignal) {
        IAnalysisModule module = startAnalysisSignal.getAnalysisModule();
        if (module instanceof Ros2MessageFlowAnalysis) {
            Ros2MessageFlowAnalysis analysis = (Ros2MessageFlowAnalysis) module;
            fMap.put(analysis.getTrace(), analysis);
        }
    }

    /**
     * Remove the closed trace's analysis to avoid resource leaks
     *
     * @param traceClosedSignal
     *            the TMF trace closed signal
     */
    @TmfSignalHandler
    public synchronized void traceClosed(TmfTraceClosedSignal traceClosedSignal) {
        fMap.remove(traceClosedSignal.getTrace());
    }
}
