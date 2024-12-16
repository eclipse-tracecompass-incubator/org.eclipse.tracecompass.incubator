/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
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

import java.util.Map;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.HealthService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

/**
 * Test the {@link HealthService}
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("null")
public class HealthServiceTest extends RestServerTest {

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testHealthStatus() {

        WebTarget application = getApplicationEndpoint();
        WebTarget healthEndpoint = application.path("health");

        Response response = healthEndpoint.request(MediaType.APPLICATION_JSON)
                .get();
        assertEquals("Health reponse should be OK", 200, response.getStatus());
        Map<Object, Object> responseValues = response.readEntity(Map.class);
        assertEquals("UP", responseValues.get("status"));

    }

}
