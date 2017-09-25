/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathAlgorithmException;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraph;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.core.Activator;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineBenchmark;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstanceGroup;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineReport;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineSegment;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.Attributes;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendStateValue;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoInvalidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineNoValidCaseException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineStateSystemTimeRangeException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineTIDNotFoundException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineUnexpectedEventException;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ActionOnMapKey;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.CpuFrequency;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.CriticalPathState;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ElementProbability;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionDuration;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionDurationSet;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionEventList;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionIntervalSet;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionIntervalSetDiff;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.InterruptionReason;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.IsiSampleIterator;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.ResponsibilityMap;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils.State;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfGraphVisitor;
import org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath.CriticalPathAlgorithmBounded;
import org.eclipse.tracecompass.internal.segmentstore.core.treemap.TreeMapStore;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Class that contains all the needed resources to perform an analysis for different variables
 * @author Raphaël Beamonte
 */
@SuppressWarnings("restriction")
public class StateMachineVariableAnalysis {

    private StateMachineVariable variable;
    private StateMachineConstraint constraint;
    private List<InstanceStepInformation> invalidIsiList;
    private List<InstanceStepInformation> validIsiList;
    private Multimap<Map<InterruptionReason, Integer>, InstanceStepInformation> invalidKeyToIsiMap;
    private Map<Map<InterruptionReason, Integer>, Collection<InstanceStepInformation>> keyMapToclosestValidIsiListMap;
    private ResponsibilityMap<InterruptionReason> analysisResponsibilityMap = null;

    private int differentInvalidCases = -1;
    private int differentValidCases = -1;

    private static final int MAXIMUM_REORDERED_ISI = 10000;

    static final double REALISTIC_FACTOR = 0.1; // 10%

    /** Index of the number of instances dropped because no state system was found */
    public static final int NO_STATE_SYSTEM_FOUND = 0;
    /** Index of the number of instances dropped because no TID was found */
    public static final int NO_TID_FOUND = 1;
    /** Index of the number of instances dropped because they were out of time range */
    public static final int OUT_OF_TIME_RANGE = 2;
    private int[] droppedInstances = new int[3];

    /**
     * Constructor of the StateMachineVariableAnalysis
     * @param variable The variable on which that StateMachineVariableAnalysis applies
     * @param constraint The constraint on which that StateMachineVariableAnalysis applies
     * @param invalidIsiList The list of invalid instance step information
     * @param validIsiList The list of valid instance step information
     */
    public StateMachineVariableAnalysis(StateMachineVariable variable, StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList) {
        this.variable = variable;
        this.constraint = constraint;
        this.invalidIsiList = invalidIsiList;
        this.validIsiList = validIsiList;
        this.invalidKeyToIsiMap = null;
        this.keyMapToclosestValidIsiListMap = null;
    }

    /**
     * Constructor of the StateMachineVariableAnalysis
     * @param variable The variable on which that StateMachineVariableAnalysis applies
     * @param constraint The constraint on which that StateMachineVariableAnalysis applies
     * @param invalidIsiList The list of invalid instance step information
     * @param validIsiList The list of valid instance step information
     * @param invalidKeyToIsiMap A map from invalid instance step information keyMap to the corresponding instance step information
     * @param closestValidKeyToIsiListMap A map from invalid instance step information keyMap to the list of the closest valid instance step information
     */
    public StateMachineVariableAnalysis(StateMachineVariable variable, StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList, Multimap<Map<InterruptionReason, Integer>, InstanceStepInformation> invalidKeyToIsiMap, Map<Map<InterruptionReason, Integer>, Collection<InstanceStepInformation>> closestValidKeyToIsiListMap) {
        this.variable = variable;
        this.constraint = constraint;
        this.invalidIsiList = invalidIsiList;
        this.validIsiList = validIsiList;
        this.invalidKeyToIsiMap = invalidKeyToIsiMap;
        this.keyMapToclosestValidIsiListMap = closestValidKeyToIsiListMap;
    }

    /**
     * To get the number of different invalid cases
     * @return The number of different invalid cases
     */
    public int getDifferentInvalidCases() {
        return differentInvalidCases;
    }

    /**
     * To get the number of different valid cases
     * @return The number of different valid cases
     */
    public int getDifferentValidCases() {
        return differentValidCases;
    }

    /**
     * To get the global analysis responsibility map
     * @return The analysis responsibility map
     */
    public ResponsibilityMap<InterruptionReason> getAnalysisResponsibilityMap() {
        return analysisResponsibilityMap;
    }

    /**
     * An InterruptionEventRunner
     * @author Raphaël Beamonte
     */
    public static abstract class InterruptionEventRunner {
        /**
         * The ResponsibilityMapGetter that will be used for this runner
         */
        protected ResponsibilityMapGetter<InterruptionReason> responsibilityMapGetter = new DefaultResponsibilityMapGetter();

        /**
         * Method to get the ResponsibilityMap of InterruptionReason for a given InterruptionIntervalSetDiff
         * @param iisd The InterruptionIntervalSetDiff for which to get the ResponsibilityMap
         * @return The ResponsibilityMap of InterruptionReason for the given InterruptionIntervalSetDiff
         */
        public ResponsibilityMap<InterruptionReason> getResponsibilityMap(InterruptionIntervalSetDiff iisd) {
            return responsibilityMapGetter.execute(iisd);
        }

        /**
         * Method to get the list of InterruptionEventList that happened during a given instance step
         * @param isi The instance step information
         * @return The List of InterruptionEventList
         * @throws StateMachineStateSystemNotFoundException When the state machine state system is not found
         * @throws StateMachineTIDNotFoundException When the instance does not have any TID
         * @throws StateMachineStateSystemTimeRangeException When the time requested is outside the state machine state system time range
         */
        public abstract List<InterruptionEventList> execute(InstanceStepInformation isi) throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException;
    }

    /**
     * An InterruptionEventGetter to get the interruption event list for a given timestamp interval, state interval, and event
     * @author Raphaël Beamonte
     */
    public static abstract class InterruptionEventGetter {
        /**
         * Method to get the InterruptionEventList
         * @param ti The timestamp interval
         * @param si The state interval
         * @param e The event
         * @return The corresponding interruption event list
         * @throws StateMachineStateSystemNotFoundException When the state machine state system is not found
         * @throws StateMachineTIDNotFoundException When the instance does not have any TID
         * @throws StateMachineStateSystemTimeRangeException When the time requested is outside the state machine state system time range
         * @throws StateMachineUnexpectedEventException  When the event received is not of the type we expected (can happen when two events share the same timestamp)
         */
        public abstract InterruptionEventList execute(TimestampInterval ti, ITmfStateInterval si, ITmfEvent e) throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException, StateMachineUnexpectedEventException;
    }

    /**
     * A ResponsibilityMapGetter to get the responsibility map from an InterruptionIntervalSetDiff
     * @param <K> The kind of ResponsibilityMap that getter returns
     * @author Raphaël Beamonte
     */
    public static abstract class ResponsibilityMapGetter<K extends Comparable<K>> {
        /**
         * Method to get the ResponsibilityMap from an InterruptionIntervalSetDiff
         * @param iisd InterruptionIntervalSetDiff from which to compute the ResponsibilityMap
         * @return The computed ResponsibilityMap
         */
        public abstract ResponsibilityMap<K> execute(InterruptionIntervalSetDiff iisd);
    }

    /**
     * The default responsibility map getter, to be used for most cases
     * @author Raphaël Beamonte
     */
    public static class DefaultResponsibilityMapGetter extends ResponsibilityMapGetter<InterruptionReason> {
        @Override
        public ResponsibilityMap<InterruptionReason> execute(InterruptionIntervalSetDiff iisd) {
            return iisd.getMeanResponsibilityMap();
        }
    }

    /**
     * The default interruption event runner, to be used for most cases
     * @author Raphaël Beamonte
     */
    public static class DefaultInterruptionEventRunner extends InterruptionEventRunner {
        private StateMachineVariable variable = null;
        private String attribute = null;
        private InterruptionEventGetter interruptionEventGetter = null;
        private Map<ITmfTrace, ITmfContext> contextPerTrace = new HashMap<>();

        /**
         * Constructor for the default interruption event runner
         * @param variable The state machine variable on which that runner is working
         * @param attribute The state machine state system attribute on which that runner is working
         * @param interruptionEventGetter The interruption event getter to use to get the events
         */
        public DefaultInterruptionEventRunner(@NonNull StateMachineVariable variable, @NonNull String attribute, @NonNull InterruptionEventGetter interruptionEventGetter) {
            this.variable = variable;
            this.attribute = attribute;
            this.interruptionEventGetter = interruptionEventGetter;
            this.responsibilityMapGetter = new DefaultResponsibilityMapGetter();
        }

        /**
         * Constructor for the default interruption event runner
         * @param variable The state machine variable on which that runner is working
         * @param attribute The state machine state system attribute on which that runner is working
         * @param interruptionEventGetter The interruption event getter to use to get the events
         * @param responsibilityMapGetter The responsibility map getter to use to get the responsibility map for an InterruptionIntervalSetDiff
         */
        public DefaultInterruptionEventRunner(@NonNull StateMachineVariable variable, @NonNull String attribute, @NonNull InterruptionEventGetter interruptionEventGetter, @NonNull ResponsibilityMapGetter<InterruptionReason> responsibilityMapGetter) {
            this.variable = variable;
            this.attribute = attribute;
            this.interruptionEventGetter = interruptionEventGetter;
            this.responsibilityMapGetter = responsibilityMapGetter;
        }

