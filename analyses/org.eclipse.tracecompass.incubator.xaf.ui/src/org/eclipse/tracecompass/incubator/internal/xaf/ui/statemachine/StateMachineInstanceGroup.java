/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraph;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraintAdaptive;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Status;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StatusValue;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariableAnalysis;
import org.eclipse.tracecompass.internal.segmentstore.core.treemap.TreeMapStore;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * @author Raphaël Beamonte
 */
@SuppressWarnings("restriction")
public class StateMachineInstanceGroup {

    /** Map of the initial transitions following the format <k, v>=<Event name, Transition> */
    private final Map<String, List<StateMachineTransition>> initialTransitions = new HashMap<>();
    /** List of the state system analysis modules corresponding to the different kernel traces */
    private final List<StateMachineBackendAnalysis> stateMachineStateSystemAnalysisModules;
    private final List<OsExecutionGraph> osExecutionGraph;
    private final List<StateMachineVariableAnalysis> smvaList = new ArrayList<>();
    private final Set<StateMachineConstraintAdaptive> adaptiveConstraintSet = new HashSet<>();

    //TODO: probably create a class to manage that more cleanly ?
    //TODO: hashMap using the next waited events ? Or too much overhead ?
    /** List of the instances */
    private List<StateMachineInstance> instancesList = new ArrayList<>();

    /** List of the instances that can still receive events */
    private List<StateMachineInstance> openInstancesList = new ArrayList<>();

    /** Map of the instances per TID */
    private Map<Integer, List<StateMachineInstance>> instancesPerTid = new HashMap<>();

    // TODO: can be improved to contain also the context information
    /** Store the list of the events used by the model */
    private Set<String> modelEvents = new HashSet<>();

    /** Whether or not all the instances have to be considered valid for adaptive constraints */
    private final boolean allInstancesValid;

    /**
     * @param initialTransition the initial transition
     */
    public StateMachineInstanceGroup(StateMachineTransition initialTransition) {
        List<StateMachineTransition> initialTransitionList = new ArrayList<>();
        initialTransitionList.add(initialTransition);
        this.initialTransitions.put(initialTransition.getEventName(), initialTransitionList);

        this.stateMachineStateSystemAnalysisModules = null;
        this.osExecutionGraph = null;
        this.allInstancesValid = false;

        prepareModelEvents();
    }

    /**
     * @param initialTransition the initial transition
     * @param stateMachineBackendAnalysis the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules the critical path analysis modules
     */
    public StateMachineInstanceGroup(StateMachineTransition initialTransition, List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules) {
        List<StateMachineTransition> initialTransitionList = new ArrayList<>();
        initialTransitionList.add(initialTransition);
        this.initialTransitions.put(initialTransition.getEventName(), initialTransitionList);

        this.stateMachineStateSystemAnalysisModules = stateMachineBackendAnalysis;
        this.osExecutionGraph = lttngKernelExecutionGraphModules;
        this.allInstancesValid = false;

        prepareModelEvents();
    }

    /**
     * @param initialTransitions the list of initial transitions
     * @param stateMachineBackendAnalysis the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules the critical path analysis modules
     */
    public StateMachineInstanceGroup(List<StateMachineTransition> initialTransitions, List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules) {
        for (StateMachineTransition smt : initialTransitions) {
            List<StateMachineTransition> initialTransitionList = this.initialTransitions.get(smt.getEventName());
            if (initialTransitionList == null) {
                initialTransitionList = new ArrayList<>();
                this.initialTransitions.put(smt.getEventName(), initialTransitionList);
            }
            initialTransitionList.add(smt);
        }

        this.stateMachineStateSystemAnalysisModules = stateMachineBackendAnalysis;
        this.osExecutionGraph = lttngKernelExecutionGraphModules;
        this.allInstancesValid = false;

        prepareModelEvents();
    }

    /**
     * @param initialTransition the initial transition
     * @param allInstancesValid Whether or not all the instances have to be considered valid for adaptive constraints
     */
    public StateMachineInstanceGroup(StateMachineTransition initialTransition, boolean allInstancesValid) {
        List<StateMachineTransition> initialTransitionList = new ArrayList<>();
        initialTransitionList.add(initialTransition);
        this.initialTransitions.put(initialTransition.getEventName(), initialTransitionList);

        this.stateMachineStateSystemAnalysisModules = null;
        this.osExecutionGraph = null;
        this.allInstancesValid = allInstancesValid;

        prepareModelEvents();
    }

