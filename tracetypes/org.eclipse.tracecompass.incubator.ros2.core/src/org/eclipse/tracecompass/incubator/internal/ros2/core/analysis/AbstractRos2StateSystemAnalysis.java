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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Abstract ROS 2 state system analysis, with some common utilities.
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRos2StateSystemAnalysis extends TmfStateSystemAnalysisModule {

    private static final @NonNull String ID_PREFIX = "org.eclipse.tracecompass.incubator.ros2.core.analysis"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param id
     *            the analysis module ID
     */
    protected AbstractRos2StateSystemAnalysis(String id) {
        setId(Objects.requireNonNull(id));
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        /**
         * Only execute analysis on non-children traces, i.e., for experiments,
         * only execute the analysis on the experiment trace, and not on the
         * individual traces, but if there is no experiment (no parent trace),
         * do execute the analysis on the trace.
         *
         * This allows this analysis to work with both single traces and
         * experiments, but if it's an experiment, we only want to run the
         * analysis on the experiment.
         */
        return trace.getParent() == null;
    }

    /**
     * Get a complete analysis ID from a ROS 2 analysis ID suffix.
     *
     * @param suffix
     *            the ID suffix
     * @return the full analysis ID
     */
    public static @NonNull String getAnalysisIdFromSuffix(@NonNull String suffix) {
        return ID_PREFIX + suffix;
    }
}
