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

package org.eclipse.tracecompass.incubator.ftrace.core.tests.event;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * FtraceField test class
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class FtraceFieldTest {

    /**
     * Testing of parse line with function using line from an ftrace output
     */
    @Test
    public void testParseSchedWakeupLine() {
        String line = "kworker/0:0-9514  [000] d..4  3210.263482: sched_wakeup: comm=daemonsu pid=16620 prio=120 success=1 target_cpu=000";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 0, field.getCpu());
        assertEquals((Integer) 9514, field.getPid());
        assertEquals((Integer) 9514, field.getTid());
        assertEquals(3210263482000L, (long) field.getTs());
        assertEquals("sched_wakeup", field.getName());

        assertEquals(5, field.getContent().getFields().size());
        assertEquals("daemonsu", field.getContent().getFieldValue(String.class, "comm"));
        assertEquals((Long) 1L, field.getContent().getFieldValue(Long.class, "success"));
        assertEquals((Long) 16620L, field.getContent().getFieldValue(Long.class, "pid"));
        assertEquals((Long) 120L, field.getContent().getFieldValue(Long.class, "prio"));
        assertEquals((Long) 0L, field.getContent().getFieldValue(Long.class, "target_cpu"));
    }

    /**
     * Testing of parse line with function using line from an ftrace output
     */
    @Test
    public void testParseSysCallEnterFTrace() {
        String line = "test/1-1316  [005] .......   713.920983: sys_recvmsg(fd: 3, msg: 7ffe3bd38070, flags: 0)";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 5, field.getCpu());
        assertEquals((Integer) 1316, field.getPid());
        assertEquals((Integer) 1316, field.getTid());
        assertEquals(713920983000L, (long) field.getTs());
        assertEquals("sys_recvmsg", field.getName());

        assertEquals(3, field.getContent().getFields().size());
        assertEquals((Long) 3L, field.getContent().getFieldValue(Long.class, "fd"));
        // OK this is still a string! to be done ...
        assertEquals("7ffe3bd38070", field.getContent().getFieldValue(String.class, "msg"));
        assertEquals((Long) 0L, field.getContent().getFieldValue(Long.class, "flags"));
    }

    /**
     * Testing of parse line with function using line from an ftrace output
     */
    @Test
    public void testParseSysCallExitFTrace() {
        String line = "test/1-1316  [005] ....   713.920988: sys_recvmsg -> 0xfffffffffffffff5";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 5, field.getCpu());
        assertEquals((Integer) 1316, field.getPid());
        assertEquals((Integer) 1316, field.getTid());
        assertEquals(713920988000L, (long) field.getTs());
        assertEquals("exit_syscall", field.getName());

        assertEquals(1, field.getContent().getFields().size());
        assertEquals((Long) (-11L), field.getContent().getFieldValue(Long.class, "ret"));
    }

    /**
     * Testing of parse line with function using line from an ftrace output
     */
    @Test
    public void testParseSysCallEnterTraceCmd() {
        String line = "test-1-1316  [005]   713.920983: sys_enter_recvmsg:     __syscall_nr=47 fd=3 msg=0x7ffe3bd38070 flags=0";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 5, field.getCpu());
        assertEquals((Integer) 1316, field.getPid());
        assertEquals((Integer) 1316, field.getTid());
        assertEquals(713920983000L, (long) field.getTs());
        assertEquals("sys_recvmsg", field.getName());

        assertEquals(4, field.getContent().getFields().size());
        assertEquals((Long) 47L, field.getContent().getFieldValue(Long.class, "__syscall_nr"));
        assertEquals((Long) 3L, field.getContent().getFieldValue(Long.class, "fd"));
        assertEquals((Long) 0x7ffe3bd38070L, field.getContent().getFieldValue(Long.class, "msg"));
        assertEquals((Long) 0L, field.getContent().getFieldValue(Long.class, "flags"));
    }

    /**
     * Testing of parse line with function using line from an ftrace output
     */
    @Test
    public void testParseSysCallExitTraceCmd() {
        String line = "test-1-1316  [005]   713.920988: sys_exit_recvmsg:      __syscall_nr=47 ret=-11";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 5, field.getCpu());
        assertEquals((Integer) 1316, field.getPid());
        assertEquals((Integer) 1316, field.getTid());
        assertEquals(713920988000L, (long) field.getTs());
        assertEquals("exit_syscall", field.getName());

        assertEquals(2, field.getContent().getFields().size());
        assertEquals((Long) 47L, field.getContent().getFieldValue(Long.class, "__syscall_nr"));
        assertEquals((Long) 0xfffffffffffffff5L, field.getContent().getFieldValue(Long.class, "ret"));
    }

    /**
     * Testing of parse line with Irq_raise event function using line from an ftrace
     * output
     */
    @Test
    public void testParseIrqRaise() {
        String line = "ksoftirqd/1-12    [001] d.s1   387.212674: softirq_raise: vec=9 [action=RCU]";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 1, field.getCpu());
        assertEquals("softirq_raise", field.getName());

        assertEquals((Long) 9L, field.getContent().getFieldValue(Long.class, "vec"));
        assertEquals("RCU", field.getContent().getFieldValue(String.class, "action"));
    }
}
