/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections;

import java.util.Objects;

/**
 * Represents one side of a connection between two ROS nodes
 *
 * TODO add connection end time
 *
 * @author Christophe Bedard
 */
public class RosConnectionEndpoint {

    private final String fTopicName;
    private final String fNodeName;
    private final ConnectionEndpoint fEndpoint;
    private final long fStartTime;

    /**
     * Constructor
     *
     * @param topic
     *            the topic name
     * @param node
     *            the node name
     * @param endpoint
     *            the endpoint
     * @param time
     *            the connection start time
     */
    public RosConnectionEndpoint(String topic, String node, ConnectionEndpoint endpoint, long time) {
        fTopicName = topic;
        fNodeName = node;
        fEndpoint = endpoint;
        fStartTime = time;
    }

    /**
     * @return the topic name
     */
    public String getTopicName() {
        return fTopicName;
    }

    /**
     * @return the node name
     */
    public String getNodeName() {
        return fNodeName;
    }

    /**
     * @return the network endpoint
     */
    public ConnectionEndpoint getEndpoint() {
        return fEndpoint;
    }

    /**
     * @return the connection start time
     */
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fTopicName, fNodeName, fEndpoint, fStartTime);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("\tRosConnectionEndpoint"); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\t\ttopic     =" + fTopicName); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\t\thost:port =" + fEndpoint.toString()); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\t\tnode      =" + fNodeName); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\t\ttime      =" + fStartTime); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        return b.toString();
    }
}