        @Override
        public List<InterruptionEventList> execute(InstanceStepInformation isi) throws StateMachineStateSystemNotFoundException, StateMachineTIDNotFoundException, StateMachineStateSystemTimeRangeException {
            // Get the instance group
            StateMachineInstanceGroup group = isi.instance.getStateMachineInstanceGroup();

            /* If we don't have kernel analysis modules for that group, we can
             * just drop that analysis as we won't have the needed information to
             * analyze that constraint. We can thus return.
             */
            if (group.getStateMachineBackendAnalysisModules().isEmpty()) {
                throw new StateMachineStateSystemNotFoundException();
            }

            /* Thread ID of our instance. We currently only manage
             * situations in which our model follows an unique TID
             */
            Integer tid = isi.instance.getVTid();
            if (tid == null) {
                throw new StateMachineTIDNotFoundException();
            }

            /* Get the timestamp interval for this variable application
             */
            TimestampInterval ti = isi.getVariableInterval(variable.getName());

            /* List in which we will store the events corresponding to an
             * increment of the counter on which that variable is based
             */
            List<InterruptionEventList> interruptionEventList = new ArrayList<>();

            /// USING THE NEW STATE ATTRIBUTE
            boolean analysisDone = false;
            // For each analysis module...
            for (StateMachineBackendAnalysis stateMachineBackendAnalysis : group.getStateMachineBackendAnalysisModules()) {
                ITmfStateSystem ss = stateMachineBackendAnalysis.getStateSystem();
                if (ss == null || ss.getCurrentEndTime() < ti.getEndTime().getValue() || ss.getStartTime() > ti.getStartTime().getValue()) {
                    continue;
                }
                analysisDone = true;

                // Kernel trace on which we're currently working
                ITmfTrace trace = stateMachineBackendAnalysis.getKernelTrace();

                // To store the events we'll read
                ITmfEvent e = null;

                // To store the context for this trace
                ITmfContext ctx = null;

                // Get all the intervals for the period of time
                StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Analysis SS Get"); //$NON-NLS-1$
                List<ITmfStateInterval> intervals = stateMachineBackendAnalysis.getAllStateIntervalInPeriod(tid, ti.getStartTime().getValue(), ti.getEndTime().getValue(), attribute);
                benchmarkObject.stop();
                //System.out.println(intervals.size() + " intervals for attribute " + attribute);

                //StateMachineReport.debug("NUMBER OF INTERVALS: " + intervals.size());
                for (ITmfStateInterval si : intervals) {
                    if (e == null) {
                        ctx = contextPerTrace.get(trace);
                        if (ctx != null) {
                            // Get the next event in the trace
                            e = trace.getNext(ctx);

                            // If our event is after our intervalStart... we'll need to seek :(
                            /* TODO: probably replace > by >= as it's possible that we're still
                             * at the right timestamp but me missed the right event if two (or
                             * more) events share the same timestamp
                             */
                            if (e.getTimestamp().getValue() > si.getStartTime()) {
                                ctx.dispose();
                                ctx = null;
                            }
                        }

                        if (ctx == null) {
                            ctx = trace.seekEvent(TmfTimestamp.create(si.getStartTime(), ITmfTimestamp.NANOSECOND_SCALE));
                            contextPerTrace.put(trace, ctx);

                            // Get the initial event of the interval
                            e = trace.getNext(ctx);
                        }
                    }

                    // Can't happen
                    if (e == null) {
                        throw new RuntimeException("e is null and can't be null... what happened?!"); //$NON-NLS-1$
                    }

                    while (e.getTimestamp().getValue() < si.getStartTime()) {
                        e = trace.getNext(ctx);
                    }

                    InterruptionEventList events = null;
                    while (events == null && e.getTimestamp().getValue() == si.getStartTime()) {
                        try {
                            events = interruptionEventGetter.execute(ti, si, e);
                        } catch (StateMachineUnexpectedEventException ex) {
                            e = trace.getNext(ctx);
                        }
                    }

                    if (events != null) {
                        interruptionEventList.add(events);
                    } else {
                        Activator.getInstance().logWarning("events is null and shouldn't be null... what happened?!"); //$NON-NLS-1$
                    }
                }
            }

            // If we couldn't do the analysis
            if (!analysisDone) {
                throw new StateMachineStateSystemTimeRangeException();
            }

            return interruptionEventList;
        }
    }

    /**
     * To perform a partial analysis using only invalid instances
     * @param runner The runner to use to get the interruption event list and the responsibility map
     * @throws StateMachineNoInvalidCaseException When no invalid case is found
     */
    public void doPartialAnalysis(InterruptionEventRunner runner) throws StateMachineNoInvalidCaseException {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("Initial analysis (PARTIAL)"); //$NON-NLS-1$
        // Local variables
        //List<InterruptionIntervalSet> invalidiisList = new ArrayList<>();
        Map<Map<InterruptionReason, Integer>, InterruptionIntervalSet> keyMapToiisMap = new HashMap<>();
        //Map<InterruptionReason, Integer> reasonToInvalidInstanceCounterMap = new HashMap<>();

        // Reset object attributes
        invalidKeyToIsiMap = HashMultimap.create();
        analysisResponsibilityMap = new ResponsibilityMap<>(0);
        keyMapToclosestValidIsiListMap = new HashMap<>();

        // Reset all counters of dropped instances
        resetDroppedInstances();

        Comparator<InstanceStepInformation> isiComparator = new Comparator<InstanceStepInformation>() {
            @Override
            public int compare(InstanceStepInformation isi1, InstanceStepInformation isi2) {
                TimestampInterval t1 = isi1.getVariableInterval(variable.getName());
                TimestampInterval t2 = isi2.getVariableInterval(variable.getName());
                return t1.compareTo(t2);
            }
        };

        StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Analysis get data"); //$NON-NLS-1$
        /////////////////////////////////////////////
        // COMPUTE SETS FOR INVALID INSTANCE STEPS //
        /////////////////////////////////////////////

        // For invalid instances
        IsiSampleIterator invalidIsiIterator = new IsiSampleIterator(invalidIsiList);

        int i = 0;
        StateMachineReport.debug("Invalid instances: " + invalidIsiList.size()); //$NON-NLS-1$
        StateMachineReport.debug("Treating ? " + invalidIsiIterator.size()); //$NON-NLS-1$
        while (invalidIsiIterator.hasNext()) {
            Iterator<InstanceStepInformation> isiIterator;
            if (invalidIsiIterator.useFullPopulation) {
                isiIterator = invalidIsiIterator;
            } else {
                Set<InstanceStepInformation> isiSet = new TreeSet<>(isiComparator);
                int limit = MAXIMUM_REORDERED_ISI;
                while (invalidIsiIterator.hasNext() && --limit > 0) {
                    isiSet.add(invalidIsiIterator.next());
                }
                isiIterator = isiSet.iterator();
            }

            while (isiIterator.hasNext()) {
                InstanceStepInformation isi = isiIterator.next();
                List<InterruptionEventList> interruptionEventLists;
                try {
                    interruptionEventLists = runner.execute(isi);
                } catch (StateMachineStateSystemNotFoundException | StateMachineTIDNotFoundException | StateMachineStateSystemTimeRangeException e) {
                    addDroppedInstance(e);
                    invalidIsiIterator.inc();
                    continue;
                }

                // Then, create an InterruptionDurationSet that will represent this case, we will put in it all
                // the events and it will represents globally what happened in here
                InterruptionDurationSet ids = new InterruptionDurationSet();
                for (InterruptionEventList taskList : interruptionEventLists) {
                    for (InterruptionDuration id : taskList.split()) {
                        ids.add(id);
                    }
                }

                Map<InterruptionReason, Integer> keyMap = ids.keyMap();
                invalidKeyToIsiMap.put(keyMap, isi);

                InterruptionIntervalSet invalidIis = keyMapToiisMap.get(keyMap);
                if (invalidIis == null) {
                    invalidIis = new InterruptionIntervalSet(constraint.getDirection());
                    keyMapToiisMap.put(keyMap, invalidIis);
                }
                invalidIis.addDurationSet(ids);

                /*for (InterruptionReason reason : ids.getReasonSet()) {
                    reasonToInvalidInstanceCounterMap.put(reason, reasonToInvalidInstanceCounterMap.getOrDefault(reason, 0) + 1);
                }*/

                // DEBUG
                i++;
                if (i%20 == 0) {
                    StateMachineReport.debug(Integer.toString(i));
                }
            }
        }
        if (invalidKeyToIsiMap.isEmpty()) {
            throw new StateMachineNoInvalidCaseException();
        }

        benchmarkObject.stop();
        benchmarkObject = new StateMachineBenchmark("Analysis weight data"); //$NON-NLS-1$

        InterruptionIntervalSetDiff iisd = new InterruptionIntervalSetDiff();
        for (InterruptionIntervalSet iis : keyMapToiisMap.values()) {
            for (InterruptionInterval ii : iis.getMergedIntervals()) {
                iisd.add(ii, ii.getCounter());
            }
        }
        analysisResponsibilityMap.mergeAll(runner.getResponsibilityMap(iisd));
        /*for (Entry<InterruptionReason, Integer> entry : reasonToInvalidInstanceCounterMap.entrySet()) {
            analysisResponsibilityMap.put(entry.getKey(), entry.getValue().doubleValue() / invalidIsiIterator.size());
        }*/
        /*System.out.println("CASE 1 => " + runner.getResponsibilityMap(iisd));
        System.out.println("CASE 2 => " + analysisResponsibilityMap);*/

        benchmarkObject.stop();

        // Update counters
        differentValidCases = -1;
        differentInvalidCases = invalidKeyToIsiMap.keySet().size();

        benchmarkObjectAnalysis.stop();
    }

