/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.analysis.core.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICpuTimeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.IThreadOnCpuProvider;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;

/**
 * Listener for the {@link CompositeHostModel} that waits for new
 * modules and adds them to the appropriate model concept.
 *
 * @author Geneviève Bastien
 */
public class ModelListener implements ITmfNewAnalysisModuleListener {

    @Override
    public void moduleCreated(@Nullable IAnalysisModule module) {
        if (module instanceof ICpuTimeProvider) {
            ICpuTimeProvider provider = (ICpuTimeProvider) module;
            for (String hostId : provider.getHostIds()) {
                IHostModel model = ModelManager.getModelFor(hostId);
                if (model instanceof CompositeHostModel) {
                    ((CompositeHostModel) model).setCpuTimeProvider(provider);
                }
            }
        }

        if (module instanceof IThreadOnCpuProvider) {
            IThreadOnCpuProvider provider = (IThreadOnCpuProvider) module;
            for (String hostId : provider.getHostIds()) {
                IHostModel model = ModelManager.getModelFor(hostId);
                if (model instanceof CompositeHostModel) {
                    ((CompositeHostModel) model).setThreadOnCpuProvider(provider);
                }
            }
        }
    }

}
