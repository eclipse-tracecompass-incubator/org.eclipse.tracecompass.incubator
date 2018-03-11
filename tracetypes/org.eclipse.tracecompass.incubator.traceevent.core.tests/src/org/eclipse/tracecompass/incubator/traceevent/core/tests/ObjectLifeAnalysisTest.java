/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.traceevent.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife.ObjectLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife.ObjectLifeDataProvider;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Object Life Analysis test
 *
 * @author Matthew Khouzam
 */
public class ObjectLifeAnalysisTest {

    ObjectLifeDataProvider fDataProvider = null;
    private long fEnd;
    private long fStart;
    private TraceEventTrace fTrace;

    private static void deleteSuppFiles(@NonNull ITmfTrace trace) {
        /* Remove supplementary files */
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        for (File file : suppDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Setup the trace for the tests
     *
     * @throws TmfTraceException
     *             trace open failed
     */
    @Before
    public void setUp() throws TmfTraceException {
        fTrace = new TraceEventTrace();
        String path = "traces/object_alloc.json";
        fTrace.initTrace(null, path, ITmfEvent.class);
        deleteSuppFiles(fTrace);
        fTrace.readStart();
        ((TmfTrace) fTrace).traceOpened(new TmfTraceOpenedSignal(this, fTrace, null));

        IAnalysisModule module = null;
        for (IAnalysisModule mod : TmfTraceUtils.getAnalysisModulesOfClass(fTrace, ObjectLifeAnalysis.class)) {
            module = mod;
        }
        assertNotNull(module);
        module.schedule();
        module.waitForCompletion();
        /* End of the FIXME block */
        fStart = fTrace.getStartTime().toNanos();
        fEnd = fTrace.getEndTime().toNanos();
        fDataProvider = new ObjectLifeDataProvider(fTrace, (ObjectLifeAnalysis) module);
        assertNotNull(fDataProvider);
    }

    @After
    public void after() {
        if (fTrace == null) {
            fTrace.dispose();
        }
    }

    /**
     * Only test for object life.
     */
    @Test
    @SuppressWarnings("restriction")
    public void test() {
        TmfModelResponse<@NonNull List<@NonNull TimeGraphEntryModel>> tree = fDataProvider.fetchTree(new TimeQueryFilter(fStart, fEnd, 1000), new NullProgressMonitor());
        List<@NonNull TimeGraphEntryModel> treeModel = tree.getModel();
        assertNotNull(treeModel);
        List<@NonNull Long> items = Arrays.asList(2L, 4L, 6L, 8L);
        assertEquals("Mufasa", treeModel.get(1).getName());
        assertEquals("Sarabi", treeModel.get(3).getName());
        assertEquals("Scar", treeModel.get(5).getName());
        assertEquals("Simba", treeModel.get(7).getName());
        // remember, state system is in nanos
        TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> resp = fDataProvider.fetchRowModel(new SelectionTimeQueryFilter(100000L, 300000L, 200, items), new NullProgressMonitor());
        List<@NonNull ITimeGraphRowModel> models = resp.getModel();
        assertNotNull(models);
        assertEquals("", models.get(0).getStates().get(0).getLabel());
        assertEquals(null, models.get(0).getStates().get(1).getLabel());
        assertEquals(null, models.get(1).getStates().get(0).getLabel());
        assertEquals("", models.get(1).getStates().get(1).getLabel());
        assertEquals(null, models.get(1).getStates().get(2).getLabel());
        assertEquals(null, models.get(2).getStates().get(0).getLabel());
        assertEquals("", models.get(2).getStates().get(1).getLabel());
        assertEquals(null, models.get(3).getStates().get(0).getLabel());
        assertEquals("", models.get(3).getStates().get(1).getLabel());
    }
}
