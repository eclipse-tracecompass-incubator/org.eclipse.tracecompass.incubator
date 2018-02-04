/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.DisksIODataProvider;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeView;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.XYView;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfCommonXAxisModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

/**
 * Service to query the DiskIO usage View
 * <p>
 * TODO this could be generalized to all XY views, see TODOs in the getXY method
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/traces")
public class DiskActivityViewService {
    @Context TmfTraceManager traceManager;

    /**
     * Query the provider for the entry tree
     *
     * @param traceName
     *            name of the trace to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @return an {@link XYView} with the results
     */
    @GET
    @Path("/{name}/DiskIO/tree")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTree(@PathParam(value = "name") String traceName,
            @QueryParam(value = "start") long start,
            @QueryParam(value = "end") long end,
            @QueryParam(value = "nb") int nb) {
        Optional<@NonNull ITmfTrace> optional = Iterables.tryFind(traceManager.getOpenedTraces(), t -> t.getName().equals(traceName));
        if (!optional.isPresent()) {
            return Response.status(Status.NOT_FOUND).entity("No Such Trace").build(); //$NON-NLS-1$
        }

        ITmfTrace trace = optional.get();
        DisksIODataProvider provider = DataProviderManager.getInstance().getDataProvider(trace,
                DisksIODataProvider.ID, DisksIODataProvider.class);
        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity("Analysis cannot run").build(); //$NON-NLS-1$
        }

        TmfModelResponse<@NonNull List<@NonNull TmfTreeDataModel>> response = provider.fetchTree(new TimeQueryFilter(start, end, nb), null);
        return Response.ok().entity(new TreeView(trace, response)).build();
    }

    /**
     * Query the provider for the XY view
     *
     * @param traceName
     *            name of the trace to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @param ids
     *            ids of the entries to query
     * @return an {@link XYView} with the results
     */
    @GET
    @Path("/{name}/DiskIO/xy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam(value = "name") String traceName,
            @QueryParam(value = "start") long start,
            @QueryParam(value = "end") long end,
            @QueryParam(value = "nb") @Min(1) int nb,
            @QueryParam(value = "ids") @NotNull Set<Long> ids) {
        Optional<@NonNull ITmfTrace> optional = Iterables.tryFind(traceManager.getOpenedTraces(), t -> t.getName().equals(traceName));
        if (!optional.isPresent()) {
            return Response.status(Status.NOT_FOUND).entity("No Such Trace").build(); //$NON-NLS-1$
        }

        ITmfTrace trace = optional.get();
        DisksIODataProvider provider = DataProviderManager.getInstance().getDataProvider(trace,
                DisksIODataProvider.ID, DisksIODataProvider.class);
        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity("Analysis cannot run").build(); //$NON-NLS-1$
        }

        TmfModelResponse<@NonNull ITmfCommonXAxisModel> response = provider.fetchXY(new SelectionTimeQueryFilter(start, end, nb, ids), null);
        return Response.ok().entity(new XYView(trace, response)).build();
    }

}
