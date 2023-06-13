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

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

import com.google.common.collect.ImmutableSet;

/**
 * This analysis is used to store metadata of ROCm traces in the state system.
 * For now it is only used to store mappings of function names and their
 * corresponding cids.
 *
 * @author Arnaud Fiorini
 */
public class RocmMetadataAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.rocm.core.analysis.functionname"; //$NON-NLS-1$

    private Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements;

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new RocmMetadataStateProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    public Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        Set<@NonNull TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
        if (requirements == null) {
            /* Initialize the requirements for the analysis */
            requirements = ImmutableSet.of(new TmfAnalysisEventRequirement(
                    ImmutableSet.of(RocmStrings.HIP_FUNCTION_NAME, RocmStrings.HSA_FUNCTION_NAME),
                    PriorityLevel.AT_LEAST_ONE));
            fAnalysisRequirements = requirements;
        }
        return requirements;
    }
}