    /**
     * To perform a full analysis comparing valid and invalid instances
     * @param runner The runner to use to get the interruption event list and the responsibility map
     * @throws StateMachineNoValidCaseException When no valid case is found for the comparison
     * @throws StateMachineNoInvalidCaseException When no invalid case is found for the comparison
     */
    public void doAnalysis(InterruptionEventRunner runner) throws StateMachineNoValidCaseException, StateMachineNoInvalidCaseException {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("Initial analysis (FULL)"); //$NON-NLS-1$
        // Local variables
        // List of the InterruptionIntervalSet
        Map<Map<InterruptionReason, Integer>, InterruptionIntervalSet> validiisMap = new HashMap<>();
        // Map of keyMap to valid InstanceStepInformation
        Multimap<Map<InterruptionReason, Integer>, InstanceStepInformation> validKeyToIsiMap = HashMultimap.create();

        Map<Map<InterruptionReason, Integer>, Map<InterruptionIntervalSet, List<InterruptionDurationSet>>> keyMapToSolutionsMap = new HashMap<>();
        Map<Map<InterruptionReason, Integer>, InterruptionIntervalSet> keyMapToiisMap = new HashMap<>();

        // Reset object attributes
        invalidKeyToIsiMap = HashMultimap.create();
        analysisResponsibilityMap = new ResponsibilityMap<>(0);
        keyMapToclosestValidIsiListMap = new HashMap<>();

        // Reset all counters of dropped instances
        resetDroppedInstances();

        Comparator<InstanceStepInformation> isiComparator = new Comparator<InstanceStepInformation>() {
            @Override
            public int compare(InstanceStepInformation isi1, InstanceStepInformation isi2) {
                TimestampInterval t1 = isi1.getVariableInterval(variable.getName());
                TimestampInterval t2 = isi2.getVariableInterval(variable.getName());
                return t1.compareTo(t2);
            }
        };

        StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Analysis get data"); //$NON-NLS-1$
        ///////////////////////////////////////////
        // COMPUTE SETS FOR VALID INSTANCE STEPS //
        ///////////////////////////////////////////

        // For valid instances
        IsiSampleIterator validIsiIterator = new IsiSampleIterator(validIsiList);

        int i = 0;
        StateMachineReport.debug("Valid instances: " + validIsiList.size()); //$NON-NLS-1$
        StateMachineReport.debug("Treating ? " + validIsiIterator.size()); //$NON-NLS-1$
        while (validIsiIterator.hasNext()) {
            Iterator<InstanceStepInformation> isiIterator;
            if (validIsiIterator.useFullPopulation) {
                isiIterator = validIsiIterator;
            } else {
                Set<InstanceStepInformation> isiSet = new TreeSet<>(isiComparator);
                int limit = MAXIMUM_REORDERED_ISI;
                while (validIsiIterator.hasNext() && --limit > 0) {
                    isiSet.add(validIsiIterator.next());
                }
                isiIterator = isiSet.iterator();
            }

            while (isiIterator.hasNext()) {
                InstanceStepInformation isi = isiIterator.next();
                List<InterruptionEventList> interruptionEventLists;
                try {
                    interruptionEventLists = runner.execute(isi);
                } catch (StateMachineStateSystemNotFoundException | StateMachineTIDNotFoundException | StateMachineStateSystemTimeRangeException e) {
                    addDroppedInstance(e);
                    validIsiIterator.inc();
                    continue;
                }

                InterruptionDurationSet ids = new InterruptionDurationSet();
                for (InterruptionEventList taskList : interruptionEventLists) {
                    for (InterruptionDuration id : taskList.split()) {
                        ids.add(id);
                    }
                }
                //interruptionEventLists.clear();

                Map<InterruptionReason, Integer> keyMap = ids.keyMap();
                validKeyToIsiMap.put(keyMap, isi);

                InterruptionIntervalSet iis = validiisMap.get(keyMap);
                if (iis == null) {
                    iis = new InterruptionIntervalSet(constraint.getDirection());
                    validiisMap.put(keyMap, iis);
                }
                iis.addDurationSet(ids);

                // DEBUG
                i++;
                if (i%20 == 0) {
                    StateMachineReport.debug(Integer.toString(i));
                }
            }
        }
        if (validiisMap.isEmpty()) {
            throw new StateMachineNoValidCaseException();
        }

        //////////////////////////
        // WORK ON INVALID ONES //
        //////////////////////////

        // For invalid instances
        IsiSampleIterator invalidIsiIterator = new IsiSampleIterator(invalidIsiList);

        i = 0;
        StateMachineReport.debug("Invalid instances: " + invalidIsiList.size()); //$NON-NLS-1$
        StateMachineReport.debug("Treating ? " + invalidIsiIterator.size()); //$NON-NLS-1$
        while (invalidIsiIterator.hasNext()) {
            Iterator<InstanceStepInformation> isiIterator;
            if (invalidIsiIterator.useFullPopulation) {
                isiIterator = invalidIsiIterator;
            } else {
                Set<InstanceStepInformation> isiSet = new TreeSet<>(isiComparator);
                int limit = MAXIMUM_REORDERED_ISI;
                while (invalidIsiIterator.hasNext() && --limit > 0) {
                    isiSet.add(invalidIsiIterator.next());
                }
                isiIterator = isiSet.iterator();
            }

            while (isiIterator.hasNext()) {
                InstanceStepInformation isi = isiIterator.next();
                List<InterruptionEventList> interruptionEventLists;
                try {
                    interruptionEventLists = runner.execute(isi);
                } catch (StateMachineStateSystemNotFoundException | StateMachineTIDNotFoundException | StateMachineStateSystemTimeRangeException e) {
                    addDroppedInstance(e);
                    invalidIsiIterator.inc();
                    continue;
                }

                // Then, create an InterruptionDurationSet that will represent this case, we will put in it all
                // the events and it will represents globally what happened in here
                InterruptionDurationSet ids = new InterruptionDurationSet();
                for (InterruptionEventList taskList : interruptionEventLists) {
                    for (InterruptionDuration id : taskList.split()) {
                        ids.add(id);
                    }
                }

                Map<InterruptionReason, Integer> keyMap = ids.keyMap();
                invalidKeyToIsiMap.put(keyMap, isi);

                InterruptionIntervalSet invalidIis = keyMapToiisMap.get(keyMap);
                if (invalidIis == null) {
                    invalidIis = new InterruptionIntervalSet(constraint.getDirection());
                    keyMapToiisMap.put(keyMap, invalidIis);
                }
                invalidIis.addDurationSet(ids);

                Map<InterruptionIntervalSet, List<InterruptionDurationSet>> dataMap = keyMapToSolutionsMap.get(keyMap);
                if (dataMap == null) {
                    dataMap = new HashMap<>();
                    keyMapToSolutionsMap.put(keyMap, dataMap);
                }

                for (InterruptionIntervalSet iis : validiisMap.values()) {
                    List<InterruptionDurationSet> data = dataMap.get(iis);
                    if (data == null) {
                        data = new ArrayList<>();
                        dataMap.put(iis, data);
                    }

                    data.add(iis.distance(ids));
                }

                // DEBUG
                i++;
                if (i%20 == 0) {
                    StateMachineReport.debug(Integer.toString(i));
                }
            }
        }
        if (invalidKeyToIsiMap.isEmpty()) {
            throw new StateMachineNoInvalidCaseException();
        }

        benchmarkObject.stop();
        benchmarkObject = new StateMachineBenchmark("Analysis weight data"); //$NON-NLS-1$

        // Compute distance and store it accordingly
        for (Entry<Map<InterruptionReason, Integer>, Map<InterruptionIntervalSet, List<InterruptionDurationSet>>> entryReasonSolutions : keyMapToSolutionsMap.entrySet()) {
            // Re-organize the solutions in order of distance to the current invalid situation
            Map<Integer, List<InterruptionIntervalSet>> validIisPerDistance = new TreeMap<>();
            for (Entry<InterruptionIntervalSet, List<InterruptionDurationSet>> entrySolutionDistances : entryReasonSolutions.getValue().entrySet()) {
                // TODO: well, I had a good reason to use -1 instead of 0, but I don't remember it!
                int distance = -1;
                for (InterruptionDurationSet ids : entrySolutionDistances.getValue()) {
                   distance += ids.size();
                }
                distance = (int) Math.ceil((double) distance / (double) entrySolutionDistances.getValue().size());

                List<InterruptionIntervalSet> iisList = validIisPerDistance.get(distance);
                if (iisList == null) {
                    iisList = new ArrayList<>();
                    validIisPerDistance.put(distance, iisList);
                }
                iisList.add(entrySolutionDistances.getKey());
            }

            // Compute the size of the invalid set
            double s = 0;
            for (int value : entryReasonSolutions.getKey().values()) {
                s += value;
            }
            double totalsize = Math.max(1.0, s);

            // To count the total number of occurrences up to now
            double totaloccurrences = 0;

            // For the penalty calculation
            double penalty = REALISTIC_FACTOR * (1.0 - 1.0 / entryReasonSolutions.getValue().keySet().size());

            Set<ElementProbability<InterruptionIntervalSet>> iisPerProbability = new TreeSet<>();
            for (Entry<Integer, List<InterruptionIntervalSet>> entryIisPerDistances : validIisPerDistance.entrySet()) {
                double distance = entryIisPerDistances.getKey();

                // We need to add to the totaloccurrences counter
                for (InterruptionIntervalSet iis : entryIisPerDistances.getValue()) {
                    totaloccurrences += iis.getCounter();
                }

                // Then we treat each list, for which we'll add the probability
                for (InterruptionIntervalSet iis : entryIisPerDistances.getValue()) {
                    double occurrences = iis.getCounter();
                    double probability = (occurrences / totaloccurrences) * (distance / totalsize) + (totalsize - distance) / totalsize - penalty;

                    // Only take that probability into account if it's more than 0%
                    if (probability > 0) {
                        iisPerProbability.add(new ElementProbability<>(iis, (int) occurrences, (int) distance, probability));
                    }
                }
            }

            // TODO: remove debug start
            /*
            System.out.println("Invalid interruption list " + //$NON-NLS-1$
                                keyMapToiisMap.get(entryReasonSolutions.getKey()) +
                                " has been encountered " + //$NON-NLS-1$
                                checkNotNull(keyMapToiisMap.get(entryReasonSolutions.getKey())).getCounter() +
                                " times"); //$NON-NLS-1$
            System.out.println("Interruption lists identified as potential expected list instead of this invalid one:"); //$NON-NLS-1$
            String[] columnNames = {
                "Valid interruption list ", //$NON-NLS-1$
                "Distance ", //$NON-NLS-1$
                "Occurrences ", //$NON-NLS-1$
                "Weight (%) ", //$NON-NLS-1$
            };
            List<Object[]> tableLines = new ArrayList<>();
            Iterator<ElementProbability<InterruptionIntervalSet>> it2 = iisPerProbability.iterator();
            while (it2.hasNext()) {
                ElementProbability<InterruptionIntervalSet> proba = it2.next();

                tableLines.add(new Object[]{
                    proba.element.toStringOneLine(),
                    new Integer(proba.distance),
                    new Integer(proba.occurrences),
                    String.format("   %.4f", proba.probability * 100), //$NON-NLS-1$
                });
            }
            Object[][] res = new Object[tableLines.size()][];
            tableLines.toArray(res);
            TextTable tt = new TextTable(columnNames, res);
            tt.setAddRowNumbering(true);
            tt.printTable();
            System.out.println(""); //$NON-NLS-1$*/
            // TODO: remove debug end

            keyMapToclosestValidIsiListMap.put(entryReasonSolutions.getKey(), validKeyToIsiMap.get(iisPerProbability.iterator().next().element.keyMap()));

            InterruptionIntervalSetDiff iisd = null;
            InterruptionIntervalSet iisError = NonNullUtils.checkNotNull(keyMapToiisMap.get(entryReasonSolutions.getKey()));
            Iterator<ElementProbability<InterruptionIntervalSet>> it = iisPerProbability.iterator();
            while (it.hasNext()) {
                ElementProbability<InterruptionIntervalSet> proba = it.next();
                if (proba.probability > .5 || iisd == null) {
                    InterruptionIntervalSetDiff iisdlocal = iisError.substract(proba.element);
                    if (iisd == null) {
                        iisd = iisdlocal;
                    } else {
                        iisd = iisd.interUnion(iisdlocal);
                    }
                } else {
                    break;
                }
            }

            // TODO: REMOVE DEBUG START
            /*String[] header = {"Valid list", "Distance", "Occurrences", "Weight"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            String[][] strs = new String[iisPerProbability.size()][4];
            int idx = 0;
            int progressSize = 10;
            it = iisPerProbability.iterator();
            while (it.hasNext()) {
                ElementProbability<InterruptionIntervalSet> proba = it.next();
                double ratioValue = proba.probability * 100;
                String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

                strs[idx][0] = String.format("%s", proba.element.toString() + "\n "); //$NON-NLS-1$
                strs[idx][1] = String.format("%d", proba.distance); //$NON-NLS-1$
                strs[idx][2] = String.format("%d", proba.occurrences); //$NON-NLS-1$
                strs[idx][3] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
                idx++;
            }
            StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1, 2, 3}));
            StateMachineReport.R.println();
            StateMachineReport.R.println(iisError);
            StateMachineReport.R.println();
            StateMachineReport.R.println(iisd);
            StateMachineReport.R.println();*/
            // TODO: REMOVE DEBUG END

            if (iisd != null) {
                ResponsibilityMap<InterruptionReason> rm = runner.getResponsibilityMap(iisd);
                analysisResponsibilityMap.mergeAll(rm);
            }
        }
        benchmarkObject.stop();

        // Update counters
        differentValidCases = validKeyToIsiMap.keySet().size();
        differentInvalidCases = invalidKeyToIsiMap.keySet().size();

        benchmarkObjectAnalysis.stop();
    }

