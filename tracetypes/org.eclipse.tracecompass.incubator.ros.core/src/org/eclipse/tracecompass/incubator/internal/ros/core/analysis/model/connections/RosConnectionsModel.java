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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections.RosConnectionsStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Lists;

/**
 * {@link IRosConnectionsModel} implementation based on a complete connections
 * analysis state system
 *
 * TODO support service connections
 *
 * @author Christophe Bedard
 */
public class RosConnectionsModel implements IRosConnectionsModel {

    private static final String TYPE_PUB_LINK = "TransportPublisherLink"; //$NON-NLS-1$
    private static final String TYPE_SUB_LINK = "TransportSubscriberLink"; //$NON-NLS-1$

    private List<RosConnection> fConnections = null;
    private final @NonNull ITmfStateSystem fSs;
    private final @NonNull IProgressMonitor fMon;

    /**
     * Constructor
     *
     * @param stateSystem
     *            the state system
     * @param monitor
     *            the progress monitor
     */
    public RosConnectionsModel(@NonNull ITmfStateSystem stateSystem, @NonNull IProgressMonitor monitor) {
        fSs = stateSystem;
        fMon = monitor;
    }

    @Override
    public void generateModel() {
        try {
            List<RosConnectionEndpoint> connectionsInfoPub = new ArrayList<>();
            // (Remote endpoint, topic name) --> local subscriber connection
            // endpoint
            Map<Pair<ConnectionEndpoint, String>, RosConnectionEndpoint> connectionsInfoSub = new HashMap<>();

            if (fMon.isCanceled()) {
                return;
            }
            gatherConnectionInfo(fSs, connectionsInfoPub, connectionsInfoSub);
            if (fMon.isCanceled()) {
                return;
            }
            fConnections = linkConnectionInfo(connectionsInfoPub, connectionsInfoSub);

            if (connectionsInfoPub.size() != connectionsInfoSub.size()) {
                Activator.getInstance().logWarning("Number of publisher connection info and subscriber connection info do not match!"); //$NON-NLS-1$
            }
        } catch (StateSystemDisposedException e) {
            // Ignore
        }
    }

