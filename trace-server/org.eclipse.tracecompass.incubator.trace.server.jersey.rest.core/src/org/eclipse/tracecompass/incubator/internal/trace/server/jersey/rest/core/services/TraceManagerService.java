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
import java.util.List;
import java.util.UUID;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 * Service to manage traces.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces")
public class TraceManagerService {

    /**
     * Getter method to access the list of traces
     *
     * @return a response containing the list of traces
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTraces() {
        return Response.ok(Collections2.filter(TmfTraceManager.getInstance().getOpenedTraces(),
                Predicates.not(TmfExperiment.class::isInstance))).build();
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
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response putTrace(@FormParam("name") @NotNull @Size(min = 1) String name,
            @FormParam("path") String path,
            @FormParam("typeID") String typeID) {
        Optional<@NonNull ITmfTrace> optional = Iterables.tryFind(TmfTraceManager.getInstance().getOpenedTraces(), t -> t.getPath().equals(path));
        if (optional.isPresent()) {
            return Response.status(Status.CONFLICT).entity(optional.get()).build();
        }
        if (!Paths.get(path).toFile().exists()) {
            return Response.status(Status.NOT_FOUND).entity("No trace at " + path).build(); //$NON-NLS-1$
        }
        try {
            ITmfTrace trace = put(path, name, typeID);
            if (trace == null) {
                return Response.status(Status.NOT_IMPLEMENTED).entity("Trace type not supported").build(); //$NON-NLS-1$
            }
            return Response.ok(trace).build();
        } catch (TmfTraceException | TmfTraceImportException | InstantiationException | IllegalAccessException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }
    }

    private ITmfTrace put(String path, String name, String typeID)
            throws TmfTraceException, TmfTraceImportException, InstantiationException, IllegalAccessException {
        List<TraceTypeHelper> traceTypes = TmfTraceType.selectTraceType(path, typeID);
        if (traceTypes.isEmpty()) {
            return null;
        }
        TraceTypeHelper helper = traceTypes.get(0);
        ITmfTrace trace = helper.getTraceClass().newInstance();
        trace.initTrace(null, path, ITmfEvent.class, name, typeID);
        trace.indexTrace(false);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, trace, null));
        return trace;
    }

    /**
     * Getter method to get a trace object
     *
     * @param uuid
     *            Unique trace ID
     * @return a response containing the trace
     */
    @GET
    @Path("/{uuid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTrace(@PathParam("uuid") @NotNull UUID uuid) {
        ITmfTrace trace = getTraceByUUID(uuid);
        if (trace == null || trace instanceof TmfExperiment) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(trace).build();
    }

    /**
     * Delete a trace from the manager and dispose of it
     *
     * @param uuid
     *            Unique trace ID
     * @return a not found response if there is no such trace or the entity.
     */
    @DELETE
    @Path("/{uuid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteTrace(@PathParam("uuid") @NotNull UUID uuid) {
        ITmfTrace trace = getTraceByUUID(uuid);
        if (trace == null || trace instanceof TmfExperiment) {
            return Response.status(Status.NOT_FOUND).build();
        }
        TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(this, trace));
        trace.dispose();
        return Response.ok(trace).build();
    }

    /**
     * Try and find a trace with the queried UUID in the {@link TmfTraceManager}.
     *
     * @param uuid
     *            queried {@link UUID}
     * @return the trace or null if none match.
     */
    public static @Nullable ITmfTrace getTraceByUUID(UUID uuid) {
        return Iterables.tryFind(TmfTraceManager.getInstance().getOpenedTraces(), t -> uuid.equals(t.getUUID())).orNull();
    }

}
