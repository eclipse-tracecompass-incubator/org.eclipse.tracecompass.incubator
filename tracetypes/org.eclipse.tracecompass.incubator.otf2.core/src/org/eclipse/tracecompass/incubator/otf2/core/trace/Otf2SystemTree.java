/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.otf2.core.trace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.Location;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.LocationGroup;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.SystemTreeNode;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * A class that gets filled with the required information to represent the
 * system tree (how the different clusters/nodes/processes/threads are
 * distributed)
 *
 * @author Yoann Heitz
 */
public class Otf2SystemTree {

    private Map<Long, SystemTreeNode> fSystemTreeNodes = new HashMap<>();
    private Map<Long, LocationGroup> fLocationGroups = new HashMap<>();
    private Map<Long, Location> fLocations = new HashMap<>();

    /**
     * Adds a new system tree node to the system tree
     *
     * @param event
     *            the event defining the new system tree node
     */
    public void addSystemTreeNode(ITmfEvent event) {
        SystemTreeNode systemTreeNode = new SystemTreeNode(event);
        fSystemTreeNodes.put(systemTreeNode.getId(), systemTreeNode);
    }

    /**
     * Adds a new location group to the system tree
     *
     * @param event
     *            the event defining the new location group
     */
    public void addLocationGroup(ITmfEvent event) {
        LocationGroup locationGroup = new LocationGroup(event);
        fLocationGroups.put(locationGroup.getId(), locationGroup);
    }

    /**
     * Adds a new location to the system tree
     *
     * @param event
     *            the event defining the new location
     */
    public void addLocation(ITmfEvent event) {
        Location location = new Location(event);
        fLocations.put(location.getId(), location);
    }

    /**
     * Constructs the different aspects that allows resolving the name of the
     * cluster/node/process/thread on which an event occurs
     *
     * @param stringIds
     *            the map associating the string IDs to their values for this
     *            trace
     * @return the different system aspects
     */
    @SuppressWarnings("null")
    public Iterator<Otf2NodeAspect> getSystemAspects(Map<Integer, String> stringIds) {
        Otf2NodeAspect clusterAspect = new Otf2ClusterAspect();
        Otf2NodeAspect physicalNodeAspect = new Otf2PhysicalNodeAspect();
        Otf2NodeAspect processAspect = new Otf2ProcessAspect();
        Otf2NodeAspect threadAspect = new Otf2ThreadAspect();

        for (Location location : fLocations.values()) {
            Long locationId = location.getId();
            threadAspect.addNodeName(locationId, location.getName(stringIds));

            Long processId = location.getLocationGroupId();
            LocationGroup process = fLocationGroups.get(processId);
            if (process != null) {
                processAspect.addNodeName(locationId, process.getFullName(stringIds));

                SystemTreeNode physicalNode = fSystemTreeNodes.get(process.getParentId());
                if (physicalNode != null) {
                    physicalNodeAspect.addNodeName(locationId, physicalNode.getFullName(stringIds));

                    SystemTreeNode cluster = fSystemTreeNodes.get(physicalNode.getParentId());
                    if (cluster != null) {
                        clusterAspect.addNodeName(locationId, cluster.getFullName(stringIds));
                    }
                }
            }
        }
        Otf2NodeAspect[] systemAspects = { clusterAspect, physicalNodeAspect, processAspect, threadAspect };
        return Arrays.asList(systemAspects).iterator();
    }
}
