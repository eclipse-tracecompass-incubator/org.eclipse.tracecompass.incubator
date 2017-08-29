/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.DataProviderManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.data.TraceManager;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.xy.XYView;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfCommonXAxisModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfXYDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;

/**
 * Service to query the DiskIO usage View
 * <p>
 * TODO this could be generalized to all XY views, see TODOs in the getXY method
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/traces")
public class DiskActivityViewService {
    private static final @NonNull String DISK_ACTIVITY_VIEW = "DiskActivityView"; //$NON-NLS-1$
    @Context
    TraceManager traceManager;
    @Context
    DataProviderManager analysisManager;

    /**
     * Query the state system for the XY view
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
    @Path("/{name}/DiskIO")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam(value = "name") String traceName, @QueryParam(value = "start") long start,
            @QueryParam(value = "end") long end, @QueryParam(value = "nb") int nb) {
        TraceModel traceModel = traceManager.get(traceName);
        if (traceModel == null) {
            return Response.status(Status.NOT_FOUND).entity("No Such Trace").build(); //$NON-NLS-1$
        }
        ITmfXYDataProvider provider = analysisManager.get(traceModel, DISK_ACTIVITY_VIEW);
        if (provider == null) {
            // The analysis has not been run yet
            // TODO use a plug-in to identify the DataProvider
            provider = DisksIODataProvider.create(traceModel.getTrace());
            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity("Analysis cannot run").build(); //$NON-NLS-1$
            }
            analysisManager.put(traceModel, DISK_ACTIVITY_VIEW, provider);
        }
        // TODO allow the Data provider to create its time query filter from the form.
        TmfModelResponse<@NonNull ITmfCommonXAxisModel> response = provider.fetchXY(new TimeQueryFilter(start, end, nb), null);
        if (response.getStatus() == TmfModelResponse.Status.CANCELLED
                || response.getStatus() == TmfModelResponse.Status.FAILED) {
            return Response.serverError().entity(response.getStatusMessage()).build();
        }

        return Response.ok().entity(new XYView(traceModel, response.getModel())).build();
    }

}
