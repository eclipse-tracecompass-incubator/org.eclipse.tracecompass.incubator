/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

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
    public Response putTrace(@PathParam("name") @NotNull @Size(min = 1) String name,
            @FormParam("path") String path,
            @FormParam("typeID") String typeID) {
        TraceModel traceModel = traceManager.get(name);
        if (traceModel != null) {
            return Response.status(Status.CONFLICT).entity(traceModel).build();
        }
        if (!Paths.get(path).toFile().exists()) {
            return Response.status(Status.NOT_FOUND).entity("No trace at " + path).build(); //$NON-NLS-1$
        }
        try {
            TraceModel model = put(path, name, typeID);
            if (model == null) {
                return Response.status(Status.NOT_IMPLEMENTED).entity("Trace type not supported").build(); //$NON-NLS-1$
            }
            return Response.ok().entity(model).build();
        } catch (TmfTraceException | TmfTraceImportException | InstantiationException | IllegalAccessException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }
    }

    private TraceModel put(String path, String name, String typeID)
            throws TmfTraceException, TmfTraceImportException, InstantiationException, IllegalAccessException {
        List<TraceTypeHelper> traceTypes = TmfTraceType.selectTraceType(path, typeID);
        if (traceTypes.isEmpty()) {
            return null;
        }
        TraceTypeHelper helper = Iterables.get(traceTypes, 0);
        ITmfTrace trace = helper.getTraceClass().newInstance();
        trace.initTrace(null, path, ITmfEvent.class, name, typeID);
        trace.indexTrace(false);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, trace, null));
        TraceModel model = new TraceModel(trace);
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
        TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(this, trace.getTrace()));
        return Response.ok().entity(trace).build();
    }

}