    /**
     * Go through state system and gather connection information (from each
     * side).
     *
     * @param ss
     *            the state system
     * @param connectionsInfoPub
     *            the list to put publisher connection endpoints into
     * @param connectionsInfoSub
     *            the map to put subscriber connection endpoints into
     * @throws StateSystemDisposedException
     * @throws TimeRangeException
     * @throws IndexOutOfBoundsException
     */
    private static void gatherConnectionInfo(ITmfStateSystem ss, List<RosConnectionEndpoint> connectionsInfoPub, Map<Pair<ConnectionEndpoint, String>, RosConnectionEndpoint> connectionsInfoSub)
            throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        // Gather info about all connections
        List<@NonNull Integer> nodeQuarks = ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false);
        for (@NonNull Integer nodeQuark : nodeQuarks) {
            String nodeName = ss.getAttributeName(nodeQuark);
            List<@NonNull Integer> typeQuarks = ss.getSubAttributes(nodeQuark, false);
            for (@NonNull Integer typeQuark : typeQuarks) {
                String type = ss.getAttributeName(typeQuark);
                if (!(type.equals(TYPE_PUB_LINK) || type.equals(TYPE_SUB_LINK))) {
                    // TODO support service connections
                    continue;
                }

                // Publisher of the following topics if this is a subscriber
                // link
                boolean isPubSide = type.equals(TYPE_SUB_LINK);
                List<@NonNull Integer> topicQuarks = ss.getSubAttributes(typeQuark, false);
                for (@NonNull Integer topicQuark : topicQuarks) {
                    String topicName = ss.getAttributeName(topicQuark);
                    List<@NonNull Integer> connectionQuarks = ss.getSubAttributes(topicQuark, false);
                    for (@NonNull Integer connectionQuark : connectionQuarks) {
                        Iterable<@NonNull ITmfStateInterval> connectionIntervalsIter = ss.query2D(Collections.singleton(connectionQuark), ss.getStartTime(), ss.getCurrentEndTime());
                        List<@NonNull ITmfStateInterval> connectionIntervals = Lists.newArrayList(connectionIntervalsIter);
                        // First interval should be null, then the second
                        // interval should have the host:port combo
                        if (!(connectionIntervals.size() >= 1)) {
                            continue;
                        }
                        ITmfStateInterval connectionInterval = connectionIntervals.get(1);
                        String hostsPorts = connectionInterval.getValueString();
                        // Local and remote host:port (in that order) with a
                        // separator in between
                        @NonNull String[] localRemote = hostsPorts.split(RosConnectionsStateProvider.CONNECTION_HOST_SEPARATOR);
                        String localHostPort = localRemote[0];
                        ConnectionEndpoint localEndpoint = ConnectionEndpoint.fromStringFormat(localHostPort);
                        RosConnectionEndpoint local = new RosConnectionEndpoint(topicName, nodeName, localEndpoint, connectionInterval.getStartTime());
                        if (isPubSide) {
                            connectionsInfoPub.add(local);
                        } else {
                            String remoteHostPort = localRemote[1];
                            ConnectionEndpoint remoteEndpoint = ConnectionEndpoint.fromStringFormat(remoteHostPort);
                            connectionsInfoSub.put(new Pair<>(remoteEndpoint, topicName), local);
                        }
                    }
                }
            }
        }
    }

    /**
     * Link matching {@link RosConnectionEndpoint} to make up a
     * {@link RosConnection}
     *
     * @param connectionsInfoPub
     *            the list of publisher connection endpoints
     * @param connectionsInfoSub
     *            the subscriber connection info map
     * @return the resulting list of {@link RosConnection}
     */
    private static List<RosConnection> linkConnectionInfo(List<RosConnectionEndpoint> connectionsInfoPub, Map<Pair<ConnectionEndpoint, String>, RosConnectionEndpoint> connectionsInfoSub) {
        List<RosConnection> connections = new ArrayList<>();
        for (RosConnectionEndpoint pubInfo : connectionsInfoPub) {
            // Both of the following have to match, since the same
            // Transport*Link can be used for two different pub/sub connections
            // (i.e. the same host:port can be used for two different topics):
            // - remote host:port on the subscriber's side == local host:port on
            // the publisher's side
            // - topic name on the subscriber's side == topic name on the
            // publisher's side
            RosConnectionEndpoint subInfo = connectionsInfoSub.get(new Pair<>(pubInfo.getEndpoint(), pubInfo.getTopicName()));
            if (subInfo == null) {
                Activator.getInstance().logError("No connection info match!"); //$NON-NLS-1$
                continue;
            }
            connections.add(new RosConnection(pubInfo, subInfo));
        }

        return connections;
    }

    @Override
    public @Nullable Collection<RosConnection> getConnections() {
        return Collections.unmodifiableCollection(fConnections);
    }

    @Override
    public List<RosConnection> getNodeConnections(String nodeName) {
        return fConnections.stream().filter(c -> c.getPub().getNodeName().equals(nodeName) || c.getSub().getNodeName().equals(nodeName)).collect(Collectors.toList());
    }

    @Override
    public @Nullable RosConnection getConnection(ConnectionEndpoint endpoint, String topicName) {
        List<RosConnection> connections = fConnections.stream().filter(c -> (c.getPub().getEndpoint().equals(endpoint) || c.getSub().getEndpoint().equals(endpoint)) && c.getPub().getTopicName().equals(topicName)).collect(Collectors.toList());
        // Since a host:port & topic name combo is unique, there should be at
        // most one match
        if (connections.size() > 1) {
            Activator.getInstance().logError("Got more than 1 connection matching a host:port and topic name pair!"); //$NON-NLS-1$
        }
        return (!connections.isEmpty()) ? connections.get(0) : null;
    }

    @Override
    public @Nullable RosConnection getConnectionFromNodePublishingOnTopic(String pubNodeName, String topicName) {
        List<RosConnection> pubConnections = fConnections.stream().filter(c -> c.getPub().getNodeName().equals(pubNodeName) && c.getPub().getTopicName().equals(topicName)).collect(Collectors.toList());
        // Should only be one match
        if (pubConnections.size() > 1) {
            Activator.getInstance().logError("Got more than 1 connection matching a node and published topic name pair!"); //$NON-NLS-1$
        }
        return (!pubConnections.isEmpty()) ? pubConnections.get(0) : null;
    }

    @Override
    public @Nullable RosConnection getConnectionBetweenEndpoints(ConnectionEndpoint source, ConnectionEndpoint destination) {
        List<RosConnection> connections = fConnections.stream().filter(c -> (c.getPub().getEndpoint().equals(source) && c.getSub().getEndpoint().equals(destination))).collect(Collectors.toList());
        // There should only be one or none
        if (connections.size() > 1) {
            Activator.getInstance().logError("Got more than 1 connection matching a source+destination endpoints pair!"); //$NON-NLS-1$
        }
        return (!connections.isEmpty()) ? connections.get(0) : null;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("RosConnectionsModel"); //$NON-NLS-1$
        b.append("\n"); //$NON-NLS-1$
        for (RosConnection connection : fConnections) {
            b.append("\t" + connection.toString()); //$NON-NLS-1$
        }
        return b.toString();
    }
}
