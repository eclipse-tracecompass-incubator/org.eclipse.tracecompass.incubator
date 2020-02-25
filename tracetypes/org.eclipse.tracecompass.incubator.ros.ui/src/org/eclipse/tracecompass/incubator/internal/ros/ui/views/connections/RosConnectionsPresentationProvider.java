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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections.RosConnectionsStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosPresentationProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.Messages;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;

import com.google.common.collect.ImmutableMap;

/**
 * ROS Connections presentation provider
 *
 * @author Christophe Bedard
 */
public class RosConnectionsPresentationProvider extends AbstractRosPresentationProvider {

    /**
     * Constructor
     */
    public RosConnectionsPresentationProvider() {
        // Nothing to do
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        String label = event.getLabel();
        if (label == null) {
            return Collections.emptyMap();
        }
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        String channelType = event.getEntry().getParent().getParent().getName();
        String connectionName = event.getEntry().getParent().getName();
        String nodeName = event.getEntry().getParent().getParent().getParent().getName();
        Pair<String, String> localRemoteHost = getLocalAndRemoteHosts(label);
        builder.put(Messages.AbstractRosPresentationProvider_ConnectionChannelType, channelType);
        builder.put(Messages.AbstractRosPresentationProvider_ConnectionName, connectionName);
        builder.put(Messages.AbstractRosPresentationProvider_NodeName, nodeName);
        builder.put(Messages.AbstractRosPresentationProvider_ConnectionHosts, StringUtils.EMPTY);
        builder.put(TAB + Messages.AbstractRosPresentationProvider_ConnectionHostLocal, localRemoteHost.getFirst());
        builder.put(TAB + Messages.AbstractRosPresentationProvider_ConnectionHostRemote, localRemoteHost.getSecond());
        return builder.build();
    }

    private static Pair<String, String> getLocalAndRemoteHosts(String label) {
        @NonNull String[] remoteLocal = label.split(RosConnectionsStateProvider.CONNECTION_HOST_SEPARATOR);
        return new Pair<>(remoteLocal[0], remoteLocal[1]);
    }
}
