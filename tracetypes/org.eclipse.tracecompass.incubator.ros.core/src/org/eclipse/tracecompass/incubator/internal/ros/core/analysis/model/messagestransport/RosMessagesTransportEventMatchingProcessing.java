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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.IRosConnectionsModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnection;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Match processing unit for ROS messages transport. Validates matches according
 * to a {@link IRosConnectionsModel} and processes the information to eventually
 * provide it to the {@link RosMessagesTransportModel}
 *
 * FIXME figure out why it sometimes does not work
 *
 * @author Christophe Bedard
 */
public class RosMessagesTransportEventMatchingProcessing implements IMatchProcessingUnit {

    private final @NonNull IRosConnectionsModel fConnectionsModel;
    private int fCount = 0;
    private final Collection<RosMessageTransport> fMatches = new ArrayList<>();

    /**
     * Constructor
     *
     * @param connectionsModel
     *            the connections model, to validate match
     */
    public RosMessagesTransportEventMatchingProcessing(@NonNull IRosConnectionsModel connectionsModel) {
        fConnectionsModel = connectionsModel;
    }

    /**
     * @return the matches
     */
    public Collection<RosMessageTransport> getMatches() {
        return fMatches;
    }

    @Override
    public void init(@NonNull Collection<@NonNull ITmfTrace> fTraces) {
        // Nothing to do
    }

    @Override
    public void addMatch(@NonNull TmfEventDependency match) {
        // Not used, since we need the event key
        Activator.getInstance().logError("Event key is needed to process a match!"); //$NON-NLS-1$
    }

    @Override
    public void addMatch(@NonNull IEventMatchingKey eventKey, @NonNull TmfEventDependency match) {
        if (!(eventKey instanceof RosMessagesTransportEventKey)) {
            return;
        }

        RosMessagesTransportEventKey key = (RosMessagesTransportEventKey) eventKey;
        // Might not be an actual ROS message
        if (!isValidMessageMatch(key)) {
            return;
        }

        // Check that the connection is in the RosConnectionsModel; ignore other
        // network traffic
        @Nullable RosConnection rosConnection = getRosConnection(key);
        if (rosConnection == null) {
            return;
        }

        // Process and add to the matches with important info
        fMatches.add(new RosMessageTransport(
                rosConnection,
                match.getSource().getTimestamp().toNanos(),
                match.getDestination().getTimestamp().toNanos(),
                key));
        fCount++;
    }

    /**
     * Figure out if the match corresponds to an actual message transmission
     *
     * @param key
     *            the event key
     * @return true if it corresponds to a message, false otherwise
     */
    private static boolean isValidMessageMatch(@NonNull RosMessagesTransportEventKey key) {
        // Filter out SYN (2) and FIN (1)
        return !((key.getFlags() & 2) != 0 || (key.getFlags() & 1) != 0);
    }

    @Override
    public void matchingEnded() {
        // Nothing to do
    }

    @Override
    public int countMatches() {
        return fCount;
    }

    private @Nullable RosConnection getRosConnection(RosMessagesTransportEventKey key) {
        return fConnectionsModel.getConnectionBetweenEndpoints(
                key.getNetworkConnectionInfo().getSourceEndpoint(),
                key.getNetworkConnectionInfo().getDestinationEndpoint());
    }
}