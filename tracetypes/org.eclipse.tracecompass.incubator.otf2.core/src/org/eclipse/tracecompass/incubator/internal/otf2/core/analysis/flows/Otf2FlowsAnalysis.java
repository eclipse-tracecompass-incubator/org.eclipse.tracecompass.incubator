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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2Analysis;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * MPI messages flow analysis
 *
 * @author Yoann Heitz
 */
public class Otf2FlowsAnalysis extends AbstractOtf2Analysis {

    /** The ID suffix of this analysis module */
    public static final String ID_SUFFIX = ".flows"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Otf2FlowsAnalysis() {
        super(getFullAnalysisId());
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new Otf2FlowsStateProvider(checkNotNull(getTrace()));
    }

    /**
     * Returns the ID of this analysis module
     *
     * @return the full ID of this analysis module
     */
    public static String getFullAnalysisId() {
        return AbstractOtf2Analysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }
}
