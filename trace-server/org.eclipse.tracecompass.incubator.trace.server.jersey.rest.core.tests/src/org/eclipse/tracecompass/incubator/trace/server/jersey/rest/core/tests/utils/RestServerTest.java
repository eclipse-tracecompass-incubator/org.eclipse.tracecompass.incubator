/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Rest server test abstract class, handles starting the server in test mode,
 * getting the correct client, and closing the traces and server once the test
 * is complete
 *
 * @author Loic Prieur-Drevon
 */
public abstract class RestServerTest {
    private static final String SERVER = "http://localhost:8378/tracecompass/traces"; //$NON-NLS-1$
    private static final WebApplication fWebApp = new WebApplication(WebApplication.TEST_PORT);
    private static final String NAME = "name";
    private static final String PATH = "path"; //$NON-NLS-1$

    private static final GenericType<Set<TraceModelStub>> TRACE_MODEL_SET_TYPE = new GenericType<Set<TraceModelStub>>() {
    };

    /**
     * {@link UUID} for {@link CtfTestTrace#TRACE2}.
     */
    protected static final UUID TRACE2_UUID = UUID.fromString("5bf45359-069d-4e45-a34d-24d037ca5676");
    /**
     * {@link TraceModelStub} to represent the object returned by the server for
     * {@link CtfTestTrace#TRACE2}.
     */
    protected static TraceModelStub TRACE2_STUB;

    /**
     * {@link UUID} for {@link CtfTestTrace#CONTEXT_SWITCHES_UST}.
     */
    protected static final UUID CONTEXT_SWITCHES_UST_UUID = UUID.fromString("8160c5b3-c482-4d86-9d81-3272e872537f");
    /**
     * {@link TraceModelStub} to represent the object returned by the server for
     * {@link CtfTestTrace#CONTEXT_SWITCHES_UST}.
     */
    protected static TraceModelStub CONTEXT_SWITCHES_UST_STUB;

    /**
     * Create the {@link TraceModelStub}s before running the tests
     *
     * @throws IOException
     *             if the URL could not be converted to a path
     */
    @BeforeClass
    public static void beforeTest() throws IOException {
        String trace2Path = FileLocator.toFileURL(CtfTestTrace.TRACE2.getTraceURL()).getPath();
        TRACE2_STUB = new TraceModelStub("trace2", trace2Path, TRACE2_UUID);

        String contextSwitchesUstPath = FileLocator.toFileURL(CtfTestTrace.CONTEXT_SWITCHES_UST.getTraceURL()).getPath();
        CONTEXT_SWITCHES_UST_STUB = new TraceModelStub("trace2", contextSwitchesUstPath, CONTEXT_SWITCHES_UST_UUID);
    }

    /**
     * Start the Eclipse / Jetty Web server
     *
     * @throws Exception
     *             if there is a problem running this application.
     */
    @Before
    public void startServer() throws Exception {
        fWebApp.start();
    }

    /**
     * Stop the server once tests are finished, and close the traces
     */
    @After
    public void stopServer() {
        WebTarget traceTarget = getTracesEndpoint();
        for (TraceModelStub trace : getTraces(traceTarget)) {
            traceTarget.path(trace.getUUID().toString()).request().delete();
        }
        fWebApp.stop();
    }

    /**
     * Getter for the {@link WebTarget} for the traces endpoint.
     *
     * @return the traces endpoint {@link WebTarget}.
     */
    public static WebTarget getTracesEndpoint() {
        Client client = ClientBuilder.newClient();
        client.register(JacksonJsonProvider.class);
        return client.target(SERVER);
    }

    /**
     * Get the traces currently open on the server.
     *
     * @param traces
     *            list of {@link TraceModelStub}s for the traces currently open on
     *            the server.
     * @return list of currently open traces.
     */
    public static Set<TraceModelStub> getTraces(WebTarget traces) {
        return traces.request(MediaType.APPLICATION_JSON).get(TRACE_MODEL_SET_TYPE);
    }

    /**
     * Post the trace from an expected {@link TraceModelStub}, ensure that the post
     * returned correctly and that the returned model was that of the expected stub.
     *
     * @param traces
     *            traces endpoint
     * @param stub
     *            expected trace stub
     */
    public static void assertPost(WebTarget traces, TraceModelStub stub) {
        Form form = new Form(PATH, stub.getPath());
        form.param(NAME, stub.getName());
        Response response = traces.request().post(Entity.form(form));
        int code = response.getStatus();
        assertEquals("Failed to POST " + stub.getName() + ", error code=" + code, 200, code);
        @Nullable TraceModelStub model = response.readEntity(TraceModelStub.class);
        assertEquals(stub, model);
    }
}
