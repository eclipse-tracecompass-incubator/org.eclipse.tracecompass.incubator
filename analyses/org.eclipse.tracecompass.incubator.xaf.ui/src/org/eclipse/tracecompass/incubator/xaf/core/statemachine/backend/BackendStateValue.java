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
 * To represent the state values
 * @author Raphaël Beamonte
 */
public enum BackendStateValue {
    // When UNKNOWN
    /** When unknown */
    UNKNOWN(-1L),
    // Basic states
    /** When the process is running */
    RUNNING(0L),
    /** When the process is preempted */
    PREEMPTED(1L),
    /** When the process is blocked */
    BLOCKED(2L),
    /** When the process is waking */
    WAKING(3L),
    /** When the process is in a syscall */
    SYSCALL(4L),
    /** When the process is in an hrtimer */
    HRTIMER(5L),
    // Soft IRQs
    /** When the process is executing a softirq HI */
    SOFTIRQ_HI(1000L),
    /** When the process is executing a softirq TIMER */
    SOFTIRQ_TIMER(1001L),
    /** When the process is executing a softirq NET_TX */
    SOFTIRQ_NET_TX(1002L),
    /** When the process is executing a softirq NET_RX */
    SOFTIRQ_NET_RX(1003L),
    /** When the process is executing a softirq BLOCK */
    SOFTIRQ_BLOCK(1004L),
    /** When the process is executing a softirq BLOCK_IOPOLL */
    SOFTIRQ_BLOCK_IOPOLL(1005L),
    /** When the process is executing a softirq TASKLET */
    SOFTIRQ_TASKLET(1006L),
    /** When the process is executing a softirq SCHED */
    SOFTIRQ_SCHED(1007L),
    /** When the process is executing a softirq HRTIMER */
    SOFTIRQ_HRTIMER(1008L),
    /** When the process is executing a softirq RCU */
    SOFTIRQ_RCU(1009L),
    // IRQs
    /** When the process is executing an IRQ */
    IRQ(5000L);

    private final long value;
    private BackendStateValue(long value) {
        this.value = value;
    }
    /**
     * To get the value of the state
     * @return The value of the state
     */
    public long getValue() {
        return value;
    }

    /**
     * To get a value object from a value number (long)
     * @param value The long representing the value
     * @return The value object that corresponds, or null if none corresponds
     */
    public static BackendStateValue getValue(long value) {
        if (value > IRQ.value) {
            return IRQ;
        }

        for (BackendStateValue v : BackendStateValue.values()) {
            if (v.getValue() == value) {
                return v;
            }
        }

        return null;
    }

    /**
     * To get a value object from a value name (String)
     * @param value The string representing the name of the value
     * @return The value object that corresponds, or null if none corresponds
     */
    public static BackendStateValue getValue(String value) {
        //return Value.valueOf(value);
        for (BackendStateValue v : BackendStateValue.values()) {
            if (v.name().equals(value)) {
                return v;
            }
        }

        return null;
    }
}