    /**
     * @param initialTransition the initial transition
     * @param stateMachineBackendAnalysis the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules the critical path analysis modules
     * @param allInstancesValid Whether or not all the instances have to be considered valid for adaptive constraints
     */
    public StateMachineInstanceGroup(StateMachineTransition initialTransition, List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules, boolean allInstancesValid) {
        List<StateMachineTransition> initialTransitionList = new ArrayList<>();
        initialTransitionList.add(initialTransition);
        this.initialTransitions.put(initialTransition.getEventName(), initialTransitionList);

        this.stateMachineStateSystemAnalysisModules = stateMachineBackendAnalysis;
        this.osExecutionGraph = lttngKernelExecutionGraphModules;
        this.allInstancesValid = allInstancesValid;

        prepareModelEvents();
    }

    /**
     * @param initialTransitions the list of initial transitions
     * @param stateMachineBackendAnalysis the state machine state system analysis modules
     * @param lttngKernelExecutionGraphModules the critical path analysis modules
     * @param allInstancesValid Whether or not all the instances have to be considered valid for adaptive constraints
     */
    public StateMachineInstanceGroup(List<StateMachineTransition> initialTransitions, List<StateMachineBackendAnalysis> stateMachineBackendAnalysis, List<OsExecutionGraph> lttngKernelExecutionGraphModules, boolean allInstancesValid) {
        for (StateMachineTransition smt : initialTransitions) {
            List<StateMachineTransition> initialTransitionList = this.initialTransitions.get(smt.getEventName());
            if (initialTransitionList == null) {
                initialTransitionList = new ArrayList<>();
                this.initialTransitions.put(smt.getEventName(), initialTransitionList);
            }
            initialTransitionList.add(smt);
        }

        this.stateMachineStateSystemAnalysisModules = stateMachineBackendAnalysis;
        this.osExecutionGraph = lttngKernelExecutionGraphModules;
        this.allInstancesValid = allInstancesValid;

        prepareModelEvents();
    }

    private void prepareModelEvents() {
        Set<StateMachineNode> seenNodes = new HashSet<>();

        ArrayList<StateMachineTransition> currentTransitions;
        ArrayList<StateMachineTransition> transitions = new ArrayList<>();
        for (List<StateMachineTransition> smtList : initialTransitions.values()) {
            transitions.addAll(smtList);
        }

        while (!transitions.isEmpty()) {
            currentTransitions = transitions;
            transitions = new ArrayList<>();

            for (StateMachineTransition smt : currentTransitions) {
                modelEvents.add(smt.getEventName());

                if (seenNodes.add(smt.getNextNode())) {
                    transitions.addAll(smt.getNextNode().getTransitions());
                }
            }
        }
    }

    /*private void prepareModelEventsNode() {
        ArrayList<StateMachineNode> nodesToProcess = new ArrayList<>();
        Set<StateMachineNode> seenNodes = new HashSet<>();

        for (StateMachineTransition smt : initialTransitions.values()) {
            modelEvents.add(smt.getEventName());
            nodesToProcess.add(smt.getNextNode());
        }

        while (!nodesToProcess.isEmpty()) {
            StateMachineNode smn = nodesToProcess.remove(nodesToProcess.size()-1);

            if (seenNodes.add(smn)) {
                for (StateMachineTransition smt : smn.getTransitions()) {
                    modelEvents.add(smt.getEventName());

                    if (!seenNodes.contains(smt.getNextNode())) {
                        nodesToProcess.add(smt.getNextNode());
                    }
                }
            }
        }
    }*/

    /**
     * Read all the events of an experiment and follow them to build the model
     * @param exp The experiment on which to build
     */
    public void buildOn(TmfExperiment exp) {
        ITmfContext ctx = exp.seekEvent(0);
        ITmfEvent event = exp.getNext(ctx);
        while (event != null) {
            this.receivedEvent(event);
            event = exp.getNext(ctx);
        }
        StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Treating adaptive constraints"); //$NON-NLS-1$
        this.finishedReceiving();
        benchmarkObject.stop();
    }

