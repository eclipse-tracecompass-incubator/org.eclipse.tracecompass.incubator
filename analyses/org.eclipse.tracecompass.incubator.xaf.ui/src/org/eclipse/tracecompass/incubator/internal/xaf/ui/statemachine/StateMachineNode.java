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
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.StateMachineVariable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * @author Raphaël Beamonte
 */
public class StateMachineNode {

    private String name = "Unnamed node"; //$NON-NLS-1$
    private List<StateMachineVariable> fVariables = new ArrayList<>();
    private Map<String, List<StateMachineTransition>> transitions = new HashMap<>();

    /**
     *
     */
    public StateMachineNode() {
        super();
    }

    /**
     * @param name the name
     */
    public StateMachineNode(String name) {
        super();
        this.name = name;
    }

    /**
     * @param name the name
     * @param variables the variables
     * @param transitionsList the list of transitions
     */
    public StateMachineNode(String name, List<StateMachineVariable> variables, List<StateMachineTransition> transitionsList) {
        super();
        this.name = name;
        fVariables = variables;
        addTransitions(transitionsList);
    }

    /**
     * @param variables the variables
     * @param transitionsList the list of transitions
     */
    public StateMachineNode(List<StateMachineVariable> variables, List<StateMachineTransition> transitionsList) {
        super();
        fVariables = variables;
        addTransitions(transitionsList);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the variables
     */
    public List<StateMachineVariable> getVariables() {
        return fVariables;
    }

    /**
     * @param variables the variables to set
     */
    public void setVariables(List<StateMachineVariable> variables) {
        this.fVariables = variables;
    }

    /**
     * @param variable the variable to add
     */
    public void addVariable(StateMachineVariable variable) {
        fVariables.add(variable);
    }

    /**
     * @param variables the variables to add
     */
    public void addVariables(List<StateMachineVariable> variables) {
        fVariables.addAll(variables);
    }

    /**
     * @return the transitions
     */
    public Collection<StateMachineTransition> getTransitions() {
        List<StateMachineTransition> listTransitions = new ArrayList<>();
        for (List<StateMachineTransition> list : transitions.values()) {
            listTransitions.addAll(list);
        }
        return listTransitions;
    }

    /**
     * @param eventName The event name
     * @return the list of transitions matching the event name or null if not found
     */
    public List<StateMachineTransition> getTransitions(String eventName) {
        return transitions.get(eventName);
    }

    /**
     * @param event The event
     * @return the transition matching the event or null if not found
     */
    public StateMachineTransition getTransition(ITmfEvent event) {
        List<StateMachineTransition> transitionsList = transitions.get(event.getType().getName());
        if (transitionsList != null) {
            for (StateMachineTransition smt : transitionsList) {
                if (smt.matches(event)) {
                    return smt;
                }
            }
        }
        return null;
    }

    /**
     * @param transition the transition to add
     */
    public void addTransition(StateMachineTransition transition) {
        List<StateMachineTransition> list = transitions.get(transition.getEventName());
        if (list == null) {
            list = new ArrayList<>();
            transitions.put(transition.getEventName(), list);
        }
        list.add(transition);
    }

    /**
     * @param transitionsList the list of transitions to add
     */
    public void addTransitions(List<StateMachineTransition> transitionsList) {
        for (StateMachineTransition smt : transitionsList) {
            List<StateMachineTransition> list = transitions.get(smt.getEventName());
            if (list == null) {
                list = new ArrayList<>();
                transitions.put(smt.getEventName(), list);
            }
            list.add(smt);
        }
    }



}
