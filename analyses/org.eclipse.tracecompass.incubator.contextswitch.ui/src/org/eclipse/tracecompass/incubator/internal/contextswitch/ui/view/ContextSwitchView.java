/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexis Cabana-Loriaux - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.contextswitch.ui.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.contextswitch.KernelContextSwitchAnalysis;
import org.eclipse.tracecompass.incubator.internal.contextswitch.ui.view.ContextSwitchTimeEvent.ContextSwitchRate;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * This class represents the Controller and the View parts of the Context Switch
 * View.
 *
 * @author Alexis Cabana-Loriaux
 */
public class ContextSwitchView extends AbstractTimeGraphView {

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /** ID string */
    public static final String ID = "org.eclipse.tracecompass.incubator.contextswitch.ui.view"; //$NON-NLS-1$

    private static final long BUILD_UPDATE_TIMEOUT = 500L;

    private final Comparator<ITimeGraphEntry> fAscendingTimeGraphEntryComparator = new Comparator<ITimeGraphEntry>() {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            int left = ((ContextSwitchEntry) o1).getId();
            int right = ((ContextSwitchEntry) o2).getId();
            return Integer.compare(left, right);
        }
    };

    /*
     * Factors used to determine the classification of the events. For example,
     * for an event to be classified as LOW, it must be 50% or less the value of
     * the mean of this group.
     */
    private static enum ClassificationFactors {
        LOW(0.5f), MODERATE(1f), HIGH(1.5f), CRITICAL(2f);

        Float fFactor;

        ClassificationFactors(Float f) {
            fFactor = f;
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     *
     * @param id
     *            the ID of this view
     * @param pres
     *            the presentation provider
     */
    public ContextSwitchView(String id, TimeGraphPresentationProvider pres) {
        super(id, pres);
    }

    /**
     * Default constructor
     */
    public ContextSwitchView() {
        super(ID, new ContextSwitchPresentationProvider());
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, KernelContextSwitchAnalysis.ID);
        if (ssq == null) {
            return;
        }

        Map<Integer, TimeGraphEntry> entryMap = new HashMap<>();
        TimeGraphEntry traceEntry = null;

        long startTime = ssq.getStartTime();
        long start = startTime;
        setStartTime(Math.min(getStartTime(), startTime));
        boolean complete = false;
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }
            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
            long end = ssq.getCurrentEndTime();
            if (start == end && !complete) { // when complete execute one last
                                             // time regardless of end time
                continue;
            }
            long endTime = end + 1;
            setEndTime(Math.max(getEndTime(), endTime));

            if (traceEntry == null) {
                traceEntry = new ContextSwitchEntry(trace, startTime, endTime);
                List<TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
                addToEntryList(parentTrace, entryList);
            } else {
                traceEntry.updateEndTime(endTime);
            }

            List<Integer> cpuQuarks = ssq.getQuarks("CPUs", "*"); //$NON-NLS-1$ //$NON-NLS-2$
            for (Integer cpuQuark : cpuQuarks) {
                TimeGraphEntry entry = entryMap.get(cpuQuark);
                if (entry == null) {
                    String cpuId = ssq.getAttributeName(cpuQuark);
                    entry = new ContextSwitchEntry(trace, Messages.ContextSwitchPresentationProvider_CPU + cpuId, startTime, endTime, Integer.parseInt(cpuId));
                    entryMap.put(cpuQuark, entry);
                    traceEntry.addChild(entry);
                } else {
                    entry.updateEndTime(endTime);
                }
            }

            traceEntry.sortChildren(fAscendingTimeGraphEntryComparator);

            if (parentTrace.equals(getTrace())) {
                refresh();
            }
            long resolution = Math.max(1, (endTime - ssq.getStartTime()) / getDisplayWidth());
            for (ITimeGraphEntry child : traceEntry.getChildren()) {
                if (monitor.isCanceled()) {
                    return;
                }
                if (child instanceof TimeGraphEntry) {
                    populateTimeGraphEntry(monitor, start, endTime, resolution, (TimeGraphEntry) child);
                }
            }
            start = end;
        }
    }

    /**
     * Populate a single time TimeGraphEntry with the relevant events within the
     * given range.
     *
     * @param monitor
     *            the progress monitor
     * @param start
     *            the start time of the TimeEvent
     * @param endTime
     *            the end time of the TimeEvent
     * @param resolution
     *            the resolution of the request
     * @param entry
     *            the entry to populate
     */
    private void populateTimeGraphEntry(@NonNull IProgressMonitor monitor, long start, long endTime, long resolution, @NonNull TimeGraphEntry entry) {
        List<ITimeEvent> eventList = getEventList(entry, start, endTime, resolution, monitor);
        if (eventList != null) {
            for (ITimeEvent event : eventList) {
                entry.addEvent(event);
            }
        }
        redraw();
    }

    /**
     * Build the list of all the states in which the CPU(s) will be inside the
     * timerange passed as an argument
     */
    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        /* Length of the intervals queried */
        long bucketLength = 20 * resolution;

        /* Get the information about the nb of cpu */
        if (!(entry instanceof ContextSwitchEntry)) {
            /* Is a parent */
            return null;
        }
        ContextSwitchEntry contextSwitchEntry = (ContextSwitchEntry) entry;
        ITmfStateSystem sS = TmfStateSystemAnalysisModule.getStateSystem(contextSwitchEntry.getTrace(), KernelContextSwitchAnalysis.ID);
        if (sS == null) {
            return null;
        }
        int nbCPU = sS.getQuarks("CPUs", "*").size(); //$NON-NLS-1$ //$NON-NLS-2$
        if (nbCPU == 0) {
            /* Can't get any information on the CPUs state */
            return null;
        }

        /* Make sure the times are correct */
        final long realStart = Math.max(startTime, sS.getStartTime());
        final long realEnd = Math.min(endTime, sS.getCurrentEndTime());
        if (realEnd <= realStart) {
            return null;
        }

        /* Retrieve analysis module */
        KernelContextSwitchAnalysis kernelContextSwitchSS = TmfTraceUtils.getAnalysisModuleOfClass(contextSwitchEntry.getTrace(), KernelContextSwitchAnalysis.class, KernelContextSwitchAnalysis.ID);
        if (kernelContextSwitchSS == null) {
            return null;
        }

        /*
         * Get the total number of context switch in the width of the view. If
         * the map returned is empty, no context switch can be retrieved for
         * this interval.
         */
        Map<Integer, Long> cs = kernelContextSwitchSS.getContextSwitchesRange(realStart, realEnd);
        if (cs.isEmpty()) {
            return null;
        }

        /*
         * Get the total nb of sched_switch events between startTime and
         * endTime, and divide by the number of cpu and the time to get the mean
         * number of sched_switch per bucket, per cpu, for better classification
         */
        Long totalCxtSwtInRange = cs.get(KernelContextSwitchAnalysis.TOTAL);
        if (totalCxtSwtInRange == null) {
            throw new IllegalStateException("A non-empty map of context switches should at least contain the total number (0)"); //$NON-NLS-1$
        }

        if (!contextSwitchEntry.hasId()) {
            long deltat = realEnd - realStart;
            double deltaPerCPU = totalCxtSwtInRange;
            deltaPerCPU /= nbCPU;

            contextSwitchEntry.setMean((int) ((deltaPerCPU * bucketLength) / deltat));
            return Collections.emptyList();
        }
        List<ITimeEvent> eventList = null;
        eventList = new ArrayList<>();
        long queryStart = realStart;
        long queryEnd = queryStart + bucketLength;

        /* Cover 100% of the width */
        while (queryStart <= realEnd) {
            if (monitor.isCanceled()) {
                return null;
            }
            Map<Integer, Long> map = kernelContextSwitchSS.getContextSwitchesRange(queryStart, queryEnd);

            if (map.containsKey(Integer.valueOf(contextSwitchEntry.getId()))) {
                Long nbOfContextSwitchForCPU = map.get(Integer.valueOf(contextSwitchEntry.getId()));
                if (nbOfContextSwitchForCPU != null) {
                    ITimeEvent event = new ContextSwitchTimeEvent(entry, queryStart, bucketLength, nbOfContextSwitchForCPU.intValue());
                    eventList.add(event);
                }
            }
            queryStart = queryEnd;
            queryEnd += bucketLength;
        }
        classifyEvents(eventList);
        // eventList = mergeTimeEvents(eventList);
        if (monitor.isCanceled()) {
            return null;
        }
        return eventList;
    }

    private static void classifyEvents(List<ITimeEvent> eventList) {
        if (eventList == null || eventList.isEmpty()) {
            return;
        }
        /*
         * when this method is called, the mean must be known, otherwise we
         * can't classify them
         */

        /* Get the concerned trace mean */
        double mean = Double.NaN;

        /* Set and check thresholds */
        long lowRateThreshold = (long) (ClassificationFactors.LOW.fFactor * mean);
        long moderateRateThreshold = (long) (ClassificationFactors.MODERATE.fFactor * mean);
        long highRateThreshold = (long) (ClassificationFactors.HIGH.fFactor * mean);

        if (!(lowRateThreshold > 0)) {
            lowRateThreshold = 1;
        }

        if (!(moderateRateThreshold > lowRateThreshold)) {
            moderateRateThreshold = lowRateThreshold + 1;
        }
        if (!(highRateThreshold > moderateRateThreshold)) {
            highRateThreshold = moderateRateThreshold + 1;
        }

        for (ITimeEvent event : eventList) {
            if (!(event instanceof ContextSwitchTimeEvent)) {
                continue;
            }
            ContextSwitchTimeEvent csEvent = (ContextSwitchTimeEvent) event;
            if (Double.isNaN(mean)) {
                mean = ((ContextSwitchEntry) event.getEntry()).getMean();
            }
            long contextSwitchRate = csEvent.getValue();
            if (contextSwitchRate == 0) {
                /* No context switch for this TimeEvent */
                csEvent.fRate = ContextSwitchRate.NONE;
            } else if (contextSwitchRate <= lowRateThreshold) {
                csEvent.fRate = ContextSwitchRate.LOW;
            } else if (contextSwitchRate <= moderateRateThreshold) {
                csEvent.fRate = ContextSwitchRate.MODERATE;
            } else if (contextSwitchRate <= highRateThreshold) {
                csEvent.fRate = ContextSwitchRate.HIGH;
            } else {
                csEvent.fRate = ContextSwitchRate.CRITICAL;
            }
        }
    }

}
