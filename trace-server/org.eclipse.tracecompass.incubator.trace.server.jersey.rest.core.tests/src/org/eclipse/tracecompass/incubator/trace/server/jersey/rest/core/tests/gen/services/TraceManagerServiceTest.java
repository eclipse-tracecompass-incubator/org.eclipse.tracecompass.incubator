/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.gen.services;

import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.TraceServerConfiguration;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.WebApplication;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.webapp.TestWebApplication;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiClient;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.Configuration;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TracesApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Trace;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceQueryParameters;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Bernd Hufmann
 */
public class TraceManagerServiceTest {

    private static final WebApplication fWebApp = new TestWebApplication(new TraceServerConfiguration(TraceServerConfiguration.TEST_PORT, false, null, null));

    private static  String sFcontextSwitchesKernelPath;

   /**
    * Start the server before tests
    *
    * @throws Exception
    *             if exception occurs
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
        fWebApp.stop();
    }

    /**
     *
     * @throws IOException
     *             if exception occurs
     */
    @BeforeClass
    public static void beforeTest() throws IOException {
        sFcontextSwitchesKernelPath = FileLocator.toFileURL(CtfTestTrace.CONTEXT_SWITCHES_KERNEL.getTraceURL()).getPath().replaceAll("/$", "");
    }

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testWithOneTrace() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost:8378/tsp/api");

       TracesApi apiInstance = new TracesApi(defaultClient);
       TraceParameters params = new TraceParameters();
       params.uri(sFcontextSwitchesKernelPath)
             .name("kernel");

        TraceQueryParameters traceQueryParameters = new TraceQueryParameters(); // TraceQueryParameters |
        traceQueryParameters.setParameters(params);

        try {
            Trace result = apiInstance.putTrace(traceQueryParameters);
            System.out.println(result);
            Trace result2 = apiInstance.deleteTrace(result.getUUID());
            System.out.println(result2);
        } catch (ApiException e) {
            System.err.println("Exception when calling TracesApi#putTrace");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }

}
