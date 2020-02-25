/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.IRosMessageFlowModel;
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
 * {@link RosMessageFlowDataProvider} factory
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowDataProviderFactory implements IDataProviderFactory {

    private final Map<ITmfTrace, RosMessageFlowAnalysis> fMap = new HashMap<>();

    /**
     * Constructor, registers the module with the {@link TmfSignalManager}
     */
    public RosMessageFlowDataProviderFactory() {
        TmfSignalManager.register(this);
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        RosMessageFlowAnalysis module = fMap.remove(trace);
        if (module == null) {
            return null;
        }
        IRosMessageFlowModel model = module.getModel();
        if (model == null) {
            return null;
        }
        return new RosMessageFlowDataProvider(trace, model);
    }

    /**
     * {@link TmfSignalHandler} for when {@link RosMessageFlowAnalysis} is
     * started, as the analysis is not registered with the trace, we use this to
     * know to associate a {@link RosMessageFlowAnalysis} to a trace.
     *
     * @param startAnalysisSignal
     *            analysis started signal
     */
    @TmfSignalHandler
    public synchronized void analysisStarted(TmfStartAnalysisSignal startAnalysisSignal) {
        IAnalysisModule module = startAnalysisSignal.getAnalysisModule();
        if (module instanceof RosMessageFlowAnalysis) {
            RosMessageFlowAnalysis analysis = (RosMessageFlowAnalysis) module;
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
