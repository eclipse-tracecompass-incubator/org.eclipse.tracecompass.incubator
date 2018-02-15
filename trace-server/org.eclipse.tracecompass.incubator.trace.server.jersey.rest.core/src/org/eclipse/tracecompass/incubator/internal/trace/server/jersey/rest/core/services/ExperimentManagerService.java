/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.List;
import java.util.UUID;

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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 * Service to manage experiments
 *
 * @author Loic Prieur-Drevon
 */
@Path("/experiments")
public class ExperimentManagerService {

    private static final String EXPERIMENTS = "experiments"; //$NON-NLS-1$

    /**
     * Getter for the list of experiments from the trace manager
     *
     * @return The set of opened experiments
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExperiments() {
        return Response.ok(Collections2.filter(TmfTraceManager.getInstance().getOpenedTraces(), TmfExperiment.class::isInstance)).build();
    }

    /**
     * Getter for an experiment by {@link UUID}.
     *
     * @param uuid
     *            UUID of the experiment to search for
     *
     * @return The experiment with the queried {@link UUID} if it exists.
     */
    @GET
    @Path("/{uuid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExperiment(@PathParam("uuid") UUID uuid) {
        ITmfTrace experiment = TraceManagerService.getTraceByUUID(uuid);
        if (experiment instanceof TmfExperiment) {
            return Response.ok(experiment).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    /**
     * Delete an experiment by {@link UUID}.
     *
     * @param uuid
     *            UUID of the experiment to delete
     *
     * @return The experiment with the queried {@link UUID} if it exists.
     */
    @DELETE
    @Path("/{uuid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteExperiment(@PathParam("uuid") UUID uuid) {
        ITmfTrace experiment = TraceManagerService.getTraceByUUID(uuid);
        if (experiment instanceof TmfExperiment) {
            TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(this, experiment));
            experiment.dispose();
            return Response.ok(experiment).build();
        }
        return Response.status(Status.NOT_FOUND).build();
    }

    /**
     * Post a new experiment encapsulating the traces from the list of
     * {@link UUID}s.
     *
     * @param name
     *            name for the experiment.
     * @param traceUUIDs
     *            {@link UUID}s of the traces to add to the experiment
     * @return no content response if one of the trace {@link UUID}s does not map to
     *         any trace.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response postExperiment(@FormParam("name") String name,
            @FormParam("traces") List<UUID> traceUUIDs) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath ipath = root.getLocation().append(EXPERIMENTS).append(name);
        IResource resource = root.findMember(ipath);

        if (Iterables.any(TmfTraceManager.getInstance().getOpenedTraces(), t -> t.getPath().equals(ipath.toOSString()))) {
            return Response.status(Status.CONFLICT).entity("There is already an experiment named: " + name).build(); //$NON-NLS-1$
        }

        ITmfTrace[] array = new ITmfTrace[traceUUIDs.size()];
        int i = 0;
        for (UUID uuid : traceUUIDs) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.noContent().build();
            }
            // traces in the experiment need to be cloned.
            try {
                ITmfTrace clone = trace.getClass().newInstance();
                clone.initTrace(trace.getResource(), trace.getPath(), ITmfEvent.class, trace.getName(), trace.getTraceTypeId());
                array[i++] = clone;
            } catch (InstantiationException | IllegalAccessException | TmfTraceException e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        }
        TmfExperiment experiment = new TmfExperiment(ITmfEvent.class, ipath.toOSString(), array, TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, resource);
        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, experiment, null));
        return Response.ok(experiment).build();
    }

}
