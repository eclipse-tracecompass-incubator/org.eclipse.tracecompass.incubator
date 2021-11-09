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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.summarytimeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import static org.eclipse.tracecompass.statesystem.core.ITmfStateSystem.ROOT_ATTRIBUTE;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the OTF2 Summary Timeline analysis
 *
 * @author Yoann Heitz
 */
public class Otf2SummaryTimelineStateProvider extends AbstractOtf2StateProvider {

    private static int VERSION_NUMBER = 1;

    /*
     * The strings that are defined in the next lines and the getQuarkFromRegion
     * function will be rewritten in a future patch to handle more APIs and
     * group the percentages by the types of the function rather than by the
     * functions themselves : for example MPI_Communication and
     * MPI_Synchronization rather than MPI_Send, MPI_Receive
     */

    /*
     * Strings used to define a regex and capture specific groups in order to
     * select the correct quark depending on the region name
     */
    private static final String API_FUNCTION_PATTERN = "^(?<API>[a-zA-Z0-9]*)_(?<function>.*)"; //$NON-NLS-1$
    private static final String FUNCTION = "function"; //$NON-NLS-1$
    private static final String API = "API"; //$NON-NLS-1$

    // Default statuses
    private static final String OTHER = "other"; //$NON-NLS-1$
    private static final String MPI = "MPI"; //$NON-NLS-1$
    private static final String PTHREAD = "pthread"; //$NON-NLS-1$
    private static final String TOTAL = "total"; //$NON-NLS-1$

    /**
     * Get the correct status quark depending on the name of the function that
     * was entered. This is a temporary function that will be rewritten in a
     * future patch
     *
     * @param regionName
     *            the name of the function that was entered
     * @param ssb
     *            the StateSystem builder
     * @return the quark associated to the function
     */
    private static int getQuarkFromRegion(String regionName, ITmfStateSystemBuilder ssb) {
        Pattern functionPattern = Pattern.compile(API_FUNCTION_PATTERN);
        Matcher matcher = functionPattern.matcher(regionName);
        if (!matcher.matches()) {
            int defaultQuark = ssb.getQuarkAbsoluteAndAdd(OTHER);
            return defaultQuark;
        }

        String api = matcher.group(API);
        String function = matcher.group(FUNCTION);

        if (api.equals(MPI)) {
            int mpiQuark = ssb.getQuarkAbsoluteAndAdd(MPI);
            int functionQuark = ssb.getQuarkRelativeAndAdd(mpiQuark, function);
            return functionQuark;
        }
        if (api.equals(PTHREAD)) {
            int pthreadQuark = ssb.getQuarkAbsoluteAndAdd(PTHREAD);
            int functionQuark = ssb.getQuarkRelativeAndAdd(pthreadQuark, function);
            return functionQuark;
        }
        int applicationQuark = ssb.getQuarkAbsoluteAndAdd(OTHER);
        return applicationQuark;
    }

    private class Location {

        private final Stack<Integer> fStatusQuarkStack;

        public Location() {
            fStatusQuarkStack = new Stack<>();
        }

        public void enter(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = event.getContent();
            Integer regionId = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_REGION);
            if (regionId == null) {
                return;
            }
            long timestamp = event.getTimestamp().toNanos();
            String regionName = getRegionNameFromRegionId(regionId);
            int newTypeQuark = getQuarkFromRegion(regionName, ssb);
            if (!fStatusQuarkStack.empty()) {
                Integer currentQuark = fStatusQuarkStack.peek();
                incrementConcernedQuarks(ssb, timestamp, currentQuark, -1. / fNumberOfLocations);
            }
            incrementConcernedQuarks(ssb, timestamp, newTypeQuark, 1. / fNumberOfLocations);
            fStatusQuarkStack.push(newTypeQuark);
        }

        public void leave(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = event.getContent();
            Integer regionId = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_REGION);
            if (regionId == null) {
                return;
            }
            long timestamp = event.getTimestamp().toNanos();
            Integer currentTypeQuark = fStatusQuarkStack.pop();
            incrementConcernedQuarks(ssb, timestamp, currentTypeQuark, -1. / fNumberOfLocations);
            if (!fStatusQuarkStack.empty()) {
                Integer newTypeQuark = fStatusQuarkStack.peek();
                incrementConcernedQuarks(ssb, timestamp, newTypeQuark, 1. / fNumberOfLocations);
            }
        }

        private void incrementConcernedQuarks(ITmfStateSystemBuilder ssb, long timestamp, int quark, double increment) {
            StateSystemBuilderUtils.incrementAttributeDouble(ssb, timestamp, quark, increment);
            int parentQuark = ssb.getParentAttributeQuark(quark);
            /*
             * If the quark has a parent different than ROOT_ATTRIBUTE, then it
             * means it is associated to a specific function inside an API. The
             * quark associated to the total of locations in this API also needs
             * to be incremented
             */
            if (parentQuark != ROOT_ATTRIBUTE) {
                int totalQuark = ssb.getQuarkRelativeAndAdd(parentQuark, TOTAL);
                StateSystemBuilderUtils.incrementAttributeDouble(ssb, timestamp, totalQuark, increment);
            }
        }
    }

    private final Map<Long, Location> fMapLocation;
    private long fNumberOfLocations;

    /**
     * Constructor for this state provider
     *
     * @param trace
     *            the trace
     */
    public Otf2SummaryTimelineStateProvider(ITmfTrace trace) {
        super(trace, Otf2SummaryTimelineAnalysis.getFullAnalysisId());
        fMapLocation = new HashMap<>();
        fNumberOfLocations = 0;
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    protected void processGlobalDefinition(ITmfEvent event, String name) {
        switch (name) {
        case IOtf2GlobalDefinitions.OTF2_STRING: {
            processStringDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_REGION: {
            processRegionDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_LOCATION: {
            ITmfEventField content = event.getContent();
            Long locationReference = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
            Integer locationType = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_LOCATION_TYPE);
            if (locationReference == null || locationType == null || locationType != 1) {
                return;
            }
            fMapLocation.put(locationReference, new Location());
            fNumberOfLocations += 1;
            break;
        }
        default:
            return;
        }

    }

    @Override
    protected void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb) {
        Long locationId = getLocationId(event);
        Location location = fMapLocation.get(locationId);
        if (location == null) {
            return;
        }
        switch (name) {
        case IOtf2Events.OTF2_ENTER: {
            location.enter(event, ssb);
            break;
        }
        case IOtf2Events.OTF2_LEAVE: {
            location.leave(event, ssb);
            break;
        }
        default:
            break;
        }

    }

}
