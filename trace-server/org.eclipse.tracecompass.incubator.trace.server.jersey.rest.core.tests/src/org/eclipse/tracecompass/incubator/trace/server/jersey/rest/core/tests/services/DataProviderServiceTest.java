/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link DataProviderService}
 *
 * @author Loic Prieur-Drevon
 */
public class DataProviderServiceTest extends RestServerTest {
    static final String PROVIDERS_PATH = "providers";
    private static final String CALL_STACK_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.tmf.core.callstack.provider.CallStackDataProvider";
    static final String TREE_PATH = "tree";

    /**
     * Ensure that the Call Stack data provider exists for the trace.
     */
    @Test
    public void testCallStackDataProvider() {
        WebTarget traces = getApplicationEndpoint().path(TRACES);
        RestServerTest.assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

        WebTarget callstackTree = traces.path(CONTEXT_SWITCHES_UST_UUID.toString())
                .path(PROVIDERS_PATH)
                .path(CALL_STACK_DATAPROVIDER_ID)
                .path(TREE_PATH);

        Response tree = callstackTree
                .queryParam("start", 0L)
                .queryParam("end", Long.MAX_VALUE)
                .queryParam("nb", 2)
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals("There should be a positive response for the data provider", 200, tree.getStatus());

        Response defaults = callstackTree
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals("Default values should return OK code", 200, defaults.getStatus());
    }

}
