/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.tasks;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosPresentationProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

import com.google.common.collect.ImmutableMap;

/**
 * ROS Tasks presentation provider
 *
 * @author Christophe Bedard
 */
public class RosTasksPresentationProvider extends AbstractRosPresentationProvider {

    /**
     * Constructor
     */
    public RosTasksPresentationProvider() {
        // Nothing to do
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        return Messages.AbstractRosPresentationProvider_TaskName;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        String nodeName = event.getEntry().getParent().getName();
        builder.put(Messages.AbstractRosPresentationProvider_NodeName, nodeName);
        return builder.build();
    }
}
