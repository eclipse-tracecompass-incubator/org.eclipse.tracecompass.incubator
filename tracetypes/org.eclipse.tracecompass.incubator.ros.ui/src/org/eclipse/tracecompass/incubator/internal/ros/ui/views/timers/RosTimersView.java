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

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.timers;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.timers.RosTimersDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosView;

/**
 * View for ROS Timers
 *
 * @author Christophe Bedard
 */
public class RosTimersView extends AbstractRosView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "timers"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public RosTimersView() {
        super(ID_SUFFIX, new RosTimersPresentationProvider(), RosTimersDataProvider.getFullDataProviderId());
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
