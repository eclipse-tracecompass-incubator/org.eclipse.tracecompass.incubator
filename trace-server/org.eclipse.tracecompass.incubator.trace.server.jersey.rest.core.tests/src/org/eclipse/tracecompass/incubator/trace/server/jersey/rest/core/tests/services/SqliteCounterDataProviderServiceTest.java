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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.XyApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.SeriesModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Trace;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntry;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntryModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeResponse;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test the {@link DataProviderService} XY endpoints with a SQLite trace and its
 * counter analysis.
 * <p>
 * This exercises the full server path for the SQLite trace type: the trace is
 * posted, added to an experiment, and the generic counter data provider
 * (fed by the SQLite counter analysis) is queried for its XY tree and series.
 * It guards against the SQLite trace-type plugin being missing from the trace
 * server product.
 *
 * @author Matthew Khouzam
 */
public class SqliteCounterDataProviderServiceTest extends RestServerTest {

    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;

    /** Generic counter data provider, fed by the SQLite counter analysis. */
    private static final String COUNTER_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.counters.core.CounterDataProvider";

    /** Bundled synthetic SQLite trace. */
    private static final String SQLITE_TRACE_RES = "traces/synthetic_trace.sqlite";
    private static final String SQLITE_TRACE_NAME = "synthetic_trace.sqlite";

    /** Bounds of the synthetic trace (22 events, deterministic timestamps). */
    private static final long SQLITE_START = 1721752204623337000L;
    private static final long SQLITE_END = 1721752204639108000L;

    private static final XyApi sfxyApi = new XyApi(sfApiClient);

    /**
     * Resolve the absolute path of the bundled synthetic SQLite trace.
     *
     * @return the OS path to the trace file
     */
    private static String sqliteTracePath() {
        Bundle bundle = Platform.getBundle("org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests");
        assertNotNull(bundle);
        URL url = FileLocator.find(bundle, new Path(SQLITE_TRACE_RES), null);
        assertNotNull("Bundled SQLite trace not found: " + SQLITE_TRACE_RES, url);
        try {
            return new Path(FileLocator.toFileURL(url).toURI().getPath()).toOSString();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Trace sqliteTraceStub() {
        return new Trace().name(SQLITE_TRACE_NAME).path(sqliteTracePath());
    }

    /**
     * Post the SQLite trace, create an experiment and verify that the counter
     * data provider returns a non-empty XY tree and at least one series with
     * data points.
     *
     * @throws InterruptedException
     *             if interrupted while waiting for the analysis
     * @throws ApiException
     *             if a server error occurs
     */
    @Test
    public void testSqliteCounterDataProvider() throws InterruptedException, ApiException {
        Experiment exp = assertPostExperiment(SQLITE_TRACE_NAME, sqliteTraceStub());

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(SQLITE_START).end(SQLITE_END));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse treeResponse = sfxyApi.getXYTree(exp.getUUID(), COUNTER_DATAPROVIDER_ID, queryParams);
        assertNotNull(treeResponse);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        XYTreeEntryModel responseModel = treeResponse.getModel();

        // Make sure the analysis ran long enough to produce a model.
        int iteration = 0;
        while (((treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.RUNNING)) || (responseModel == null)) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfxyApi.getXYTree(exp.getUUID(), COUNTER_DATAPROVIDER_ID, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Verify tree entries: the synthetic trace has numeric columns
        // (counter1/counter2/counter3) that must surface as counter entries.
        assertNotNull(responseModel);
        List<XYTreeEntry> entries = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG, entries);
        assertFalse("Expected counter tree entries for the SQLite trace", entries.isEmpty());

        boolean hasCounter = entries.stream()
                .map(XYTreeEntry::getLabels)
                .filter(labels -> labels != null && !labels.isEmpty())
                .flatMap(List::stream)
                .anyMatch(label -> label != null && label.startsWith("counter"));
        assertTrue("Expected at least one 'counterN' entry in the counter tree", hasCounter);

        // Request the series for all entries.
        List<Integer> items = new ArrayList<>();
        for (XYTreeEntry entry : entries) {
            items.add(entry.getId().intValue());
        }

        RequestedParameters reqParams = new RequestedParameters()
                .requestedTimerange(new TimeRange().start(SQLITE_START).end(SQLITE_END).nbTimes(10))
                .requestedItems(items);
        RequestedQueryParameters reqQueryParameters = new RequestedQueryParameters().parameters(reqParams);

        XYResponse xyModelResponse = sfxyApi.getXY(exp.getUUID(), COUNTER_DATAPROVIDER_ID, reqQueryParameters);
        assertNotNull(xyModelResponse);
        XYModel xyModel = xyModelResponse.getModel();
        assertNotNull(MODEL_NULL_MSG, xyModel);

        List<SeriesModel> xySeries = xyModel.getSeries();
        assertFalse("Expected at least one counter series", xySeries.isEmpty());
        SeriesModel series = xySeries.get(0);
        assertFalse("Expected x values in the counter series", series.getxValues().isEmpty());
    }

    /**
     * Verify the SQLite trace can be posted to the server at all, i.e. that the
     * SQLite trace type is available in the server product.
     */
    @Test
    public void testSqliteTracePost() {
        Trace stub = sqliteTraceStub();
        Trace trace = assertPost(stub);
        assertNotNull(trace);
        if (!SQLITE_TRACE_NAME.equals(trace.getName())) {
            fail("SQLite trace was not posted with the expected name: " + trace.getName());
        }
    }
}
