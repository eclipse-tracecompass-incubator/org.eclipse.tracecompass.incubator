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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Loic Prieur-Drevon
 */
public class TraceManagerServiceTest extends RestServerTest {

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testWithOneTrace() {
        WebTarget traces = getTracesEndpoint();

        Set<TraceModelStub> traceModels = getTraces(traces);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertTrue("Expected empty set of traces", traceModels.isEmpty());

        assertPost(traces, TRACE2_STUB);

        @Nullable TraceModelStub actual = traces.path(TRACE2_STUB.getUUID().toString()).request().get(TraceModelStub.class);
        assertEquals(TRACE2_STUB, actual);

        traceModels = getTraces(traces);
        assertEquals("Expected set of traces to contain trace2 stub",
                Collections.singleton(TRACE2_STUB), traceModels);

        Response deleteResponse = traces.path(TRACE2_UUID.toString()).request().delete();
        int deleteCode = deleteResponse.getStatus();
        assertEquals("Failed to DELETE trace2, error code=" + deleteCode, 200, deleteCode);
        assertEquals(TRACE2_STUB, deleteResponse.readEntity(TraceModelStub.class));

        traceModels = getTraces(traces);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Trace should have been deleted", Collections.emptySet(), traceModels);
    }

    /**
     * Test the server with two traces, to eliminate the server trace manager bug
     */
    @Test
    public void testWithTwoTraces() {
        WebTarget traces = getTracesEndpoint();

        assertPost(traces, TRACE2_STUB);
        assertPost(traces, CONTEXT_SWITCHES_UST_STUB);

        Set<TraceModelStub> expected = new HashSet<>();
        expected.add(CONTEXT_SWITCHES_UST_STUB);
        expected.add(TRACE2_STUB);
        assertEquals(expected, getTraces(traces));
    }

}
