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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceField;
import org.junit.Test;

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
     * where the command name contains a space (comm=daemo su), check that the
     * parsed comm field value is the complete name, including the space.
     */
    @Test
    public void testParseEventWithCommPropertyWithSpace() {
        String line = "kworker/0:0-9514  [000] d..4  3210.263482: sched_wakeup: comm=daemo su pid=16620 prio=120 success=1 target_cpu=000";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals((Integer) 0, field.getCpu());
        assertEquals((Integer) 9514, field.getPid());
        assertEquals((Integer) 9514, field.getTid());
        assertEquals(3210263482000L, (long) field.getTs());
        assertEquals("sched_wakeup", field.getName());

        assertEquals(5, field.getContent().getFields().size());
        assertEquals("daemo su", field.getContent().getFieldValue(String.class, "comm"));
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
     * Testing of parse line with Irq_raise event function using line from an
     * ftrace output
     */
    @Test
    public void testParseIrqRaise() {
        String line = "ksoftirqd/1-12    [001] d.s1   387.212674: softirq_raise: vec=9 [action=RCU]";

        GenericFtraceField field = GenericFtraceField.parseLine(line);

        assertNotNull(field);
        assertEquals(2, field.getContent().getFields().size());
        assertEquals((Integer) 1, field.getCpu());
        assertEquals("softirq_raise", field.getName());

        assertEquals((Long) 9L, field.getContent().getFieldValue(Long.class, "vec"));
        assertEquals("RCU", field.getContent().getFieldValue(String.class, "action"));
    }

    /**
     * Testing of parse line with odd comm names
     */
    @Test
    public void testSpecialCharsInComm() {
        List<@NonNull ResultsParse> tests = List.of(new ResultsParse(
                "           <...>-919973  [019] ..... 40313.809636: sched_process_fork: comm=runc:[2:INIT] pid=919973 child_comm=runc:[2:INIT] child_pid=919974",
                19, "sched_process_fork", Objects.requireNonNull(Map.of("comm", "runc:[2:INIT]")), 4),
                new ResultsParse("ksoftirqd/16-112     [016] ..s.. 40318.937233: sched_process_free: comm=runc:[2:INIT] pid=920437 prio=120",
                        16, "sched_process_free", Objects.requireNonNull(Map.of("comm", "runc:[2:INIT]")), 3),
                new ResultsParse("<idle>-0       [021] ..s1. 40318.941173: sched_process_free: comm=runc:[0:PARENT] pid=920430 prio=120",
                        21, "sched_process_free", Objects.requireNonNull(Map.of("comm", "runc:[0:PARENT]")), 3),
                new ResultsParse("<idle>-0       [021] ..s1. 40318.941177: sched_process_free: comm=runc:[1:CHILD] pid=920431 prio=120",
                        21, "sched_process_free", Objects.requireNonNull(Map.of("comm", "runc:[1:CHILD]")), 3),
                new ResultsParse("kworker/0:0-9514  [000] d..4  3210.263482: sched_wakeup: comm=daemo su pid=16620 prio=120 success=1 target_cpu=000",
                        0, "sched_wakeup", Objects.requireNonNull(Map.of("comm", "daemo su")), 5));
        for (ResultsParse test : tests) {
            test.test();
        }
    }

    @NonNullByDefault
    private static class ResultsParse {

        private final String fInput;
        private final Integer fCpu;
        private final String fName;
        private Map<String, Object> fFields;
        private int fFieldCount;

        public ResultsParse(String input, Integer cpu, String name, Map<String, Object> fields, int fieldCount) {
            assertNotNull(input);
            assertNotNull(cpu);
            assertNotNull(name);
            assertNotNull(fields);
            assertNotNull(fieldCount);
            fInput = input;
            fCpu = cpu;
            fName = name;
            fFields = fields;
            fFieldCount = fieldCount;
        }

        public void test() {
            GenericFtraceField field = GenericFtraceField.parseLine(fInput);
            assertNotNull(fInput, field);
            assertEquals(fInput, fCpu, field.getCpu());
            assertEquals(fInput, fName, field.getName());
            assertEquals(fInput, fFieldCount, field.getContent().getFields().size());
            for (Entry<String, Object> currentField : fFields.entrySet()) {
                assertEquals(fInput + ' ' + currentField.getKey(), currentField.getValue(), field.getContent().getFieldValue(String.class, currentField.getKey()));
            }
        }
    }
}
