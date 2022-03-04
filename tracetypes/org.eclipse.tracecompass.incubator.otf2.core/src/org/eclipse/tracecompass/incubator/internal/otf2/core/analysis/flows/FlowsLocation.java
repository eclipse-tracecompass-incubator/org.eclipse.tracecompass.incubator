/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.Location;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * A class representing a location for the flows analysis
 *
 * @author Yoann Heitz
 */
class FlowsLocation extends Location implements IFlowsNode {

    private int fInputQuark = UNKNOWN_ID;
    private int fOutputQuark = UNKNOWN_ID;
    private long fLatestEnteredTimestamp = 0;
    private long fInputMessageSize = 0;
    private long fOutputMessageSize = 0;

    /**
     * Constructor for this class
     *
     * @param event
     *            a GlobalDef_Location event
     */
    public FlowsLocation(ITmfEvent event) {
        super(event);
    }

    /**
     * Initializes the quarks related to this location. This method should be
     * called by the state provider after all definitions have been read
     *
     * @param ssb
     *            the state system builder
     * @param locationGroupMap
     *            a map containing the different location groups and their IDs
     * @param stringIdMap
     *            a map containing the different strings and their IDs
     */
    public void initializeQuarks(ITmfStateSystemBuilder ssb, FlowsNodeMap<Long, FlowsLocationGroup> locationGroupMap, Map<Integer, String> stringIdMap) {
        String locationName = getName(stringIdMap);
        long parentId = getLocationGroupId();
        int parentInputQuark = locationGroupMap.getNodeQuark(parentId, INPUT);
        int parentOutputQuark = locationGroupMap.getNodeQuark(parentId, OUTPUT);
        fInputQuark = ssb.getQuarkRelativeAndAdd(parentInputQuark, locationName);
        fOutputQuark = ssb.getQuarkRelativeAndAdd(parentOutputQuark, locationName);
    }

    @Override
    public int getInputQuark() {
        return fInputQuark;
    }

    @Override
    public int getOutputQuark() {
        return fOutputQuark;
    }

    /**
     * Gets the timestamp of the last Enter event for this location
     *
     * @return the timestamp of the last Enter event for this location
     */
    public long getLatestEnteredTimestamp() {
        return fLatestEnteredTimestamp;
    }

    /**
     * Sets the timestamp of the last Enter event for this location
     *
     * @param latestEnteredTimestamp
     *            the timestamp when this location entered a region the last
     *            time
     */
    public void setLatestEnteredTimestamp(long latestEnteredTimestamp) {
        fLatestEnteredTimestamp = latestEnteredTimestamp;
    }

    /**
     * Update the flow value of this location for the correct quark, depending
     * on the direction of the communication. The flow value is in
     * bytes/nanoseconds
     *
     * @param ssb
     *            the state system builder
     * @param messageLength
     *            the number of bytes that were exchanged
     * @param endTimestamp
     *            the timestamp of the end of the communication
     * @param direction
     *            the direction of the communication
     */
    private void updateFlow(ITmfStateSystemBuilder ssb, Long messageLength, long endTimestamp, String direction) {
        int correctQuark = getQuark(direction);
        if (correctQuark == UNKNOWN_ID) {
            return;
        }
        long operationDuration = endTimestamp - fLatestEnteredTimestamp;
        double flow = (double) messageLength / operationDuration;
        StateSystemBuilderUtils.incrementAttributeDouble(ssb, fLatestEnteredTimestamp, correctQuark, flow);
        StateSystemBuilderUtils.incrementAttributeDouble(ssb, endTimestamp, correctQuark, -flow);
    }

    /**
     * Updates the fOutputMessageSize field with a new value
     *
     * @param messageLength
     *            the new value for the fOutputMessageSize field
     */
    public void updateOutputMessageSize(Long messageLength) {
        fOutputMessageSize = messageLength;
    }

    /**
     * Updates the fInputMessageSize field with a new value
     *
     * @param messageLength
     *            the new value for the fInputMessageSize field
     */
    public void updateInputMessageSize(Long messageLength) {
        fInputMessageSize = messageLength;
    }

    /**
     * Method to call when an event representing a region exit for this location
     * occurs
     *
     * @param ssb
     *            the state system builder
     * @param leaveTimestamp
     *            the timestamp when this location exited a region
     */
    public void processLeave(ITmfStateSystemBuilder ssb, long leaveTimestamp) {
        if (fInputMessageSize != 0) {
            updateFlow(ssb, fInputMessageSize, leaveTimestamp, INPUT);
            fInputMessageSize = 0;
        }
        if (fOutputMessageSize != 0) {
            updateFlow(ssb, fOutputMessageSize, leaveTimestamp, OUTPUT);
            fOutputMessageSize = 0;
        }
    }
}
