/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendState;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendStateValue;

/**
 * Element representing a process State
 *
 * @author Raphaël Beamonte
 */
public class State extends InterruptionReason {
    private Long state;
    private String data;

    /**
     * Constructor
     */
    public State() {
        state = 0L;
        data = null;
    }

    /**
     * Constructor
     *
     * @param state The state to represent
     */
    public State(Long state) {
        this.state = state;
        data = null;
    }

    /**
     * Constructor
     *
     * @param backendState The state to represent
     */
    public State(BackendState backendState) {
        this.state = backendState.getValue();
        data = null;
    }

    /**
     * Constructor
     *
     * @param backendStateValue The state to represent
     */
    public State(BackendStateValue backendStateValue) {
        this.state = backendStateValue.getValue();
        data = null;
    }

    /**
     * Constructor
     *
     * @param state The state to represent
     * @param data The data to store along the state
     */
    public State(Long state, String data) {
        this.state = state;
        this.data = data;
    }

    /**
     * @return The backend state value corresponding to the state represented by this object
     */
    public BackendStateValue getValue() {
        return BackendStateValue.getValue(state);
    }

    @Override
    public String getID() {
        return new BackendState(state).getPrintName(data);
    }

    @Override
    public String getShortID() {
        return new BackendState(state).getName();
    }

    @Override
    public String getTaskId() {
        return data.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof State)) {
            return false;
        }

        State b = (State)o;
        return getID().equals(b.getID());
    }

    @Override
    public int hashCode() {
        int hash = 13;

        String id = getID();
        hash = 93 * hash + (id != null ? id.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return getID();
    }

    @Override
    public int compareTo(InterruptionReason ir) {
        if (ir instanceof State) {
            State s = (State)ir;
            if (state == null) {
                if (s.state == null) {
                    return 0;
                }
                return -1;
            }
            int cmp = state.compareTo(s.state);
            if (cmp == 0) {
                if (data == null) {
                    if (s.data == null) {
                        return 0;
                    }
                    return -1;
                }
                return data.compareTo(s.data);
            }
            return cmp;
        }
        return 0;
    }
}