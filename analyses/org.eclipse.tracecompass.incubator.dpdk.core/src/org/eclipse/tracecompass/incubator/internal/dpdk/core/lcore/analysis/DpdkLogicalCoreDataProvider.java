/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.AbstractDpdkDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Data provider fo the logical core analysis
 *
 * @author Arnaud Fiorini
 */
public class DpdkLogicalCoreDataProvider extends AbstractDpdkDataProvider {

    /**
     * ID of the Data provider to use in plugin extensions
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.lcore.dataprovider"; //$NON-NLS-1$

    /**
     * @param trace
     *            Source trace for the analysis
     * @param analysisModule
     *            Analysis module that generate the state system
     */
    public DpdkLogicalCoreDataProvider(ITmfTrace trace, DpdkLogicalCoreAnalysisModule analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Factory method to use in the data provider factory
     *
     * @param trace
     *            Source trace for the analysis
     * @return A DpdkLogicalCoreDataProvider instance
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        DpdkLogicalCoreAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkLogicalCoreAnalysisModule.class, DpdkLogicalCoreAnalysisModule.ID);
        return module != null ? new DpdkLogicalCoreDataProvider(trace, module) : null;
    }
}
