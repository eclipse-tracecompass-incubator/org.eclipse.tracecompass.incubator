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
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Version;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ServerInfoResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 * Service to identify important information regarding the trace server and the
 * system it is running on.
 *
 * @author Vlad Arama
 *
 */
@Path("/identifier")
@Tag(name = EndpointConstants.IDF)
public class IdentifierService {
    private static final String OS_NAME = "os.name"; //$NON-NLS-1$
    private static final String OS_ARCH = "os.arch"; //$NON-NLS-1$
    private static final String OS_VERSION = "os.version"; //$NON-NLS-1$
    private static final String PRODUCT_ID = "eclipse.product"; //$NON-NLS-1$
    private static final String LAUNCHER_NAME = "eclipse.launcher.name"; //$NON-NLS-1$
    private static final String QUALIFIER = "qualifier"; //$NON-NLS-1$
    private static final String SEPARATOR = "."; //$NON-NLS-1$
    private static final String HARD_CODED_VERSION = "0.8.1"; //$NON-NLS-1$

    /**
     * Getter returning important information about the system, including server
     * version, OS, CPU count, memory size, launcher name and product id.
     *
     * @return A JSON response containing the system's details.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieves system and server information", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the system and server information", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ServerInfoResponse.class)))
    })
    public Response getSystemInfo() {
        IProduct product = Platform.getProduct();
        ServerInfoResponseImpl response = new ServerInfoResponseImpl();

        response.setOs(System.getProperty(OS_NAME));
        response.setOsArch(System.getProperty(OS_ARCH));
        response.setOsVersion(System.getProperty(OS_VERSION));
        response.setCpuCount(Runtime.getRuntime().availableProcessors());
        response.setMaxMemory(Runtime.getRuntime().maxMemory());
        response.setProductId(System.getProperty(PRODUCT_ID));
        response.setLauncherName(System.getProperty(LAUNCHER_NAME));

        if (product != null) {
            Version version = product.getDefiningBundle().getVersion();
            response.setVersion(version.getMajor() + SEPARATOR + version.getMinor() + SEPARATOR + version.getMicro());
            String qualifier = version.getQualifier();
            if (!QUALIFIER.equalsIgnoreCase(qualifier) && qualifier != null) {
                response.setBuildTime(qualifier);
            }
        } else {
            response.setVersion(HARD_CODED_VERSION);
        }
        response.setTspVersion(EndpointConstants.VERSION);
        return Response.ok(response).build();
    }

}
