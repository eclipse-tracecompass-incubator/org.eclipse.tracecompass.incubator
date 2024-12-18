/*******************************************************************************
 * Copyright (c) 2024 Ericsson
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
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.IdentifierService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ServerInfoResponseImpl;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link IdentifierService}
 *
 * @author Vlad Arama
 */
@SuppressWarnings("null")
public class IdentifierServiceTest extends RestServerTest {

    /**
     * Test basic operations on the Identifier Service
     */
    @Test
    public void testIdentifier() {
        WebTarget application = getApplicationEndpoint();
        WebTarget identifierEndpoint = application.path("identifier");

        try (Response response = identifierEndpoint.request(MediaType.APPLICATION_JSON)
                .get()) {
            ServerInfoResponseImpl responseValues = response.readEntity(ServerInfoResponseImpl.class);

            assertNotNull("Server version should not be null", responseValues.getVersion());
            assertNotNull("OS should not be null", responseValues.getOs());
            assertNotNull("OS Architecture should not be null", responseValues.getOsArch());
            assertNotNull("OS Version should not be null", responseValues.getOsVersion());
            assertNotNull("CPU count should not be null", responseValues.getCpuCount());
            assertNotNull("Max memory should not be null", responseValues.getMaxMemory());
            assertNotNull("Product ID should not be null", responseValues.getProductId());
            assertNotNull("The TSP version should not be null", responseValues.getProductId());
            assertEquals(EndpointConstants.VERSION, responseValues.getTspVersion());
        }
    }

}
