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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.communicators;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;


import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2Analysis;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * MPI Communicators analysis
 *
 * @author Yoann Heitz
 */

public class Otf2CommunicatorsAnalysis extends AbstractOtf2Analysis {

    /** The ID suffix of this analysis module */
    public static final String ID_SUFFIX = ".communicators"; //$NON-NLS-1$


    /**
     * Constructor
     */
    public Otf2CommunicatorsAnalysis() {
        super(getFullAnalysisId());
    }

    @Override
    protected Class<?> getOtf2AnalysisStateProviderClass() {
        return Otf2CommunicatorsStateProvider.class;
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new Otf2CommunicatorsStateProvider(checkNotNull(getTrace()));
    }

    /**
     * @return the full ID of this analysis module
     */
    public static String getFullAnalysisId() {
        return AbstractOtf2Analysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

}