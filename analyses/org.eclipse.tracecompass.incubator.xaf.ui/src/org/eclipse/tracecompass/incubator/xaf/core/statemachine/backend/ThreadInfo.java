/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * To compute the thread information to put in the
 * state system
 * @author Raphaël Beamonte
 */
class ThreadInfo {
    public Long cumul_cpu_usage = 0L;
    public Long cumul_wait_blocked = 0L;
    public Long cumul_wait_for_cpu = 0L;
    public Long last_state = null;
    public Long last_ts = null;

    public Long cumul_wakeup_latency = 0L;
    public Long last_wakeup = null;

    public Integer counter_syscalls = 0;
    public Integer counter_preempt = 0;

    public Long sched_pi_cumul = 0L;
    public Long sched_pi_lastTs = null;
    //public Long sched_pi_toTid = null;
    public List<Long> sched_pi_fromTids = new LinkedList<>();

    public Integer cpu_last = null;
    public Integer prio_last = null;

    Deque<BackendState> stack_state = new ArrayDeque<>();
}