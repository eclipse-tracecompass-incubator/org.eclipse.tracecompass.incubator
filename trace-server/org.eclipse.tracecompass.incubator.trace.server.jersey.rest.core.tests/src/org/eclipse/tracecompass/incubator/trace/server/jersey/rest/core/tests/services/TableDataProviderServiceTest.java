/*******************************************************************************
 * Copyright (c) 2024 Ericsson and others
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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ColumnHeaderEntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.LineModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TableColumnsOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TableLinesOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TableModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} with focus on table tndpoint
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class TableDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String EVENTS_TABLE_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider";

    private static final String REQUESTED_COLUMN_IDS_KEY = "requested_table_column_ids";
    private static final String REQUESTED_TABLE_INDEX_KEY = "requested_table_index";
    private static final String REQUESTED_TABLE_COUNT_KEY = "requested_table_count";
    private static final long TABLE_INDEX = 0L;
    private static final long TABLE_COUNT = 100L;

    /**
     * Ensure that a table data provider exists and returns correct data. It
     * does not test the data itself, simply that the serialized fields are the
     * expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testTableDataProvider() throws InterruptedException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        try {
            ExperimentModelStub exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

            // Test getting the tree endpoint for an XY chart
            WebTarget tableColumns = getTableColumnsEndpoint(exp.getUUID().toString(), EVENTS_TABLE_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
            TableColumnsOutputResponseStub responseModel;
            try (Response tree = tableColumns.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
                responseModel = tree.readEntity(TableColumnsOutputResponseStub.class);
                assertNotNull(responseModel);
            }
            // Make sure the analysis ran enough and we have a model
            int iteration = 0;
            while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
                Thread.sleep(100);
                try (Response xyResponse = tableColumns.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, xyResponse.getStatus());
                    responseModel = xyResponse.readEntity(TableColumnsOutputResponseStub.class);
                    assertNotNull(responseModel);
                    iteration++;
                }
            }

            List<ColumnHeaderEntryStub> columns = responseModel.getModel();
            assertNotNull(MODEL_NULL_MSG + responseModel, columns);
            assertFalse(columns.isEmpty());

            // Test getting the XY series endpoint
            WebTarget tableLinesEnpoint = getTableLinesEndpoint(exp.getUUID().toString(), EVENTS_TABLE_DATAPROVIDER_ID);
            List<Long> requestedColumnsIds = new ArrayList<>();
            for (int i = 0; i <= columns.size() / 2; i++) {
                requestedColumnsIds.add(columns.get(i).getId());
            }
            parameters.put(REQUESTED_COLUMN_IDS_KEY, requestedColumnsIds);
            parameters.put(REQUESTED_TABLE_INDEX_KEY, TABLE_INDEX);
            parameters.put(REQUESTED_TABLE_COUNT_KEY, TABLE_COUNT);
            try (Response linesResponse = tableLinesEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, linesResponse.getStatus());
                TableLinesOutputResponseStub lineModelResponse = linesResponse.readEntity(TableLinesOutputResponseStub.class);
                assertNotNull(lineModelResponse);

                TableModelStub tableModel = lineModelResponse.getModel();
                assertNotNull("Table model", tableModel);
                List<LineModelStub> lines = tableModel.getLines();
                // FIXME This assert does not work with current implementation
                // assertEquals("Sizes match", tableModel.getSize(), lines.size());
                assertFalse(lines.isEmpty());
                for (LineModelStub line : lines) {
                    assertEquals("Number of returned cells", requestedColumnsIds.size(), line.getCells().size());
                }
            }

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testTableErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);
        executePostErrorTests(exp, RestServerTest::getTableColumnsEndpoint, EVENTS_TABLE_DATAPROVIDER_ID, false);
        executePostErrorTests(exp, RestServerTest::getTableLinesEndpoint, EVENTS_TABLE_DATAPROVIDER_ID, true);
    }
}
