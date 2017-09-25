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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelThreadInformationProvider;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.Status;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StatusValue;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.base.Joiner;

/**
 * @author Raphaël Beamonte
 */
public class StateMachineInstance implements Cloneable {

    private StateMachineNode currentNode;

    /**
     * Represents the information of a step of an instance. A step of an
     * instance is the status of the instance at a given node in the model.
     * @author Raphaël Beamonte
     */
    public class InstanceStepInformation {
        /** The state machine instance in which this step is */
        public StateMachineInstance instance = StateMachineInstance.this;
        /** The state machine node for which this step is */
        public StateMachineNode node = null;
        /** The state machine event at which this step enters the node */
        public ITmfEvent event = null;
        /** The status of the instance, according to its constraints, at this step */
        public StatusValue stepStatus = null;
        /** The list of the constraint status for each constraints at this step */
        public List<Status> stepConstraintsStatus = null;
        /** The map of last initialization step for each variables of this instance */
        public Map<String, Integer> variablesLastInit = new HashMap<>();

        @Override
        public InstanceStepInformation clone() {
            instance = StateMachineInstance.this;
            InstanceStepInformation isi = new InstanceStepInformation();
            isi.node = node;
            isi.event = event;
            isi.stepStatus = stepStatus;
            isi.stepConstraintsStatus.addAll(isi.stepConstraintsStatus);
            isi.variablesLastInit.putAll(isi.variablesLastInit);

            return isi;
        }

        /**
         * Clean up the adaptive constraints that couldn't be deduced
         */
        public void cleanUpAdaptive() {
            Iterator<Status> it = stepConstraintsStatus.iterator();
            while (it.hasNext()) {
                Status cs = it.next();
                if (!cs.getConstraint().canBeUsed()) {
                    it.remove();
                }
            }
        }

        /**
         * @param variableName The name of the variable for which to return the interval
         * @return The interval of time for which we computed the given variable until the current step
         */
        public TimestampInterval getVariableInterval(String variableName) {
            InstanceStepInformation initIsi = instance.getInstanceStepInformation(NonNullUtils.checkNotNull(variablesLastInit.get(variableName)));

            // Timestamp of the start of the interval
            ITmfTimestamp intervalStart = initIsi.event.getTimestamp();

            // Timestamp of the end of the interval
            ITmfTimestamp intervalEnd = event.getTimestamp();

            return new TimestampInterval(intervalStart, intervalEnd);
        }
    }

    private List<InstanceStepInformation> isiList = new ArrayList<>();
    private StatusValue statusValue = StatusValue.VALID;
    private Map<String,StateMachineVariable> currentVariables = new HashMap<>();
    private Map<String,Integer> stepInitVariables = new HashMap<>();

    private Integer vtid = null;
    private StateMachineInstanceGroup stateMachineInstanceGroup;

    /**
     * @param initialNode the initial node
     * @param initialEvent the initial event
     * @param stateMachineInstanceGroup the state machine instance group
     */
    public StateMachineInstance(StateMachineNode initialNode, ITmfEvent initialEvent, StateMachineInstanceGroup stateMachineInstanceGroup) {
        this.stateMachineInstanceGroup = stateMachineInstanceGroup;
        setCurrentNode(initialNode, initialEvent);
        vtid = getEventTid(initialEvent, stateMachineInstanceGroup.getStateMachineBackendAnalysisModules());
    }

    /**
     * @param instance the original instance to copy
     */
    public StateMachineInstance(StateMachineInstance instance) {
        vtid = instance.vtid;
        currentNode = instance.currentNode;
        for (InstanceStepInformation isi : instance.isiList) {
            isiList.add(isi.clone());
        }
        stateMachineInstanceGroup = instance.stateMachineInstanceGroup;
        statusValue = instance.statusValue;
        currentVariables.putAll(instance.currentVariables);
        stepInitVariables.putAll(instance.stepInitVariables);
    }

