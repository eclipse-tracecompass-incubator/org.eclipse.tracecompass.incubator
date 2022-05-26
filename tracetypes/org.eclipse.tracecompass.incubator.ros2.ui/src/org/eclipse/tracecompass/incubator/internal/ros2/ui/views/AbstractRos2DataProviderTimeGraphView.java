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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;

import com.google.common.collect.ImmutableSet;

/**
 * Abstract ROS 2 data provider time graph view, with some common utilities.
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRos2DataProviderTimeGraphView extends BaseDataProviderTimeGraphView {

    private static final @NonNull String ID_PREFIX = "org.eclipse.tracecompass.incubator.ros2.ui.views"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param id
     *            the view ID
     * @param pres
     *            the presentation provider
     * @param providerId
     *            the provider ID
     */
    protected AbstractRos2DataProviderTimeGraphView(String id, TimeGraphPresentationProvider pres, String providerId) {
        super(id, pres, providerId);
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        /**
         * We need to build the view using the given trace itself, not the
         * individual children traces, as opposed to the default implementation.
         * This means that, for an experiment, we build the view using the whole
         * experiment and not the individual traces that make up the experiment.
         */
        return (null != trace) ? ImmutableSet.of(trace) : ImmutableSet.of();
    }

    /**
     * Get a complete view ID from a ROS 2 view ID suffix.
     *
     * @param suffix
     *            the ID suffix
     * @return the full view ID
     */
    public static @NonNull String getViewIdFromSuffix(@NonNull String suffix) {
        return ID_PREFIX + suffix;
    }
}
