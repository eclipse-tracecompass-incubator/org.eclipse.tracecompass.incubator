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
 * Data obtained from CPU 2 from the Binary FTrace with 4 CPUs (trace_4cpu.dat).
 * It is used for {@link BinaryFTraceCPUSectionIteratorTest}.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceCPUSectionIteratorTestData {

    private BinaryFTraceCPUSectionIteratorTestData() {
        // Do nothing
    }

    /**
     * The CPU number of this CPU section. Should be 2.
     */
    public final static int CPU_NUMBER = 2;

    /**
     * The default time stamp
     */
    public final static long DEFAULT_TS = 0;
    /**
     * The timestamp of the first event in the CPU section
     */

    // PAGE 0
    public final static long PAGE0_FIRST_EVENT_TS = 93083942254L;
    /**
     * The imprecise timestamp of an event in the middle of this section
     */
    public final static String PAGE0_FIRST_EVENT_NAME = "local_timer_entry"; //$NON-NLS-1$

    /**
     * The timestamp of the event at the end of the first page
     */
    public final static long PAGE0_LAST_EVENT_TS = 93084692716L;
    /**
     * The imprecise timestamp of an event in the middle of this section
     */
    public final static String PAGE0_LAST_EVENT_NAME = "sys_exit_read"; //$NON-NLS-1$

    // PAGE 1
    /**
     * The timestamp of the second page in this CPU section
     */
    public final static long PAGE1_TS = 93084693295L;
    /**
     * The name of the first event on page 1 (the second page)
     */
    public final static String PAGE1_FIRST_EVENT_TS = "sys_enter"; //$NON-NLS-1$
    /**
     * The timestamp of an event in the middle of page 1 in this section
     */
    public final static long MID_PAGE1_EVENT_TS = 93084697041L;
    /**
     * The name of an event in the middle of page 1 in this section
     */
    public final static String MID_PAGE1_EVENT_NAME = "irq_work_entry"; //$NON-NLS-1$

    // IN GENERAL
    /**
     * The imprecise timestamp of an event in the middle of this section
     */
    public final static long MID_SECTION_IMPRECISE_TS_CPU2 = 93084660000L;
    /**
     * The precise timestamp of an event in the middle of this section
     */
    public final static long MID_SECTION_PRECISE_TS_CPU2 = 93084660518L;
    /**
     * The name of an event in the middle of the section
     */
    public final static String MID_SECTION_EVENT_NAME = "sched_switch"; //$NON-NLS-1$
    /**
     * The timestamp of the next to last event in this CPU section
     */
    public final static long NEXT_TO_LAST_TS = 93084745421L;
    /**
     * An invalid timestamp in this CPU section (more than the last event)
     */
    public final static long INVALID_TS = 93084747000L;
}
