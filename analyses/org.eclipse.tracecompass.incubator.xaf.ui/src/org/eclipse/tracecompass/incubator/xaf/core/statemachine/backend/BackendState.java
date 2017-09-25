/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend;

/**
 * To represent a state
 * @author Raphaël Beamonte
 */
public class BackendState {

    // Type for the instance
    /** State type */
    public static final int TYPE_STATE = 0;
    /** SoftIRQ type */
    public static final int TYPE_SOFTIRQ = 1;
    /** IRQ type */
    public static final int TYPE_IRQ = 2;

    private long state = 0;

    /**
     * To create a new state of a given type with a given value
     * @param type The type of state
     * @param value The value of the state
     */
    public BackendState(int type, long value) {
        switch (type) {
        case TYPE_SOFTIRQ:
            state = value + BackendStateValue.SOFTIRQ_HI.getValue();
            break;
        case TYPE_IRQ:
            state = value + BackendStateValue.IRQ.getValue();
            break;
        case TYPE_STATE:
        default:
            state = value;
            break;
        }
    }

    /**
     * To create a new state with that value
     * @param value The value of the state
     */
    public BackendState(long value) {
        state = value;
    }

    /**
     * To create a new state with that value
     * @param backendStateValue The value of the state
     */
    public BackendState(BackendStateValue backendStateValue) {
        state = backendStateValue.getValue();
    }

    /**
     * To get the value of the current state
     * @return The state value
     */
    public long getValue() {
        return state;
    }

    /**
     * To get the name of the current state
     * @return The state name
     */
    public String getName() {
        String name = BackendStateValue.getValue(state).name();
        if (BackendStateValue.getValue(state) == BackendStateValue.IRQ) {
            name += Long.toString(state - BackendStateValue.IRQ.getValue());
        }
        return name;
    }

    /**
     * To get the print name of the current state
     * @param data The data to add after the print name
     * @return The state name
     */
    public String getPrintName(String data) {
        String name;
        BackendStateValue backendStateValue = BackendStateValue.getValue(state);
        switch (backendStateValue) {
        case BLOCKED:
            name = backendStateValue.name() + " on"; //$NON-NLS-1$
            break;
        case IRQ:
            name = "Interrupted by " + backendStateValue.name() + Long.toString(state - BackendStateValue.IRQ.getValue()); //$NON-NLS-1$
            break;
        case PREEMPTED:
            name = backendStateValue.name() + " by"; //$NON-NLS-1$
            break;
        case HRTIMER:
        case SOFTIRQ_BLOCK:
        case SOFTIRQ_BLOCK_IOPOLL:
        case SOFTIRQ_HI:
        case SOFTIRQ_HRTIMER:
        case SOFTIRQ_NET_RX:
        case SOFTIRQ_NET_TX:
        case SOFTIRQ_RCU:
        case SOFTIRQ_SCHED:
        case SOFTIRQ_TASKLET:
        case SOFTIRQ_TIMER:
            name = "Interrupted by " + backendStateValue.name(); //$NON-NLS-1$
            break;
        case RUNNING:
        case SYSCALL:
        case UNKNOWN:
        case WAKING:
        default:
            name = backendStateValue.name();
            break;
        }
        if (data != null) {
            name += " " + data; //$NON-NLS-1$
        }
        return name;
    }
}
