/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.GenericView;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.events.TmfEventTableFilterModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.EventTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.EventTableLine;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlOutputElement;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils.OutputType;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlDataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;

import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;

/**
 * Service to query the {@link ITmfTreeDataProvider}s
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/traces/{uuid}/providers/{providerId}")
public class DataProviderService {
    private static final String NO_PROVIDER = "Analysis cannot run"; //$NON-NLS-1$
    private static final String NO_SUCH_TRACE = "No Such Trace"; //$NON-NLS-1$

    private final DataProviderManager manager = DataProviderManager.getInstance();

    /**
     * Query the provider for the entry tree
     *
     * @param uuid
     *            desired trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @return an {@link GenericView} with the results
     */
    @GET
    @Path("/tree")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTree(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("start") long start,
            @QueryParam("end") @DefaultValue("1") long end,
            @QueryParam("nb") @DefaultValue("2") int nb) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(trace,
                providerId, ITmfTreeDataProvider.class);

        if (provider == null && providerId != null) {
            // try and find the XML provider for the ID.
            provider = getXmlProvider(trace, providerId, EnumSet.allOf(OutputType.class));
        }

        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        TmfModelResponse<?> treeResponse = provider.fetchTree(new TimeQueryFilter(start, end, nb), null);
        return Response.ok(new GenericView<>(trace, treeResponse)).build();
    }

    /**
     * Query the provider for the XY view
     *
     * @param uuid
     *            {@link UUID} of the trace to query
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @param ids
     *            ids of the entries to query
     * @return an {@link GenericView} with the results
     */
    @GET
    @Path("/xy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("start") long start,
            @QueryParam("end") long end,
            @QueryParam("nb") @Min(1) int nb,
            @QueryParam("ids") @NotNull Set<Long> ids) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> provider = manager.getDataProvider(trace,
                providerId, ITmfTreeXYDataProvider.class);

        if (provider == null && providerId != null) {
            // try and find the XML provider for the ID.
            provider = getXmlProvider(trace, providerId, EnumSet.of(OutputType.XY));
        }

        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        TmfModelResponse<@NonNull ITmfXyModel> response = provider.fetchXY(new SelectionTimeQueryFilter(start, end, nb, ids), null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    /**
     * Query the provider for the time graph states
     *
     * @param uuid
     *            desired trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @param ids
     *            ids of the entries to query
     * @return an {@link GenericView} with the results
     */
    @GET
    @Path("/states")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStates(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("start") long start,
            @QueryParam("end") long end,
            @QueryParam("nb") int nb,
            @QueryParam("ids") @NotNull Set<Long> ids) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, providerId);

        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        TmfModelResponse<List<@NonNull ITimeGraphRowModel>> response = provider.fetchRowModel(new SelectionTimeQueryFilter(start, end, nb, ids), null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    /**
     * Query the provider for the time graph arrows
     *
     * @param uuid
     *            desired trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @return an {@link GenericView} with the results
     */
    @GET
    @Path("/arrows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArrows(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("start") long start,
            @QueryParam("end") long end,
            @QueryParam("nb") int nb) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, providerId);

        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> response = provider.fetchArrows(new TimeQueryFilter(start, end, nb), null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    /**
     * Query the provider for the time graph tooltips
     *
     * @param uuid
     *            desired trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param start
     *            lower bound for the query
     * @param end
     *            upper bound for the query
     * @param nb
     *            nanoseconds between two data points
     * @param ids
     *            ids of the entries to query
     * @return an {@link GenericView} with the results
     */
    @GET
    @Path("/tooltip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTooltip(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("start") long start,
            @QueryParam("end") long end,
            @QueryParam("nb") int nb,
            @QueryParam("ids") @NotNull Set<Long> ids) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, providerId);

        if (provider == null) {
            // The analysis cannot be run on this trace
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = provider.fetchTooltip(new SelectionTimeQueryFilter(start, end, nb, ids), null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    private ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> getTimeGraphProvider(@NonNull ITmfTrace trace, String providerId) {
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = manager.getDataProvider(trace,
                providerId, ITimeGraphDataProvider.class);

        if (provider == null && providerId != null) {
            // try and find the XML provider for the ID.
            provider = getXmlProvider(trace, providerId, EnumSet.of(OutputType.TIME_GRAPH));
        }
        return provider;
    }

    /**
     * Query the provider for table lines
     *
     * @param uuid
     *            Trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param low
     *            Low index
     * @param size
     *            Number of lines to return
     * @param columnIds
     *            Desired column IDs
     * @return A {@link GenericView} with the results
     */
    @GET
    @Path("/lines")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLines(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("low") @Min(0) long low,
            @QueryParam("size") @Min(0) int size,
            @QueryParam("columnId") List<@NonNull Long> columnIds) {

        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        ITmfVirtualTableDataProvider<? extends IVirtualTableLine, ? extends ITmfTreeDataModel> provider = manager.getDataProvider(trace, providerId, ITmfVirtualTableDataProvider.class);
        if (provider == null) {
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        VirtualTableQueryFilter queryFilter = new VirtualTableQueryFilter(columnIds, low, size);
        TmfModelResponse<?> response = provider.fetchLines(queryFilter, null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    /**
     * Query the provider for event table lines with filters
     *
     * @param uuid
     *            Trace UUID
     * @param providerId
     *            Eclipse extension point ID for the data provider to query
     * @param low
     *            Low index
     * @param size
     *            Number of lines to return
     * @param columnIds
     *            Desired column IDs
     * @param presetFilter
     *            List of preset filter IDs to apply
     * @param isCollapseFilterEnabled
     *            True if a collapse filter should be applied
     * @param multivaluedMap
     *            Map that contains the table filter (column ID with the associated
     *            regex)
     * @return A {@link GenericView} with the results
     */
    @PUT
    @Path("/lines")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLines(@PathParam("uuid") UUID uuid,
            @PathParam("providerId") String providerId,
            @QueryParam("low") @Min(0) long low,
            @QueryParam("size") @Min(0) int size,
            @QueryParam("columnId") List<@NonNull Long> columnIds,
            @FormParam("presetFilterId") List<@NonNull String> presetFilter,
            @FormParam("collapseFilter") boolean isCollapseFilterEnabled,
            MultivaluedMap<@NonNull String, @NonNull String> multivaluedMap) {

        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
        }

        TmfEventTableDataProvider provider = manager.getDataProvider(trace, providerId, TmfEventTableDataProvider.class);
        if (provider == null) {
            return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
        }

        /*
         * The multivaluedMap contains all the entry from the form, including the
         * presetFilter list and the collapse boolean. In order to extract the table
         * filters (mapping between column ID and regex) the tryParse is use to capture
         * just the keys that are longs (column IDs)
         */
        Map<@NonNull Long, @NonNull String> tableFilter = null;
        for (Entry<String, List<String>> paramEntry : multivaluedMap.entrySet()) {
            Long columnId = Longs.tryParse(paramEntry.getKey());
            if (columnId == null) {
                continue;
            }

            if (tableFilter == null) {
                tableFilter = new HashMap<>();
            }
            tableFilter.put(columnId, paramEntry.getValue().get(0));
        }

        TmfEventTableFilterModel filters = new TmfEventTableFilterModel(tableFilter, null, presetFilter, null, isCollapseFilterEnabled);
        EventTableQueryFilter queryFilter = new EventTableQueryFilter(columnIds, low, size, filters);
        TmfModelResponse<@NonNull ITmfVirtualTableModel<@NonNull EventTableLine>> response = provider.fetchLines(queryFilter, null);
        return Response.ok(new GenericView<>(trace, response)).build();
    }

    /**
     * Get the XML data provider for a trace, provider id and XML {@link OutputType}
     *
     * @param trace
     *            the queried trace
     * @param id
     *            the queried ID
     * @param types
     *            the data provider type
     * @return the provider if an XML containing the ID exists and applies to the
     *         trace, else null
     */
    private static <@Nullable P extends ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel>> P
        getXmlProvider(@NonNull ITmfTrace trace, @NonNull String id, EnumSet<OutputType> types) {
        for (OutputType viewType : types) {
            for (XmlOutputElement element : Iterables.filter(XmlUtils.getXmlOutputElements().values(),
                    element -> element.getXmlElem().equals(viewType.getXmlElem()) && id.equals(element.getId()))) {
                Element viewElement = TmfXmlUtils.getElementInFile(element.getPath(), viewType.getXmlElem(), id);
                if (viewElement != null && viewType == OutputType.XY) {
                    return (P) XmlDataProviderManager.getInstance().getXyProvider(trace, viewElement);
                } else if (viewElement != null && viewType == OutputType.TIME_GRAPH) {
                    return (P) XmlDataProviderManager.getInstance().getTimeGraphProvider(trace, viewElement);
                }
            }
        }
        return null;
    }

}