    /**
     * @return the initial transition
     */
    public Collection<StateMachineTransition> getInitialTransitions() {
        List<StateMachineTransition> listTransitions = new ArrayList<>();
        for (List<StateMachineTransition> list : initialTransitions.values()) {
            listTransitions.addAll(list);
        }
        return listTransitions;
    }

    /**
     * @return the instances
     */
    public List<StateMachineInstance> getInstances() {
        return instancesList;
    }

    /**
     * @param event the received event
     */
    public void receivedEvent(ITmfEvent event) {
        // Get the event name
        String eventName = event.getType().getName();

        // Exit if we don't need that event
        if (!modelEvents.contains(eventName)) {
            return;
        }

        // Get the event TID
        Integer eventTid = StateMachineInstance.getEventTid(event);

        // Using the event TID, we can reduce the number of instances we have to watch
        List<StateMachineInstance> instances = null;
        if (eventTid != null && instancesPerTid.containsKey(eventTid)) {
            instances = NonNullUtils.checkNotNull(instancesPerTid.get(eventTid));
        } else {
            instances = openInstancesList;
        }

        // Check if we have an instance that can use this one
        boolean hasBeenUsed = false;
        for (StateMachineInstance instance : instances) {
            hasBeenUsed = instance.receivedEvent(event);

            if(hasBeenUsed) {
                if (!instance.hasNextNode()) {
                    if (eventTid != null && instancesPerTid.containsKey(eventTid)) {
                        NonNullUtils.checkNotNull(instancesPerTid.get(eventTid)).remove(instance);
                    }
                    openInstancesList.remove(instance);
                }

                break;
            }
        }

        // Or, if it matches for the link to the first event, create a new instance
        if (!hasBeenUsed) {
            List<StateMachineTransition> transitionsList = initialTransitions.get(event.getType().getName());
            if (transitionsList != null) {
                for (StateMachineTransition smt : transitionsList) {
                    if (smt.matches(event)) {
                        StateMachineInstance smi = new StateMachineInstance(smt.getNextNode(), event, this);

                        // Add to the list of all instances
                        instancesList.add(smi);

                        // And only if it has a next node
                        if (smi.hasNextNode()) {
                            // Add to the list of open instances
                            openInstancesList.add(smi);

                            // Add to the fast get hashmap
                            if (eventTid != null) {
                                if (!instancesPerTid.containsKey(eventTid)) {
                                    instancesPerTid.put(eventTid, new ArrayList<StateMachineInstance>());
                                }

                                NonNullUtils.checkNotNull(instancesPerTid.get(eventTid)).add(smi);
                            }
                        }

                        break;
                    }
                }
            }
        }
    }

    /**
     * @param adaptiveConstraint The adaptive constraint to add to the set of adaptive constraints
     * @return Whether it was added to the set (true) or it was already in it (false)
     */
    public boolean addAdaptiveConstraint(StateMachineConstraintAdaptive adaptiveConstraint) {
        adaptiveConstraint.setAllInstancesValid(allInstancesValid);
        return adaptiveConstraintSet.add(adaptiveConstraint);
    }

    /**
     * To perform the operations to do when finishing receiving the data
     */
    public void finishedReceiving() {
        for (StateMachineConstraintAdaptive adaptiveConstraint : adaptiveConstraintSet) {
            adaptiveConstraint.revalidate();
        }
    }

    @Override
    public String toString() {
        StringBuilder path = new StringBuilder();
        int i = 0;
        for (StateMachineInstance smi : instancesList) {
            if (i > 0) {
                path.append("\n"); //$NON-NLS-1$
            } else {
                i++;
            }
            path.append(smi.toString());
        }
        return path.toString();
    }


