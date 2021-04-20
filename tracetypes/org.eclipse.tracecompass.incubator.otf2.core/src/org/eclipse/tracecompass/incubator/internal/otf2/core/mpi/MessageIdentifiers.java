/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.mpi;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Identifiers of a point to point MPI message
 *
 * @author Yoann Heitz
 */
public class MessageIdentifiers {

    private static final long UNKNOWN_TIMESTAMP = -1L;

    private final int fCommunicator;
    private final int fSrcRank;
    private final int fDestRank;
    private final int fMessageTag;
    private final long fBeginTimestamp;

    /**
     * @param communicator
     *            the ID of the communicator
     * @param srcRank
     *            the rank of the sender
     * @param destRank
     *            the rank of the receiver
     * @param messageTag
     *            the tag of the message
     * @param beginTimestamp
     *            the timestamp when the communication was initialized
     */
    public MessageIdentifiers(int communicator, int srcRank, int destRank, int messageTag, long beginTimestamp) {
        fCommunicator = communicator;
        fSrcRank = srcRank;
        fDestRank = destRank;
        fMessageTag = messageTag;
        fBeginTimestamp = beginTimestamp;
    }

    /**
     * This constructor should be called to identify the second part of the
     * communication (usually the reception)
     *
     * @param communicator
     *            the ID of the communicator
     * @param srcRank
     *            the rank of the sender
     * @param destRank
     *            the rank of the receiver
     * @param messageTag
     *            the tag of the message
     */
    public MessageIdentifiers(int communicator, int srcRank, int destRank, int messageTag) {
        this(communicator, srcRank, destRank, messageTag, UNKNOWN_TIMESTAMP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCommunicator, fSrcRank, fDestRank, fMessageTag, fBeginTimestamp);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        MessageIdentifiers that = (MessageIdentifiers) other;
        return (fCommunicator == that.fCommunicator) && (fSrcRank == that.fSrcRank) && (fDestRank == that.fDestRank) && (fMessageTag == that.fMessageTag);
    }

    /**
     * @return the begin timestamp for this communication
     */
    public long getBeginTimestamp() {
        return fBeginTimestamp;
    }
}