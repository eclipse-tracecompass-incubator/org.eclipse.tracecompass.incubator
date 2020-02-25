/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.RosTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * Abstract ROS analysis. To be extended by all ROS analyses.
 *
 * @author Christophe Bourque Bedard
 */
public abstract class AbstractRosAnalysis extends TmfStateSystemAnalysisModule implements IRosStateProviderInstantiator {

    /** The ID prefix of ROS analysis modules */
    public static final @NonNull String ID_PREFIX = "org.eclipse.tracecompass.incubator.ros.core.analysis"; //$NON-NLS-1$

    /** The analysis requirements */
    protected @Nullable Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    /**
     * Constructor
     *
     * @param id
     *            the full analysis ID
     */
    public AbstractRosAnalysis(String id) {
        setId(checkNotNull(id));
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return getNewRosStateProviderInstance(getRosAnalysisStateProviderClass(), getTrace());
    }

    /**
     * Get the associated ROS state provider for this analysis
     *
     * @return the ROS state provider class for this analysis
     */
    abstract protected Class<?> getRosAnalysisStateProviderClass();

    @Override
    public RosTrace getTrace() {
        return (RosTrace) super.getTrace();
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return super.getAnalysisRequirements();
    }

    /**
     * Get a complete analysis ID from a ROS analysis ID suffix
     *
     * @param suffix
     *            the analysis ID suffix
     * @return the complete analysis ID
     */
    public static @NonNull String getAnalysisIdFromSuffix(@NonNull String suffix) {
        return ID_PREFIX + suffix;
    }
}
