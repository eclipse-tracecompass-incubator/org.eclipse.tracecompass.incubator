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

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Loic Prieur-Drevon
 */
public class DataProviderServiceTest extends RestServerTest {
    private static final String CALL_STACK_DATAPROVIDER_ID = "org.eclipse.tracecompass.internal.tmf.core.callstack.provider.CallStackDataProvider";

    private static String CONTEXT_SWITCHES_UST_PATH;
    private static TraceModelStub CONTEXT_SWITCHES_UST_STUB;
    private static final UUID CONTEXT_SWITCHES_UST_UUID = UUID.fromString("8160c5b3-c482-4d86-9d81-3272e872537f");

    /**
     * Get the paths to the desired traces statically
     *
     * @throws IOException
     *             if the URL could not be converted to a path
     */
    @BeforeClass
    public static void beforeTest() throws IOException {
        CONTEXT_SWITCHES_UST_PATH = FileLocator.toFileURL(CtfTestTrace.CONTEXT_SWITCHES_UST.getTraceURL()).getPath();
        CONTEXT_SWITCHES_UST_STUB = new TraceModelStub("trace2", CONTEXT_SWITCHES_UST_PATH, CONTEXT_SWITCHES_UST_UUID);
    }

    /**
     * Ensure that the Call Stack data provider exists for the trace.
     */
    @Test
    public void testCallStackDataProvider() {
        WebTarget traces = getTracesEndpoint();
        RestServerTest.assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

        Response tree = traces.path(CONTEXT_SWITCHES_UST_UUID.toString())
                .path(CALL_STACK_DATAPROVIDER_ID)
                .queryParam("start", 0L)
                .queryParam("end", Long.MAX_VALUE)
                .queryParam("nb", 2)
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals("There should be a positive response for the data provider", 200, tree.getStatus());
    }

}
