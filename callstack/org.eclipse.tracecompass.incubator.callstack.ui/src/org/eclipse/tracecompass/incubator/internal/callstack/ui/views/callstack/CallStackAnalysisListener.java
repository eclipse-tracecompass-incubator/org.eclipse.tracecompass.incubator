/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.callstack;

import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackProvider;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.IEventCallStackProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph.FlameGraphView;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.views.stacktable.CallStackTableView;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;
import org.eclipse.tracecompass.tmf.ui.analysis.TmfAnalysisViewOutput;

/**
 * Registers the {@link CallStackView} to {@link ICallStackProvider}. The
 * analysis being an abstract class, it is not possible to use the output
 * extension to add the view, but the listener fixes the issue.
 *
 * @author Geneviève Bastien
 */
public class CallStackAnalysisListener implements ITmfNewAnalysisModuleListener {

    @Override
    public void moduleCreated(IAnalysisModule module) {
        if (module instanceof ICallStackProvider) {
            module.registerOutput(new TmfAnalysisViewOutput(CallStackView.ID, module.getId()));
        }
        if (module instanceof ICallGraphProvider) {
            module.registerOutput(new TmfAnalysisViewOutput(FlameGraphView.ID, module.getId()));
        }
        if (module instanceof IEventCallStackProvider) {
            module.registerOutput(new TmfAnalysisViewOutput(CallStackTableView.ID, module.getId()));
        }
    }

}
