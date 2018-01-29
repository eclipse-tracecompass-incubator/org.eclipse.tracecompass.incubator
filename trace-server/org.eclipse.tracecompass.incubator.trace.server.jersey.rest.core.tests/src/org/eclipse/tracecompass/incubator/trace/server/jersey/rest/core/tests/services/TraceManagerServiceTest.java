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

import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.application.Application;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Loic Prieur-Drevon
 */
public class TraceManagerServiceTest {
    private static final String SERVER = "http://localhost:8378/tracecompass/traces"; //$NON-NLS-1$
    private static final String PATH = "path"; //$NON-NLS-1$
    private static String TRACE2_PATH;
    private static final GenericType<List<TraceModelStub>> TRACE_MODEL_LIST_TYPE = new GenericType<List<TraceModelStub>>() {
    };

    private static final Application fWebApp = new Application(WebApplication.TEST_PORT);

    /**
     * Start the Eclipse / Jetty Web server
     *
     * @throws Exception
     *             if there is a problem running this application.
     */
    @BeforeClass
    public static void startServer() throws Exception {
        TRACE2_PATH = FileLocator.toFileURL(CtfTestTrace.TRACE2.getTraceURL()).getPath();
        fWebApp.start(null);
    }

    /**
     * Stop the server once tests are finished
     */
    @AfterClass
    public static void stopServer() {
        fWebApp.stop();
    }

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testWithOneTrace() {
        Client client = ClientBuilder.newClient();
        // FIXME this should be done at the application level?
        client.register(JacksonJsonProvider.class);
        WebTarget traces = client.target(SERVER);

        List<TraceModelStub> traceModels = traces.request(MediaType.APPLICATION_JSON).get(TRACE_MODEL_LIST_TYPE);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Expected empty list of traces", Collections.emptyList(), traceModels);

        WebTarget traceTarget = traces.path("trace2");

        Form form = new Form(PATH, TRACE2_PATH);
        Response postResponse = traceTarget.request().post(Entity.form(form));
        int code = postResponse.getStatus();
        assertEquals("Failed to POST trace2, error code=" + code, 200, code);
        assertEqualsTrace2Model(postResponse);

        traceModels = traces.request(MediaType.APPLICATION_JSON).get(TRACE_MODEL_LIST_TYPE);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Expected list of traces to contain one trace", 1, traceModels.size());

        Response deleteResponse = traces.path("trace2").request().delete();
        int deleteCode = deleteResponse.getStatus();
        assertEquals("Failed to DELETE trace2, error code=" + deleteCode, 200, deleteCode);
        assertEqualsTrace2Model(deleteResponse);

        traceModels = traces.request(MediaType.APPLICATION_JSON).get(TRACE_MODEL_LIST_TYPE);
        assertNotNull("Model returned by server should not be null", traceModels);
        assertEquals("Trace should have been deleted", Collections.emptyList(), traceModels);
    }

    /**
     * Assert that the entity contained in a response matches trace2's name, path
     * and start time (We are not sure that the trace is completely indexed, so the
     * number of events and the end time might not be final.
     *
     * @param response
     *            {@link Response} that may contain a {@link TraceModelStub}.
     */
    private static void assertEqualsTrace2Model(Response response) {
        @Nullable
        TraceModelStub model = response.readEntity(TraceModelStub.class);
        assertNotNull(model);
        assertEquals("trace2", model.getName());
        assertEquals(TRACE2_PATH, model.getPath());
        assertEquals(1331668247314038062L, model.getStart());
    }

}
