/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_CONFIG_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_CREATE_DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_KEYS_DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_PATH_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_TYPE_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_UPDATE_DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INVALID_PARAMETERS;

import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ConfigurationQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to manage configurations
 *
 * @author Bernd Hufmann
 */
@Hidden
@Path("/config")
@Tag(name = EndpointConstants.CFG)
public class ConfigurationManagerService {

    /**
     * GET a list of available configuration source types
     *
     * @return list of available configuration source types
     */
    @GET
    @Path("/types/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of configuration source types defined on the server", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of configuration source types", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ConfigurationSourceType.class))))
    })
    public Response getConfigurationTypes() {
        return Response.ok(Collections.emptyList()).build();
    }

    /**
     * GET a single configuration source type
     *
     * @param typeId
     *            the configuration source type ID
     *
     * @return a configuration source type
     */
    @GET
    @Path("/types/{typeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a single configuration source type defined on the server", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a single configuration source type", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ConfigurationSourceType.class)))
    })
    public Response getConfigurationType(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not Implemented").build(); //$NON-NLS-1$
    }

    /**
     * GET a list of configuration instances of a given configuration source
     * type.
     *
     * @param typeId
     *            the configuration source type ID
     * @return list of configuration instances of a given configuration source
     *         type.
     */
    @GET
    @Path("/types/{typeId}/configs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of configurations that are instantiated of a given configuration source type", responses = {
            @ApiResponse(responseCode = "200", description = "Get the list of configuration descriptors ", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Configuration.class)))),
            @ApiResponse(responseCode = "404", description = EndpointConstants.NO_SUCH_CONFIGURATION, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getConfigurations(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId) {
        return Response.ok(Collections.emptyList()).build();
    }

    /**
     * POST a custom configuration to the server
     *
     * @param typeId
     *            the configuration source type ID
     * @param queryParameters
     *            the query parameters
     * @return status and collection of configuration descriptor, if successful
     */
    @POST
    @Path("/types/{typeId}/configs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a configuration instance for the given configuration source type", responses = {
            @ApiResponse(responseCode = "200", description = "The configuration instance was successfully created", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Configuration.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = EndpointConstants.NO_SUCH_CONFIGURATION, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal trace-server error while trying to create configuration instance", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response postConfiguration(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId,
            @RequestBody(description = CFG_CREATE_DESC + " " + CFG_KEYS_DESC, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + CFG_PATH_EX +
                            "}}"), schema = @Schema(implementation = ConfigurationQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not Implemented").build(); //$NON-NLS-1$
    }

    /**
     * GET a custom configuration by ID
     *
     * @param typeId
     *            the configuration source type ID
     * @param configId
     *            the configuration instance ID
     * @return status and the configuration instance, if successful
     */
    @GET
    @Path("/types/{typeId}/configs/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a configuration instance of a given configuration source type", responses = {
            @ApiResponse(responseCode = "200", description = "Get a configuration instance", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Configuration.class))),
            @ApiResponse(responseCode = "404", description = EndpointConstants.NO_SUCH_CONFIGURATION, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getConfiguration(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId,
            @Parameter(description = CFG_CONFIG_ID) @PathParam("configId") String configId) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not Implemented").build(); //$NON-NLS-1$
    }

    /**
     * PUT a custom configuration to the server
     *
     * @param typeId
     *            the configuration source type ID
     * @param configId
     *            the configuration instance ID
     * @param queryParameters
     *            the query parameters
     * @return status and the updated configuration instance, if successful
     */
    @PUT
    @Path("/types/{typeId}/configs/{configId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a configuration instance of a given configuration source type", responses = {
            @ApiResponse(responseCode = "200", description = "The configuration instance was successfully updated", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Configuration.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = EndpointConstants.NO_SUCH_CONFIGURATION, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal trace-server error while trying to update configuration instance", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response putConfiguration(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId,
            @Parameter(description = CFG_CONFIG_ID) @PathParam("configId") String configId,
            @RequestBody(description = CFG_UPDATE_DESC + " " + CFG_KEYS_DESC, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + CFG_PATH_EX +
                            "}}"), schema = @Schema(implementation = ConfigurationQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not Implemented").build(); //$NON-NLS-1$
    }

    /**
     * DELETE a configuration by type and instance id
     *
     * @param typeId
     *            the configuration source type ID
     * @param configId
     *            the configuration instance ID
     * @return status and the deleted configuration instance, if successful
     */
    @DELETE
    @Path("/types/{typeId}/configs/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a configuration instance of a given configuration source type", responses = {
            @ApiResponse(responseCode = "200", description = "The configuration instance was successfully deleted", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Configuration.class))),
            @ApiResponse(responseCode = "404", description = EndpointConstants.NO_SUCH_CONFIGURATION, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Internal trace-server error while trying to delete configuration instance", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response deleteConfiguration(@Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId,
            @Parameter(description = CFG_CONFIG_ID) @PathParam("configId") String configId) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not Implemented").build(); //$NON-NLS-1$
    }
}
