/*******************************************************************************
 * Copyright (c) 2017, 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.GenericView;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.VirtualTableQueryFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlOutputElement;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils.OutputType;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.output.XmlDataProviderManager;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModuleHelper;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;

import com.google.common.collect.Iterables;

/**
 * Service to query the {@link ITmfTreeDataProvider}s
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/experiments/{uuid}/outputs")
public class DataProviderService {
    private static final String WRONG_PARAMETERS = "Wrong query parameters"; //$NON-NLS-1$
    private static final String NO_PROVIDER = "Analysis cannot run"; //$NON-NLS-1$
    private static final String NO_SUCH_TRACE = "No Such Trace"; //$NON-NLS-1$
    private static final String MISSING_OUTPUTID = "Missing parameter outputId"; //$NON-NLS-1$
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(DataProviderService.class);

    private final DataProviderManager manager = DataProviderManager.getInstance();

    /**
     * Getter for the list of data provider descriptions
     *
     * @param uuid
     *            UUID of the trace to search for
     * @return the data provider descriptions with the queried {@link UUID} if it exists.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProviders(@PathParam("uuid") UUID uuid) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        List<IDataProviderDescriptor> list = DataProviderManager.getInstance().getAvailableProviders(trace);
        list.addAll(getXmlDataProviderDescriptors(trace, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(getXmlDataProviderDescriptors(trace, EnumSet.of(OutputType.XY)));

        return Response.ok(list).build();
    }

    /**
     * Getter for the list of data provider descriptions
     *
     * @param uuid
     *            UUID of the trace to search for
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @return the data provider descriptions with the queried {@link UUID} if it exists.
     */
    @GET
    @Path("/{outputId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProvider(@PathParam("uuid") UUID uuid, @PathParam("outputId") String outputId) {
        ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        List<IDataProviderDescriptor> list = DataProviderManager.getInstance().getAvailableProviders(trace);
        list.addAll(getXmlDataProviderDescriptors(trace, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(getXmlDataProviderDescriptors(trace, EnumSet.of(OutputType.XY)));

        Optional<IDataProviderDescriptor> provider = list.stream().filter(p -> p.getId().equals(outputId)).findFirst();

        if (provider.isPresent()) {
            return Response.ok(provider.get()).build();
        }

        return Response.status(Status.NOT_FOUND).build();
    }

    /**
     * Query the provider for the XY tree
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch an XY tree as described by
     *            {@link QueryParameters}
     * @return an {@link GenericView} with the results
     */
    @POST
    @Path("/XY/{outputId}/tree")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXYTree(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId, QueryParameters queryParameters) {
        return getTree(uuid, outputId, queryParameters);
    }

    /**
     * Query the provider for the XY view
     *
     * @param uuid
     *            {@link UUID} of the trace to query
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch XY as described by {@link QueryParameters}
     * @return an {@link GenericView} with the results
     */
    @POST
    @Path("/XY/{outputId}/xy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId, QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getXY") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> provider = manager.getDataProvider(trace,
                    outputId, ITmfTreeXYDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = getXmlProvider(trace, outputId, EnumSet.of(OutputType.XY));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            SelectionTimeQueryFilter selectionTimeQueryFilter = FetchParametersUtils.createSelectionTimeQuery(queryParameters.getParameters());
            if (selectionTimeQueryFilter == null) {
                return Response.status(Status.UNAUTHORIZED).entity(WRONG_PARAMETERS).build();
            }

            TmfModelResponse<@NonNull ITmfXyModel> response = provider.fetchXY(queryParameters.getParameters(), null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for XY tooltip, currently not implemented
     *
     * @param uuid
     *            {@link UUID} of the trace to query
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param xValue
     *            Given X value to fetch the tooltip
     * @param yValue
     *            Given Y value to help fetch the tooltip, used to get the right
     *            point if two points have the same X value
     * @param entryId
     *            Entry Id or series Id
     * @return {@link GenericView} with the results
     */
    @GET
    @Path("/XY/{outputId}/tooltip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXYTooltip(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            @QueryParam("xValue") long xValue,
            @QueryParam("yValue") long yValue,
            @QueryParam("entryId") long entryId) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("XY tooltip are not implemented yet").build(); //$NON-NLS-1$
    }

    /**
     * Query the provider for the time graph tree
     *
     * @param uuid
     *            {@link UUID} of the trace to query
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph tree as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/tree")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeGraphTree(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        return getTree(uuid, outputId, queryParameters);
    }

    /**
     * Query the provider for the time graph states
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph states as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/states")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStates(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStates") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            SelectionTimeQueryFilter selectionTimeQueryFilter = FetchParametersUtils.createSelectionTimeQuery(queryParameters.getParameters());
            if (selectionTimeQueryFilter == null) {
                return Response.status(Status.UNAUTHORIZED).entity(WRONG_PARAMETERS).build();
            }

            TmfModelResponse<TimeGraphModel> response = provider.fetchRowModel(queryParameters.getParameters(), null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for the time graph arrows
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph arrows as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/arrows")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArrows(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getArrows") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            TimeQueryFilter timeQueryFilter = FetchParametersUtils.createTimeQuery(queryParameters.getParameters());
            if (timeQueryFilter == null) {
                return Response.status(Status.UNAUTHORIZED).entity(WRONG_PARAMETERS).build();
            }

            TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> response = provider.fetchArrows(queryParameters.getParameters(), null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for the time graph tooltips
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param time
     *            Time where to fetch the tooltip
     * @param entryId
     *            Entry Id to identify the state for the tooltip
     * @param targetId
     *            Target Id if the tooltip is for an arrow
     * @return {@link GenericView} with the results
     */
    @GET
    @Path("/timeGraph/{outputId}/tooltip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeGraphTooltip(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            @QueryParam("time") long time,
            @QueryParam("entryId") long entryId,
            @QueryParam("targetId") long targetId) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTimeGraphTooltip") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(trace, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(time, time, 1, Arrays.asList(entryId, targetId))), null);
            return Response.ok(response).build();
        }
    }

    private ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> getTimeGraphProvider(@NonNull ITmfTrace trace, String outputId) {
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = manager.getDataProvider(trace,
                outputId, ITimeGraphDataProvider.class);

        if (provider == null && outputId != null) {
            // try and find the XML provider for the ID.
            provider = getXmlProvider(trace, outputId, EnumSet.of(OutputType.TIME_GRAPH));
        }
        return provider;
    }

    /**
     * Query the provider for table columns
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table columns as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/table/{outputId}/columns")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColumns(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        return getTree(uuid, outputId, queryParameters);
    }

    /**
     * Query the provider for table lines
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table lines as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/table/{outputId}/lines")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLines(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getLines") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITmfVirtualTableDataProvider<? extends IVirtualTableLine, ? extends ITmfTreeDataModel> provider = manager.getDataProvider(trace, outputId, ITmfVirtualTableDataProvider.class);
            if (provider == null) {
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            VirtualTableQueryFilter tableQueryFilter = FetchParametersUtils.createVirtualTableQueryFilter(queryParameters.getParameters());
            if (tableQueryFilter == null) {
                return Response.status(Status.UNAUTHORIZED).entity(WRONG_PARAMETERS).build();
            }

            TmfModelResponse<?> response = provider.fetchLines(queryParameters.getParameters(), null);
            return Response.ok(response).build();
        }
    }

    /**
     * Get the XML data provider for a trace, provider id and XML
     * {@link OutputType}
     *
     * @param trace
     *            the queried trace
     * @param id
     *            the queried ID
     * @param types
     *            the data provider type
     * @return the provider if an XML containing the ID exists and applies to
     *         the trace, else null
     */
    private static <@Nullable P extends ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel>> P getXmlProvider(@NonNull ITmfTrace trace, @NonNull String id, EnumSet<OutputType> types) {
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

    private static @NonNull List<IDataProviderDescriptor> getXmlDataProviderDescriptors(@NonNull ITmfTrace trace, EnumSet<OutputType> types) {
        /*
         *  TODO: find a better way to create the data provider descriptors.
         *  This should be part of the XmlDataProviderManager.
         */
        List<IDataProviderDescriptor> descriptors = new ArrayList<>();
        Map<String, IAnalysisModuleHelper> modules = TmfAnalysisManager.getAnalysisModules(trace.getClass());
        for (OutputType viewType : types) {
            for (XmlOutputElement element : Iterables.filter(XmlUtils.getXmlOutputElements().values(), element -> element.getXmlElem().equals(viewType.getXmlElem()))) {
                DataProviderDescriptor.Builder builder = new DataProviderDescriptor.Builder();
                String label = String.valueOf(element.getLabel());
                String elemId = element.getId();
                if (elemId == null) {
                    // Ignore element
                    continue;
                }
                builder.setId(elemId).setName(label).setDescription(label);
                if (viewType == OutputType.XY) {
                    builder.setProviderType(ProviderType.TREE_TIME_XY);
                } else if (viewType == OutputType.TIME_GRAPH) {
                    builder.setProviderType(ProviderType.TIME_GRAPH);
                }
                for (String id : element.getAnalyses()) {
                    if (modules.containsKey(id)) {
                        descriptors.add(builder.build());
                        break;
                    }
                }
            }
        }
        return descriptors;
    }

    private Response getTree(UUID uuid, String outputId, QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTree") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(trace,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = getXmlProvider(trace, outputId, EnumSet.allOf(OutputType.class));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            TimeQueryFilter timeQueryFilter = FetchParametersUtils.createTimeQuery(queryParameters.getParameters());
            if (timeQueryFilter == null) {
                return Response.status(Status.UNAUTHORIZED).entity(WRONG_PARAMETERS).build();
            }

            TmfModelResponse<?> treeResponse = provider.fetchTree(queryParameters.getParameters(), null);
            return Response.ok(treeResponse).build();
        }
    }

    /**
     * Query the provider for styles
     *
     * @param uuid
     *            desired trace UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch styles as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/{outputId}/style")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@PathParam("uuid") UUID uuid,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity(MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStyles") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            ITmfTrace trace = TraceManagerService.getTraceByUUID(uuid);
            if (trace == null) {
                return Response.status(Status.NOT_FOUND).entity(NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(trace,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(NO_PROVIDER).build();
            }

            if (provider instanceof IOutputStyleProvider) {
                TmfModelResponse<@NonNull OutputStyleModel> styleModelResponse = ((IOutputStyleProvider) provider).fetchStyle(queryParameters.getParameters(), null);
                return Response.ok(styleModelResponse).build();
            }

            // Return an empty model if the provider is not an
            // IOutputStyleProvider and let the client decide the style
            return Response.ok(new TmfModelResponse<>(new OutputStyleModel(Collections.emptyMap()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
        }
    }
}
