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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS Connections analysis
 *
 * @author Christophe Bedard
 */
public class RosConnectionsStateProvider extends AbstractRosStateProvider {

    private static int VERSION_NUMBER = 0;

    /** Separator between localhostport and remotehostport */
    public static @NonNull String CONNECTION_HOST_SEPARATOR = "/"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public RosConnectionsStateProvider(ITmfTrace trace) {
        super(trace, RosConnectionsAnalysis.getFullAnalysisId());
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        super.eventHandle(event);

        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        // new_connection
        if (isEvent(event, fLayout.eventNewConnection())) {
            long timestamp = event.getTimestamp().toNanos();
            String nodeName = getNodeName(event);
            String channelType = (String) getField(event, fLayout.fieldChannelType());
            String name = (String) getField(event, fLayout.fieldName());
            String localHostPort = (String) getField(event, fLayout.fieldLocalHostport());
            String remoteHostPort = (String) getField(event, fLayout.fieldRemoteHostport());
            String localRemoteHosts = getLocalAndRemoteCombo(localHostPort, remoteHostPort);

            // Increment number of remote connections to local host port
            int typeQuark = ss.getQuarkAbsoluteAndAdd(nodeName, channelType);
            Object typeCounterObject = ss.queryOngoing(typeQuark);
            int typeCounter = (typeCounterObject != null) ? ((Integer) typeCounterObject) + 1 : 1;
            ss.modifyAttribute(timestamp, typeCounter, typeQuark);

            // Push remote and local
            int connectionQuark = ss.getQuarkRelativeAndAdd(typeQuark, name);
            ss.pushAttribute(timestamp, localRemoteHosts, connectionQuark);

            return;
        }
    }

    private static String getLocalAndRemoteCombo(String local, String remote) {
        return local + CONNECTION_HOST_SEPARATOR + remote;
    }
}
