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

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceCPUPageIterator;

/**
 * Contains validation data for unit tests for
 * {@link BinaryFTraceCPUPageIterator}
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceCPUPageIteratorTestData {

    private BinaryFTraceCPUPageIteratorTestData() {
        // Do nothing
    }

    // This data is extracted from CPU 0 - Page 0 from trace_4cpu.dat
    /**
     * The number of events in CPU 0 - Page 0
     */
    public final static int EVENT_COUNT = 19;
    /**
     * The default timestamp
     */
    public final static long DEFAULT_TS = 0;

    /**
     * The timestamp for the first event in the page
     */
    public final static long FIRST_EVENT_TS = 93083912657L;
    /**
     * The name for the first event in the page
     */
    public final static String FIRST_EVENT_NAME = "local_timer_entry"; //$NON-NLS-1$
    /**
     * The imprecise timestamp of an event in the middle of the page.
     */
    public final static long MID_PAGE_EVENT_IMPRECISE_TS = 93083917000L;
    /**
     * The precise timestamp of an event in the middle of the page.
     */
    public final static long MID_PAGE_EVENT_PRECISE_TS = 93083917143L;
    /**
     * The name for the first event in the page
     */
    public final static String MID_PAGE_EVENT_NAME = "rcu_utilization"; //$NON-NLS-1$
    /**
     * The timestamp of the event that is next to last
     */
    public final static long NEXT_TO_LAST_EVENT = 93083928867L;
    /**
     * The time stamp of the last event
     */
    public final static long LAST_EVENT_TS = 93083929247L;
    /**
     * The time stamp of the last event
     */
    public final static String LAST_EVENT_NAME = "cpu_idle"; //$NON-NLS-1$
    /**
     * An valid timestamp for this page
     */
    public final static long INVALID_TS = 93083930000L;

    // Extracted from absolute_ts.dat. This file contains a single event.
    /**
     * The timestamp of the only event in absolute_ts.dat
     */
    public final static long SINGLE_TRACE_EVENT_TS = 2235794234042L;
    /**
     * The name of the event in the single trace event
     */
    public final static String SINGLE_TRACE_EVENT_NAME = "sys_enter_eventfd2"; //$NON-NLS-1$
    /**
     * The count property of the event in the single trace event
     */
    public final static Long SINGLE_TRACE_EVENT_COUNT = 0L;
    /**
     * The flag property of the event in the single trace event
     */
    public final static Long SINGLE_TRACE_EVENT_FLAGS = 0L;
}
