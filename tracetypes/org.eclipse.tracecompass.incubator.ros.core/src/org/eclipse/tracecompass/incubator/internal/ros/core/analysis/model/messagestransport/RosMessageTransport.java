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

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnection;

/**
 * Contains information about a specific instance of a message that was
 * transported between two nodes
 *
 * @author Christophe Bedard
 */
public class RosMessageTransport {

    private final RosConnection fConnection;
    private final long fSourceTimestamp;
    private final long fDestinationTimestamp;
    // TODO remove, debug
    private final RosMessagesTransportEventKey fKey;

    /**
     * Constructor
     *
     * @param connection
     *            the {@link RosConnection} responsible for this message
     *            transport
     * @param sourceTimestamp
     *            the timestamp of the network transport source event (e.g.
     *            net_dev_queue)
     * @param destinationTimestamp
     *            the timestamp of the network transport destination event (e.g.
     *            netif_receive_skb)
     */
    public RosMessageTransport(RosConnection connection, long sourceTimestamp, long destinationTimestamp, RosMessagesTransportEventKey key) {
        fConnection = connection;
        fSourceTimestamp = sourceTimestamp;
        fDestinationTimestamp = destinationTimestamp;
        fKey = key;
    }

    /**
     * @return the {@link RosConnection} responsible for this message transport
     */
    public RosConnection getConnection() {
        return fConnection;
    }

    /**
     * @return the timestamp of the network transport source (e.g.
     *         net_dev_queue)
     */
    public long getSourceTimestamp() {
        return fSourceTimestamp;
    }

    /**
     * @return the timestamp of the network transport destination (e.g.
     *         netif_receive_skb)
     */
    public long getDestinationTimestamp() {
        return fDestinationTimestamp;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RosMessageTransport"); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\t" + fConnection.toString()); //$NON-NLS-1$
        b.append("\tSource     : " + String.valueOf(fSourceTimestamp)); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\tDestination: " + String.valueOf(fDestinationTimestamp)); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\tkey: " + fKey.toString()); //$NON-NLS-1$
        return b.toString();
    }
}
