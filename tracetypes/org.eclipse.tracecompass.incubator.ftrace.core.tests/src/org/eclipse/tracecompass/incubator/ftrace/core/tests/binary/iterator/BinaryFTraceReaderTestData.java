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
 * {@link BinaryFTraceReaderTest}. The file is a trace file obtained on a Ubuntu
 * machine with 4 cores by running trace-cmd record -e all ls. All events are
 * counted, including raw system events.
 *
 * Raw system events are sys_enter and sys_exit.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceReaderTestData {

    private BinaryFTraceReaderTestData() {
        // Do nothing
    }

    /**
     * The default timestamp
     */
    public final static long DEFAULT_TS = 0;

    /**
     * The timestamp of the first event in the trace
     */
    public final static long FIRST_TRACE_EVENT_TS = 93083211466L;
    /**
     * The name of the first event in the trace
     */
    public final static String FIRST_TRACE_EVENT_NAME = "sys_exit"; //$NON-NLS-1$

    /**
     * The timestamp of the second event in the trace
     */
    public final static long SECOND_TRACE_EVENT_TS = 93083211504L;
    /**
     * The name of the second event in the trace
     */
    public final static String SECOND_TRACE_EVENT_NAME = "sched_stat_runtime"; //$NON-NLS-1$

    /**
     * The imprecise timestamp of a random trace event in the middle of the
     * trace
     */
    public final static long MID_TRACE_EVENT_IMPRECISE_TS = 93084660000L;
    /**
     * The exact timestamp of a random trace event in the middle of the trace
     */
    public final static long MID_TRACE_EVENT_TS = 93084660518L;
    /**
     * The name of a random trace event in the middle of the trace
     */
    public final static String MID_TRACE_EVENT_NAME = "sched_switch"; //$NON-NLS-1$

    /**
     * The timestamp of the last trace in the trace
     */
    public final static long LAST_TRACE_EVENT_TS = 93084876467L;
    /**
     * The name of the last trace in the trace
     */
    public final static long LAST_TRACE_EVENT_NAME = 93084876467L;
}
