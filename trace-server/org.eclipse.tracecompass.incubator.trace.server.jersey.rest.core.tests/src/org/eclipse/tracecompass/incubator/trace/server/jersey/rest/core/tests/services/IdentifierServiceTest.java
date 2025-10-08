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

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.IdentifierService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.IdentifierApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ServerInfoResponse;
import org.junit.Test;

/**
 * Test the {@link IdentifierService}
 *
 * @author Vlad Arama
 */
public class IdentifierServiceTest extends RestServerTest {

    private static final IdentifierApi sfIdentifierApi = new IdentifierApi(sfApiClient);

    /**
     * Test basic operations on the Identifier Service
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testIdentifier() throws ApiException {

        ServerInfoResponse responseValues = sfIdentifierApi.getSystemInfo();
        assertNotNull(responseValues);
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
