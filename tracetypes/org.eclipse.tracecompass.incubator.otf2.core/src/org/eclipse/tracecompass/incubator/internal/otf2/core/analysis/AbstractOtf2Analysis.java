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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.otf2.core.trace.Otf2Trace;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * Abstract OTF2 analysis. To be extended by all OTF2 analyses.
 *
 * @author Yoann Heitz
 */
public abstract class AbstractOtf2Analysis extends TmfStateSystemAnalysisModule {

    /** The ID prefix of OTF2 analysis modules */
    public static final String ID_PREFIX = "org.eclipse.tracecompass.incubator.otf2.core.analysis"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param id
     *            the full analysis ID
     */
    protected AbstractOtf2Analysis(String id) {
        setId(Objects.requireNonNull(id));
    }

    @Override
    public @Nullable Otf2Trace getTrace() {
        return (Otf2Trace) super.getTrace();
    }

    /**
     * Get a complete analysis ID from a OTF2 analysis ID suffix
     *
     * @param suffix
     *            the analysis ID suffix
     * @return the complete analysis ID
     */
    public static String getAnalysisIdFromSuffix(String suffix) {
        return ID_PREFIX + suffix;
    }
}