    /**
     * @param ti The timestamp interval
     * @return All the steps that where met during the given time interval
     */
    public List<InstanceStepInformation> getStepsInInterval(TimestampInterval ti) {
        List<InstanceStepInformation> isiIntvl = new ArrayList<>();

        for (InstanceStepInformation isi : isiList) {
            if (isi.event.getTimestamp().compareTo(ti.getEndTime()) > 0) {
                break;
            } else if (isi.event.getTimestamp().compareTo(ti.getStartTime()) < 0) {
                continue;
            }
            isiIntvl.add(isi);
        }

        return isiIntvl;
    }

    /**
     * @param event The event for which to get the TID
     * @return The TID
     */
    public static Integer getEventTid(ITmfEvent event) {
        return getEventTid(event, null);
    }

    /**
     * @param event The event for which to get the TID
     * @param smssaList The list of StateMAchineBackendAnalysisModule that we could use to get the TID
     * @return The TID
     */
    public static Integer getEventTid(ITmfEvent event, List<StateMachineBackendAnalysis> smssaList) {
        Integer tid = null;

        final String vtidContext = "context._vtid"; //$NON-NLS-1$
        if (event.getContent().getFieldNames().contains(vtidContext)) {
            tid = Integer.parseInt(event.getContent().getField(vtidContext).getFormattedValue());

        } else if (smssaList != null) {
            Integer cpu = (Integer)TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
            if (cpu != null) {
                long ts = event.getTimestamp().getValue();

                for (StateMachineBackendAnalysis smssa : smssaList) {
                    ITmfTrace kernelTrace = smssa.getKernelTrace();
                    if (kernelTrace.getStartTime().getValue() > ts || kernelTrace.getEndTime().getValue() < ts) {
                        continue;
                    }

                    KernelAnalysisModule kernelAnalysisModule = TmfTraceUtils.getAnalysisModuleOfClass(kernelTrace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
                    if (kernelAnalysisModule != null) {
                        Integer eventTid = KernelThreadInformationProvider.getThreadOnCpu(kernelAnalysisModule, cpu, ts);
                        if (eventTid != null) {
                            tid = eventTid;
                            break;
                        }
                    }
                }
            }
        }

        return tid;
    }

    /**
     * @return the vtid
     */
    public Integer getVTid() {
        return vtid;
    }

    /**
     * @return the state machine instance group
     */
    public StateMachineInstanceGroup getStateMachineInstanceGroup() {
        return stateMachineInstanceGroup;
    }

    /**
     * @return the currentNode
     */
    public StateMachineNode getCurrentNode() {
        return currentNode;
    }

    /**
     * @param nextNode the next node
     * @param event the event
     */
    private void setCurrentNode(StateMachineNode nextNode, ITmfEvent event) {
        currentNode = nextNode;

        InstanceStepInformation isi = new InstanceStepInformation();
        isiList.add(isi);

        isi.node = nextNode;
        isi.event = event;

        // Constraints verification
        List<Status> listIsValid = new ArrayList<>();
        StatusValue isValid = StatusValue.VALID;
        if (isiList.size() > 1) {
            List<StateMachineConstraint> constraints = isiList.get(isiList.size()-2).node.getTransition(event).getConstraints();
            for (StateMachineConstraint c : constraints) {
                Status cs = c.verify(currentVariables, event, this);
                listIsValid.add(cs);
                isValid = StateMachineConstraint.worstStatus(isValid, cs.getStatus());

                //TODO: Stop when invalid? Not for a thorough report... add an option?
                /*if (isValid == StateMachineConstraint.Status.INVALID) {
                    break;
                }*/
            }
        }
        statusValue = StateMachineConstraint.worstStatus(statusValue, isValid);
        isi.stepConstraintsStatus = listIsValid;
        isi.stepStatus = isValid;
        isi.variablesLastInit.putAll(stepInitVariables);

        // Updating local variables
        for (StateMachineVariable nodeVar : nextNode.getVariables()) {
            StateMachineVariable v = currentVariables.get(nodeVar.getName());

            Comparable<?> value = nodeVar.getInitValue(event);

            if (v == null) {
                v = nodeVar.getCopy();
                v.setValue(value);
                currentVariables.put(v.getName(), v);
            } else {
                v.setValue(value);
            }

            // Update the step we're at
            stepInitVariables.put(v.getName(), (isiList.size() - 1));
        }
    }

    /**
     * @return Whether or not the current instance has a next node
     */
    public boolean hasNextNode() {
        return !currentNode.getTransitions().isEmpty();
    }

    /**
     * @param event the received event
     * @return Whether this instance has used the event or not
     */

    public boolean receivedEvent(ITmfEvent event) {
        StateMachineTransition transition = currentNode.getTransition(event);
        if (transition != null) {
            //Integer tid = getTid(event);
            if (vtid != null && !getEventTid(event).equals(vtid)) {
                return false;
            }

            setCurrentNode(transition.getNextNode(), event);

            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder path = new StringBuilder();
        if (vtid != null) {
            path.append("Instance TID: "); //$NON-NLS-1$
            path.append(vtid);
            path.append("\n"); //$NON-NLS-1$
        }
        for (InstanceStepInformation isi : isiList) {
            path.append("Received "); //$NON-NLS-1$
            path.append(isi.event.getType().getName());
            ITmfEventField content = isi.event.getContent();
            if (content != null) {
                path.append("["); //$NON-NLS-1$
                path.append(Joiner.on(", ").join( //$NON-NLS-1$
                        content.getFields().stream()
                            .sorted(new Comparator<ITmfEventField>() {
                                @Override
                                public int compare(ITmfEventField field0, ITmfEventField field1) {
                                    if (field0.getName().startsWith("_") && !field1.getName().startsWith("_")) { //$NON-NLS-1$ //$NON-NLS-2$
                                        return 1;
                                    } else if (!field0.getName().startsWith("_") && field1.getName().startsWith("_")) { //$NON-NLS-1$ //$NON-NLS-2$
                                        return -1;
                                    }
                                    return field0.getName().compareTo(field1.getName());
                                }
                            })
                            .filter(f -> f != null)
                            .filter(f -> !NonNullUtils.checkNotNull(f).getName().startsWith("context.")) //$NON-NLS-1$
                            .map(f -> String.format("%s=%s", //$NON-NLS-1$
                                    NonNullUtils.checkNotNull(f).getName(),
                                    NonNullUtils.checkNotNull(f).getValue().toString()
                                ))
                            .collect(Collectors.toList())));
                path.append("]"); //$NON-NLS-1$
            }
            path.append(" at "); //$NON-NLS-1$
            path.append(isi.event.getTimestamp());
            /*if (i > 0) {
                path.append("("); //$NON-NLS-1$
                path.append(usedPathEvents.get(i).getTimestamp().getDelta(usedPathEvents.get(i-1).getTimestamp()));
                path.append(" since previous)"); //$NON-NLS-1$
            }*/
            path.append("\n\tEntering state: "); //$NON-NLS-1$
            path.append(isi.node.getName());
            if (!isi.node.getVariables().isEmpty()) {
                path.append("\n\tVariables:"); //$NON-NLS-1$
                for (StateMachineVariable v : isi.node.getVariables()) {
                    path.append("\n\t\t - "); //$NON-NLS-1$
                    path.append(v.getName());
                    path.append(" = "); //$NON-NLS-1$
                    path.append(v.getValue());
                }
            }
            if (!isi.stepConstraintsStatus.isEmpty()) {
                path.append("\n\tConstraints:"); //$NON-NLS-1$
                for (Status cs : isi.stepConstraintsStatus) {
                    path.append("\n\t\t - "); //$NON-NLS-1$

                    path.append(cs.getConstraintLeftOperand());
                    path.append(" "); //$NON-NLS-1$
                    path.append(cs.getConstraintOperator());
                    path.append(" "); //$NON-NLS-1$
                    path.append(cs.getConstraintRightOperand());

                    path.append(" "); //$NON-NLS-1$

                    path.append("["); //$NON-NLS-1$
                    path.append(cs.getStatus().toString());
                    path.append("]"); //$NON-NLS-1$
                    if (cs.getStatus() == StatusValue.INVALID) {
                        path.append(" value: "); //$NON-NLS-1$
                        path.append(cs.getActualLeftValue().toString());
                    }
                }
            }
            path.append("\n"); //$NON-NLS-1$
        }
        return path.toString();
    }

    /**
     * @return the step status list
     */
    public List<StatusValue> getStepStatus() {
        List<StatusValue> stepStatusList = new ArrayList<>();

        for (InstanceStepInformation isi : isiList) {
            stepStatusList.add(isi.stepStatus);
        }

        return stepStatusList;
    }

    /**
     * @param nodeIndex the index of the node
     * @return the step status of that node
     */
    public StatusValue getStepStatus(int nodeIndex) {
        return isiList.get(nodeIndex).stepStatus;
    }

    /**
     * @return the constraintsValidations
     */
    public List<List<Status>> getStepConstraintsStatus() {
        List<List<Status>> stepConstraintsStatusList = new ArrayList<>();

        for (InstanceStepInformation isi : isiList) {
            stepConstraintsStatusList.add(isi.stepConstraintsStatus);
        }

        return stepConstraintsStatusList;
    }

    /**
     * @param nodeIndex the index of the node
     * @return the constraintsValidations of that node
     */
    public List<Status> getStepConstraintsStatus(int nodeIndex) {
        return isiList.get(nodeIndex).stepConstraintsStatus;
    }

    /**
     * @param nodeIndex the index of the node
     * @param constraintIndex the index of the constraint
     * @return the constraintsValidations of that node
     */
    public Status getStepConstraintsStatus(int nodeIndex, int constraintIndex) {
        return isiList.get(nodeIndex).stepConstraintsStatus.get(constraintIndex);
    }

    /**
     * @return the status
     */
    public StatusValue getStatus() {
        return statusValue;
    }

    /**
     * @return the number of nodes
     */
    public int getNumNodes() {
        return isiList.size();
    }

    /**
     * @param index the index of the node
     * @return the node
     */
    public StateMachineNode getNode(int index) {
        return isiList.get(index).node;
    }

    /**
     * @param index the index of the event
     * @return the event
     */
    public ITmfEvent getEvent(int index) {
        return isiList.get(index).event;
    }

    /**
     * @param index The index of the instance step information to get
     * @return The instance step information
     */
    public InstanceStepInformation getInstanceStepInformation(int index) {
        return isiList.get(index);
    }

    /**
     * @return The list of instance step information
     */
    public List<InstanceStepInformation> getInstanceStepInformation() {
        return isiList;
    }

    /**
     * @param name The name of the variable
     * @return The variable
     */
    public StateMachineVariable getVariable(String name) {
        return currentVariables.get(name);
    }

    /**
     * Allows to verify that the instance given as parameter and the
     * current instance followed the same chain of nodes, provoked by
     * the same chain of events
     * @param smi the instance
     * @return true if it is comparable
     */
    public boolean isComparable(StateMachineInstance smi) {
        if (isiList.size() != smi.isiList.size()) {
            return false;
        }
        for (int i = 0; i < isiList.size(); i++) {
            if (isiList.get(i).node != smi.isiList.get(i).node
                    || isiList.get(i).event.getType().getName() != smi.isiList.get(i).event.getType().getName()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clean up the adaptive constraints that couldn't be deduced
     */
    public void cleanUpAdaptive() {
        for (InstanceStepInformation isi : isiList) {
            isi.cleanUpAdaptive();
        }
    }

}
