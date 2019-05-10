/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.traceevent.core.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack.TraceEventCallstackAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.IntervalInfo;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.After;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the trace event callstack for various traces
 *
 * @author Geneviève Bastien
 */
public class CallStackStateProviderTest {

    private ITmfTrace fTrace;

    /**
     * Dispose the trace used in this test
     */
    @After
    public void disposeTrace() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    /**
     * Test the 'b' and 'e' events
     *
     * @throws Exception
     *             Exception thrown by initialization
     */
    @Test
    public void testEventsbe() throws Exception {
        TraceEventCallstackAnalysis csModule = getTraceEventModule("traces/nestable_async_annotation.json");
        try {
            ITmfStateSystem ss = csModule.getStateSystem();
            assertNotNull(ss);
            Set<@NonNull IntervalInfo> intervalInfos = new HashSet<>();
            // First process, request bar
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 99999, "REQUEST_BAR"),
                    new StateIntervalStub(100000, 200000, (Object) null)),
                    "Processes", "26037", "26065", "CallStack", "1"));
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 19999, (Object) null),
                    new StateIntervalStub(20000, 29999, "URL_REQUEST_DELEGATE"),
                    new StateIntervalStub(30000, 39999, (Object) null),
                    new StateIntervalStub(40000, 69999, "URL_REQUEST_START_JOB"),
                    new StateIntervalStub(70000, 79999, (Object) null),
                    new StateIntervalStub(80000, 99999, "URL_REQUEST_DELEGATE_UNMATCHED_BEGIN"),
                    new StateIntervalStub(100000, 200000, (Object) null)),
                    "Processes", "26037", "26065", "CallStack", "2"));
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 19999, (Object) null),
                    new StateIntervalStub(20000, 39999, (Object) null),
                    new StateIntervalStub(40000, 49999, (Object) null),
                    new StateIntervalStub(50000, 59999, "URL_REQUEST_INNER"),
                    new StateIntervalStub(60000, 79999, (Object) null),
                    new StateIntervalStub(80000, 200000, (Object) null)),
                    "Processes", "26037", "26065", "CallStack", "3"));

            // Second process, request foo
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 109999, (Object) null),
                    new StateIntervalStub(110000, 199999, "REQUEST_FOO"),
                    new StateIntervalStub(200000, 200000, (Object) null)),
                    "Processes", "26037", "26066", "CallStack", "1"));
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 109999, (Object) null),
                    new StateIntervalStub(110000, 129999, "URL_REQUEST_DELEGATE_UNMATCHED_END"),
                    new StateIntervalStub(130000, 139999, (Object) null),
                    new StateIntervalStub(140000, 169999, "URL_REQUEST_START_JOB"),
                    new StateIntervalStub(170000, 179999, (Object) null),
                    new StateIntervalStub(180000, 189999, "URL_REQUEST_DELEGATE"),
                    new StateIntervalStub(190000, 200000, (Object) null)),
                    "Processes", "26037", "26066", "CallStack", "2"));
            intervalInfos.add(new IntervalInfo(ImmutableList.of(new StateIntervalStub(10000, 139999, (Object) null),
                    new StateIntervalStub(140000, 149999, (Object) null),
                    new StateIntervalStub(150000, 159999, "URL_REQUEST_INNER"),
                    new StateIntervalStub(160000, 179999, (Object) null),
                    new StateIntervalStub(180000, 200000, (Object) null)),
                    "Processes", "26037", "26066", "CallStack", "3"));
            StateSystemTestUtils.testIntervals(ss, intervalInfos);
        } finally {
            csModule.dispose();
        }
    }

    private @NonNull TraceEventCallstackAnalysis getTraceEventModule(String path) throws Exception {
        TraceEventTrace trace = new TraceEventTrace();
        TraceEventCallstackAnalysis csModule = null;
        try {
            IStatus validate = trace.validate(null, path);
            assertTrue(validate.getMessage(), validate.isOK());
            trace.initTrace(null, path, ITmfEvent.class);
            csModule = new TraceEventCallstackAnalysis();
            assertTrue(csModule.setTrace(trace));
            assertTrue(TmfTestHelper.executeAnalysis(csModule));
            fTrace = trace;

            return csModule;
        } catch (TmfTraceException | TmfAnalysisException e) {
            trace.dispose();
            if (csModule != null) {
                csModule.dispose();
            }
            fTrace = null;
            throw e;
        }

    }

}