    /**
     * Run the CPU Top analysis
     */
    public void runCpuTop() {
        runCpuTop(null, null);
    }

    /**
     * Run the CPU Top analysis
     * @param cpus The CPUs on which to run the analysis
     */
    public void runCpuTop(Collection<Integer> cpus) {
        runCpuTop(cpus, null);
    }

    /**
     * Run the CPU Top analysis
     * @param cpus The CPUs on which to run the analysis
     * @param receivedIntervals The intervals on which to run the analysis
     */
    public void runCpuTop(Collection<Integer> cpus, Collection<TimestampInterval> receivedIntervals) {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("CPUTop analysis"); //$NON-NLS-1$
        StateMachineReport.R.println_subsubsubsection("CPUTop analysis:"); //$NON-NLS-1$

        Set<Integer> sortedCpusSet = new TreeSet<>(cpus);
        StateMachineReport.R.println("Analyzed CPUs: " + Joiner.on(", ").join(sortedCpusSet)); //$NON-NLS-1$ //$NON-NLS-2$

        Collection<TimestampInterval> intervals = receivedIntervals;
        if (intervals == null || intervals.isEmpty()) {
            intervals = new LinkedList<>();
            for (InstanceStepInformation isi : invalidIsiList) {
                TimestampInterval ti = isi.getVariableInterval(variable.getName());
                intervals.add(ti);
            }
        }

        HashMultimap<String, TimestampInterval> intervalsPerTrace = HashMultimap.create();

        StateMachineInstanceGroup group = invalidIsiList.get(0).instance.getStateMachineInstanceGroup();
        for (TimestampInterval ti : intervals) {
            for (StateMachineBackendAnalysis module : group.getStateMachineBackendAnalysisModules()) {
                if (module.getKernelTrace().getTimeRange().contains(checkNotNull(ti.getStartTime()))
                        || module.getKernelTrace().getTimeRange().contains(checkNotNull(ti.getEndTime()))) {
                    intervalsPerTrace.put(module.getKernelTrace().getPath(), ti);
                }
            }
        }

        List<String> command = new LinkedList<>();
        command.add("/home/gbastien/Dorsal/sources/lttng-analyses/lttng-cputop-mi"); //$NON-NLS-1$
        command.add("--accumulate"); //$NON-NLS-1$
        //command.add("--no-progress"); //$NON-NLS-1$
        command.add("--skip-validation"); //$NON-NLS-1$
        command.add("--limit"); command.add("5"); //$NON-NLS-1$ //$NON-NLS-2$
        if (cpus != null && !cpus.isEmpty()) {
            command.add("--cpu"); command.add(Joiner.on(",").join(sortedCpusSet)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (Entry<String, Collection<TimestampInterval>> entry : intervalsPerTrace.asMap().entrySet()) {
            List<String> localCommand = new LinkedList<>(command);

            for (TimestampInterval ti : entry.getValue()) {
                String intvlStart = ti.getStartTime().toString().replaceAll("\\s",""); //$NON-NLS-1$ //$NON-NLS-2$
                String intvlEnd = ti.getEndTime().toString().replaceAll("\\s",""); //$NON-NLS-1$ //$NON-NLS-2$
                localCommand.add("--timerange"); localCommand.add("[" + intvlStart + "," + intvlEnd + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }

            localCommand.add(entry.getKey());

            ProcessBuilder pb = new ProcessBuilder(localCommand);
            pb.redirectErrorStream(true);
            try {
                Process pr = pb.start();
                BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String fullTxt = ""; //$NON-NLS-1$
                String line = null;
                while ((line = in.readLine()) != null) {
                    //System.out.println(line);
                    fullTxt += line;
                }
                JSONObject jsonPerProcess = null;
                JSONObject fullJson = new JSONObject(fullTxt);
                JSONArray jarray = fullJson.getJSONArray("results"); //$NON-NLS-1$
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject jobject = jarray.getJSONObject(i);
                    if (jobject.has("class") && jobject.getString("class").equals("per-process")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        jsonPerProcess = jobject;
                        break;
                    }
                }

                if (jsonPerProcess == null) {
                    StateMachineReport.debug("Information not found ?!"); //$NON-NLS-1$
                    continue;
                }

                JSONObject jsonPerProcessTimerange = jsonPerProcess.getJSONObject("time-range"); //$NON-NLS-1$
                JSONArray jsonPerProcessData = jsonPerProcess.getJSONArray("data"); //$NON-NLS-1$

                StateMachineReport.R.println(String.format(
                        "Analyzed timerange: [%s, %s]", //$NON-NLS-1$
                        TmfTimestamp.create(jsonPerProcessTimerange.getLong("begin"), ITmfTimestamp.NANOSECOND_SCALE), //$NON-NLS-1$
                        TmfTimestamp.create(jsonPerProcessTimerange.getLong("end"), ITmfTimestamp.NANOSECOND_SCALE) //$NON-NLS-1$
                        ));

                String[] header = {"Per-TID Usage", "Process", "Migrations", "Priorities"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                String[][] strs = new String[jsonPerProcessData.length()][4];
                int progressSize = 8;
                for (int idx = 0; idx < jsonPerProcessData.length(); idx++) {
                    JSONArray jsonEntry = jsonPerProcessData.getJSONArray(idx);

                    JSONObject process = jsonEntry.getJSONObject(0);
                    int migrations = jsonEntry.getInt(1);
                    String priorities = jsonEntry.getString(2);
                    JSONObject ratio = jsonEntry.getJSONObject(3);

                    double ratioValue = ratio.getDouble("value") * 100; //$NON-NLS-1$
                    String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

                    strs[idx][0] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
                    strs[idx][1] = String.format("%s (%d)", //$NON-NLS-1$
                            process.getString("name"), //$NON-NLS-1$
                            process.getLong("tid") //$NON-NLS-1$
                            );
                    strs[idx][2] = String.format("%d", migrations); //$NON-NLS-1$
                    strs[idx][3] = priorities;
                }
                StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{0, 2, 3}));

                pr.waitFor();
            } catch (IOException | InterruptedException | JSONException e) {
                Activator.getInstance().logWarning("Error running cpu top analysis: " + e.getMessage()); //$NON-NLS-1$
            }
        }
        benchmarkObjectAnalysis.stop();
    }

    /**
     * To run the priority inheritance analysis
     * @return The percent of time spent in sched_pi_setprio for invalid instances compared to valid ones
     */
    public double runPriorityInheritance() {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("PI analysis"); //$NON-NLS-1$
        double meanSchedPiInvalid = 0;
        double meanSchedPiInvalidNb = invalidIsiList.size();
        for (InstanceStepInformation isi : invalidIsiList) {
            TimestampInterval ti = isi.getVariableInterval(variable.getName());

            Integer tid = isi.instance.getVTid();
            if (tid == null) {
                //throw new StateMachineTIDNotFoundException();
                meanSchedPiInvalidNb--;
                continue;
            }

            for (StateMachineBackendAnalysis module : isi.instance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules()) {
                Long schedPiDuration = module.getSchedPiIntvl(tid, ti.getStartTime().getValue(), ti.getEndTime().getValue());
                meanSchedPiInvalid += schedPiDuration;
                //System.out.printf("INVALID schedPiDuration: %.3fms\n", ((double)schedPiDuration / 1.0e6));
            }
        }

        meanSchedPiInvalid /= meanSchedPiInvalidNb;

        if (meanSchedPiInvalid == 0) {
            return 0;
        }

        Long maxSchedPiValid = -1L;
        for (InstanceStepInformation isi : validIsiList) {
            TimestampInterval ti = isi.getVariableInterval(variable.getName());

            Integer tid = isi.instance.getVTid();
            if (tid == null) {
                //throw new StateMachineTIDNotFoundException();
                continue;
            }

            for (StateMachineBackendAnalysis module : isi.instance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules()) {
                Long schedPiDuration = module.getSchedPiIntvl(tid, ti.getStartTime().getValue(), ti.getEndTime().getValue());
                if (schedPiDuration > maxSchedPiValid) {
                    maxSchedPiValid = schedPiDuration;
                }
                //System.out.printf("VALID schedPiDuration: %.3fms\n", ((double)schedPiDuration / 1.0e6));
            }

        }

        StateMachineReport.R.println_subsubsubsection("Priority inheritance analysis:"); //$NON-NLS-1$
        StateMachineReport.R.println("Duration of active 'sched_pi_setprio'..."); //$NON-NLS-1$
        StateMachineReport.R.println(String.format(
                "... in valid instances (maximum): %.3fus", //$NON-NLS-1$
                maxSchedPiValid / 1e6
                ));
        StateMachineReport.R.println(String.format(
                "... in invalid instances (average): %.3fus", //$NON-NLS-1$
                meanSchedPiInvalid / 1e6
                ));
        StateMachineReport.R.println();

        String probaLvlTxt;
        double piProba = 0;
        if (meanSchedPiInvalid > maxSchedPiValid) {
            piProba = (meanSchedPiInvalid - maxSchedPiValid) / maxSchedPiValid;
            if (piProba > .75) {
                probaLvlTxt = "Very high probability of"; //$NON-NLS-1$
            } else if (piProba > .5) {
                probaLvlTxt = "High probability of"; //$NON-NLS-1$
            } else if (piProba > .25) {
                probaLvlTxt = "Medium probability of"; //$NON-NLS-1$
            } else if (piProba > .1) {
                probaLvlTxt = "Probability of"; //$NON-NLS-1$
            } else if (piProba > .05) {
                probaLvlTxt = "Low probability of"; //$NON-NLS-1$
            } else {
                probaLvlTxt = "Very low probability of"; //$NON-NLS-1$
            }

            StateMachineReport.R.println(String.format(
                    "Active %.2f%% more time in invalid instances than in valid instances.", //$NON-NLS-1$
                    piProba * 100
                    ));
            StateMachineReport.R.println();
        } else {
            probaLvlTxt = "Probably not"; //$NON-NLS-1$
        }

        StateMachineReport.R.println(String.format(
                "Verdict: %s a priority inversion", //$NON-NLS-1$
                probaLvlTxt
                ));
        StateMachineReport.R.println();

        benchmarkObjectAnalysis.stop();
        return piProba;
    }

    /**
     * To run the critical path analysis
     */
    public void runCriticalPath() {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("Critical path analysis"); //$NON-NLS-1$
        StateMachineReport.R.println_subsubsubsection("Critical path analysis:"); //$NON-NLS-1$

        ResponsibilityMap<InterruptionInterval> responsibilityMap = new ResponsibilityMap<>(new InterruptionInterval.IntervalToReasonComparator());
        responsibilityMap.setWeight(0);

        ActionOnMapKey<InterruptionInterval> actionOnMapKey = new ActionOnMapKey<InterruptionInterval>() {
            @Override
            public InterruptionInterval execute(InterruptionInterval key1, InterruptionInterval key2) {
                InterruptionInterval key = new InterruptionInterval(key1);
                key.addInterval(key2);
                //key.addTsInterval(key2.getTsIntervals());
                return key;
            }
        };

        for (Entry<Map<InterruptionReason, Integer>, Collection<InstanceStepInformation>> entry : invalidKeyToIsiMap.asMap().entrySet()) {
            // Get the keyMap
            Map<InterruptionReason, Integer> keyMap = entry.getKey();
            InterruptionIntervalSet iis = new InterruptionIntervalSet(constraint.getDirection());
            List<InterruptionDuration> criticalPathSteps;

            // Compute an InterruptionIntervalSet for the critical paths of the invalid cases
            IsiSampleIterator isiIterator = new IsiSampleIterator(entry.getValue());
            while (isiIterator.hasNext()) {
                InstanceStepInformation isi = isiIterator.next();
            //for (InstanceStepInformation isi : entry.getValue()) {
                try {
                    criticalPathSteps = getCriticalPathSteps(isi);
                } catch (CriticalPathAlgorithmException | StateMachineTIDNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }

                //criticalPathSteps = InterruptionDuration.accumulateList(criticalPathSteps);
                InterruptionDurationSet ids = new InterruptionDurationSet(criticalPathSteps);
                iis.addDurationSet(ids);
            }

            // Compute it for one of the valid cases
            Iterator<InstanceStepInformation> it = checkNotNull(keyMapToclosestValidIsiListMap.get(keyMap)).iterator();
            InterruptionDurationSet validIds = null;
            while (validIds == null && it.hasNext()) {
                InstanceStepInformation isi = it.next();
                try {
                    criticalPathSteps = getCriticalPathSteps(isi);
                } catch (CriticalPathAlgorithmException | StateMachineTIDNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }

                validIds = new InterruptionDurationSet(criticalPathSteps);
            }

            if (validIds == null || iis.isEmpty()) {
                System.out.println("Not enough information for a comparison"); //$NON-NLS-1$
                continue;
            }

            InterruptionIntervalSetDiff iisdLocal = iis.substract(validIds, constraint.getDirection());

            /*if (iisdLocal.getIntervalMeanResponsibilityMap().getSum() != 1) {
                StateMachineReport.debug("!!!!!!!!!! SUM != 1 ==> " + iisdLocal.getIntervalMeanResponsibilityMap().getSum());
            }
            StateMachineReport.debug("IIS: " + iis);
            StateMachineReport.debug("validIds: " + validIds);
            StateMachineReport.debug("iisdLocal: " + iisdLocal);
            StateMachineReport.debug("iisdLocal.getResponsibilityMap():");
            for (Entry<InterruptionInterval, Double[]> entryLocal : iisdLocal.getResponsibilityMap().entrySet()) {
                StateMachineReport.debug("  I: " + entryLocal.getKey() + " -- [" + entryLocal.getValue()[0] + ", " + entryLocal.getValue()[1] + "]");
            }*/

            responsibilityMap.mergeAll(iisdLocal.getIntervalMeanResponsibilityMap(), actionOnMapKey);
        }

        StateMachineReport.R.println("General computed difference between invalid and valid instances:"); //$NON-NLS-1$
        String[] header = {"Critical path state", "Responsibility for added time"}; //$NON-NLS-1$ //$NON-NLS-2$
        String[][] strs = new String[responsibilityMap.getCollection().size()][2];
        int idx = 0;
        int progressSize = 20;
        for (Entry<InterruptionInterval, Double> entry : responsibilityMap.getCollection()) {
            double ratioValue = entry.getValue() * 100;
            String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

            strs[idx][0] = entry.getKey().getReason().getID();
            strs[idx][1] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
            idx++;
        }
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1}));
        StateMachineReport.R.println(String.format(
                "Minimum responsibility for a case to be considered: %.2f%%", //$NON-NLS-1$
                responsibilityMap.getMinProba() * 100
                ));
        StateMachineReport.R.println();

        benchmarkObjectAnalysis.stop();

        StateMachineReport.R.println_subsubsubsubsection("Analysis of the states to keep for consideration:"); //$NON-NLS-1$
        for (Entry<InterruptionInterval, Double> entry : responsibilityMap.getTopStdDevCollection()) {
            StateMachineReport.R.println(String.format(
                            "%s for %.2f%% of the time", //$NON-NLS-1$
                            entry.getKey().getReason().getID(),
                            (entry.getValue() * 100)
                    ));
            StateMachineReport.R.inc();

            if (!(entry.getKey().getReason() instanceof CriticalPathState)) {
                throw new RuntimeException("Well, shouldn't happen now!!"); //$NON-NLS-1$
            }

            CriticalPathState cps = (CriticalPathState)entry.getKey().getReason();

            switch (cps.getType()) {
            case PREEMPTED:
                Set<Integer> setOfCpu = new TreeSet<>();
                Set<Integer> setOfPrio = new TreeSet<>();

                /* Search on which CPU we should run: these CPUs are the ones on which the preempted process
                 * was running at some point during the intervals we saved for the preempted state.
                 *
                 * We also take advantage of that loop to get the priority of our preempted process during
                 * those intervals. This part will probably be changed once the main state system is fixed
                 * to get the best priority information.
                 */
                int tid = cps.getWorker().getHostThread().getTid();
                StateMachineInstanceGroup group = invalidIsiList.get(0).instance.getStateMachineInstanceGroup();
                for (StateMachineBackendAnalysis smss : group.getStateMachineBackendAnalysisModules()) {
                    for (TimestampInterval ti : entry.getKey().getTsIntervals()) {
                        for (ITmfStateInterval itsi : smss.getAllStateIntervalInPeriod(tid, ti.getStartTime().getValue(), ti.getEndTime().getValue(), Attributes.CPU)) {
                            setOfCpu.add(itsi.getStateValue().unboxInt());
                        }
                        for (ITmfStateInterval itsi : smss.getAllStateIntervalInPeriod(tid, ti.getStartTime().getValue(), ti.getEndTime().getValue(), Attributes.PRIO)) {
                            setOfPrio.add(itsi.getStateValue().unboxInt());
                        }
                    }
                }

                runCpuTop(setOfCpu, entry.getKey().getTsIntervals());

                StateMachineReport.R.println(String.format(
                        "Priorities of the PREEMPTED process during that interval: %s", //$NON-NLS-1$
                        Joiner.on(", ").join(setOfPrio) //$NON-NLS-1$
                        ));
                break;
            case BLOCKED:
            case BLOCK_DEVICE:
            case DEFAULT:
            case EPS:
            case INTERRUPTED:
            case IPI:
            case NETWORK:
            case RUNNING:
            case TIMER:
            case UNKNOWN:
            case USER_INPUT:
                StateMachineReport.R.println("We don't know what to do for that yet!"); //$NON-NLS-1$
                break;
            default:
                throw new RuntimeException("We never should arrive there."); //$NON-NLS-1$
            }

            StateMachineReport.R.dec();
        }
    }

    private List<InterruptionDuration> getCriticalPathSteps(InstanceStepInformation isi) throws CriticalPathAlgorithmException, StateMachineTIDNotFoundException {
        TimestampInterval ti = isi.getVariableInterval(variable.getName());

        Integer tid = isi.instance.getVTid();
        if (tid == null) {
            throw new StateMachineTIDNotFoundException();
        }

        List<InterruptionDuration> criticalPathSteps = new ArrayList<>();

        // We search the execution graph that matches
        boolean graphFound = false;
        boolean workerFound = false;
        for (OsExecutionGraph g : isi.instance.getStateMachineInstanceGroup().getLttngKernelExecutionGraphModules()) {
            // Just in case it didn't finish its work for now, wait for it...
            g.waitForCompletion();

            // Get the general graph for the trace
            TmfGraph graph = g.getGraph();
            if (graph == null) {
                System.out.println("graph == null"); //$NON-NLS-1$
                continue;
            }
            graphFound = true;

            // Get the worker for the process we're following
            OsWorker lw = null;
            for (IGraphWorker worker : graph.getWorkers()) {
                if (worker instanceof OsWorker) {
                    OsWorker lworker = (OsWorker) worker;
                    if (lworker.getHostThread().getTid().equals(tid)) {
                        lw = lworker;
                        break;
                    }
                }
            }

            if (lw == null) {
                System.out.println("lw == null"); //$NON-NLS-1$
                continue;
            }
            workerFound = true;

            // Compute the path for the period we're interested into
            TmfVertex vstart = graph.getVertexAt(checkNotNull(ti.getStartTime()), lw);
            TmfVertex vend = graph.getVertexAt(checkNotNull(ti.getEndTime()), lw);
            TmfGraph path = new CriticalPathAlgorithmBounded(graph).compute(checkNotNull(vstart), checkNotNull(vend));

            // Then traverse that path to get the data for what happened in there
            path.scanLineTraverse(path.getHead(lw), new TmfGraphVisitor() {
                @Override
                public void visit(TmfEdge link, boolean horizontal) {
                    if (!horizontal) {
                        return;
                    }

                    IGraphWorker w = path.getParentOf(link.getVertexTo());
                    if (!(w instanceof OsWorker)) {
                        throw new RuntimeException("Should not have something that's not a OsWorker here..."); //$NON-NLS-1$
                    }
                    OsWorker llw = (OsWorker)w;

                    ITmfTimestamp tsStart = TmfTimestamp.create(link.getVertexFrom().getTs(), ITmfTimestamp.NANOSECOND_SCALE);
                    ITmfTimestamp tsEnd = TmfTimestamp.create(link.getVertexTo().getTs(), ITmfTimestamp.NANOSECOND_SCALE);

                    // Change the worker name to the actual process name if available
                    for (StateMachineBackendAnalysis smssa : isi.instance.getStateMachineInstanceGroup().getStateMachineBackendAnalysisModules()) {
                        ITmfTrace kernelTrace = smssa.getKernelTrace();
                        if (kernelTrace.getStartTime().compareTo(tsEnd) > 0 || kernelTrace.getEndTime().compareTo(tsEnd) < 0) {
                            //System.out.println("DEBUG: time does not fit");
                            continue;
                        }

                        KernelAnalysisModule kernelAnalysisModule = TmfTraceUtils.getAnalysisModuleOfClass(kernelTrace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
                        if (kernelAnalysisModule == null) {
                           //System.out.println("DEBUG: kernelAnalysisModule is null");
                            continue;
                        }

                        // Just in case it wasn't finished yet
                        kernelAnalysisModule.waitForCompletion();

                        ITmfStateSystem ss = kernelAnalysisModule.getStateSystem();
                        if (ss == null) {
                            //System.out.println("DEBUG: ss is null");
                            continue;
                        }

                        try {
                            Integer execNameNode = ss.getQuarkAbsolute(org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes.THREADS, llw.getHostThread().getTid().toString(), org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes.EXEC_NAME);

                            ITmfStateInterval si = ss.querySingleState(tsEnd.getValue(), execNameNode);
                            while (si.getStateValue().isNull() && si.getStartTime() - 1 >= ss.getStartTime()) {
                                si = ss.querySingleState(si.getStartTime() - 1, execNameNode);
                            }

                            if (!si.getStateValue().isNull()) {
                                llw.setName(si.getStateValue().unboxStr());
                            }
                        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
                            //e.printStackTrace(); // DEBUG
                        }
                    }

                    /*System.out.printf("%s (%d) was %s for %.04fms (between %s and %s)\n", //$NON-NLS-1$
                            llw.getName(),
                            llw.getHostThread().getTid(),
                            link.getType().name(),
                            link.getDuration() / 1.0e6,
                            new TmfTimestamp(link.getVertexFrom().getTs(), ITmfTimestamp.NANOSECOND_SCALE).toString(),
                            new TmfTimestamp(link.getVertexTo().getTs(), ITmfTimestamp.NANOSECOND_SCALE).toString());*/

                    InterruptionReason ir = new CriticalPathState(link.getType(), llw);
                    TimestampInterval tsInterval = new TimestampInterval(tsStart, tsEnd);
                    criticalPathSteps.add(new InterruptionDuration(ir, link.getDuration(), tsInterval));
                }
            });

            //System.out.println("Interval Start: " + intervalStart + " (" + intervalStart.getValue() + ")");
            //System.out.println("Interval End: " + intervalEnd + " (" + intervalEnd.getValue() + ")");
        }

        // If we didn't found the graph or the worker... we couldn't do that analysis
        if (!graphFound) {
            throw new CriticalPathAlgorithmException("No graph found"); //$NON-NLS-1$
        } else if (!workerFound) {
            throw new CriticalPathAlgorithmException("No worker found"); //$NON-NLS-1$
        }

        // Finally, return the steps of the critical path for the period of time we were interested into
        return criticalPathSteps;
    }

    /**
     * To run an analysis on the instances duration
     */
    public void runInstancesDurationAnalysis() {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("Instances duration analysis"); //$NON-NLS-1$
        StateMachineReport.R.println_subsubsubsection("Instances duration analysis:"); //$NON-NLS-1$

        double minValid = Double.MAX_VALUE, maxValid = Double.MIN_VALUE, sumValid = 0;
        double avgValid = 0, varValid = 0, stdevValid = 0;
        for (InstanceStepInformation isi : validIsiList) {
            double duration = isi.getVariableInterval(variable.getName()).getDuration() / 1.e6;

            if (duration > maxValid) {
                maxValid = duration;
            }

            if (duration < minValid) {
                minValid = duration;
            }

            sumValid += duration;
        }
        avgValid = sumValid / validIsiList.size();
        for (InstanceStepInformation isi : validIsiList) {
            double duration = isi.getVariableInterval(variable.getName()).getDuration() / 1.e6;

            varValid += Math.pow(duration - avgValid, 2);
        }
        varValid /= validIsiList.size();
        stdevValid = Math.sqrt(varValid);

        double minInvalid = Double.MAX_VALUE, maxInvalid = Double.MIN_VALUE, sumInvalid = 0;
        double avgInvalid = 0, varInvalid = 0, stdevInvalid = 0;
        for (InstanceStepInformation isi : invalidIsiList) {
            double duration = isi.getVariableInterval(variable.getName()).getDuration() / 1.e6;

            if (duration > maxInvalid) {
                maxInvalid = duration;
            }

            if (duration < minInvalid) {
                minInvalid = duration;
            }

            sumInvalid += duration;
        }
        avgInvalid = sumInvalid / invalidIsiList.size();
        for (InstanceStepInformation isi : invalidIsiList) {
            double duration = isi.getVariableInterval(variable.getName()).getDuration() / 1.e6;

            varInvalid += Math.pow(duration - avgInvalid, 2);
        }
        varInvalid /= invalidIsiList.size();
        stdevInvalid = Math.sqrt(varInvalid);


        //StateMachineReport.R.println("General computed difference between invalid and valid instances:"); //$NON-NLS-1$
        String[] header = {"Instances", "Count", "Min (ms)", "Avg (ms)", "Max (ms)", "StDev (ms)"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        String[][] strs = new String[2][6];

        strs[0][0] = "Valid"; //$NON-NLS-1$
        strs[0][1] = String.format("%d", validIsiList.size()); //$NON-NLS-1$
        strs[0][2] = String.format("%.3f", minValid); //$NON-NLS-1$
        strs[0][3] = String.format("%.3f", avgValid); //$NON-NLS-1$
        strs[0][4] = String.format("%.3f", maxValid); //$NON-NLS-1$
        strs[0][5] = String.format("%.3f", stdevValid); //$NON-NLS-1$

        strs[1][0] = "Invalid"; //$NON-NLS-1$
        strs[1][1] = String.format("%d", invalidIsiList.size()); //$NON-NLS-1$
        strs[1][2] = String.format("%.3f", minInvalid); //$NON-NLS-1$
        strs[1][3] = String.format("%.3f", avgInvalid); //$NON-NLS-1$
        strs[1][4] = String.format("%.3f", maxInvalid); //$NON-NLS-1$
        strs[1][5] = String.format("%.3f", stdevInvalid); //$NON-NLS-1$

        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1, 2, 3, 4, 5}));
        StateMachineReport.R.println();
        benchmarkObjectAnalysis.stop();
    }

    private ResponsibilityMap<InterruptionReason> cpuFrequencyScalingAnalysisRunner(Collection<InstanceStepInformation> isiCollection) {
        ResponsibilityMap<InterruptionReason> responsibilityMap = new ResponsibilityMap<>(0);

        IsiSampleIterator isiIterator = new IsiSampleIterator(isiCollection);
        while (isiIterator.hasNext()) {
            InstanceStepInformation isi = isiIterator.next();
        //for (InstanceStepInformation isi : isiCollection) {
            TimestampInterval ti = isi.getVariableInterval(variable.getName());
            StateMachineInstanceGroup group = isi.instance.getStateMachineInstanceGroup();

            InterruptionDurationSet ids = new InterruptionDurationSet();

            for (StateMachineBackendAnalysis smss : group.getStateMachineBackendAnalysisModules()) {
                for (ITmfStateInterval cpuItsi : smss.getAllStateIntervalInPeriod(isi.instance.getVTid(), ti.getStartTime().getValue(), ti.getEndTime().getValue(), Attributes.CPU)) {
                    TimestampInterval cpuTi = new TimestampInterval(
                            Math.max(ti.getStartTime().getValue(), cpuItsi.getStartTime()),
                            Math.min(ti.getEndTime().getValue(), cpuItsi.getEndTime()));

                    if (cpuItsi.getStateValue().isNull()) {
                        InterruptionReason ir = new CpuFrequency(-1L);
                        InterruptionDuration id = new InterruptionDuration(ir, cpuTi.getDuration(), cpuTi);

                        ids.add(id);

                        continue;
                    }

                    Integer cpu = cpuItsi.getStateValue().unboxInt();

                    for (ITmfStateInterval freqItsi : smss.getAllStateIntervalInPeriod(cpuTi.getStartTime().getValue(), cpuTi.getEndTime().getValue(), Attributes.CPU_FREQ, cpu.toString())) {
                        Long freq = -1L;
                        if (!freqItsi.getStateValue().isNull()) {
                            freq = freqItsi.getStateValue().unboxLong();
                        }

                        TimestampInterval freqTi = new TimestampInterval(
                                Math.max(cpuTi.getStartTime().getValue(), freqItsi.getStartTime()),
                                Math.min(cpuTi.getEndTime().getValue(), freqItsi.getEndTime()));

                        InterruptionReason ir = new CpuFrequency(freq);
                        InterruptionDuration id = new InterruptionDuration(ir, freqTi.getDuration(), freqTi);

                        ids.add(id);
                    }

                }
            }

            responsibilityMap.mergeAll(new InterruptionIntervalSetDiff(ids).getMeanResponsibilityMap());
        }

        return responsibilityMap;
    }

    private static double runCpuFrequencyScalingAnalysis(ResponsibilityMap<InterruptionReason> responsibilityMap) {
        long sumFrequency = 0;
        double sumPercent = 0;

        String[] header = {"CPU Frequency", "Percent of time"}; //$NON-NLS-1$ //$NON-NLS-2$
        String[][] strs = new String[responsibilityMap.getCollection().size()][2];
        int idx = 0;
        int progressSize = 20;
        for (Entry<InterruptionReason, Double> entry : responsibilityMap.getCollection()) {
            double ratioValue = entry.getValue() * 100;
            String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

            if (entry.getKey().getID().equals("-1")) { //$NON-NLS-1$
                strs[idx][0] = "Unknown"; //$NON-NLS-1$
            } else {
                long frequency = ((CpuFrequency)entry.getKey()).getFreq();
                strs[idx][0] = String.format("%.2f MHz", frequency / 1000.0); //$NON-NLS-1$

                sumFrequency += frequency * entry.getValue();
                sumPercent += entry.getValue();
            }
            strs[idx][1] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
            idx++;
        }
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1}));

        double meanFreq = sumFrequency / sumPercent;
        StateMachineReport.R.println(String.format("Average frequency: %.2f MHz", //$NON-NLS-1$
                meanFreq / 1000));

        StateMachineReport.R.println();

        return meanFreq;
    }

    /**
     * To run an analysis on the cpu frequency
     */
    public void runCpuFrequencyScalingAnalysis() {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("CPU Frequency scaling analysis"); //$NON-NLS-1$

        ResponsibilityMap<InterruptionReason> responsibilityMapValid, responsibilityMapInvalid;

        responsibilityMapInvalid = cpuFrequencyScalingAnalysisRunner(invalidIsiList);
        // If we don't have enough information for the invalid instances
        if (responsibilityMapInvalid.isEmpty() ||
                responsibilityMapInvalid.size() == 1 && ((CpuFrequency)responsibilityMapInvalid.keySet().iterator().next()).getFreq() == -1) {
            return;
        }

        responsibilityMapValid = cpuFrequencyScalingAnalysisRunner(validIsiList);
        // If we don't have enough information for the valid instances
        if (responsibilityMapValid.isEmpty() ||
                responsibilityMapValid.size() == 1 && ((CpuFrequency)responsibilityMapValid.keySet().iterator().next()).getFreq() == -1) {
            return;
        }

        // If we have exactly the same information for both the valid and invalid instances
        if (responsibilityMapValid.size() == 1 && responsibilityMapInvalid.size() == 1 &&
                ((CpuFrequency)responsibilityMapValid.keySet().iterator().next()).getFreq() == ((CpuFrequency)responsibilityMapInvalid.keySet().iterator().next()).getFreq()) {
            return;
        }

        StateMachineReport.R.println_subsubsubsection("CPU frequency analysis:"); //$NON-NLS-1$

        StateMachineReport.R.println("CPU frequency for valid instances:"); //$NON-NLS-1$
        double meanFreqValid = runCpuFrequencyScalingAnalysis(responsibilityMapValid);

        StateMachineReport.R.println("CPU frequency for invalid instances:"); //$NON-NLS-1$
        double meanFreqInvalid = runCpuFrequencyScalingAnalysis(responsibilityMapInvalid);


        String higherOrLowerTxt = "higher"; //$NON-NLS-1$
        String probaLvlTxt;
        double freqProba;
        switch (constraint.getDirection()) {
        case VARIABLE_EQ_VALUE:
            if (meanFreqValid < meanFreqInvalid) {
                higherOrLowerTxt = "lower"; //$NON-NLS-1$
            }
            freqProba = Math.abs(meanFreqValid - meanFreqInvalid) / Math.min(meanFreqValid, meanFreqInvalid);
            break;
        case VARIABLE_OVER_VALUE:
            higherOrLowerTxt = "lower"; //$NON-NLS-1$
            freqProba = (meanFreqInvalid - meanFreqValid) / meanFreqValid;
            break;
        case VARIABLE_UNDER_VALUE:
            freqProba = (meanFreqValid - meanFreqInvalid) / meanFreqInvalid;
            break;
        default:
            freqProba = 0;
            break;
        }

        if (freqProba > 0) {
            if (freqProba > .75) {
                probaLvlTxt = "Very high probability of"; //$NON-NLS-1$
            } else if (freqProba > .5) {
                probaLvlTxt = "High probability of"; //$NON-NLS-1$
            } else if (freqProba > .25) {
                probaLvlTxt = "Medium probability of"; //$NON-NLS-1$
            } else if (freqProba > .1) {
                probaLvlTxt = "Probability of"; //$NON-NLS-1$
            } else if (freqProba > .05) {
                probaLvlTxt = "Low probability of"; //$NON-NLS-1$
            } else {
                probaLvlTxt = "Very low probability of"; //$NON-NLS-1$
            }

            StateMachineReport.R.println(String.format(
                    "%.2f%% %s average frequency in valid instances than in invalid instances.", //$NON-NLS-1$
                    freqProba * 100,
                    higherOrLowerTxt
                    ));
            StateMachineReport.R.println();
        } else {
            probaLvlTxt = "No probability of"; //$NON-NLS-1$
        }

        StateMachineReport.R.println(String.format(
                "Verdict: %s a frequency scaling problem", //$NON-NLS-1$
                probaLvlTxt
                ));
        StateMachineReport.R.println();

        benchmarkObjectAnalysis.stop();
    }

    private InterruptionIntervalSet stateMachineStateAnalysisRunner(Collection<InstanceStepInformation> isiCollection) {
        InterruptionIntervalSet iis = new InterruptionIntervalSet(constraint.getDirection());

        IsiSampleIterator isiIterator = new IsiSampleIterator(isiCollection);
        while (isiIterator.hasNext()) {
            InstanceStepInformation isi = isiIterator.next();
            TimestampInterval ti = isi.getVariableInterval(variable.getName());
            List<InstanceStepInformation> isiList = isi.instance.getStepsInInterval(ti);

            InterruptionDurationSet ids = new InterruptionDurationSet();

            Iterator<InstanceStepInformation> it = isiList.iterator();
            InstanceStepInformation current, next;
            next = (it.hasNext()) ? it.next() : null;

            while (next != null) {
                current = next;
                next = (it.hasNext()) ? it.next() : null;

                if (next != null) {
                    InterruptionReason ir = new State(BackendStateValue.RUNNING.getValue(), current.node.getName());
                    TimestampInterval tInterval = new TimestampInterval(current.event.getTimestamp(), next.event.getTimestamp());
                    InterruptionDuration id = new InterruptionDuration(ir, tInterval.getDuration(), tInterval);

                    ids.add(id);
                }
            }

            iis.addDurationSet(ids);
        }

        return iis;
    }

    /**
     * To run an analysis on the state machine states
     */
    public void runStateMachineStateAnalysis() {
        StateMachineBenchmark benchmarkObjectAnalysis = new StateMachineBenchmark("State machine state analysis"); //$NON-NLS-1$
        StateMachineReport.R.println_subsubsubsection("State machine state analysis:"); //$NON-NLS-1$

        InterruptionIntervalSet validIis = stateMachineStateAnalysisRunner(validIsiList);
        InterruptionIntervalSet invalidIis = stateMachineStateAnalysisRunner(invalidIsiList);

        ResponsibilityMap<InterruptionReason> responsibilityMap = invalidIis.substract(validIis).getMeanResponsibilityMap();
        StateMachineReport.R.println("General computed difference between invalid and valid instances:"); //$NON-NLS-1$
        String[] header = {"Model state", "Responsibility for added time"}; //$NON-NLS-1$ //$NON-NLS-2$
        String[][] strs = new String[responsibilityMap.getCollection().size()][2];
        int idx = 0;
        int progressSize = 20;
        for (Entry<InterruptionReason, Double> entry : responsibilityMap.getCollection()) {
            double ratioValue = entry.getValue() * 100;
            String symbol = StateMachineReport.progressBar(ratioValue, progressSize);

            strs[idx][0] = entry.getKey().getTaskId();
            strs[idx][1] = String.format("%.2f%% %s", ratioValue, symbol); //$NON-NLS-1$
            idx++;
        }
        StateMachineReport.R.println_table(header, strs, Arrays.asList(new Integer[]{1}));
        StateMachineReport.R.println();

        benchmarkObjectAnalysis.stop();
    }

    /**
     * To get the number of dropped instances during the analysis for a given reason
     * @param reason The reason for which instances were dropped
     * @return The number of dropped instances during the analysis for that reason
     */
    public int getDroppedInstances(int reason) {
        return droppedInstances[reason];
    }

    /**
     * To get the total number of dropped instances during the analysis
     * @return The total number of dropped instances during the analysis
     */
    public int getDroppedInstances() {
        return droppedInstances[NO_STATE_SYSTEM_FOUND]
                + droppedInstances[NO_TID_FOUND]
                + droppedInstances[OUT_OF_TIME_RANGE];
    }

    /**
     * To add to the dropped instances when an exception is happening
     * @param e The exception that happened
     */
    private void addDroppedInstance(Exception e) {
        if (e instanceof StateMachineStateSystemNotFoundException) {
            droppedInstances[NO_STATE_SYSTEM_FOUND]++;
        } else if (e instanceof StateMachineTIDNotFoundException) {
            droppedInstances[NO_TID_FOUND]++;
        } else if (e instanceof StateMachineStateSystemTimeRangeException) {
            droppedInstances[OUT_OF_TIME_RANGE]++;
        }
    }

    /**
     * To reset the number of dropped instances
     */
    private void resetDroppedInstances() {
        droppedInstances[NO_STATE_SYSTEM_FOUND] = 0;
        droppedInstances[NO_TID_FOUND] = 0;
        droppedInstances[OUT_OF_TIME_RANGE] = 0;
    }

    /**
     * @return The list of invalid instance step information on which this
     * state machine variable analysis applies
     */
    public List<InstanceStepInformation> getInvalidIsiList() {
        return invalidIsiList;
    }

    /**
     * @return The variable on which this state machine variable analysis applies
     */
    public StateMachineVariable getVariable() {
        return variable;
    }

    /**
     * @return The list of valid instance step information on which this
     * state machine variable analysis applies
     */
    public List<InstanceStepInformation> getValidIsiList() {
        return validIsiList;
    }

    /**
     * @return The segment store for constraint status for the instances steps,
     * constraint and variable on which this state machine variable analysis applies
     */
    public @NonNull ISegmentStore<@NonNull ISegment> getInstanceConstraintsSegmentStore() {
        @NonNull ISegmentStore<@NonNull ISegment> segmentStore = new TreeMapStore<>();

        for (InstanceStepInformation isi : invalidIsiList) {
            TimestampInterval ti = isi.getVariableInterval(variable.getName());
            segmentStore.add(new StateMachineSegment(isi.instance.getVTid(), ti.getStartTime().getValue(), ti.getEndTime().getValue(), constraint.toString()));
        }

        for (InstanceStepInformation isi : validIsiList) {
            TimestampInterval ti = isi.getVariableInterval(variable.getName());
            segmentStore.add(new StateMachineSegment(isi.instance.getVTid(), ti.getStartTime().getValue(), ti.getEndTime().getValue()));
        }

        return segmentStore;
    }

}
