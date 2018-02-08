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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.client.WebTarget;
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
public class TraceManagerServiceTest extends RestServerTest {

    private static String TRACE2_PATH;
    private static TraceModelStub TRACE2_STUB;
    private static final UUID TRACE2_UUID = UUID.fromString("5bf45359-069d-4e45-a34d-24d037ca5676") ;

    private static String KERNEL_PATH;
    private static TraceModelStub KERNEL_STUB;
    private static final UUID KERNEL_UUID = UUID.fromString("d18e6374-35a1-cd42-8e70-a9cffa712793") ;

    /**
     * Get the paths to the desired traces statically
     *
     * @throws IOException
     *             if the URL could not be converted to a path
     */
    @BeforeClass
    public static void beforeTest() throws IOException {
        TRACE2_PATH = FileLocator.toFileURL(CtfTestTrace.TRACE2.getTraceURL()).getPath();
        TRACE2_STUB = new TraceModelStub("trace2", TRACE2_PATH, TRACE2_UUID);

        KERNEL_PATH = FileLocator.toFileURL(CtfTestTrace.KERNEL.getTraceURL()).getPath();
        KERNEL_STUB = new TraceModelStub("kernel", KERNEL_PATH, KERNEL_UUID);
    }

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testWithOneTrace() {
        WebTarget traces = getTracesEndpoint();

        List<TraceModelStub> traceModels = getTraces(traces);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertTrue("Expected empty list of traces", traceModels.isEmpty());

        assertPost(traces, TRACE2_STUB);

        traceModels = getTraces(traces);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Expected list of traces to contain one trace", 1, traceModels.size());

        Response deleteResponse = traces.path(TRACE2_UUID.toString()).request().delete();
        int deleteCode = deleteResponse.getStatus();
        assertEquals("Failed to DELETE trace2, error code=" + deleteCode, 200, deleteCode);
        assertEquals(TRACE2_STUB, deleteResponse.readEntity(TraceModelStub.class));

        traceModels = getTraces(traces);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Trace should have been deleted", Collections.emptyList(), traceModels);
    }

    /**
     * Test the server with two traces, to eliminate the server trace manager bug
     */
    @Test
    public void testWithTwoTraces() {
        WebTarget traces = getTracesEndpoint();

        assertPost(traces, TRACE2_STUB);
        assertPost(traces, KERNEL_STUB);

        List<TraceModelStub> traceModels = getTraces(traces);
        assertNotNull(traceModels);
        assertEquals(2, traceModels.size());
        assertTrue(traceModels.contains(KERNEL_STUB));
        assertTrue(traceModels.contains(TRACE2_STUB));
    }

}
