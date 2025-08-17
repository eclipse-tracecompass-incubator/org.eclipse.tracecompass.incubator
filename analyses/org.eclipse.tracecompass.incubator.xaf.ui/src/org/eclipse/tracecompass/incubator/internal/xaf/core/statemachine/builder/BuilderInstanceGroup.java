/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraph;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.constraint.Operator;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.constraint.StateMachineConstraintAdaptive;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable.StateMachineVariableTimer;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstanceGroup;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineNode;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineTransition;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.StateMachineToDot;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * TODO Review this class
 *
 * @author Raphaël Beamonte
 */
public class BuilderInstanceGroup {

    private static final String LTTNG_UST_STATEDUMP = "lttng_ust_statedump:"; //$NON-NLS-1$

    /**
     * List of the state system analysis modules corresponding to the different
     * kernel traces
     */
    private final List<StateMachineBackendAnalysis> stateMachineBackendAnalysis;
    private final List<OsExecutionGraph> lttngKernelExecutionGraphModules;
    private final Map<Integer, List<BuilderEventInfo>> eventsPerTid = new TreeMap<>();
    private Set<String> variableTypesSet;
    private List<TimestampInterval> timeRangesList;
    private int currentTimeRange = 0;
    // TODO: remove
    private TmfExperiment fExperiment;

    /**
     * @param stateMachineBackendAnalysis
     *            the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules
     *            the critical path analysis modules
     */
    public BuilderInstanceGroup(List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules) {
        this.stateMachineBackendAnalysis = stateMachineBackendAnalysis;
        this.lttngKernelExecutionGraphModules = lttngKernelExecutionGraphModules;
        this.variableTypesSet = null;
        this.timeRangesList = null;
    }

    /**
     * @param stateMachineBackendAnalysis
     *            the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules
     *            the critical path analysis modules
     * @param variableTypes
     *            the types
     * @param timeRanges
     *            the time intervals
     */
    public BuilderInstanceGroup(List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules, Collection<String> variableTypes, Collection<TimestampInterval> timeRanges) {
        this.stateMachineBackendAnalysis = stateMachineBackendAnalysis;
        this.lttngKernelExecutionGraphModules = lttngKernelExecutionGraphModules;
        this.variableTypesSet = (variableTypes == null) ? null : new HashSet<>(variableTypes);
        this.timeRangesList = (timeRanges == null || timeRanges.isEmpty()) ? null : new ArrayList<>(new TreeSet<>(timeRanges));
    }

    /**
     * Build on an expirement and its events
     *
     * @param exp
     *            the experiment
     */
    public void buildOn(TmfExperiment exp) {
        this.currentTimeRange = 0;
        this.fExperiment = exp;
        ITmfContext ctx = exp.seekEvent(0);
        ITmfEvent event = exp.getNext(ctx);
        while (event != null) {
            this.receivedEvent(event);
            event = exp.getNext(ctx);
        }
    }

    /**
     * Event reception handling
     *
     * @param event
     *            the event
     */
    public void receivedEvent(ITmfEvent event) {
        if (event.getName().startsWith(LTTNG_UST_STATEDUMP)) {
            return;
        }

        if (timeRangesList != null) {
            if (currentTimeRange >= timeRangesList.size()) {
                return;
            }

            while (timeRangesList.get(currentTimeRange).getEndTime().compareTo(event.getTimestamp()) < 0) {
                currentTimeRange++;

                if (currentTimeRange >= timeRangesList.size()) {
                    return;
                }
            }

            if (!timeRangesList.get(currentTimeRange).contains(event.getTimestamp())) {
                return;
            }
        }

        Integer eventTid = StateMachineInstance.getEventTid(event);
        if (eventTid == null) {
            return;
        }

        List<BuilderEventInfo> beiList = eventsPerTid.get(eventTid);
        if (beiList == null) {
            beiList = new ArrayList<>();
            eventsPerTid.put(eventTid, beiList);
        }
        beiList.add(new BuilderEventInfo(event));
    }

    /**
     * Get the longest common subsequence between two lists
     *
     * @param l0
     *            the first list
     * @param l1
     *            the second list
     * @return the longest common subsequence
     */
    public static <T> List<T> lcs(List<T> l0, List<T> l1) {
        return lcs(l0, l1, new EqualityRunner<>());
    }

