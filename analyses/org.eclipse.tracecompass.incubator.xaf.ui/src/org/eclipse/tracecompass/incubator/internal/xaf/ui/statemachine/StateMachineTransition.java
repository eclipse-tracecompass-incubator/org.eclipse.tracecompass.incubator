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

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.constraint.StateMachineConstraint;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @author Raphaël Beamonte
 */
public class StateMachineTransition {

    private StateMachineNode nextNode;
    private String eventName;
    private Map<String, String> eventContexts;
    private List<StateMachineConstraint> fConstraints = new ArrayList<>();

    /**
     * @param nextNode the next node
     * @param eventName the event name
     */
    public StateMachineTransition(StateMachineNode nextNode, String eventName) {
        super();
        this.nextNode = nextNode;
        this.eventName = eventName;
        this.eventContexts = new HashMap<>();
    }

    /**
     * @param nextNode the next node
     * @param eventName the event name
     * @param eventContexts The contexts of the events
     */
    public StateMachineTransition(StateMachineNode nextNode, String eventName, Map<String, String> eventContexts) {
        super();
        this.nextNode = nextNode;
        this.eventName = eventName;
        this.eventContexts = eventContexts;
    }

    /**
     * @return the next node
     */
    public StateMachineNode getNextNode() {
        return nextNode;
    }

    /**
     * @param nextNode the next node
     */
    public void setNextNode(StateMachineNode nextNode) {
        this.nextNode = nextNode;
    }

    /**
     * @return the full event name with potential contexts
     */
    public String getFullEvent() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        joiner.setEmptyValue(""); //$NON-NLS-1$
        if (!eventContexts.isEmpty()) {
            for (Entry<String, String> entry : eventContexts.entrySet()) {
                joiner.add(String.format("%s=%s", //$NON-NLS-1$
                        entry.getKey(),
                        entry.getValue()));
            }

        }

        return String.format("%s%s", //$NON-NLS-1$
                eventName,
                joiner.toString());
    }

    /**
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * @param eventName the event name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * @return the event contexts
     */
    public Map<String, String> getEventContexts() {
        return eventContexts;
    }

    /**
     * @param index the index
     * @return the constraint
     */
    public StateMachineConstraint getConstraint(int index) {
        return fConstraints.get(index);
    }

    /**
     * @return the constraints
     */
    public List<StateMachineConstraint> getConstraints() {
        return fConstraints;
    }

    /**
     * @param constraint the constraint
     */
    public void addConstraint(StateMachineConstraint constraint) {
        this.fConstraints.add(constraint);
    }

    /**
     * @param constraints the constraints
     */
    public void addConstraints(List<StateMachineConstraint> constraints) {
        this.fConstraints.addAll(constraints);
    }

    /**
     * @param e The event
     * @return Whether or not the event matches the transition
     */
    public boolean matches(ITmfEvent e) {
        if (!e.getType().getName().equals(this.getEventName())) {
            return false;
        }

        if (this.getEventContexts() != null && this.getEventContexts().size() != 0) {
            ITmfEventField context = e.getContent();
            for (Entry<String, String> entry : this.getEventContexts().entrySet()) {
                ITmfEventField field = context.getField(entry.getKey());
                if (field == null || !field.getFormattedValue().equals(entry.getValue())) {
                    return false;
                }
            }
        }

        return true;
    }


}
