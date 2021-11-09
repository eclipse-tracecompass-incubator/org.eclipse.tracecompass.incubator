/*******************************************************************************
 * Copyright (c) 2017, 2021 Ericsson
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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TableColumnHeader;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeModelWrapper;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.VirtualTableModelWrapper;
import org.eclipse.tracecompass.internal.analysis.timing.core.event.matching.EventMatchingLatencyAnalysis;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.AnnotationCategoriesModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.AnnotationModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.IOutputAnnotationProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.TraceAnnotationProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlOutputElement;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils.OutputType;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.output.XmlDataProviderManager;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerConfigXmlParser;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerSet;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.TmfXmlUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModuleHelper;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
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
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to query the {@link ITmfTreeDataProvider}s
 *
 * @author Loic Prieur-Drevon
 */
@OpenAPIDefinition(info = @Info(title = EndpointConstants.TITLE, description = EndpointConstants.DESC, termsOfService = EndpointConstants.TERMS, contact = @Contact(email = EndpointConstants.EMAIL), license = @License(name = EndpointConstants.LICENSE, url = EndpointConstants.LICENSE_URL), version = EndpointConstants.VERSION), servers = {
        @Server(url = EndpointConstants.SERVER)
}, tags = {
        @Tag(name = EndpointConstants.ANN, description = "Retrieve annotations for different outputs."),
        @Tag(name = EndpointConstants.BMR, description = "How to bookmark areas of interest in the trace."),
        @Tag(name = EndpointConstants.DIA, description = "Refer to the server's status."),
        @Tag(name = EndpointConstants.DTR, description = "Learn about querying generic data tree models."),
        @Tag(name = EndpointConstants.EXP, description = "How to manage experiments on your server; an experiment represents a collection of traces, which can produce output models."),
        @Tag(name = EndpointConstants.FEA, description = "Discover the features which are available on a given server."),
        @Tag(name = EndpointConstants.FIL, description = "How to filter and query."),
        @Tag(name = EndpointConstants.STY, description = "Retrieve styles for different outputs."),
        @Tag(name = EndpointConstants.TGR, description = "Learn about querying Time Graph models."),
        @Tag(name = EndpointConstants.TRA, description = "How to manage physical traces on your server."),
        @Tag(name = EndpointConstants.VTB, description = "Learn about querying virtual table models (e.g. Events Table)."),
        @Tag(name = EndpointConstants.XML, description = "Learn about querying XML analyses."),
        @Tag(name = EndpointConstants.X_Y, description = "Learn about querying XY models.")
})
@SuppressWarnings("restriction")
@Path("/experiments/{expUUID}/outputs")
public class DataProviderService {
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(DataProviderService.class);

    private final DataProviderManager manager = DataProviderManager.getInstance();