    /**
     * Get the longest common subsequence between two lists using an equality
     * runner
     *
     * @param l0
     *            the first list
     * @param l1
     *            the second list
     * @param eq
     *            the equality runner for comparison
     * @return the longest common subsequence
     */
    public static <T> List<T> lcs(List<T> l0, List<T> l1, @NonNull EqualityRunner<T> eq) {
        int[][] lengths = new int[l0.size() + 1][l1.size() + 1];

        // row 0 and column 0 are initialized to 0 already
        for (int i = 0; i < l0.size(); i++) {
            for (int j = 0; j < l1.size(); j++) {
                if (eq.isEqual(l0.get(i), l1.get(j))) {
                    lengths[i + 1][j + 1] = lengths[i][j] + 1;
                } else {
                    lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);
                }
            }
        }

        // read the substring out from the matrix
        List<T> lcs = new ArrayList<>();
        int x = l0.size();
        int y = l1.size();
        while (x != 0 && y != 0) {
            if (lengths[x][y] == lengths[x - 1][y]) {
                x--;
            } else if (lengths[x][y] == lengths[x][y - 1]) {
                y--;
            } else {
                lcs.add(eq.commonValue(l0.get(x - 1), l1.get(y - 1)));
                x--;
                y--;
            }
        }

        Collections.reverse(lcs);
        return lcs;
    }

    private Collection<List<BuilderEventInfo>> getLCSgroups() {
        return getLCSgroups(new EqualityRunner<>());
    }

    private Collection<List<BuilderEventInfo>> getLCSgroups(@NonNull EqualityRunner<BuilderEventInfo> eq) {
        List<List<List<BuilderEventInfo>>> eventListPerGroup = new ArrayList<>();
        Map<Integer, List<BuilderEventInfo>> commonListMap = new HashMap<>();

        for (Entry<Integer, List<BuilderEventInfo>> entry : eventsPerTid.entrySet()) {
            // Search the list in which to add the entry by proximity
            int mostProbableListIdx = -1;
            // int proximity = Integer.MIN_VALUE;
            List<BuilderEventInfo> bestLCS = null;

            for (int i = 0; i < eventListPerGroup.size(); i++) {
                /*
                 * TODO: improve! Currently, it is sufficient that 1 event is
                 * common to merge groups... it would be interesting to consider
                 * a minimum proximity given the size of both lists... like 50%
                 * at least have to be common to be merged in the same group...
                 */
                List<BuilderEventInfo> computedLCS = lcs(commonListMap.get(i), entry.getValue(), eq);
                if (computedLCS != null && !computedLCS.isEmpty()) {
                    bestLCS = computedLCS;
                    mostProbableListIdx = i;
                    break;
                }
            }

            // If we didn't find a suitable list
            List<List<BuilderEventInfo>> mostProbableList;
            if (mostProbableListIdx < 0) {
                mostProbableList = new ArrayList<>();
                mostProbableListIdx = eventListPerGroup.size();
                eventListPerGroup.add(mostProbableList);
            } else {
                mostProbableList = eventListPerGroup.get(mostProbableListIdx);
            }

            mostProbableList.add(entry.getValue());
            List<BuilderEventInfo> commonList = commonListMap.get(mostProbableListIdx);
            if (commonList == null) {
                commonListMap.put(mostProbableListIdx, entry.getValue());
            } else {
                if (bestLCS == null) {
                    bestLCS = lcs(commonList, entry.getValue(), eq);
                }
                commonListMap.put(mostProbableListIdx, bestLCS);
            }
        }

        return commonListMap.values();
    }

    private Collection<List<BuilderEventInfo>> getModelFlows() {
        Collection<List<BuilderEventInfo>> eventListPerGroup = new ArrayList<>();
        if (eventsPerTid.size() == 1) {
            SuffixTree<BuilderEventInfo> st = new SuffixTree<>(eventsPerTid.values().iterator().next());
            eventListPerGroup.add(st.getLongestNonOverlappingRepeatedSubstringWithoutRepeat());
        } else {
            Collection<List<BuilderEventInfo>> lcsGroups = getLCSgroups();
            Activator.logInfo("DEBUG: Group NB = " + lcsGroups.size()); //$NON-NLS-1$ //
                                                                         // FIXME:
                                                                         // DEBUG
                                                                         // OUTPUT
            double groupingRate = (eventsPerTid.size() - lcsGroups.size()) * 1. / eventsPerTid.size();
            Activator.logInfo("DEBUG: Grouping rate = " + groupingRate); //$NON-NLS-1$ //
                                                                          // FIXME:
                                                                          // DEBUG
                                                                          // OUTPUT

            // If the grouping rate is less than 50%, try reducing the
            // requirements
            if (groupingRate < .5) {
                // We first compute the maximum number of events available
                int maxNumberOfContent = eventsPerTid.values().stream()
                        .map(beiList -> beiList.stream()
                                .max((bei0, bei1) -> Integer.compare(bei0.getContentSize(), bei1.getContentSize()))
                                .map(bei -> bei.getContentSize())
                                .orElse(0))
                        .max((s0, s1) -> Integer.compare(s0, s1))
                        .orElse(0);

                Activator.logInfo("DEBUG: Max number of content = " + maxNumberOfContent); //$NON-NLS-1$ //
                                                                                            // FIXME:
                                                                                            // DEBUG
                                                                                            // OUTPUT

                // Then we compute the ideal grouping that considers only event
                // names
                Collection<List<BuilderEventInfo>> idealLcsGroups = getLCSgroups(new BuilderEventInfoNameEqualityRunner());
                double idealGroupingRate = (eventsPerTid.size() - idealLcsGroups.size()) * 1. / eventsPerTid.size();
                Activator.logInfo("DEBUG: Ideal group NB = " + idealLcsGroups.size()); //$NON-NLS-1$ //
                                                                                        // FIXME:
                                                                                        // DEBUG
                                                                                        // OUTPUT
                Activator.logInfo("DEBUG: Ideal grouping rate = " + idealGroupingRate); //$NON-NLS-1$ //
                                                                                         // FIXME:
                                                                                         // DEBUG
                                                                                         // OUTPUT

                // If the ideal grouping meets the 50% mark, try reducing the
                // required rate
                if (idealGroupingRate >= .5) {
                    if (maxNumberOfContent == 1) {
                        // If we only had one element in the events content,
                        // just move to the ideal groups
                        lcsGroups = idealLcsGroups;
                        groupingRate = idealGroupingRate;
                    } else {
                        /*
                         * Else, we will loop until we reach a rate of at least
                         * 50% (we know it's reachable as the ideal rate is over
                         * 50%).
                         */
                        Collection<List<BuilderEventInfo>> currentLcsGroups = lcsGroups;
                        double currentGroupingRate = groupingRate;
                        for (int numberOfContent = maxNumberOfContent - 1; currentGroupingRate < .5 && numberOfContent >= 0; numberOfContent--) {
                            double rate = numberOfContent * 1. / maxNumberOfContent;
                            currentLcsGroups = getLCSgroups(new BuilderEventInfoRateEqualityRunner(rate));
                            currentGroupingRate = (eventsPerTid.size() - currentLcsGroups.size()) * 1. / eventsPerTid.size();
                        }

                        if (currentGroupingRate >= .5) {
                            lcsGroups = currentLcsGroups;
                            groupingRate = currentGroupingRate;
                        }
                    }
                }

                Activator.logInfo("DEBUG: Final group NB = " + lcsGroups.size()); //$NON-NLS-1$ //
                                                                                   // FIXME:
                                                                                   // DEBUG
                                                                                   // OUTPUT
                Activator.logInfo("DEBUG: Final grouping rate = " + groupingRate); //$NON-NLS-1$ //
                                                                                    // FIXME:
                                                                                    // DEBUG
                                                                                    // OUTPUT
            }

            for (List<BuilderEventInfo> lcsGroup : lcsGroups) {
                SuffixTree<BuilderEventInfo> st = new SuffixTree<>(lcsGroup);
                eventListPerGroup.add(st.getLongestNonOverlappingRepeatedSubstringWithoutRepeat());
            }
        }
        return eventListPerGroup;
    }

    /**
     * Test
     */
    @SuppressWarnings("null")
    public void doTest() {
        Activator.logInfo("DEBUG: Number of instances: " + eventsPerTid.size()); //$NON-NLS-1$
        for (Entry<Integer, List<BuilderEventInfo>> entry : eventsPerTid.entrySet()) {
            Activator.logInfo("DEBUG:    - instance " + entry.getKey() + ": " + entry.getValue().size() + " events"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }


        List<StateMachineTransition> initialTransitions = getBasicInitialTransitions();

        StateMachineInstanceGroup smig = new StateMachineInstanceGroup(initialTransitions, stateMachineBackendAnalysis, lttngKernelExecutionGraphModules);
        smig.buildOn(fExperiment);

        Activator.logInfo(
                StateMachineToDot.drawStateMachine(initialTransitions));
    }

    /**
     * Get the list of basic initial transitions
     *
     * @return the list of basic initial transitions
     */
    public List<StateMachineTransition> getBasicInitialTransitions() {
        List<StateMachineTransition> initialTransitions = new ArrayList<>();
        Collection<List<BuilderEventInfo>> eventListPerGroup = getModelFlows();
        int modelNb = 0;
        for (List<BuilderEventInfo> modelflow : eventListPerGroup) {
            List<StateMachineNode> nodes = new ArrayList<>();
            List<StateMachineTransition> modelInitTransitions = new ArrayList<>();
            List<StateMachineVariable> variables = new ArrayList<>();

            // Create the nodes, variables and initial transitions
            for (int i = 0; i < modelflow.size(); i++) {
                // Create the node
                StateMachineNode newNode = new StateMachineNode(String.format("m%d/state%d", modelNb, i)); //$NON-NLS-1$
                nodes.add(newNode);

                // Create each of each type of variable that will be created for
                // this node
                for (@NonNull
                Entry<String, Class<?>> entry : StateMachineVariable.VARIABLE_TYPES.entrySet()) {
                    if (variableTypesSet != null && !variableTypesSet.contains(entry.getKey())) {
                        continue;
                    }

                    String varName = String.format("%s/%s%d.%d", //$NON-NLS-1$
                            entry.getKey(),
                            entry.getKey().substring(0, 1),
                            modelNb,
                            i);
                    try {
                        Constructor<?> variableConstructor = entry.getValue().getConstructor(String.class, Comparable.class);
                        StateMachineVariable newVariable = (StateMachineVariable) variableConstructor.newInstance(varName, "0"); //$NON-NLS-1$
                        variables.add(newVariable);
                        newNode.addVariable(newVariable);
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        Activator.logError(e.getMessage(), e);
                    }
                }

                // Create the initial transition that gets to that node
                modelInitTransitions.add(new StateMachineTransition(
                        newNode,
                        modelflow.get(i).getEventName(),
                        modelflow.get(i).getContent()));
            }

            // Add transitions from one node to the next
            for (int i = 0; i < nodes.size(); i++) {
                int nextNode = (i + 1) % nodes.size();
                StateMachineTransition newTransition = new StateMachineTransition(
                        nodes.get(nextNode),
                        modelflow.get(nextNode).getEventName(),
                        modelflow.get(nextNode).getContent());
                nodes.get(i).addTransition(newTransition);

                // Add the adaptive constraints for each variables
                for (StateMachineVariable variable : variables) {
                    String cond = "?"; //$NON-NLS-1$
                    if (StateMachineVariableTimer.class.isAssignableFrom(variable.getClass())) {
                        cond += "%"; //$NON-NLS-1$
                    }

                    newTransition.addConstraint(new StateMachineConstraintAdaptive(
                            variable.getName(),
                            Operator.ADAPTIVE,
                            cond));
                }
            }

            // Add all this model's initial transitions to the global initial
            // transitions list
            initialTransitions.addAll(modelInitTransitions);

            // Increment model number
            modelNb++;
        }
        return initialTransitions;
    }

    /**
     * Clean unused variables and constraints
     *
     * @param initialTransitions
     *            the initial transitions
     */
    public void cleanUnusedVariablesAndConstraints(List<StateMachineTransition> initialTransitions) {
        Set<String> variablesUsed = new HashSet<>();
        Set<StateMachineNode> readNode = new HashSet<>();

        LinkedList<StateMachineTransition> transitionsToRead = new LinkedList<>(initialTransitions);
        while (!transitionsToRead.isEmpty()) {
            StateMachineTransition transition = transitionsToRead.pop();

            Iterator<StateMachineConstraint> it = transition.getConstraints().iterator();
            while (it.hasNext()) {
                StateMachineConstraint constraint = it.next();
                if (constraint instanceof StateMachineConstraintAdaptive
                        && (constraint.getOperator().isAdaptive()
                                || StateMachineUtils.isValueAdaptive(constraint.getValue()))) {
                    it.remove();
                } else {
                    variablesUsed.add(constraint.getVarName());
                }
            }

            if (readNode.add(transition.getNextNode())) {
                transitionsToRead.addAll(transition.getNextNode().getTransitions());
            }
        }

        for (StateMachineNode node : readNode) {
            Iterator<StateMachineVariable> it = node.getVariables().iterator();
            while (it.hasNext()) {
                StateMachineVariable variable = it.next();
                if (!variablesUsed.contains(variable.getName())) {
                    it.remove();
                }
            }
        }
    }
}
