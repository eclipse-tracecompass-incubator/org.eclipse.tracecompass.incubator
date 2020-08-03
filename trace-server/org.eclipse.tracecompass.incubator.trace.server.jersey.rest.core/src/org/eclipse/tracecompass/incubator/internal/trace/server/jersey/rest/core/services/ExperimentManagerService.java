/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.component.ITmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.base.Optional;
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
     * Get the outputs for an experiment
     *
     * @param uuid
     *            UUID of the experiment to get the outputs for
     * @return The outputs for the experiment
     */
    @GET
    @Path("/{uuid}/outputs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutputs(@PathParam("uuid") UUID uuid) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not implemented for " + uuid).build(); //$NON-NLS-1$
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
     *  @param queryParameters
     *            Parameters to post a experiment as described by
     *            {@link QueryParameters}
     *            - name -> name for the experiment.
     *            - traces -> List of UUID strings of the traces to add to the experiment
     *
     * @return no content response if one of the trace {@link UUID}s does not map to
     *         any trace.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postExperiment(QueryParameters queryParameters) {
        Map<String, Object> parameters = queryParameters.getParameters();
        String name = (String) parameters.get("name"); //$NON-NLS-1$
        Object traces = parameters.get("traces"); //$NON-NLS-1$
        if (! (traces instanceof List<?>)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        List<?> traceUUIDs = (List<?>) traces;
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath ipath = root.getLocation().append(EXPERIMENTS).append(name);
        IResource resource = root.findMember(ipath);

        Optional<@NonNull ITmfTrace> optional = Iterables.tryFind(TmfTraceManager.getInstance().getOpenedTraces(), t -> t.getPath().equals(ipath.toOSString()));
        if (optional.isPresent()) {
            ITmfTrace trace = optional.get();
            if (!(trace instanceof TmfExperiment)) {
                // Something else than an experiment exists with that name,
                // return a conflict
                return Response.status(Status.CONFLICT).entity(optional.get()).build();
            }
            TmfExperiment experiment = (TmfExperiment) trace;
            if (experiment.getChildren().size() != traceUUIDs.size()) {
                // Not the same number of children, the current and posted
                // experiments are different
                return Response.status(Status.CONFLICT).entity(optional.get()).build();
            }
            for (Object uuidObj : traceUUIDs) {
                if (uuidObj instanceof String) {
                    UUID uuid = UUID.fromString((String) uuidObj);
                    ITmfTrace childTrace = TraceManagerService.getTraceByUUID(uuid);
                    if (childTrace == null) {
                        return Response.noContent().build();
                    }
                    ITmfEventProvider child = experiment.getChild(childTrace.getName());
                    if (child == null || !(child instanceof ITmfTrace) || !uuid.equals(((ITmfTrace) child).getUUID())) {
                        // The requested child is not a child of the
                        // experiment, the experiment is different
                        return Response.status(Status.CONFLICT).entity(optional.get()).build();
                    }
                } else {
                    return Response.status(Status.BAD_REQUEST).build();
                }
            }
            // It's the same experiment, return OK
            return Response.ok(experiment).build();

        }

        ITmfTrace[] array = new ITmfTrace[traceUUIDs.size()];
        int i = 0;
        for (Object uuidObj : traceUUIDs) {
            String uuidStr;
            if (uuidObj instanceof String) {
                uuidStr = (String) uuidObj;
            } else {
                return Response.status(Status.BAD_REQUEST).build();
            }
            UUID uuid = UUID.fromString(uuidStr);
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.noContent().build();
            }
            // traces in the experiment need to be cloned.
            try {
                ITmfTrace clone = trace.getClass().getDeclaredConstructor().newInstance();
                clone.initTrace(trace.getResource(), trace.getPath(), ITmfEvent.class, trace.getName(), trace.getTraceTypeId());
                array[i++] = clone;
            } catch (InstantiationException | IllegalAccessException | TmfTraceException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        }

        TmfExperiment experiment = new TmfExperiment(ITmfEvent.class, ipath.toOSString(), array, TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, resource);
        experiment.indexTrace(false);
        // read first event to make sure start time is initialized
        ITmfContext ctx = experiment.seekEvent(0);
        experiment.getNext(ctx);
        ctx.dispose();

        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, experiment, null));
        return Response.ok(experiment).build();
    }

}
