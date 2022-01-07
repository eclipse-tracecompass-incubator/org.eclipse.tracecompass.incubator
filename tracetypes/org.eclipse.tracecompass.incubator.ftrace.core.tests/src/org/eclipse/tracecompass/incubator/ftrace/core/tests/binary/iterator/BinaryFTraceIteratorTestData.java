/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.iterator;

/**
 * A class containing test data for the trace_4cpu.dat for
 * {@link BinaryFTraceIteratorTest}. The file is a trace file obtained on a
 * Ubuntu machine with 4 cores by running trace-cmd record -e all ls. Only
 * non-raw system events should be counted. Thus, the first event is actually
 * the first non-raw system event in the trace, and may not not be the actual
 * first trace events.
 *
 * Raw system events are sys_enter and sys_exit.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceIteratorTestData {

    private BinaryFTraceIteratorTestData() {
        // Do nothing
    }

    /**
     * The default timestamp
     */
    public static final long DEFAULT_TS = 0;

    /**
     * The timestamp of the first event in the trace
     */
    public static final long FIRST_TRACE_EVENT_TS = 93083211504L;
    /**
     * The name of the first event in the trace
     */
    public static final String FIRST_TRACE_EVENT_NAME = "sched_stat_runtime"; //$NON-NLS-1$

    /**
     * The timestamp of the second event in the trace
     */
    public static final long SECOND_TRACE_EVENT_TS = 93083212042L;
    /**
     * The name of the second event in the trace
     */
    public static final String SECOND_TRACE_EVENT_NAME = "exit_syscall"; //$NON-NLS-1$

    /**
     * The imprecise timestamp of a random trace event in the middle of the
     * trace
     */
    public static final long MID_TRACE_EVENT_IMPRECISE_TS = 93084660000L;
    /**
     * The exact timestamp of a random trace event in the middle of the trace
     */
    public static final long MID_TRACE_EVENT_TS = 93084660518L;
    /**
     * The name of a random trace event in the middle of the trace
     */
    public static final String MID_TRACE_EVENT_NAME = "sched_switch"; //$NON-NLS-1$

    /**
     * The timestamp of the last trace in the trace
     */
    public static final long LAST_TRACE_EVENT_TS = 93084876467L;
    /**
     * The name of the last trace in the trace
     */
    public static final long LAST_TRACE_EVENT_NAME = 93084876467L;
}
