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
 * The attribute names that are used in the state provider
 * @author Raphaël Beamonte
 */
public class Attributes {

    /** For CPU frequency of each CPU */
    public static final String CPU_FREQ = "cpu:frequency"; //$NON-NLS-1$


    /** Tid */
    public static final String TID = "tid"; //$NON-NLS-1$

    /** CPU Usage */
    public static final String TIMER_CPU_USAGE = "timer:cpu_usage"; //$NON-NLS-1$

    /** Wait blocked */
    public static final String TIMER_WAIT_BLOCKED = "timer:wait_blocked"; //$NON-NLS-1$

    /** Wait for CPU */
    public static final String TIMER_WAIT_FOR_CPU = "timer:wait_for_cpu"; //$NON-NLS-1$

    /** Wake-up latency */
    public static final String TIMER_WAKEUP_LATENCY = "timer:wakeup_latency"; //$NON-NLS-1$

    /** Syscalls latency */
    public static final String TIMER_SYSCALLS_LATENCY = "timer:syscalls_latency"; //$NON-NLS-1$

    /** sched_pi_setprio */
    public static final String TIMER_SCHED_PI = "timer:sched_pi"; //$NON-NLS-1$

    /** Syscalls */
    public static final String COUNTER_SYSCALLS = "counter:syscalls"; //$NON-NLS-1$

    /** Syscalls */
    public static final String COUNTER_PREEMPT = "counter:preempt"; //$NON-NLS-1$

    /** For CPU used by each process */
    public static final String CPU = "cpu"; //$NON-NLS-1$

    /** For PRIO of each process */
    public static final String PRIO = "prio"; //$NON-NLS-1$

    /** States */
    public static final String STATE = "state"; //$NON-NLS-1$
}