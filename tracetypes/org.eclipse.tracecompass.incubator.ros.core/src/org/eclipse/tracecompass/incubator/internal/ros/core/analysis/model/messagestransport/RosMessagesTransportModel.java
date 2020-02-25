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
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.IRosConnectionsModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnection;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnectionsModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.IRosMessageFlowModel;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * {@link IRosMessageFlowModel} implementation. Uses network event matching
 * along with a {@link RosConnectionsModel}.
 *
 * @author Christophe Bedard
 */
public class RosMessagesTransportModel implements IRosMessagesTransportModel {

    private static RosMessagesTransportEventMatching sfMatching = null;

    private final @NonNull IRosConnectionsModel fConnectionsModel;
    private final RosMessagesTransportEventMatchingProcessing fProcessingUnit;
    private final TmfEventMatching fMatching;
    private Collection<RosMessageTransport> fMessageTransports = null;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param connectionsModel
     *            the connections model
     */
    public RosMessagesTransportModel(ITmfTrace trace, @NonNull IRosConnectionsModel connectionsModel) {
        fConnectionsModel = connectionsModel;
        fProcessingUnit = new RosMessagesTransportEventMatchingProcessing(fConnectionsModel);
        fMatching = new TmfEventMatching(Collections.singleton(Objects.requireNonNull(trace)), fProcessingUnit);
    }

    @Override
    public void generateModel() {
        // TODO move to somewhere more appropriate?
        if (sfMatching == null) {
            sfMatching = new RosMessagesTransportEventMatching();
            TmfEventMatching.registerMatchObject(sfMatching);
        }

        fMatching.matchEvents();
        fMessageTransports = fProcessingUnit.getMatches();
    }

    @Override
    public @Nullable RosMessageTransport getNextMessageTransport(long timestamp, String node, String topicName) {
        if (fMessageTransports == null) {
            return null;
        }

        // Get connection
        RosConnection connection = fConnectionsModel.getConnectionFromNodePublishingOnTopic(node, topicName);
        if (connection == null) {
            Activator.getInstance().logError("Could not find connection!"); //$NON-NLS-1$
            return null;
        }

        // Get closest (timewise) transport for that connection
        long matchDiff = Long.MAX_VALUE;
        RosMessageTransport match = null;
        for (RosMessageTransport transport : fMessageTransports) {
            if (transport.getConnection().equals(connection)) {
                long diff = transport.getSourceTimestamp() - timestamp;
                if (diff < matchDiff) {
                    matchDiff = diff;
                    match = transport;
                }
            }
        }

        if (match == null) {
            Activator.getInstance().logError("Could not find closest transport!"); //$NON-NLS-1$
        }

        return match;
    }

    @Override
    public @Nullable Collection<RosMessageTransport> getMessageTransports() {
        return Collections.unmodifiableCollection(fMessageTransports);
    }
}
