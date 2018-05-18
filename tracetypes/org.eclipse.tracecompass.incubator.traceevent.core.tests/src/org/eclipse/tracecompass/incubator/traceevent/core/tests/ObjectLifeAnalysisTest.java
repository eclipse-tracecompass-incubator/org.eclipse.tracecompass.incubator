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
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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
     *                               trace open failed
     */
    @Before
    public void setUp() throws TmfTraceException {
        TraceEventTrace trace = new TraceEventTrace();
        fTrace = trace;
        String path = "traces/object_alloc.json";
        fTrace.initTrace(null, path, ITmfEvent.class);
        deleteSuppFiles(fTrace);
        fTrace.readStart();
        fTrace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        ObjectLifeAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ObjectLifeAnalysis.class, ObjectLifeAnalysis.ID);
        assertNotNull(module);
        module.schedule();
        module.waitForCompletion();
        fStart = fTrace.getStartTime().toNanos();
        fEnd = fTrace.getEndTime().toNanos();
        fDataProvider = new ObjectLifeDataProvider(trace, module);
        assertNotNull(fDataProvider);
    }

    /**
     * Dispose the trace
     */
    @After
    public void after() {
        TraceEventTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    /**
     * Only test for object life.
     */
    @Test
    @SuppressWarnings("restriction")
    public void test() {
        TmfModelResponse<@NonNull List<@NonNull TimeGraphEntryModel>> tree = fDataProvider.fetchTree(new TimeQueryFilter(fStart, fEnd, 1000), new NullProgressMonitor());
        assertEquals(Status.COMPLETED, tree.getStatus());
        List<@NonNull TimeGraphEntryModel> treeModel = tree.getModel();
        assertNotNull(treeModel);

        TimeGraphEntryModel mufasaEntry = getValueEntry(treeModel, "Mufasa");
        TimeGraphEntryModel sarabiEntry = getValueEntry(treeModel, "Sarabi");
        TimeGraphEntryModel scarEntry = getValueEntry(treeModel, "Scar");
        TimeGraphEntryModel simbaEntry = getValueEntry(treeModel, "Simba");
        assertNotNull(mufasaEntry);
        assertNotNull(sarabiEntry);
        assertNotNull(scarEntry);
        assertNotNull(simbaEntry);
        List<@NonNull Long> items = Arrays.asList(mufasaEntry.getId(), sarabiEntry.getId(), scarEntry.getId(), simbaEntry.getId());
        /*
         * Remember, state system is in nanos, time range is known since we know the
         * trace
         */
        TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> resp = fDataProvider.fetchRowModel(new SelectionTimeQueryFilter(100000L, 300000L, 200, items), new NullProgressMonitor());
        assertEquals(Status.COMPLETED, resp.getStatus());
        List<@NonNull ITimeGraphRowModel> models = resp.getModel();
        assertNotNull(models);
        ImmutableMap<Long, @NonNull ITimeGraphRowModel> groupRowsById = Maps.uniqueIndex(models, ITimeGraphRowModel::getEntryID);
        ITimeGraphRowModel mufasaRow = groupRowsById.get(mufasaEntry.getId());
        assertNotNull(mufasaRow);
        assertEquals("", mufasaRow.getStates().get(0).getLabel());
        assertEquals(null, mufasaRow.getStates().get(1).getLabel());
        ITimeGraphRowModel sarabiRow = models.get(1);
        assertNotNull(sarabiRow);
        assertEquals(null, sarabiRow.getStates().get(0).getLabel());
        assertEquals("", sarabiRow.getStates().get(1).getLabel());
        assertEquals(null, sarabiRow.getStates().get(2).getLabel());
        ITimeGraphRowModel scarRow = models.get(2);
        assertNotNull(scarRow); // The bad guy
        assertEquals(null, scarRow.getStates().get(0).getLabel());
        assertEquals("", scarRow.getStates().get(1).getLabel());
        ITimeGraphRowModel simbaRow = models.get(3);
        assertNotNull(simbaRow);
        assertEquals(null, simbaRow.getStates().get(0).getLabel());
        assertEquals("", simbaRow.getStates().get(1).getLabel());
    }

    private static TimeGraphEntryModel getValueEntry(List<@NonNull TimeGraphEntryModel> treeModel, String name) {
        Long parentID = null;
        for (TimeGraphEntryModel entryModel : treeModel) {
            if (entryModel.getName().equals(name)) {
                parentID = entryModel.getId();
                break;
            }
        }
        if (parentID == null) {
            return null;
        }
        for (TimeGraphEntryModel entryModel : treeModel) {
            if (entryModel.getParentId() == parentID) {
                return entryModel;
            }
        }
        return null;
    }

}
