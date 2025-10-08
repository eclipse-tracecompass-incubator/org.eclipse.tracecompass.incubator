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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.VirtualTablesApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.LinesParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.LinesQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OptionalQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TableColumnHeader;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TableColumnHeadersResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.VirtualTableLine;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.VirtualTableModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.VirtualTableResponse;
import org.junit.Test;

/**
 * Test the {@link DataProviderService} with focus on table tndpoint
 *
 * @author Bernd Hufmann
 */
public class TableDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String EVENTS_TABLE_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider";

    private static final long TABLE_INDEX = 0L;
    private static final int TABLE_COUNT = 100;

    private static final VirtualTablesApi sfTableApi = new VirtualTablesApi(sfApiClient);

    /**
     * Ensure that a table data provider exists and returns correct data. It
     * does not test the data itself, simply that the serialized fields are the
     * expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException if such exception occurs
     */
    @Test
    public void testTableDataProvider() throws InterruptedException, ApiException {
        long start = 1412670961211260539L;
        long end = 1412670967217750839L;
        Experiment exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);

        // Test getting the columns
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REQUESTED_TIMES_KEY, List.of(start, end));
        OptionalQueryParameters queryParameters = new OptionalQueryParameters().parameters(parameters);
        TableColumnHeadersResponse responseModel = sfTableApi.getColumns(exp.getUUID(), EVENTS_TABLE_DATAPROVIDER_ID, queryParameters);
        assertNotNull(responseModel);
        assertFalse(DATA_PROVIDER_RESPONSE_FAILED_MSG, TableColumnHeadersResponse.StatusEnum.FAILED.equals(responseModel.getStatus()));

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((responseModel.getStatus().equals(TableColumnHeadersResponse.StatusEnum.RUNNING)) || (iteration < MAX_ITER)) {
            Thread.sleep(100);
            responseModel = sfTableApi.getColumns(exp.getUUID(), EVENTS_TABLE_DATAPROVIDER_ID, queryParameters);
            assertNotNull(responseModel);
            iteration++;
        }

        List<TableColumnHeader> columns = responseModel.getModel();
        assertNotNull(MODEL_NULL_MSG + responseModel, columns);
        assertFalse(columns.isEmpty());

        List<Long> requestedColumnsIds = new ArrayList<>();
        for (int i = 0; i <= columns.size() / 2; i++) {
            requestedColumnsIds.add(columns.get(i).getId());
        }

        // Test getting the lines
        LinesParameters params = new LinesParameters()
                .requestedTableColumnIds(requestedColumnsIds)
                .requestedTableIndex(TABLE_INDEX)
                .requestedTableCount(TABLE_COUNT)
                .requestedTimes(null) // set to null to make sure the field is not serialized as empty array
                .tableSearchExpressions(null); // set to null to make sure the field is not serialized as empty map

        LinesQueryParameters linesQueryParameter = new LinesQueryParameters().parameters(params);
        VirtualTableResponse lineModelResponse = sfTableApi.getLines(exp.getUUID(), EVENTS_TABLE_DATAPROVIDER_ID, linesQueryParameter);
        assertNotNull(lineModelResponse);
        VirtualTableModel tableModel =  lineModelResponse.getModel();
        assertNotNull("Table model", tableModel);
        List<VirtualTableLine> lines = tableModel.getLines();
        // FIXME This assert does not work with current implementation
        // assertEquals("Sizes match", tableModel.getSize(), lines.size());
        assertFalse(lines.isEmpty());
        for (VirtualTableLine line : lines) {
            assertEquals("Number of returned cells", requestedColumnsIds.size(), line.getCells().size());
        }

        // TODO add search tests
    }

    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testTableErrors() {
        Experiment exp = assertPostExperiment(sfArm64KernelNotIntitialzedStub.getName(), sfArm64KernelNotIntitialzedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getTableColumnsEndpoint, EVENTS_TABLE_DATAPROVIDER_ID, false);
        executePostErrorTests(exp.getUUID(), RestServerTest::getTableLinesEndpoint, EVENTS_TABLE_DATAPROVIDER_ID, true);
    }
}
