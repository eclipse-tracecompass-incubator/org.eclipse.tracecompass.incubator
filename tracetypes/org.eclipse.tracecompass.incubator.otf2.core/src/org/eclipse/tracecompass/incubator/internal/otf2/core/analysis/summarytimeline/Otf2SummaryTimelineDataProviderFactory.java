/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.summarytimeline;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Otf2CommunicatorsDataProvider factory
 *
 * @author Yoann Heitz
 */

public class Otf2SummaryTimelineDataProviderFactory implements IDataProviderFactory {

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        Otf2SummaryTimelineAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, Otf2SummaryTimelineAnalysis.class, Otf2SummaryTimelineAnalysis.getFullAnalysisId());
        if (module == null) {
            return null;
        }
        module.schedule();
        return new Otf2SummaryTimelineDataProvider(trace, module);
    }
}