    /**
     * @param kernelTraces the kernel traces
     */
    public void analyze(TmfExperiment kernelTraces) {
        StateMachineReport.R.println_section("STATE MACHINE ANALYSIS REPORT"); //$NON-NLS-1$

        // We need to find all the nodes for which we have a problem
        Map<StateMachineConstraint, ArrayList<InstanceStepInformation>> invalid = new HashMap<>();

        // We need to keep in mind the valid instances step too to be able to compare
        Map<StateMachineConstraint, ArrayList<InstanceStepInformation>> valid = new HashMap<>();

        StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("Split data"); //$NON-NLS-1$
        for (StateMachineInstance smi : instancesList) {
            // If the instance status is invalid
            //if (smi.getStatus() == Status.INVALID) {
                // Get the list of steps for which it was invalid
                List<InstanceStepInformation> listIsi = smi.getInstanceStepInformation();
                for (InstanceStepInformation isi : listIsi) {
                    //if (isi.stepStatus == Status.INVALID) {
                        for (Status cs : isi.stepConstraintsStatus) {
                            if (cs.getStatus() == StatusValue.INVALID) {
                                ArrayList<InstanceStepInformation> list = invalid.get(cs.getConstraint());
                                if (list == null) {
                                    list = new ArrayList<>();
                                    invalid.put(cs.getConstraint(), list);
                                }
                                list.add(isi);
                            } else if (cs.getStatus() == StatusValue.VALID) {
                                ArrayList<InstanceStepInformation> list = valid.get(cs.getConstraint());
                                if (list == null) {
                                    list = new ArrayList<>();
                                    valid.put(cs.getConstraint(), list);
                                }
                                list.add(isi);
                            }
                        }
                    //}
                }
            //}
        }
        benchmarkObject.stop();

        String Sconstraints = ""; //$NON-NLS-1$
        String Sinstances = ""; //$NON-NLS-1$
        if (invalid.keySet().size() > 1) {
            Sconstraints = "s"; //$NON-NLS-1$
        }
        if (instancesList.size() > 1) {
            Sinstances = "s"; //$NON-NLS-1$
        }
        StateMachineReport.R.println("\n" + //$NON-NLS-1$
                invalid.keySet().size() +
                " different constraint" + Sconstraints + //$NON-NLS-1$
                " with invalid status on " + //$NON-NLS-1$
                instancesList.size() +
                " different instance" + Sinstances //$NON-NLS-1$
                );
        if (invalid.keySet().size() > 1) {
            StateMachineReport.R.println("\n" + //$NON-NLS-1$
                    "Constraints will be treated separately." //$NON-NLS-1$
                    );
        }

        for (Entry<StateMachineConstraint, ArrayList<InstanceStepInformation>> entry : invalid.entrySet()) {
            StateMachineConstraint c = entry.getKey();
            List<InstanceStepInformation> invalidIsiList = entry.getValue();
            List<InstanceStepInformation> validIsiList = valid.get(entry.getKey());

            StateMachineReport.R.println();
            StateMachineReport.R.println_subsection(c.toString() + " on state '" + invalidIsiList.get(0).node.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            StateMachineReport.R.println("Number of invalid times: " + invalidIsiList.size()); //$NON-NLS-1$

            StateMachineVariable var = invalidIsiList.get(0).instance.getVariable(c.getVarName());
            StateMachineVariableAnalysis smva = var.analyze(c, invalidIsiList, validIsiList);
            if (smva != null) {
                smvaList.add(smva);
            }
        }
    }

    /**
     * @return the StateMachineStateSystemAnalysis modules
     */
    public List<StateMachineBackendAnalysis> getStateMachineBackendAnalysisModules() {
        return stateMachineStateSystemAnalysisModules;
    }

    /**
     * @return The list of critical path modules
     */
    public List<OsExecutionGraph> getLttngKernelExecutionGraphModules() {
        return osExecutionGraph;
    }

    /**
     * @return The segment store for that state machine instance group
     */
    public @NonNull ISegmentStore<@NonNull ISegment> getSegmentStore() {
        @NonNull ISegmentStore<@NonNull ISegment> segmentStore = new TreeMapStore<>();

        for (StateMachineVariableAnalysis smva : smvaList) {
            for (ISegment segment : smva.getInstanceConstraintsSegmentStore()) {
                if (segment instanceof StateMachineSegment) {
                    StateMachineSegment sms = (StateMachineSegment)segment;
                    boolean stored = false;

                    for (ISegment storedSegment : segmentStore) {
                        if (sms.matches(storedSegment)) {
                            ((StateMachineSegment)storedSegment).addInvalidConstraints(sms.getInvalidConstraintsList());
                            stored = true;
                            break;
                        }
                    }

                    if (stored) {
                        continue;
                    }
                }
                segmentStore.add(segment);
            }
            //segmentStore.addAll(smva.getSegmentStore());
        }

        return segmentStore;
    }

    /**
     * Clean up the adaptive constraints that couldn't be deduced
     */
    public void cleanUpAdaptive() {
        for (StateMachineInstance smi : instancesList) {
            smi.cleanUpAdaptive();
        }
    }

}
