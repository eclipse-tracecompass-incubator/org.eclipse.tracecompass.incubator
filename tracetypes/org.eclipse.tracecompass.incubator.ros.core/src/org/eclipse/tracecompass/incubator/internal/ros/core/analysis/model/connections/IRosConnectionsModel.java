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

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModel;

/**
 * Interface that represents the model for ROS connections, which are network
 * connections between publishers and subscribers
 *
 * @author Christophe Bedard
 */
public interface IRosConnectionsModel extends IRosModel {

    /**
     * Get all connections
     *
     * @return an unmodifiable view of the {@link RosConnection}s, or
     *         {@code null} if it hasn't been created
     */
    @Nullable
    Collection<RosConnection> getConnections();

    /**
     * Get a node's connections
     *
     * @param nodeName
     *            the node name
     * @return the node's {@link RosConnection}s
     */
    List<RosConnection> getNodeConnections(String nodeName);

    /**
     * Get connection corresponding to an endpoint and a topic name
     *
     * @param endpoint
     *            the connection endpoint
     * @param topicName
     *            the topic name
     * @return the corresponding {@link RosConnection}, or {@code null} if it
     *         doesn't exist
     */
    @Nullable
    RosConnection getConnection(ConnectionEndpoint endpoint, String topicName);

    /**
     * Get connection corresponding to a node and a published topic
     *
     * @param pubNodeName
     *            the publisher node name
     * @param topicName
     *            the topic name
     * @return the corresponding {@link RosConnection}, or {@code null} if it
     *         doesn't exist
     */
    @Nullable
    RosConnection getConnectionFromNodePublishingOnTopic(String pubNodeName, String topicName);

    /**
     * Get connection corresponding to a source+destination endpoints pair
     *
     * @param source
     *            the source endpoint
     * @param destination
     *            the destination endpoint
     * @return the corresponding {@link RosConnection}, or {@code null} if it
     *         doesn't exist
     */
    @Nullable
    RosConnection getConnectionBetweenEndpoints(ConnectionEndpoint source, ConnectionEndpoint destination);
}
