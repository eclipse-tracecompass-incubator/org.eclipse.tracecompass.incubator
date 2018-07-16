/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.tasks;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.tasks.RosTasksDataProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;

/**
 * View for ROS Tasks
 *
 * @author Christophe Bedard
 */
public class RosTasksView extends BaseDataProviderTimeGraphView {

    /** View ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.ros.ui.views.tasks"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public RosTasksView() {
        super(ID, new RosTasksPresentationProvider(), RosTasksDataProvider.getFullDataProviderId());
    }

    /**
     * Extendable constructor
     *
     * @param id
     *            the view ID
     * @param pres
     *            the presentation provider
     * @param dpId
     *            the dataprovider ID
     */
    public RosTasksView(String id, TimeGraphPresentationProvider pres, String dpId) {
        super(id, pres, dpId);
    }
}
