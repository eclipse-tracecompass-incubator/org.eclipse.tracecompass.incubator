/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Service to manage traces. Singleton ensures that there is only one per
 * server.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces")
public class TraceManagerService {
    @Context
    TraceManager traceManager;

    /**
     * Getter method to access the list of traces
     *
     * @return a response containing the list of traces
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTraces() {
        GenericEntity<Collection<TraceModel>> entity = new GenericEntity<Collection<TraceModel>>(traceManager.getTraces()) {
        };
        return Response.ok().entity(entity).build();
    }

    /**
     * Method to put trace to the service. TODO only CTF traces are supported for
     * now...
     *
     * @param name
     *            the name to assign to the trace files
     * @param path
     *            the path to the trace
     * @return the new trace model object or the exception if it failed to load.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response putTrace(@FormParam("name") String name, @FormParam("path") String path) {
        if (name == null) {
            return Response.status(Status.NO_CONTENT).entity("Invalid name (null)").build(); //$NON-NLS-1$
        }
        TraceModel traceModel = traceManager.get(name);
        if (traceModel != null) {
            return Response.status(Status.CONFLICT).entity(traceModel).build();
        }
        try {
            TraceModel model = put(path, name);
            return Response.ok().entity(model).build();
        } catch (TmfTraceException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }
    }

    /**
     * Delete a trace from the manager and dispose of it
     *
     * @param name
     *            name of the trace
     * @return a not found response if there is no such trace or the entity.
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteTrace(@QueryParam("name") String name) {
        if (name == null) {
            return Response.status(Status.NO_CONTENT).entity("Invalid name (null)").build(); //$NON-NLS-1$
        }
        TraceModel model = traceManager.remove(NonNullUtils.nullToEmptyString(name));
        return Response.ok().entity(model).build();
    }

    private TraceModel put(String path, @NonNull String name) throws TmfTraceException {
        ITmfTrace trace = new CtfTmfTrace();
        trace.initTrace(null, path, ITmfEvent.class);
        trace.indexTrace(false);
        TraceModel model = new TraceModel(name, trace);
        traceManager.put(name, model);
        return model;
    }

}
