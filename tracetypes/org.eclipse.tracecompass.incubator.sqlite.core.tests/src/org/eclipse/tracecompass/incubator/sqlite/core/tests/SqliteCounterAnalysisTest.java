/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.sqlite.core.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.analysis.SqliteCounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.trace.SQLiteTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests for {@link SqliteCounterAnalysis}: numeric columns become counters.
 *
 * @author Matthew Khouzam
 */
public class SqliteCounterAnalysisTest {

    private static final String SAMPLE = "res/synthetic_trace.sqlite"; //$NON-NLS-1$

    private static String samplePath() {
        Bundle bundle = FrameworkUtil.getBundle(SqliteCounterAnalysisTest.class);
        assertNotNull(bundle);
        URL url = FileLocator.find(bundle, new Path(SAMPLE), null);
        assertNotNull("Sample trace not found", url); //$NON-NLS-1$
        try {
            return new Path(FileLocator.toFileURL(url).getPath()).toOSString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Run the counter analysis and check that numeric columns yield counter
     * attributes while text columns (time, flag) do not.
     *
     * @throws Exception
     *             on trace/analysis failure
     */
    @Test
    public void testNumericColumnsAreCounters() throws Exception {
        SQLiteTrace trace = new SQLiteTrace();
        SqliteCounterAnalysis analysis = new SqliteCounterAnalysis();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);
            analysis.setTrace(trace);
            assertTrue(analysis.schedule().isOK());
            assertTrue(analysis.waitForCompletion());

            ITmfStateSystem ss = analysis.getStateSystem();
            assertNotNull(ss);

            int ungrouped = ss.getQuarkAbsolute(CounterAnalysis.UNGROUPED_COUNTER_ASPECTS_ATTRIB);
            // There is at least one event-type sub-tree with counters.
            List<@org.eclipse.jdt.annotation.NonNull Integer> eventTypeQuarks = ss.getSubAttributes(ungrouped, false);
            assertTrue("Expected counter sub-trees", !eventTypeQuarks.isEmpty()); //$NON-NLS-1$

            // sensor.alpha has numeric 'counter1'/'counter2'/'counter3'
            // columns -> counters, and text 'time'/'flag' columns -> no
            // counter.
            int eventQuark = ss.getQuarkAbsolute(CounterAnalysis.UNGROUPED_COUNTER_ASPECTS_ATTRIB, "sensor.alpha"); //$NON-NLS-1$
            int numericQuark = ss.getQuarkRelative(eventQuark, "counter3"); //$NON-NLS-1$
            assertTrue("numeric column should be a counter", numericQuark >= 0); //$NON-NLS-1$

            assertNull("text column 'time' must not be a counter", quarkOrNull(ss, eventQuark, "time")); //$NON-NLS-1$ //$NON-NLS-2$
            assertNull("text column 'flag' must not be a counter", quarkOrNull(ss, eventQuark, "flag")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            analysis.dispose();
            trace.dispose();
        }
    }

    private static @org.eclipse.jdt.annotation.Nullable Integer quarkOrNull(ITmfStateSystem ss, int parent, String name) {
        try {
            return Integer.valueOf(ss.getQuarkRelative(parent, name));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }
}
