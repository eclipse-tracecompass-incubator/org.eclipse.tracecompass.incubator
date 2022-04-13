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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateSystemAnalysis;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * ROS 2 objects analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectsAnalysis extends AbstractRos2StateSystemAnalysis {

    private static final @NonNull String ID_SUFFIX = ".objects"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Ros2ObjectsAnalysis() {
        super(getFullAnalysisId());
    }

    /**
     * @return the full ID of this analysis module
     */
    public static @NonNull String getFullAnalysisId() {
        return AbstractRos2StateSystemAnalysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new Ros2ObjectsStateProvider(getTrace());
    }
}
