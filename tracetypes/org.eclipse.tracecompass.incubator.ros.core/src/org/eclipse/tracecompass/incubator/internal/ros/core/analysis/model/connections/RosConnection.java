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
 * Represents a connection between two ROS nodes. Right now this only applies to
 * a publisher and a subscriber.
 *
 * @author Christophe Bedard
 */
public class RosConnection {

    private final RosConnectionEndpoint fPub;
    private final RosConnectionEndpoint fSub;

    /**
     * Constructor
     *
     * @param pub
     *            the publisher's connection info
     * @param sub
     *            the subscriber's connection info
     */
    public RosConnection(RosConnectionEndpoint pub, RosConnectionEndpoint sub) {
        fPub = pub;
        fSub = sub;
    }

    /**
     * @return the publisher connection
     */
    public RosConnectionEndpoint getPub() {
        return fPub;
    }

    /**
     * @return the subscriber connection
     */
    public RosConnectionEndpoint getSub() {
        return fSub;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fPub, fSub);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RosConnection)) {
            return false;
        }
        RosConnection other = (RosConnection) obj;
        return Objects.equals(fPub, other.fPub) && Objects.equals(fSub, other.fSub);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RosConnection"); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\tPUB: " + fPub.toString()); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        b.append("\tSUB: " + fSub.toString()); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        return b.toString();
    }
}
