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

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.connections;

import org.eclipse.jface.action.IToolBarManager;
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

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        super.fillLocalToolBar(manager);
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
