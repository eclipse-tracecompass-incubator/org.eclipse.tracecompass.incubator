/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.XyApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.SeriesModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeColumnHeader;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntry;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntryModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeResponse;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} with focus on XY endpoints
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
public class XyDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String XY_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.CpuUsageDataProvider";
    private static final String XY_HISTOGRAM_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.tmf.core.histogram.HistogramDataProvider";

    private static final  List<TreeColumnHeader> EXPECTED_XY_TREE_HEADERS = List.of(
            new TreeColumnHeader().name("Process").tooltip(""),
            new TreeColumnHeader().name("TID").tooltip(""),
            new TreeColumnHeader().name("%").tooltip(""),
            new TreeColumnHeader().name("Time").tooltip(""));

    /**
     * XY API
     */
    private static final XyApi sfxyApi = new XyApi(sfApiClient);

    /**
     * Ensure that an XY data provider exists and returns correct data. It does
     * not test the data itself, simply that the serialized fields are the
     * expected ones according to the protocol.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurred
     */
    @Test
    public void testXYDataProvider() throws InterruptedException, ApiException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        Experiment exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(start).end(end));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse treeResponse = sfxyApi.getXYTree(exp.getUUID(), XY_DATAPROVIDER_ID, queryParams);

        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        XYTreeEntryModel responseModel = treeResponse.getModel();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfxyApi.getXYTree(exp.getUUID(), XY_DATAPROVIDER_ID, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Verify tree model
        assertNotNull(responseModel);
        List<TreeColumnHeader> headers = responseModel.getHeaders();
        assertNotNull(headers);
        assertEquals(EXPECTED_XY_TREE_HEADERS.size(), headers.size());
        // Verify tree headers
        for (int i = 0; i < headers.size(); i++ ) {
            TreeColumnHeader header = headers.get(i);
            TreeColumnHeader expHeader = EXPECTED_XY_TREE_HEADERS.get(i);
            assertTrue(expHeader.getName().equals(header.getName()) && expHeader.getTooltip().equals(header.getTooltip()));
        }

        // Verify Entries
        List<XYTreeEntry> entries = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG, entries);
        assertFalse(entries.isEmpty());

        // Test getting the XY series endpoint
        List<Integer> items = new ArrayList<>();
        for (XYTreeEntry entry : entries) {
            items.add(entry.getId().intValue()); // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
        }

        RequestedParameters reqParams = new RequestedParameters()
                .requestedTimerange(new TimeRange().start(start).end(end).nbTimes(10))
                .requestedItems(items);
        RequestedQueryParameters reqQueryParameters = new RequestedQueryParameters().parameters(reqParams);

        /*
         * FIXME: Remove try/catch after fixing issue:
         * https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/issues/236
         */
        try {
            XYResponse xyModelResponse = sfxyApi.getXY(exp.getUUID(), XY_DATAPROVIDER_ID, reqQueryParameters);
            assertNotNull(xyModelResponse);

            XYModel xyModel = xyModelResponse.getModel();
            List<SeriesModel> xySeries = xyModel.getSeries();
            assertFalse(xySeries.isEmpty());
        } catch (Exception ex) {

        }
    }

    /**
     * Verify that Histogram Data Provider fetchTree() interface and verify that
     * the serialized fields are the expected ones according to the protocol.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurred
     */
    @Test
    public void testHistogramDataProvider() throws InterruptedException, ApiException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        Experiment exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(start).end(end));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse treeResponse = sfxyApi.getXYTree(exp.getUUID(), XY_HISTOGRAM_DATAPROVIDER_ID, queryParams);

        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        XYTreeEntryModel responseModel = treeResponse.getModel();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfxyApi.getXYTree(exp.getUUID(), XY_HISTOGRAM_DATAPROVIDER_ID, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Verify Entries
        assertNotNull(responseModel);
        List<XYTreeEntry> entries = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG, entries);
        assertFalse(entries.isEmpty());

        // Test getting the XY series endpoint
        List<Integer> items = new ArrayList<>();
        for (XYTreeEntry entry : entries) {
            items.add(entry.getId().intValue()); // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
        }

        RequestedParameters reqParams = new RequestedParameters()
                .requestedTimerange(new TimeRange().start(start).end(end).nbTimes(10))
                .requestedItems(items);

        RequestedQueryParameters reqQueryParameters = new RequestedQueryParameters().parameters(reqParams);

        XYResponse xyModelResponse = sfxyApi.getXY(exp.getUUID(), XY_HISTOGRAM_DATAPROVIDER_ID, reqQueryParameters);
        assertNotNull(xyModelResponse);

        XYModel xyModel = xyModelResponse.getModel();
        List<SeriesModel> xySeries = xyModel.getSeries();
        assertFalse(xySeries.isEmpty());
        SeriesModel series = xySeries.get(0);

        List<Long> xValues = series.getxValues();
        assertFalse(xValues.isEmpty());

        for (XYTreeEntry entry : entries) {
            if (entry.getParentId() == -1) {
                assertTrue(entry.getIsDefault() == null || !entry.getIsDefault());
            } else {
                assertTrue(entry.getIsDefault() != null && entry.getIsDefault());
            }
        }
    }

    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testXYErrors() {
        Experiment exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getXYTreeEndpoint, XY_DATAPROVIDER_ID, false);
        executePostErrorTests(exp.getUUID(), RestServerTest::getXYSeriesEndpoint, XY_DATAPROVIDER_ID, true);
    }
}
