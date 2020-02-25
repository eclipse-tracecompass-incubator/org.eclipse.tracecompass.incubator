/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.event;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Constants for the ftrace format
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
@NonNullByDefault
public interface IGenericFtraceConstants {

    /** Process dump event name */
    String PROCESS_DUMP_EVENT_NAME = "ftrace_process_dump"; //$NON-NLS-1$

    /** Process dump event type */
    String PROCESS_DUMP_EVENT_TYPE = "ftrace_process_dump_event"; //$NON-NLS-1$

    /**
     * Character that identifies a comment when at the start of a line
     */
    String FTRACE_COMMENT_CHAR = "#"; //$NON-NLS-1$

    /**
     * Timestamp field name
     */
    String TIMESTAMP = "ts"; //$NON-NLS-1$
    /**
     * Name field name
     */
    String NAME = "name"; //$NON-NLS-1$
    /**
     * TID field name
     */
    String TID = "tid"; //$NON-NLS-1$
    /**
     * PID field name
     */
    String PID = "pid"; //$NON-NLS-1$
    /**
     * Pattern to match an ftrace event line
     */
    Pattern FTRACE_PATTERN = Pattern.compile(
    /**
     * Pattern to match an ftrace event preamble (thread, tid, timestamp), like:
     * kworker/u16:6-214   [002] d...   149.136514:
     */
    "^\\s*(?<comm>.*)-(?<pid>\\d+)(?:\\s+\\([^0-9]*(?<tgid>\\d+)?\\))?\\s+\\[(?<cpu>\\d+)\\](?:\\s+[^\\s]*)?\\s+(?<timestamp>[0-9]+(?<us>\\.[0-9]+)?): " + //$NON-NLS-1$
    /**
     * Pattern to match the rest of the event (event name, event fields), like:
     * sched_switch: prev_comm=kworker/u16:6 prev_pid=214 prev_prio=120 prev_state=R+ ==> next_comm=swapper/2 next_pid=0 next_prio=120
     */
    "(?<name>\\w+)(?<separator>:\\s+|\\(|\\s+->\\s+)(?<data>[^\\)]*)(\\))?" //$NON-NLS-1$
    );
    /**
     * comm group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_COMM_GROUP = "comm"; //$NON-NLS-1$
    /**
     * PID group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_PID_GROUP = "pid"; //$NON-NLS-1$
    /**
     * CPU group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_CPU_GROUP = "cpu"; //$NON-NLS-1$
    /**
     * Timestamp group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_TIMESTAMP_GROUP = "timestamp"; //$NON-NLS-1$
    /**
     * Name group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_NAME_GROUP = "name"; //$NON-NLS-1$
    /**
     * Data group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_DATA_GROUP = "data"; //$NON-NLS-1$
    /**
     * Data group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_SEPARATOR_GROUP = "separator"; //$NON-NLS-1$
    /**
     * TGID group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_TGID_GROUP = "tgid"; //$NON-NLS-1$
    /**
     * A syscall starts with this prefix, followed by syscall name.
     */
    String FTRACE_SYSCALL_PREFIX = "sys_"; //$NON-NLS-1$
    /**
     * A syscall enter from trace-cmd starts with this prefix, followed by the syscall name.
     */
    String FTRACE_SYSCALL_ENTER_TRACECMD_PREFIX = "sys_enter_"; //$NON-NLS-1$
    /**
     * A syscall exit from trace-cmd starts with this prefix, followed by the syscall name.
     */
    String FTRACE_SYSCALL_EXIT_TRACECMD_PREFIX = "sys_exit_"; //$NON-NLS-1$
    /**
     * Event name to mark the end of a previous recorded syscall.
     */
    String FTRACE_EXIT_SYSCALL = "exit_syscall"; //$NON-NLS-1$
    /**
     * Separates the syscall event name from the exit return value
     */
    String FTRACE_EXIT_SYSCALL_SEPARATOR = "->"; //$NON-NLS-1$
}
