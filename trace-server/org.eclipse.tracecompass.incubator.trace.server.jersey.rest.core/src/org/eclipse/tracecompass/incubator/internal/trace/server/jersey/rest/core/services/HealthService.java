/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.IServerStatus;

import com.google.common.collect.ImmutableMap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to query the health status of the server
 *
 * @author Geneviève Bastien
 */
@Path("/health")
@Tag(name = EndpointConstants.DIA)
public class HealthService {

    private static final String STATUS_KEY = "status"; //$NON-NLS-1$

    /**
     * Getter for the health status
     *
     * @return UP status.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the health status of this server", responses = {
            @ApiResponse(responseCode = "200", description = "The trace server is running and ready to receive requests", content = @Content(schema = @Schema(implementation = IServerStatus.class))),
            @ApiResponse(responseCode = "503", description = "The trace server is unavailable or in maintenance and cannot receive requests")
    })
    public Response getHealthStatus() {
        // If the server can answer this call, it is up!!
        return Response.ok(ImmutableMap.of(STATUS_KEY, IServerStatus.Status.UP.name())).build();
    }
}
