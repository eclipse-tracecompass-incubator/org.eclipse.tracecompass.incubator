/*******************************************************************************
 * Copyright (c) 2018-2020 Ericsson and others
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
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.DataProviderDescriptorStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgEntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgStatesOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgTreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphEntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphRowStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XySeriesStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the {@link DataProviderService}
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 */
public class DataProviderServiceTest extends RestServerTest {
    private static final int MAX_ITER = 40;
    private static final String CALL_STACK_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.analysis.profiling.callstack.provider.CallStackDataProvider";
    private static final String XY_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.CpuUsageDataProvider";

    /**
     * Test getting the data provider descriptors
     */
    @Test
    public void testProviders() {

        WebTarget traces = getApplicationEndpoint().path(TRACES);
        RestServerTest.assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

        WebTarget experiments = getApplicationEndpoint().path(EXPERIMENTS);
        WebTarget providers = experiments.path(CONTEXT_SWITCHES_UST_UUID.toString())
                .path(OUTPUTS_PATH);

        Set<DataProviderDescriptorStub> descriptors = getDataProviderDescriptors(providers);
        for (DataProviderDescriptorStub desc : EXPECTED_DATA_PROVIDER_DESCRIPTOR) {
            assertTrue(desc.getName(), descriptors.contains(desc));
        }
    }

    /**
     * Ensure that the Call Stack data provider exists for the trace.
     */
    @Test
    public void testCallStackDataProvider() {
        WebTarget traces = getApplicationEndpoint().path(TRACES);
        RestServerTest.assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

        WebTarget callstackTree = getTimeGraphTreeEndpoint(CONTEXT_SWITCHES_UST_UUID.toString(), CALL_STACK_DATAPROVIDER_ID);

        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0L, Long.MAX_VALUE, 2));
        Response tree = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("There should be a positive response for the data provider", 200, tree.getStatus());

        parameters = new HashMap<>();
        parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, Collections.emptyList());
        Response defaults = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals("Default values should return OK code", 200, defaults.getStatus());
    }

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
            WebTarget traces = getApplicationEndpoint().path(TRACES);
            RestServerTest.assertPost(traces, ARM_64_KERNEL_STUB);

            // Test getting the tree endpoint for an XY chart
            WebTarget xyTree = getXYTreeEndpoint(ARM_64_KERNEL_UUID.toString(), XY_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(start, end));
            TreeOutputResponseStub responseModel;
            Response tree = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
            assertEquals("There should be a positive response for the data provider", 200, tree.getStatus());
            responseModel = tree.readEntity(TreeOutputResponseStub.class);
            assertNotNull(responseModel);
            tree.close();

            // Make sure the analysis ran enough and we have a model
            int iteration = 0;
            while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
                Thread.sleep(100);
                Response xyResponse = xyTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
                assertEquals("There should be a positive response for the data provider", 200, xyResponse.getStatus());
                responseModel = xyResponse.readEntity(TreeOutputResponseStub.class);
                assertNotNull(responseModel);
                iteration++;
                xyResponse.close();
            }

            EntryModelStub model = responseModel.getModel();
            assertNotNull("The model is null, maybe the analysis did not run long enough?" + responseModel, model);
            Set<EntryStub> entries = model.getEntries();
            assertFalse(entries.isEmpty());

            // Test getting the XY series endpoint
            WebTarget xySeriesEnpoint = getXYSeriesEndpoint(ARM_64_KERNEL_UUID.toString(), XY_DATAPROVIDER_ID);
            List<Integer> items = new ArrayList<>();
            for (EntryStub entry : entries) {
                items.add(entry.getId());
            }
            parameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, items);
            Response series = xySeriesEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
            assertEquals("There should be a positive response for the data provider", 200, series.getStatus());
            XyOutputResponseStub xyModelResponse = series.readEntity(XyOutputResponseStub.class);
            assertNotNull(xyModelResponse);

            XyModelStub xyModel = xyModelResponse.getModel();
            Set<XySeriesStub> xySeries = xyModel.getSeries();
            assertFalse(xySeries.isEmpty());
            series.close();

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    /**
     * Ensure that a time graph data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testTimeGraphDataProvider() throws InterruptedException {
        long start = 1450193697034689597L;
        long end = 1450193745774189602L;
        try {
            WebTarget traces = getApplicationEndpoint().path(TRACES);
            RestServerTest.assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

            // Test getting the time graph tree
            WebTarget callstackTree = getTimeGraphTreeEndpoint(CONTEXT_SWITCHES_UST_UUID.toString(), CALL_STACK_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            TgTreeOutputResponseStub responseModel;
            parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(start, end));
            Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
            assertEquals("There should be a positive response for the data provider", 200, treeResponse.getStatus());
            responseModel = treeResponse.readEntity(TgTreeOutputResponseStub.class);
            assertNotNull(responseModel);
            treeResponse.close();

            // Make sure the analysis ran enough and we have a model
            int iteration = 0;
            while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
                Thread.sleep(100);
                treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
                assertEquals("There should be a positive response for the data provider", 200, treeResponse.getStatus());
                responseModel = treeResponse.readEntity(TgTreeOutputResponseStub.class);
                assertNotNull(responseModel);
                iteration++;
                treeResponse.close();
            }

            TgEntryModelStub model = responseModel.getModel();
            assertNotNull("The model is null, maybe the analysis did not run long enough?" + responseModel, model);
            Set<TimeGraphEntryStub> entries = model.getEntries();
            assertFalse(entries.isEmpty());
            // add entries for the states query, and make sure they don't have
            // extra time fields
            List<Integer> items = new ArrayList<>();
            for (TimeGraphEntryStub entry : entries) {
                items.add(entry.getId());
            }

            // Test getting the time graph row data
            WebTarget tgStatesEnpoint = getTimeGraphStatesEndpoint(CONTEXT_SWITCHES_UST_UUID.toString(), CALL_STACK_DATAPROVIDER_ID);
            parameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, items);
            Response statesResponse = tgStatesEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
            assertEquals("There should be a positive response for the data provider", 200, statesResponse.getStatus());

            TgStatesOutputResponseStub tgStateModelResponse = statesResponse.readEntity(TgStatesOutputResponseStub.class);
            assertNotNull(tgStateModelResponse);

            TimeGraphModelStub tgModel = tgStateModelResponse.getModel();
            Set<TimeGraphRowStub> rows = tgModel.getRows();
            assertFalse(rows.isEmpty());
            statesResponse.close();

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

}
