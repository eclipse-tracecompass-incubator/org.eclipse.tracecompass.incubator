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

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.nodes;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.nodes.RosNodesDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosView;

/**
 * View for ROS Nodes
 *
 * @author Christophe Bedard
 */
public class RosNodesView extends AbstractRosView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "nodes"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public RosNodesView() {
        super(ID_SUFFIX, new RosNodesPresentationProvider(), RosNodesDataProvider.getFullDataProviderId());
        // Enable view filter dialog
        setFilterColumns(new String[] { StringUtils.EMPTY });
        setFilterLabelProvider(new RosNodesViewFilterLabelProvider());
    }

    private static class RosNodesViewFilterLabelProvider extends TreeLabelProvider {
        // Empty; only used to enable view filter dialog/action
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
