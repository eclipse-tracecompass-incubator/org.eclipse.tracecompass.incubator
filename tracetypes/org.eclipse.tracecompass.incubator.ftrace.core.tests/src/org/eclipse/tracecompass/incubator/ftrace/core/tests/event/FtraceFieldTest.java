/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

        assertEquals(7, field.getContent().getFields().size());
        assertEquals("sched_wakeup", field.getContent().getFieldValue(String.class, "name"));
        assertEquals("daemonsu", field.getContent().getFieldValue(String.class, "comm"));
        assertEquals((Long) 1L, field.getContent().getFieldValue(Long.class, "success"));
        assertEquals((Long) 16620L, field.getContent().getFieldValue(Long.class, "pid"));
        assertEquals((Long) 3210263482000L, field.getContent().getFieldValue(Long.class, "ts"));
        assertEquals((Long) 120L, field.getContent().getFieldValue(Long.class, "prio"));
        assertEquals((Long) 0L, field.getContent().getFieldValue(Long.class, "target_cpu"));
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
