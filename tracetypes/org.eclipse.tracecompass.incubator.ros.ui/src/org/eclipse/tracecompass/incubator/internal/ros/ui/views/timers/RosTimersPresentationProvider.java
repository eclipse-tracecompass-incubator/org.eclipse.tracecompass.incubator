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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.ElementReferenceState;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosPresentationProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

import com.google.common.collect.ImmutableMap;

/**
 * ROS Timers presentation provider
 *
 * @author Christophe Bedard
 */
public class RosTimersPresentationProvider extends AbstractRosPresentationProvider {

    /**
     * Constructor
     */
    public RosTimersPresentationProvider() {
        // Nothing to do
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        String entryName = entry.getName();
        if (entryName.startsWith(ElementReferenceState.HEX_PREFIX)) {
            return Messages.AbstractRosPresentationProvider_TimerCallback;
        } else if (entryName.equals(AbstractRosStateProvider.TIMER_SCHEDULED)) {
            return null;
        } else if (StringUtils.isNumeric(entryName)) {
            return Messages.AbstractRosPresentationProvider_TimerIndex;
        }
        return null;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        String label = event.getLabel();
        String entryName = event.getEntry().getName();
        String nodeName = Messages.AbstractRosPresentationProvider_Unknown;
        if (entryName.startsWith(ElementReferenceState.HEX_PREFIX)) {
            nodeName = event.getEntry().getParent().getName();
            builder.put(Messages.AbstractRosPresentationProvider_TimerPeriod, label);
        } else if (entryName.equals(AbstractRosStateProvider.TIMER_SCHEDULED)) {
            nodeName = event.getEntry().getParent().getParent().getName();
            builder.put(Messages.AbstractRosPresentationProvider_TimerCount, label);
        } else if (StringUtils.isNumeric(entryName)) {
            nodeName = event.getEntry().getParent().getParent().getParent().getName();
            builder.put(Messages.AbstractRosPresentationProvider_TimerQueueReference, label);
        }
        builder.put(Messages.AbstractRosPresentationProvider_NodeName, nodeName);
        return builder.build();
    }
}
