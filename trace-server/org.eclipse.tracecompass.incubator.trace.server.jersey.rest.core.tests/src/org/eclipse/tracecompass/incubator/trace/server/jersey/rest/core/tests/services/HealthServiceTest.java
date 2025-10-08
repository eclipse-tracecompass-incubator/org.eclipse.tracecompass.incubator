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
import static org.junit.Assert.assertNotNull;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.HealthService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.DiagnosticApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ServerStatus;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ServerStatus.StatusEnum;
import org.junit.Test;

/**
 * Test the {@link HealthService}
 *
 * @author Geneviève Bastien
 */
public class HealthServiceTest extends RestServerTest {

    private static final DiagnosticApi sfDiagnosticApi = new DiagnosticApi(sfApiClient);

    /**
     * Test basic operations on the {@link TraceManagerService}.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testHealthStatus() throws ApiException {
        ServerStatus status = sfDiagnosticApi.getHealthStatus();
        assertNotNull(status);
        assertEquals(StatusEnum.UP, status.getStatus());
    }
}
