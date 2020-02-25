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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModel;

/**
 * Interface that represents the model for ROS messages transport, with info on
 * message exchanges between nodes
 *
 * @author Christophe Bedard
 */
public interface IRosMessagesTransportModel extends IRosModel {

    /**
     * Get the next message transport corresponding to a node publishing on a
     * topic after a timestamp
     *
     * @param timestamp
     *            the timestamp after which to find the transport
     * @param node
     *            the node name (publisher)
     * @param topicName
     *            the topic name
     * @return the next message transport, or {@code null} if it doesn't exist
     */
    @Nullable
    RosMessageTransport getNextMessageTransport(long timestamp, String node, String topicName);

    /**
     * @return an unmodifiable view of the {@link RosMessageTransport} instances
     *         the model has, or {@code null} if the model has not been
     *         generated yet
     */
    @Nullable
    Collection<RosMessageTransport> getMessageTransports();
}
