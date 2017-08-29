/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.perf.profiling.core.tests.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ISamplingDataProvider;
import org.eclipse.tracecompass.incubator.internal.perf.profiling.core.callgraph.PerfCallchainAnalysisModule;
import org.eclipse.tracecompass.incubator.perf.profiling.core.tests.ActivatorTest;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the perf callchain analysis
 *
 * @author Geneviève Bastien
 */
public class PerfCallGraphTest {

    private static final String PERF_TRACE_FILE = "testfiles/traces/perf_trace.xml";

    private ITmfTrace fTrace;
    private PerfCallchainAnalysisModule fModule;

    /**
     * Setup the trace for the tests
     */
    @Before
    public void setUp() {
        TmfXmlTraceStub trace = new TmfXmlTraceStubNs();
        IPath filePath = ActivatorTest.getAbsoluteFilePath(PERF_TRACE_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        fTrace = trace;
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));

        PerfCallchainAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, PerfCallchainAnalysisModule.class, PerfCallchainAnalysisModule.ID);
        assertNotNull(module);

        module.schedule();
        assertTrue(module.waitForCompletion());
        fModule = module;
    }

    /**
     * Dispose of the test data
     */
    @After
    public void tearDown() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
        PerfCallchainAnalysisModule module = fModule;
        if (module != null) {
            module.dispose();
        }
    }

    /**
     * Test the {@link ISamplingDataProvider#getSamplingData(int, long, long)}
     * method of the perf callchain analysis
     */
    @Test
    public void testSamplingDataProvider() {
        PerfCallchainAnalysisModule module = fModule;
        assertNotNull(module);

        Collection<AggregatedCallSite> samplingData = module.getSamplingData(2, 0, 50);
        assertFalse(samplingData.isEmpty());
        assertEquals(2, samplingData.size());

        // Validate symbol at the beginning of the trace
        samplingData = module.getSamplingData(2, 0, 5);
        assertFalse(samplingData.isEmpty());
        assertEquals(1, samplingData.size());

        AggregatedCallSite callsite = samplingData.iterator().next();
        assertEquals("0x1", callsite.getSymbol().resolve(Collections.emptySet()));

        // Validate sampling data towards the end
        samplingData = module.getSamplingData(2, 12, 20);
        assertFalse(samplingData.isEmpty());
        assertEquals(1, samplingData.size());

        callsite = samplingData.iterator().next();
        assertEquals("0x4", callsite.getSymbol().resolve(Collections.emptySet()));
    }

}
