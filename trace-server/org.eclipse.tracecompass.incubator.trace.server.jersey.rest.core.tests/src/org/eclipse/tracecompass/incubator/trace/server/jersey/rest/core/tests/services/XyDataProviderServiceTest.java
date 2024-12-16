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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryHeaderStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyEntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyEntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XySeriesStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyTreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the {@link DataProviderService} with focus on XY endpoints
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 */
@SuppressWarnings("null")
public class XyDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String XY_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.CpuUsageDataProvider";
    private static final String XY_HISTOGRAM_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.tmf.core.histogram.HistogramDataProvider";
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange";
    private static final String REQUESTED_ITEMS_KEY = "requested_items";
    private static final String START = "start";
    private static final String END = "end";
    private static final String NB_TIMES = "nbTimes";

    private static final  List<EntryHeaderStub> EXPECTED_XY_TREE_HEADERS = List.of(new EntryHeaderStub("Process", "", null), new EntryHeaderStub("TID", "", null), new EntryHeaderStub("%", "", null), new EntryHeaderStub("Time", "", null));

    /**
     * Ensure that an XY data provider exists and returns correct data. It does
     * not test the data itself, simply that the serialized fields are the
     * expected ones according to the protocol.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testXYDataProvider() throws InterruptedException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        try {
            ExperimentModelStub exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

            // Test getting the tree endpoint for an XY chart
            WebTarget xyTree = getXYTreeEndpoint(exp.getUUID().toString(), XY_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
            XyTreeOutputResponseStub responseModel;
            try (Response tree = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
                responseModel = tree.readEntity(XyTreeOutputResponseStub.class);
                assertNotNull(responseModel);
            }
            // Make sure the analysis ran enough and we have a model
            int iteration = 0;
            while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
                Thread.sleep(100);
                try (Response xyResponse = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, xyResponse.getStatus());
                    responseModel = xyResponse.readEntity(XyTreeOutputResponseStub.class);
                    assertNotNull(responseModel);
                    iteration++;
                }
            }

            // Verify tree model
            XyEntryModelStub model = responseModel.getModel();
            assertNotNull(model);
            List<EntryHeaderStub> headers = model.getHeaders();
            assertNotNull(headers);
            assertEquals(EXPECTED_XY_TREE_HEADERS.size(), headers.size());
            // Verify tree headers
            for (int i = 0; i < headers.size(); i++ ) {
                EntryHeaderStub header = headers.get(i);
                EntryHeaderStub expHeader = EXPECTED_XY_TREE_HEADERS.get(i);
                assertTrue(expHeader.getName().equals(header.getName()) && expHeader.getTooltip().equals(header.getTooltip()));
            }
            // Verify Entries
            assertNotNull(MODEL_NULL_MSG + responseModel, model);
            List<XyEntryStub> entries = model.getEntries();
            assertFalse(entries.isEmpty());

            // Test getting the XY series endpoint
            WebTarget xySeriesEnpoint = getXYSeriesEndpoint(exp.getUUID().toString(), XY_DATAPROVIDER_ID);
            List<Integer> items = new ArrayList<>();
            for (XyEntryStub entry : entries) {
                items.add(entry.getId());
            }
            parameters.remove(REQUESTED_TIMES_KEY);
            parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, start, END, end, NB_TIMES, 10));
            parameters.put(REQUESTED_ITEMS_KEY, items);
            try (Response series = xySeriesEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, series.getStatus());
                XyOutputResponseStub xyModelResponse = series.readEntity(XyOutputResponseStub.class);
                assertNotNull(xyModelResponse);

                XyModelStub xyModel = xyModelResponse.getModel();
                Set<XySeriesStub> xySeries = xyModel.getSeries();
                assertFalse(xySeries.isEmpty());
            }

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    /**
     * Verify that Histogram Data Provider fetchTree() interface and verify that
     * the serialized fields are the expected ones according to the protocol.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testHistogramDataProvider() throws InterruptedException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        try {
            ExperimentModelStub exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

            // Test getting the tree endpoint for an XY chart
            WebTarget xyTree = getXYTreeEndpoint(exp.getUUID().toString(), XY_HISTOGRAM_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
            XyTreeOutputResponseStub responseModel;
            try (Response tree = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
                responseModel = tree.readEntity(XyTreeOutputResponseStub.class);
                assertNotNull(responseModel);
            }
            // Make sure the analysis ran enough and we have a model
            int iteration = 0;
            while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
                Thread.sleep(100);
                try (Response xyResponse = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, xyResponse.getStatus());
                    responseModel = xyResponse.readEntity(XyTreeOutputResponseStub.class);
                    assertNotNull(responseModel);
                    iteration++;
                }
            }

            // Verify tree model
            XyEntryModelStub model = responseModel.getModel();
            assertNotNull(model);
            // Verify Entries
            assertNotNull(MODEL_NULL_MSG + responseModel, model);
            List<XyEntryStub> entries = model.getEntries();
            assertFalse(entries.isEmpty());

            for (XyEntryStub entry : entries) {
                if (entry.getParentId() == -1) {
                    assertFalse(entry.isDefault());
                } else {
                    assertTrue(entry.isDefault());
                }
            }
        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }
}
