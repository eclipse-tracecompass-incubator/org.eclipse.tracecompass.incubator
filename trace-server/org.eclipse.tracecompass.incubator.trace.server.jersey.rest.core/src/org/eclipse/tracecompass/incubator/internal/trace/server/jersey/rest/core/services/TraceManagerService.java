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
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;

import com.google.common.collect.Iterables;

/**
 * Service to manage traces.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces")
public class TraceManagerService {
    @Context TraceManager traceManager;

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
     * Method to open the trace, initialize it, index it and add it to the trace
     * manager.
     *
     * @param name
     *            the name to assign to the trace files
     * @param path
     *            the path to the trace
     * @param typeID
     *            the ID of a trace (like "o.e.l.specifictrace" )
     * @return the new trace model object or the exception if it failed to load.
     */
    @POST
    @Path("/{name}")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response putTrace(@NotNull @PathParam("name") String name,
            @FormParam("path") String path,
            @FormParam("typeID") String typeID) {
        if (name == null) {
            // parameter validation does not apply to FormParam.
            return Response.status(Status.NO_CONTENT).entity("Invalid name (null)").build(); //$NON-NLS-1$
        }
        TraceModel traceModel = traceManager.get(name);
        if (traceModel != null) {
            return Response.status(Status.CONFLICT).entity(traceModel).build();
        }
        try {
            TraceModel model = put(path, name, typeID);
            if (model == null) {
                return Response.status(Status.NOT_IMPLEMENTED).entity("Trace type not supported").build(); //$NON-NLS-1$
            }
            return Response.ok().entity(model).build();
        } catch (TmfTraceException | TmfTraceImportException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }
    }

    private TraceModel put(String path, @NonNull String name, String typeID) throws TmfTraceException, TmfTraceImportException {
        List<TraceTypeHelper> traceTypes = TmfTraceType.selectTraceType(path, typeID);
        if (traceTypes.isEmpty()) {
            return null;
        }
        TraceTypeHelper helper = Iterables.get(traceTypes, 0);
        ITmfTrace trace = helper.getTrace();
        trace.initTrace(null, path, ITmfEvent.class);
        trace.indexTrace(false);
        if (trace instanceof TmfTrace) {
            ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        }
        TraceModel model = new TraceModel(name, trace);
        traceManager.put(name, model);
        return model;
    }

    /**
     * Delete a trace from the manager and dispose of it
     *
     * @param name
     *            name of the trace
     * @return a not found response if there is no such trace or the entity.
     */
    @DELETE
    @Path("/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteTrace(@PathParam("name") @NotNull String name) {
        TraceModel trace = traceManager.remove(name);
        if (trace == null) {
            return Response.ok().status(Status.NOT_FOUND).build();
        }
        return Response.ok().entity(trace).build();
    }

}
