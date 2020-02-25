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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.matching.TcpEventKey;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Event matching key based on {@link TcpEventKey} with additional information
 * on the connection information
 *
 * @author Christophe Bedard
 */
public class RosMessagesTransportEventKey extends TcpEventKey {

    private static final HashFunction HF = checkNotNull(Hashing.goodFastHash(32));

    private final NetworkConnection fConnectionInfo;
    private final long fFlags;

    /**
     * Constructor
     *
     * @param sequence
     *            The sequence number of the TCP packet
     * @param ack
     *            The acknowledgement number of the TCP packet
     * @param flags
     *            The 16 bits following the acknowledgment: data offset,
     *            reserved and flags)
     * @param connectionInfo
     *            the network connection information
     */
    public RosMessagesTransportEventKey(long sequence, long ack, long flags, NetworkConnection connectionInfo) {
        super(sequence, ack, flags);
        fConnectionInfo = connectionInfo;
        // Needed for match confirmation
        fFlags = flags;
    }

    /**
     * @return the flags associated with this packet event
     */
    public long getFlags() {
        return fFlags;
    }

    /**
     * @return the network connection information
     */
    public NetworkConnection getNetworkConnectionInfo() {
        return fConnectionInfo;
    }

    @Override
    public int hashCode() {
        return HF.newHasher().putInt(super.hashCode()).putInt(fConnectionInfo.hashCode()).hash().asInt();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof RosMessagesTransportEventKey) {
            RosMessagesTransportEventKey key = (RosMessagesTransportEventKey) o;
            return super.equals(o) && key.fConnectionInfo.equals(fConnectionInfo);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RosMessagesTransportEventKey:\n"); //$NON-NLS-1$
        b.append("\t\tflags: " + fFlags); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        return b.toString();
    }
}
