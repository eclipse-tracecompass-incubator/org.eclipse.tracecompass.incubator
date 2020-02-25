/*******************************************************************************
 * Copyright (c) 2018, 2019 Ericsson
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
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.DataProviderDescriptorStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.junit.Test;

/**
 * Test the {@link DataProviderService}
 *
 * @author Loic Prieur-Drevon
 */
public class DataProviderServiceTest extends RestServerTest {
    private static final String CALL_STACK_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.analysis.profiling.callstack.provider.CallStackDataProvider";

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
        Response tree = callstackTree.request().post(Entity.json(new QueryParameters(parameters , Collections.emptyList())));
        assertEquals("There should be a positive response for the data provider", 200, tree.getStatus());

        parameters = new HashMap<>();
        parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, Collections.emptyList());
        Response defaults = callstackTree.request().post(Entity.json(new QueryParameters(parameters , Collections.emptyList())));
        assertEquals("Default values should return OK code", 200, defaults.getStatus());
    }

}
