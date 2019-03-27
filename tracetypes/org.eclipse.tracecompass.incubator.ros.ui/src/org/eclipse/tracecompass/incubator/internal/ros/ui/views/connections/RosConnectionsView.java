/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.connections;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections.RosConnectionsDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosView;

/**
 * View for ROS Connections
 *
 * @author Christophe Bedard
 */
public class RosConnectionsView extends AbstractRosView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "connections"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public RosConnectionsView() {
        super(ID_SUFFIX, new RosConnectionsPresentationProvider(), RosConnectionsDataProvider.getFullDataProviderId());
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
