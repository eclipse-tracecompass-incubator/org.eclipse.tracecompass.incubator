/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineInstance.InstanceStepInformation;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.exception.StateMachineVariableException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Class that represents a state machine variable
 *
 * @author Raphaël Beamonte
 */
public abstract class StateMachineVariable implements Comparable<StateMachineVariable> {

    // public static final HashMap<String, Class<?>> VARIABLE_TYPES = new
    // HashMap<>();
    /** Map of the available variable types */
    public static final Map<String, Class<?>> VARIABLE_TYPES = createMapTypes();

    private static Map<String, Class<?>> createMapTypes() {
        HashMap<String, Class<?>> map = new HashMap<>();
        map.put("deadline", StateMachineVariableDeadline.class); //$NON-NLS-1$
        map.put("cputime", StateMachineVariableTimerCpuTime.class); //$NON-NLS-1$
        // map.put("waitblocked", StateMachineVariableTimerWaitBlocked.class);
        // //$NON-NLS-1$
        map.put("preempt", StateMachineVariableCounterPreempt.class); //$NON-NLS-1$
        map.put("syscalls", StateMachineVariableCounterSyscalls.class); //$NON-NLS-1$
        return Collections.unmodifiableMap(map);
    }

    private String name;
    @SuppressWarnings("rawtypes")
    private Comparable fValue;

    /**
     * @param name
     *            the name
     * @param value
     *            the value
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariable(String name, Comparable value) {
        super();
        this.name = name;
        this.fValue = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the raw value
     */
    @SuppressWarnings("rawtypes")
    public Comparable getValue() {
        return fValue;
    }

    /**
     * @param format
     *            the value to get the format from
     * @param event
     *            the event which we're at
     * @param stateMachineInstance
     *            The state machine for which to get the value
     * @return the numerical value
     */
    @SuppressWarnings("rawtypes")
    public abstract Double getNumericalValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance);

    /**
     * @param format
     *            the value to get the format from
     * @param event
     *            the event which we're at
     * @param stateMachineInstance
     *            The state machine for which to get the value
     * @return the formatted value
     */
    @SuppressWarnings("rawtypes")
    public abstract String getFormattedValue(Comparable format, ITmfEvent event, StateMachineInstance stateMachineInstance);

    /**
     * @param format
     *            the value to get the format from
     * @param value
     *            the value to format
     * @return the formatted value
     */
    @SuppressWarnings("rawtypes")
    public abstract String getFormattedValue(Comparable format, Double value);

    /**
     * @param event
     *            the event which we're at
     * @param stateMachineInstance
     *            The state machine for which to get the value
     * @return the value depending on the event
     * @throws StateMachineVariableException
     *             exception happening when the variable can't be computed
     */
    @SuppressWarnings("rawtypes")
    public abstract Comparable getValue(ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException;

    /**
     * @param event
     *            the event which we're at
     * @return the value to set depending on the timestamp
     */
    @SuppressWarnings("rawtypes")
    public Comparable getInitValue(ITmfEvent event) {
        return event.getTimestamp();
    }

    /**
     * @param value
     *            the value
     */
    @SuppressWarnings("rawtypes")
    public void setValue(Comparable value) {
        this.fValue = value;
    }

    @Override
    public int compareTo(StateMachineVariable v) {
        return name.compareTo(v.name);
    }

    /**
     * @param toCompare
     *            the value to compare to
     * @param event
     *            the event received in case it is needed
     * @param stateMachineInstance
     *            the state machine instance
     * @return the comparison value
     * @throws StateMachineVariableException
     *             exception happening when the variable can't be computed
     */
    @SuppressWarnings({ "rawtypes", "unchecked", "unused" })
    public Integer compareTo(Comparable toCompare, ITmfEvent event, StateMachineInstance stateMachineInstance) throws StateMachineVariableException {
        return fValue.compareTo(toCompare);
    }

    /**
     * Analyze the reason why a constraint was not valid
     *
     * @param constraint
     *            The constraint that was not satisfied
     * @param invalidIsiList
     *            the list of InstanceStepInformation objects corresponding to
     *            steps where the constraint was invalid
     * @param validIsiList
     *            the list of InstanceStepInformation objects corresponding to
     *            steps where the constraint was valid
     * @return the StateMachineVariableAnalysis
     */
    public abstract StateMachineVariableAnalysis analyze(StateMachineConstraint constraint, List<InstanceStepInformation> invalidIsiList, List<InstanceStepInformation> validIsiList);

    @Override
    public String toString() {
        StringBuilder path = new StringBuilder();
        path.append(name);
        path.append(" = "); //$NON-NLS-1$
        path.append(fValue);
        return path.toString();
    }

    /**
     * @return a copy of the current variable
     */
    public abstract StateMachineVariable getCopy();

    /**
     * @return The name of the current variable
     */
    public String getVariableType() {
        return this.getClass()
                .getSimpleName()
                .substring(StateMachineVariableTimer.class.getName().length())
                .toLowerCase();
    }
}
