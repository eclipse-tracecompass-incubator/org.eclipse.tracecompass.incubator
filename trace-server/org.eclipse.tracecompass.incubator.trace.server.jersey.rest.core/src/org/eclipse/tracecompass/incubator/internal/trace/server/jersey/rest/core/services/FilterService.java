/**********************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

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

/**
 * Service to manage filters
 *
 * @author Simon Delisle
 */
@Path("/filters")
public class FilterService {

    /**
     * Get a list of all the filters available on the server
     *
     * @return List of filters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilters() {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Create a new filter on the server
     *
     * @param filter
     *            Filter to create
     * @return Created filter
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFilter(Object filter) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a specific filter
     *
     * @param filterId
     *            Filter Id
     * @return Filter
     */
    @GET
    @Path("/{filterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilter(@PathParam("filterId") String filterId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Update a specific filter
     *
     * @param filterId
     *            Filter Id
     * @param filter
     *            Filter used to update the given filter
     * @return Updated filter
     */
    @PUT
    @Path("/{filterId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFilter(@PathParam("filterId") String filterId,
            Object filter) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a specific filter
     *
     * @param filterId
     *            Id of the filter to delete
     * @return Deleted filter
     */
    @DELETE
    @Path("/{filterId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFilter(@PathParam("filterId") String filterId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }
}
