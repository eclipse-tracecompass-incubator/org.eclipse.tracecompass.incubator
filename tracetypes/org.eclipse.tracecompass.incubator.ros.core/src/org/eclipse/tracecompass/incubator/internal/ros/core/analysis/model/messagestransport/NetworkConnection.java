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

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.ConnectionEndpoint;

/**
 * Represents a network connection, with source and destination
 * {@link ConnectionEndpoint}s
 *
 * @author Christophe Bedard
 */
public class NetworkConnection {

    private final ConnectionEndpoint fSourceEndpoint;
    private final ConnectionEndpoint fDestinationEndpoint;

    /**
     * Constructor
     *
     * @param source
     *            the source endpoint
     * @param destination
     *            the destination endpoint
     */
    public NetworkConnection(ConnectionEndpoint source, ConnectionEndpoint destination) {
        fSourceEndpoint = source;
        fDestinationEndpoint = destination;
    }

    /**
     * @return the source endpoint
     */
    public ConnectionEndpoint getSourceEndpoint() {
        return fSourceEndpoint;
    }

    /**
     * @return the destination endpoint
     */
    public ConnectionEndpoint getDestinationEndpoint() {
        return fDestinationEndpoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fDestinationEndpoint, fSourceEndpoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetworkConnection)) {
            return false;
        }
        NetworkConnection other = (NetworkConnection) obj;
        return Objects.equals(fDestinationEndpoint, other.fDestinationEndpoint)
                && Objects.equals(fSourceEndpoint, other.fSourceEndpoint);
    }
}