    /**
     * Getter for the list of data provider descriptions
     *
     * @param expUUID
     *            UUID of the experiment to search for
     * @return the data provider descriptions with the queried {@link UUID} if it exists.
     */
    @GET
    @Tag(name = EndpointConstants.EXP)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProviders(@PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        List<IDataProviderDescriptor> list = DataProviderManager.getInstance().getAvailableProviders(experiment);
        list.addAll(getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.XY)));

        /*
         * Bug 576402:
         * Filter out all "Event Matching Analysis" related data providers, e.g. "Latency vs Time",
         * because are part in every experiment but won't provide any data.
         */
        list.removeIf(dp -> dp.getId().endsWith(EventMatchingLatencyAnalysis.ID));
        return Response.ok(list).build();
    }

    /**
     * Getter for the list of data provider descriptions
     *
     * @param expUUID
     *            UUID of the experiment to search for
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @return the data provider descriptions with the queried {@link UUID} if it exists.
     */
    @GET
    @Path("/{outputId}")
    @Tag(name = EndpointConstants.EXP)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProvider(@PathParam("expUUID") UUID expUUID, @PathParam("outputId") String outputId) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        List<IDataProviderDescriptor> list = DataProviderManager.getInstance().getAvailableProviders(experiment);
        list.addAll(getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.XY)));

        Optional<IDataProviderDescriptor> provider = list.stream().filter(p -> p.getId().equals(outputId)).findFirst();

        if (provider.isPresent()) {
            return Response.ok(provider.get()).build();
        }

        return Response.status(Status.NOT_FOUND).build();
    }

    /**
     * Query the provider for the XY tree
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch an XY tree as described by
     *            {@link QueryParameters}
     * @return an {@link GenericView} with the results
     */
    @POST
    @Path("/XY/{outputId}/tree")
    @Tag(name = EndpointConstants.X_Y)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXYTree(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId, QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the XY view
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch XY as described by {@link QueryParameters}
     * @return an {@link GenericView} with the results
     */
    @POST
    @Path("/XY/{outputId}/xy")
    @Tag(name = EndpointConstants.X_Y)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXY(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId, QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_OUTPUTID).build();
        }
        Map<String, Object> params = queryParameters.getParameters();
        if (params == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        SelectionTimeQueryFilter selectionTimeQueryFilter = FetchParametersUtils.createSelectionTimeQuery(params);
        if (selectionTimeQueryFilter == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getXY") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> provider = manager.getDataProvider(experiment,
                    outputId, ITmfTreeXYDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = getXmlProvider(experiment, outputId, EnumSet.of(OutputType.XY));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            TmfModelResponse<@NonNull ITmfXyModel> response = provider.fetchXY(params, null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for XY tooltip, currently not implemented
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
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
    @Tag(name = EndpointConstants.X_Y)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXYTooltip(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            @QueryParam("xValue") long xValue,
            @QueryParam("yValue") long yValue,
            @QueryParam("entryId") long entryId) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("XY tooltip are not implemented yet").build(); //$NON-NLS-1$
    }

    /**
     * Query the provider for the time graph tree
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph tree as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/tree")
    @Tag(name = EndpointConstants.TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeGraphTree(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the time graph states
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph states as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/states")
    @Tag(name = EndpointConstants.TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStates(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        SelectionTimeQueryFilter selectionTimeQueryFilter = FetchParametersUtils.createSelectionTimeQuery(params);
        if (selectionTimeQueryFilter == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStates") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            TmfModelResponse<TimeGraphModel> response = provider.fetchRowModel(params, null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for the time graph arrows
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph arrows as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/arrows")
    @Tag(name = EndpointConstants.TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArrows(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        TimeQueryFilter timeQueryFilter = FetchParametersUtils.createTimeQuery(params);
        if (timeQueryFilter == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getArrows") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> response = provider.fetchArrows(params, null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the provider for available marker sets
     *
     * @param expUUID
     *            desired experiment UUID
     * @return {@link TmfModelResponse} containing the list of {@link MarkerSet}s
     */
    @GET
    @Path("/markerSets")
    @Tag(name = EndpointConstants.ANN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMarkerSets(@PathParam("expUUID") UUID expUUID) {

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getMarkerSets").build()) { //$NON-NLS-1$
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }
            List<MarkerSet> markerSets = MarkerConfigXmlParser.getMarkerSets();
            return Response.ok(new TmfModelResponse<>(markerSets, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
        }
    }

    /**
     * Query the provider for all annotation categories
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param markerSetId
     *            marker set ID
     * @return {@link TmfModelResponse} containing {@link AnnotationCategoriesModel}
     */
    @GET
    @Path("/{outputId}/annotations")
    @Tag(name = EndpointConstants.ANN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnnotationCategories(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            @QueryParam("markerSetId") String markerSetId) {

        if (outputId == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_OUTPUTID).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getAnnotationCategories") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            boolean isComplete = true;
            AnnotationCategoriesModel model = null;
            // Fetch trace annotation categories
            TraceAnnotationProvider traceAnnotationProvider = ExperimentManagerService.getTraceAnnotationProvider(expUUID);
            if (traceAnnotationProvider != null) {
                // Parameter is only applicable for trace annotation provider
                @NonNull Map<@NonNull String, @NonNull Object> params =
                        (markerSetId == null) ? Collections.emptyMap() :
                            ImmutableMap.of(DataProviderParameterUtils.REQUESTED_MARKER_SET_KEY, markerSetId);
                TmfModelResponse<@NonNull AnnotationCategoriesModel> traceAnnotations = traceAnnotationProvider.fetchAnnotationCategories(params, null);
                if (traceAnnotations.getStatus() == ITmfResponse.Status.CANCELLED || traceAnnotations.getStatus() == ITmfResponse.Status.FAILED) {
                    return Response.ok(new TmfModelResponse<>(new AnnotationCategoriesModel(Collections.emptyList()), traceAnnotations.getStatus(), traceAnnotations.getStatusMessage())).build();
                }
                isComplete &= traceAnnotations.getStatus() == ITmfResponse.Status.COMPLETED;
                model = traceAnnotations.getModel();
            }
            // Fetch data provider annotation categories
            if (provider instanceof IOutputAnnotationProvider) {
                TmfModelResponse<@NonNull AnnotationCategoriesModel> annotations = ((IOutputAnnotationProvider) provider).fetchAnnotationCategories(Collections.emptyMap(), null);
                if (annotations.getStatus() == ITmfResponse.Status.CANCELLED || annotations.getStatus() == ITmfResponse.Status.FAILED) {
                    return Response.ok(new TmfModelResponse<>(new AnnotationCategoriesModel(Collections.emptyList()), annotations.getStatus(), annotations.getStatusMessage())).build();
                }
                isComplete &= annotations.getStatus() == ITmfResponse.Status.COMPLETED;
                model = AnnotationCategoriesModel.of(model, annotations.getModel());
            }

            if (isComplete) {
                return Response.ok(new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
            }
            return Response.ok(new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING)).build();
        }
    }

    /**
     * Query the provider for all annotations for the time range defined by Query parameters
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch annotations as described by
     *            {@link QueryParameters}
     * @return {@link TmfModelResponse} containing {@link AnnotationModel}
     */
    @POST
    @Path("/{outputId}/annotations")
    @Tag(name = EndpointConstants.ANN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnnotations(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getAnnotations") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            boolean isComplete = true;
            AnnotationModel model = null;

            // Fetch trace annotations
            TraceAnnotationProvider traceAnnotationProvider = ExperimentManagerService.getTraceAnnotationProvider(expUUID);
            if (traceAnnotationProvider != null) {
                TmfModelResponse<@NonNull AnnotationModel> traceAnnotations = traceAnnotationProvider.fetchAnnotations(params, null);
                if (traceAnnotations.getStatus() == ITmfResponse.Status.CANCELLED || traceAnnotations.getStatus() == ITmfResponse.Status.FAILED) {
                    return Response.ok(new TmfModelResponse<>(new AnnotationModel(Collections.emptyMap()), traceAnnotations.getStatus(), traceAnnotations.getStatusMessage())).build();
                }
                isComplete &= traceAnnotations.getStatus() == ITmfResponse.Status.COMPLETED;
                model = traceAnnotations.getModel();
            }
            // Fetch data provider annotations
            if (provider instanceof IOutputAnnotationProvider) {
                TmfModelResponse<@NonNull AnnotationModel> annotations = ((IOutputAnnotationProvider) provider).fetchAnnotations(params, null);
                if (annotations.getStatus() == ITmfResponse.Status.CANCELLED || annotations.getStatus() == ITmfResponse.Status.FAILED) {
                    return Response.ok(new TmfModelResponse<>(new AnnotationModel(Collections.emptyMap()), annotations.getStatus(), annotations.getStatusMessage())).build();
                }
                isComplete &= annotations.getStatus() == ITmfResponse.Status.COMPLETED;
                model = AnnotationModel.of(model, annotations.getModel());
            }

            @Nullable Set<@NonNull String> selectedCategories = DataProviderParameterUtils.extractSelectedCategories(params);
            if (selectedCategories != null && model != null) {
                // Make sure that only requested annotations are returned
                @NonNull Map<@NonNull String, @NonNull Collection<@NonNull Annotation>> allAnnotations = new LinkedHashMap<>(model.getAnnotations());
                allAnnotations.keySet().removeIf(cat -> !selectedCategories.contains(cat));
                model = new AnnotationModel(allAnnotations);
            }
            if (isComplete) {
                return Response.ok(new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
            }
            return Response.ok(new TmfModelResponse<>(model, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING)).build();
        }
    }

    /**
     * Query the provider for the time graph tooltips
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph tooltip as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/timeGraph/{outputId}/tooltip")
    @Tag(name = EndpointConstants.TGR)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimeGraphTooltip(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTimeGraphTooltip") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = provider.fetchTooltip(params, null);
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
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table columns as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/table/{outputId}/columns")
    @Tag(name = EndpointConstants.VTB)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getColumns(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {
        Response response = getTree(expUUID, outputId, queryParameters);
        Object entity = response.getEntity();
        if (!(entity instanceof TmfModelResponse<?>)) {
            return response;
        }
        Object model = ((TmfModelResponse<?>) entity).getModel();
        if (!(model instanceof TreeModelWrapper)) {
            return response;
        }
        List<@NonNull ITmfTreeDataModel> entries = ((TreeModelWrapper) model).getEntries();
        List<TableColumnHeader> columns = new ArrayList<>();
        for (ITmfTreeDataModel dataModel : entries) {
            columns.add(new TableColumnHeader(dataModel));
        }
        return Response.ok(new TmfModelResponse<>(columns, ((TmfModelResponse<?>) entity).getStatus(), ((TmfModelResponse<?>) entity).getStatusMessage())).build();
    }

    /**
     * Query the provider for table lines
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table lines as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/table/{outputId}/lines")
    @Tag(name = EndpointConstants.VTB)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLines(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getLines") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfVirtualTableDataProvider<? extends IVirtualTableLine, ? extends ITmfTreeDataModel> provider = manager.getDataProvider(experiment, outputId, ITmfVirtualTableDataProvider.class);
            if (provider == null) {
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            TmfModelResponse<?> response = provider.fetchLines(params, null);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                return Response.status(Status.UNAUTHORIZED).entity(response.getStatusMessage()).build();
            }
            return Response.ok(new TmfModelResponse<>(new VirtualTableModelWrapper((ITmfVirtualTableModel) response.getModel()), response.getStatus(), response.getStatusMessage())).build();
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

    private Response getTree(UUID expUUID, String outputId, QueryParameters queryParameters) {
        if (outputId == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_OUTPUTID).build();
        }
        Map<String, Object> params = queryParameters.getParameters();
        if (params == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        List<Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(params);
        if (timeRequested != null && timeRequested.size() == 1) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.INVALID_PARAMETERS).build();
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTree") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = getXmlProvider(experiment, outputId, EnumSet.allOf(OutputType.class));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }
            if (timeRequested == null || timeRequested.isEmpty()) {
                // Make a shallow copy to be able to modify the map
                params = new HashMap<>(params);
                timeRequested = ImmutableList.of(experiment.getStartTime().toNanos(), experiment.getEndTime().toNanos());
                params.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, timeRequested);
            }
            TmfModelResponse<?> treeResponse = provider.fetchTree(params, null);
            Object model = treeResponse.getModel();
            return Response.ok(model instanceof TmfTreeModel ? new TmfModelResponse<>(new TreeModelWrapper((TmfTreeModel<@NonNull ITmfTreeDataModel>) model), treeResponse.getStatus(), treeResponse.getStatusMessage()) : treeResponse).build();
        }
    }

    /**
     * Query the provider for styles
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Eclipse extension point ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch styles as described by
     *            {@link QueryParameters}
     * @return {@link GenericView} with the results
     */
    @POST
    @Path("/{outputId}/style")
    @Tag(name = EndpointConstants.STY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            QueryParameters queryParameters) {

        Map<String, Object> params = queryParameters.getParameters();
        Response errorResponse = validateQueryParameters(outputId, params);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStyles") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.NO_SUCH_TRACE).build();
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.getDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return Response.status(Status.METHOD_NOT_ALLOWED).entity(EndpointConstants.NO_PROVIDER).build();
            }

            if (provider instanceof IOutputStyleProvider) {
                TmfModelResponse<@NonNull OutputStyleModel> styleModelResponse = ((IOutputStyleProvider) provider).fetchStyle(params, null);
                return Response.ok(styleModelResponse).build();
            }

            // Return an empty model if the provider is not an
            // IOutputStyleProvider and let the client decide the style
            return Response.ok(new TmfModelResponse<>(new OutputStyleModel(Collections.emptyMap()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED)).build();
        }
    }

    private static Response validateQueryParameters(String outputId, Map<String, Object> params) {
        if (outputId == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_OUTPUTID).build();
        }
        if (params == null) {
            return Response.status(Status.BAD_REQUEST).entity(EndpointConstants.MISSING_PARAMETERS).build();
        }
        return null;
    }
}
