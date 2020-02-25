/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.traceevent.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context.ContextAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context.ContextDataProvider;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context.ContextDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Test bookmark generators.
 *
 * @author Matthew Khouzam
 */
public class ContextTest {

    /**
     * Test context generation (bookmarks)
     *
     * @throws TmfTraceException
     *             should not happen
     * @throws TmfAnalysisException
     *             should not happen
     */
    @Test
    public void testContexts() throws TmfTraceException, TmfAnalysisException {
        String path = "traces/bing-truncated.json";
        IAnalysisModule analysis = new ContextAnalysis();
        ITmfTrace trace = new TraceEventTrace() {
            @Override
            public @NonNull Iterable<IAnalysisModule> getAnalysisModules() {
                return Iterables.concat(super.getAnalysisModules(), Collections.singleton(analysis));
            }
        };
        analysis.setTrace(trace);
        try {
            trace.initTrace(null, path, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            trace.getNext(context);
            analysis.setId(ContextAnalysis.ID);
            analysis.schedule();
            analysis.waitForCompletion();
            ContextDataProviderFactory factory = new ContextDataProviderFactory();
            ContextDataProvider provider = factory.createProvider(trace);
            /*
             * Do we have a provider?
             */
            assertNotNull(provider);
            TimeQueryFilter filter = new TimeQueryFilter(trace.getStartTime().toNanos(), trace.getEndTime().toNanos(), 1000);
            TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> tree = provider.fetchTree(FetchParametersUtils.timeQueryToMap(filter), new NullProgressMonitor());
            assertEquals(tree.getStatus(), TmfModelResponse.Status.COMPLETED);
            TmfTreeModel<@NonNull TimeGraphEntryModel> model = tree.getModel();
            /*
             * Does the query have the right data?
             */
            assertNotNull(model);
            TimeGraphEntryModel rootEntry = model.getEntries().get(0);
            assertEquals("blink", rootEntry.getName());
            SelectionTimeQueryFilter selectionFilter = new SelectionTimeQueryFilter(trace.getStartTime().toNanos(), trace.getEndTime().toNanos(), 1000, Collections.singleton(rootEntry.getId()));
            TmfModelResponse<@NonNull TimeGraphModel> rowModel = provider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(selectionFilter), new NullProgressMonitor());
            /*
             * Does the second query have the bookmarks?
             */
            assertNotNull(rowModel);
            TimeGraphModel markerList = rowModel.getModel();
            assertNotNull(markerList);
            assertFalse(markerList.getRows().isEmpty());
            List<@NonNull ITimeGraphState> bookmarks = markerList.getRows().get(0).getStates();
            assertEquals(3, bookmarks.size());
            assertEquals(Collections.singleton("FrameBlameContext"), Sets.newHashSet(Lists.transform(bookmarks, bookmark -> bookmark.getLabel())));
        } finally {
            trace.dispose();
        }
    }

}
