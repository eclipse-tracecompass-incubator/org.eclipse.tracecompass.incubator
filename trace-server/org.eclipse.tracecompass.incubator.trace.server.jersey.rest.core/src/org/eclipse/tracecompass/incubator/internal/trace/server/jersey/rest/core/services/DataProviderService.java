/*******************************************************************************
 * Copyright (c) 2017, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ANN;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_CREATE_DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_KEYS_DESC;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_OUTPUT_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CFG_TYPE_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.COLUMNS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.COLUMNS_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CONSISTENT_PARENT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.COUNT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.COUNT_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DERIVED_OUTPUT_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DIRECTION;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DIRECTION_COUNT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DIRECTION_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DP_CFG_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.DT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ELEMENT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ELEMENT_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXP;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXPRESSIONS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXPRESSIONS_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXP_UUID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.FILTER_QUERY_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.FILTER_QUERY_PARAMETERS_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.GXY;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INDEX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INDEX_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INVALID_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ITEMS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ITEMS_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ITEMS_EX_TT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ITEMS_TT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ITEMS_XY;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MARKER_CATEGORIES;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MARKER_CATEGORIES_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MARKER_SET;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MARKER_SET_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MARKER_SET_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_OUTPUTID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_PROVIDER;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_CONFIGURATION_TYPE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_DERIVED_PROVIDER;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_PROVIDER;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_TRACE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.OCG;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.ONE_OF;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.OUTPUT_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.PARENT_OUTPUT_ID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.PROVIDER_CONFIG_NOT_FOUND;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.PROVIDER_NOT_FOUND;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.STY;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TABLE_TIMES;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TGR;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMERANGE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMERANGE_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMERANGE_EX_TREE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMERANGE_SAMPLING_EX;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMERANGE_TREE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMES_EX_TT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TIMES_TT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TREE_ENTRIES;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.VTB;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.X_Y;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import javax.ws.rs.DELETE;
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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.AnnotationCategoriesResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.AnnotationResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.AnnotationsQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ArrowsQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.DataProvider;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.DataTreeResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.GenericXYQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.LinesQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.MarkerSetsResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.OptionalQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.RequestedQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.StylesResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TableColumnHeadersResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TimeGraphArrowsResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TimeGraphStatesResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TimeGraphTooltipResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TimeGraphTreeResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TooltipQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.VirtualTableResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.XYResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.XYTreeResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.ConfigurationQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.OutputConfigurationQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TableColumnHeader;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeModelWrapper;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.VirtualTableModelWrapper;
import org.eclipse.tracecompass.internal.analysis.timing.core.event.matching.EventMatchingLatencyAnalysis;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableDataProvider;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.module.XmlUtils.OutputType;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.output.XmlDataProviderManager;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerConfigXmlParser;
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerSet;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.tmf.core.model.annotations.AnnotationCategoriesModel;
import org.eclipse.tracecompass.tmf.core.model.annotations.AnnotationModel;
import org.eclipse.tracecompass.tmf.core.model.annotations.IOutputAnnotationProvider;
import org.eclipse.tracecompass.tmf.core.model.annotations.TraceAnnotationProvider;
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
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.FlowScopeLogBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to query the {@link ITmfTreeDataProvider}s
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@Path("/experiments/{expUUID}/outputs")
public class DataProviderService {

    // InAndOut analysis ID
    private static final String INANDOUT_ANALYSIS_ID = "org.eclipse.tracecompass.incubator.inandout.analysis"; //$NON-NLS-1$

    // Reports factory ID
    private static final String REPORTS_FACTORY_ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.reportsDataProviderFactory"; //$NON-NLS-1$

    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(DataProviderService.class);

    private final DataProviderManager manager = DataProviderManager.getInstance();
    private final XmlDataProviderManager xmlManager = XmlDataProviderManager.getInstance();

    /**
     * Getter for the list of data provider descriptions
     *
     * @param expUUID
     *            UUID of the experiment to search for
     * @return a {@link Response} with the result, if successful a list of
     *         {@link IDataProviderDescriptor}
     */
    @GET
    @Tag(name = EXP)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of outputs for this experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of output provider descriptors", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DataProvider.class)))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getProviders(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
        }
        List<IDataProviderDescriptor> list = DataProviderManager.getInstance().getAvailableProviders(experiment);
        list.addAll(xmlManager.getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(xmlManager.getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.XY)));
        list.sort(Comparator.comparing(IDataProviderDescriptor::getName));

        /*
         * Remove unwanted data providers:
         *
         * - Bug 576402:
         *   Filter out all "Event Matching Analysis" related data providers, e.g. "Latency vs Time",
         *   because are part in every experiment but won't provide any data.
         *
         * - Legacy InAndOut
         *   Remove default InAndOutAnalysis used in legacy Trace Compass with Eclipse UI
         *   For trace server, InAndOutAnlaysis is done using the createDataProvider endpoint below.
         *
         * - Reports Factory
         *   Remove the reports factory data provider as it is not yet supported in the frond-end products.
         */
        list.removeIf(dp -> dp.getId().endsWith(EventMatchingLatencyAnalysis.ID) || dp.getId().endsWith(INANDOUT_ANALYSIS_ID) || dp.getId().endsWith(REPORTS_FACTORY_ID));

        return Response.ok(list).build();
    }

    /**
     * Getter for a specific data provider description
     *
     * @param expUUID
     *            UUID of the experiment to search for
     * @param outputId
     *            Output ID for the data provider to query
     * @return a {@link Response} with the result, if successful a
     *         {@link IDataProviderDescriptor}
     */
    @GET
    @Path("/{outputId}")
    @Tag(name = EXP)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the output descriptor for this experiment and output", responses = {
            @ApiResponse(responseCode = "200", description = "Returns the output provider descriptor", content = @Content(schema = @Schema(implementation = DataProvider.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getProvider(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
        }

        IDataProviderDescriptor provider = getDescriptor(experiment, outputId);
        if (provider != null) {
            return Response.ok(provider).build();
        }
        return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
    }

    /**
     * Query the provider for the data tree entries.
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch a data tree as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link TreeModelWrapper}
     */
    @POST
    @Path("/data/{outputId}/tree")
    @Tag(name = DT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the data tree", description = TREE_ENTRIES, responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of data tree entries. " +
                    CONSISTENT_PARENT, content = @Content(schema = @Schema(implementation = DataTreeResponse.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getDataTree(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the data tree entries. " + TIMERANGE_TREE, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX_TREE +
                            "}}"), schema = @Schema(implementation = TreeQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the XY tree
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch an XY tree as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link TreeModelWrapper}
     */
    @POST
    @Path("/XY/{outputId}/tree")
    @Tag(name = X_Y)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the XY tree", description = TREE_ENTRIES, responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of XY entries. " +
                    CONSISTENT_PARENT, content = @Content(schema = @Schema(implementation = XYTreeResponse.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getXYTree(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the XY tree. " + TIMERANGE_TREE, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX_TREE +
                            "}}"), schema = @Schema(implementation = TreeQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the XY view
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch XY as described by {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link ITmfXyModel}
     */
    @POST
    @Path("/XY/{outputId}/xy")
    @Tag(name = X_Y)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the XY model", description = "Unique endpoint for all xy models, " +
            "ensures that the same template is followed for all endpoints.", responses = {
                    @ApiResponse(responseCode = "200", description = "Return the queried XYResponse", content = @Content(schema = @Schema(implementation = XYResponse.class))),
                    @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getXY(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the XY model. " + TIMERANGE + " " + ITEMS_XY, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX + "," + ITEMS_EX +
                            "}}"), schema = @Schema(implementation = RequestedQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getXY") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeXYDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = xmlManager.getXmlProvider(experiment, outputId, EnumSet.of(OutputType.XY));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateRequestedQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
            }

            TmfModelResponse<@NonNull ITmfXyModel> response = provider.fetchXY(params, null);
            return Response.ok(response).build();
        }
    }

    /**
     * Query the generic xy chart data provider for its tree structure.
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters used to request the tree and axis descriptions, as
     *            defined in {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link TreeModelWrapper}
     */
    @POST
    @Path("/genericXY/{outputId}/tree")
    @Tag(name = GXY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the tree for generic xy chart", description = TREE_ENTRIES, responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of generic xy chart entries. " +
                    CONSISTENT_PARENT, content = @Content(schema = @Schema(implementation = XYTreeResponse.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getGenericXYChartTree(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the generic XY tree. " + TIMERANGE_TREE, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX_TREE +
                            "}}"), schema = @Schema(implementation = TreeQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the generic xy chart.
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch xy as described by {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link ITmfXyModel}
     */
    @POST
    @Path("/genericXY/{outputId}/xy")
    @Tag(name = GXY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the xy model", description = "Unique endpoint for all xy models, " +
            "ensures that the same template is followed for all endpoints.", responses = {
                    @ApiResponse(responseCode = "200", description = "Return the queried xy response", content = @Content(schema = @Schema(implementation = XYResponse.class))),
                    @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getGenericXY(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the xy model. " + TIMERANGE + " " + ITEMS_XY, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_SAMPLING_EX + "," + ITEMS_EX +
                            "}}"), schema = @Schema(implementation = GenericXYQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#fetchXY") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeXYDataProvider<@NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeXYDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateGenericXYQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);            }

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
     *            Output ID for the data provider to query
     * @param xValue
     *            Given X value to fetch the tooltip
     * @param yValue
     *            Given Y value to help fetch the tooltip, used to get the right
     *            point if two points have the same X value
     * @param entryId
     *            Entry Id or series Id
     * @return a {@link Response} with the result
     */
    @GET
    @Hidden
    @Path("/XY/{outputId}/tooltip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getXYTooltip(@PathParam("expUUID") UUID expUUID,
            @PathParam("outputId") String outputId,
            @QueryParam("xValue") long xValue,
            @QueryParam("yValue") long yValue,
            @QueryParam("entryId") long entryId) {
        return ErrorResponseUtil.newErrorResponse(Status.NOT_IMPLEMENTED, "XY tooltip are not implemented yet"); //$NON-NLS-1$
    }

    /**
     * Query the provider for the time graph tree
     *
     * @param expUUID
     *            {@link UUID} of the experiment to query
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph tree as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link TreeModelWrapper}
     */
    @POST
    @Path("/timeGraph/{outputId}/tree")
    @Tag(name = TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the Time Graph tree", description = TREE_ENTRIES, responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of Time Graph entries. " +
                    CONSISTENT_PARENT, content = @Content(schema = @Schema(implementation = TimeGraphTreeResponse.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getTimeGraphTree(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the timegraph tree. " + TIMERANGE_TREE, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX_TREE +
                            "}}"), schema = @Schema(implementation = TreeQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {
        return getTree(expUUID, outputId, queryParameters);
    }

    /**
     * Query the provider for the time graph states
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph states as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link TimeGraphModel}
     */
    @POST
    @Path("/timeGraph/{outputId}/states")
    @Tag(name = TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the Time Graph states", description = "Unique entry point for all TimeGraph states, ensures that the same template is followed for all views", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of time graph rows", content = @Content(schema = @Schema(implementation = TimeGraphStatesResponse.class))),
            @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getStates(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the timegraph states. " + TIMERANGE + " " + ITEMS + " " + FILTER_QUERY_PARAMETERS, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX + "," + ITEMS_EX + "," + FILTER_QUERY_PARAMETERS_EX +
                            "}}"), schema = @Schema(implementation = RequestedQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStates") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateRequestedQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
            }

            errorMessage = QueryParametersUtil.validateFilterQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
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
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph arrows as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a list of {@link ITimeGraphArrow}
     */
    @POST
    @Path("/timeGraph/{outputId}/arrows")
    @Tag(name = TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the Time Graph arrows", description = "Unique entry point for all TimeGraph models, " +
            "ensures that the same template is followed for all models", responses = {
                    @ApiResponse(responseCode = "200", description = "Returns a sampled list of TimeGraph arrows", content = @Content(schema = @Schema(implementation = TimeGraphArrowsResponse.class))),
                    @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getArrows(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the timegraph arrows. " + TIMERANGE, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMERANGE_EX +
                            "}}"), schema = @Schema(implementation = ArrowsQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getArrows") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateArrowsQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
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
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a list of {@link MarkerSet}
     */
    @GET
    @Path("/markerSets")
    @Tag(name = ANN)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get marker sets available for this experiment", responses = {
            @ApiResponse(responseCode = "200", description = "List of marker sets", content = @Content(schema = @Schema(implementation = MarkerSetsResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getMarkerSets(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getMarkerSets").build()) { //$NON-NLS-1$
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
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
     *            Output ID for the data provider to query
     * @param markerSetId
     *            marker set ID
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link AnnotationCategoriesModel}
     */
    @GET
    @Path("/{outputId}/annotations")
    @Tag(name = ANN)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get annotation categories associated to this experiment and output", responses = {
            @ApiResponse(responseCode = "200", description = "Annotation categories", content = @Content(schema = @Schema(implementation = AnnotationCategoriesResponse.class))),
            @ApiResponse(responseCode = "400", description = MISSING_OUTPUTID, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAnnotationCategories(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @Parameter(description = MARKER_SET_ID) @QueryParam("markerSetId") String markerSetId) {

        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID);
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getAnnotationCategories") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
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
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch annotations as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link AnnotationModel}
     */
    @POST
    @Path("/{outputId}/annotations")
    @Tag(name = ANN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the annotations associated to this experiment and output", responses = {
            @ApiResponse(responseCode = "200", description = "Annotation", content = @Content(schema = @Schema(implementation = AnnotationResponse.class))),
            @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getAnnotations(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the annotations. " +
                    TIMERANGE + " " + ITEMS + " " + MARKER_SET + MARKER_CATEGORIES, content = {
                            @Content(examples = @ExampleObject("{\"parameters\":{" +
                                    TIMERANGE_EX + "," + ITEMS_EX + "," + MARKER_SET_EX + MARKER_CATEGORIES_EX +
                                    "}}"), schema = @Schema(implementation = AnnotationsQueryParameters.class))
                    }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getAnnotations") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateAnnotationsQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
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
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch time graph tooltip as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a map of key-value pairs
     */
    @POST
    @Path("/timeGraph/{outputId}/tooltip")
    @Tag(name = TGR)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get a Time Graph tooltip", description = "Endpoint to retrieve tooltips for time graph", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a map of tooltip keys to values", content = @Content(schema = @Schema(implementation = TimeGraphTooltipResponse.class))),
            @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getTimeGraphTooltip(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the timegraph tooltip. " + TIMES_TT + ITEMS_TT + ELEMENT, content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{" + TIMES_EX_TT + ITEMS_EX_TT + ELEMENT_EX +
                            "}}"), schema = @Schema(implementation = TooltipQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTimeGraphTooltip") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = getTimeGraphProvider(experiment, outputId);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateTooltipQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
            }

            TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = provider.fetchTooltip(params, null);
            return Response.ok(response).build();
        }
    }

    private ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> getTimeGraphProvider(@NonNull ITmfTrace trace, String outputId) {
        ITimeGraphDataProvider<@NonNull ITimeGraphEntryModel> provider = manager.fetchOrCreateDataProvider(trace,
                outputId, ITimeGraphDataProvider.class);

        if (provider == null && outputId != null) {
            // try and find the XML provider for the ID.
            provider = xmlManager.getXmlProvider(trace, outputId, EnumSet.of(OutputType.TIME_GRAPH));
        }
        return provider;
    }

    /**
     * Query the provider for table columns
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table columns as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a list of {@link TableColumnHeader}
     */
    @POST
    @Path("/table/{outputId}/columns")
    @Tag(name = VTB)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get table columns", description = "Unique entry point for output providers, " +
            "to get the column entries", responses = {
                    @ApiResponse(responseCode = "200", description = "Returns a list of table headers", content = @Content(schema = @Schema(implementation = TableColumnHeadersResponse.class))),
                    @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public Response getColumns(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the table columns", content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{}}"), schema = @Schema(implementation = OptionalQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        Map<String, Object> params = queryParameters.getParameters();
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getColumns") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfVirtualTableDataProvider<? extends @NonNull ITmfTreeDataModel, ? extends IVirtualTableLine> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfVirtualTableDataProvider.class);
            if (provider == null) {
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            TmfModelResponse<?> response = provider.fetchColumns(params, null);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, response.getStatusMessage());
            }
            List<ITableColumnDescriptor> model = (List<ITableColumnDescriptor>) response.getModel();
            if (model == null) {
                return Response.ok(response).build();
            }
            List<@NonNull TableColumnHeader> columns = new ArrayList<>();
            model.forEach(descriptor -> columns.add(new TableColumnHeader(descriptor)));
            return Response.ok(new TmfModelResponse<>(columns, response.getStatus(), response.getStatusMessage())).build();
        }
    }

    /**
     * Query the provider for table lines
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch table lines as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link VirtualTableModelWrapper}
     */
    @POST
    @Path("/table/{outputId}/lines")
    @Tag(name = VTB)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get virtual table lines", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a table model with a 2D array of strings and metadata", content = @Content(schema = @Schema(implementation = VirtualTableResponse.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Error reading the experiment", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getLines(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the table lines. " + ONE_OF +
                    INDEX + TABLE_TIMES + COUNT + COLUMNS + EXPRESSIONS + DIRECTION + DIRECTION_COUNT, content = {
                            @Content(examples = @ExampleObject("{\"parameters\":{" +
                                    INDEX_EX + COUNT_EX + COLUMNS_EX + EXPRESSIONS_EX + DIRECTION_EX +
                                    "}}"), schema = @Schema(implementation = LinesQueryParameters.class))
                    }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getLines") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfVirtualTableDataProvider<? extends IVirtualTableLine, ? extends ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment, outputId, ITmfVirtualTableDataProvider.class);
            if (provider == null) {
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }

            Map<String, Object> params = queryParameters.getParameters();
            String errorMessage = QueryParametersUtil.validateLinesQueryParameters(params);
            if (errorMessage != null) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
            }

            TmfModelResponse<?> response = provider.fetchLines(params, null);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, response.getStatusMessage());
            }
            return Response.ok(new TmfModelResponse<>(new VirtualTableModelWrapper((ITmfVirtualTableModel) response.getModel()), response.getStatus(), response.getStatusMessage())).build();
        }
    }

    private Response getTree(UUID expUUID, String outputId, QueryParameters queryParameters) {
        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        Map<String, Object> params = queryParameters.getParameters();
        String errorMessage = QueryParametersUtil.validateTreeQueryParameters(params);
        if (errorMessage != null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getTree") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // try and find the XML provider for the ID.
                provider = xmlManager.getXmlProvider(experiment, outputId, EnumSet.allOf(OutputType.class));
            }

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
            }
            List<Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(params);
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
     *            Output ID for the data provider to query
     * @param queryParameters
     *            Parameters to fetch styles as described by
     *            {@link QueryParameters}
     * @return a {@link Response} with the result, if successful a
     *         {@link TmfModelResponse} with a {@link OutputStyleModel}
     */
    @POST
    @Path("/{outputId}/style")
    @Tag(name = STY)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "API to get the style map associated to this experiment and output", responses = {
            @ApiResponse(responseCode = "200", description = "Style model that can be used jointly with OutputElementStyle to retrieve specific style values", content = @Content(schema = @Schema(implementation = StylesResponse.class))),
            @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "405", description = NO_PROVIDER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getStyles(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @RequestBody(description = "Query parameters to fetch the style map", content = {
                    @Content(examples = @ExampleObject("{\"parameters\":{}}"), schema = @Schema(implementation = OptionalQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        Response errorResponse = validateParameters(outputId, queryParameters);
        if (errorResponse != null) {
            return errorResponse;
        }
        Map<String, Object> params = queryParameters.getParameters();
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#getStyles") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            ITmfTreeDataProvider<? extends @NonNull ITmfTreeDataModel> provider = manager.fetchOrCreateDataProvider(experiment,
                    outputId, ITmfTreeDataProvider.class);

            if (provider == null) {
                // The analysis cannot be run on this trace
                return ErrorResponseUtil.newErrorResponse(Status.METHOD_NOT_ALLOWED, NO_PROVIDER);
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

    /**
     * Query the data provider for a list of available configuration source
     * types.
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @return a {@link Response} with the result, if successful a
     *         {@link IDataProviderDescriptor}
     */
    @GET
    @Path("/{outputId}/configTypes/")
    @Tag(name = OCG)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of configuration types defined on the server for a given output and experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of configuration types that this output supports.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ConfigurationSourceType.class)))),
            @ApiResponse(responseCode = "404", description = PROVIDER_CONFIG_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response getConfigurationTypes(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId
            ) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
        }

        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID);
        }

        IDataProviderDescriptor sourceDescriptor = getDescriptor(experiment, outputId);
        IDataProviderFactory factory = manager.getFactory(outputId);
        if (sourceDescriptor == null || factory == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
        }

        ITmfDataProviderConfigurator configurator = factory.getAdapter(ITmfDataProviderConfigurator.class);
        if (configurator != null) {
            return Response.ok(configurator.getConfigurationSourceTypes()).build();
        }
        return Response.ok(Collections.emptyList()).build();
    }

    /**
     * Query the provider to get one selected configuration source type.
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID for the data provider to query
     * @param typeId
     *            the configuration source type ID
     * @return a {@link Response} with the result, if successful a
     *         {@link ITmfConfigurationSourceType}
     */
    @GET
    @Path("/{outputId}/configTypes/{typeId}")
    @Tag(name = OCG)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a single configuration source type defined on the server for a given data provider and experiment.", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a single configuration source type", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ConfigurationSourceType.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_CONFIG_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response getConfigurationType(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = OUTPUT_ID) @PathParam("outputId") String outputId,
            @Parameter(description = CFG_TYPE_ID) @PathParam("typeId") String typeId) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
        }

        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID);
        }

        IDataProviderDescriptor sourceDescriptor = getDescriptor(experiment, outputId);
        IDataProviderFactory factory = manager.getFactory(outputId);
        if (sourceDescriptor == null || factory == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
        }

        ITmfDataProviderConfigurator configurator = factory.getAdapter(ITmfDataProviderConfigurator.class);
        if (configurator == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
        }

        Optional<ITmfConfigurationSourceType> optional = configurator.getConfigurationSourceTypes().stream().filter(type -> type.getId().equals(typeId)).findAny();

        if (!optional.isPresent()) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_CONFIGURATION_TYPE);
        }
        return Response.ok(optional.get()).build();
    }

    /**
     * POST a custom configuration to the data provider to create derived data
     * providers.
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            ID of the output provider to create a derived output from
     * @param queryParameters
     *            the query parameters used to create a output provider
     * @return a {@link Response} with the result, if successful a
     *         {@link IDataProviderDescriptor}
     */
    @SuppressWarnings("null")
    @POST
    @Path("/{outputId}")
    @Tag(name = OCG)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a derived data provider from a input configuration", responses = {
            @ApiResponse(responseCode = "200", description = "Returns the derived data provider descriptor.", content = @Content(schema = @Schema(implementation = DataProvider.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_CONFIG_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response createProvider(
                @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
                @Parameter(description = CFG_OUTPUT_ID) @PathParam("outputId") String outputId,
                @RequestBody(description = CFG_CREATE_DESC + " " + CFG_KEYS_DESC, content = {
                        @Content(examples = @ExampleObject(DP_CFG_EX), schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.OutputConfigurationQueryParameters.class))
                }, required = true) OutputConfigurationQueryParameters queryParameters) {

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#createDataProvider") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            Response errorResponse = validateOutputConfigParameters(outputId, queryParameters);
            if (errorResponse != null) {
                return errorResponse;
            }

            IDataProviderDescriptor parentDescriptor = getDescriptor(experiment, outputId);
            if (parentDescriptor == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER + ": " + outputId); //$NON-NLS-1$
            }

            IDataProviderFactory factory = manager.getFactory(outputId);
            if (factory == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
            }

            ITmfDataProviderConfigurator configurator = factory.getAdapter(ITmfDataProviderConfigurator.class);
            if (configurator == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
            }

            ITmfConfiguration inputConfig = new TmfConfiguration.Builder()
                    .setName(queryParameters.getName())
                    .setDescription(queryParameters.getDescription())
                    .setSourceTypeId(queryParameters.getSourceTypeId())
                    .setParameters(queryParameters.getParameters())
                    .build();
            String typeId = inputConfig.getSourceTypeId();
            Optional<ITmfConfigurationSourceType> optional = configurator.getConfigurationSourceTypes().stream().filter(type -> type.getId().equals(typeId)).findAny();
            if (!optional.isPresent()) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_CONFIGURATION_TYPE);
            }
            IDataProviderDescriptor returnDescr = configurator.createDataProviderDescriptors(experiment, inputConfig);
            return Response.ok(returnDescr).build();
        } catch (TmfConfigurationException e) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * DELETE a derived data provider created from a given data provider
     *
     * @param expUUID
     *            desired experiment UUID
     * @param outputId
     *            Output ID of the parent output provider
     * @param derivedOutputId
     *            Output ID for the derived output provider to delete
     * @return a {@link Response} with the result, if successful a
     *         {@link IDataProviderDescriptor}
     */
    @DELETE
    @Path("/{outputId}/{derivedOutputId}")
    @Tag(name = OCG)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a derived output (and its configuration).", responses = {
            @ApiResponse(responseCode = "200", description = "Returns the deleted derived data provider descriptor. The derived data provider (and its configuration) was successfully deleted.", content = @Content(schema = @Schema(implementation = DataProvider.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = PROVIDER_CONFIG_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public Response deleteDerivedProvider(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = PARENT_OUTPUT_ID) @PathParam("outputId") String outputId,
            @Parameter(description = DERIVED_OUTPUT_ID) @PathParam("derivedOutputId") String derivedOutputId) {

        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID + " (parent)"); //$NON-NLS-1$
        }

        if (derivedOutputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID + " (derived)"); //$NON-NLS-1$
        }

        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DataProviderService#removeDataProvider") //$NON-NLS-1$
                .setCategory(outputId).build()) {
            TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
            if (experiment == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_TRACE);
            }

            IDataProviderDescriptor parentDescriptor = getDescriptor(experiment, outputId);
            if (parentDescriptor == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER + ": " + outputId); //$NON-NLS-1$
            }

            IDataProviderDescriptor derivedDescriptor = getDescriptor(experiment, derivedOutputId);
            if (derivedDescriptor == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_DERIVED_PROVIDER + ": " + derivedOutputId); //$NON-NLS-1$
            }

            IDataProviderFactory factory = manager.getFactory(outputId);
            if (factory == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
            }

            ITmfDataProviderConfigurator configurator = factory.getAdapter(ITmfDataProviderConfigurator.class);
            if (configurator == null) {
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, NO_SUCH_PROVIDER);
            }

            // Get all descriptors of all corresponding derived data providers
            List<IDataProviderDescriptor> allDerivedDps = manager.getAvailableProviders(experiment, derivedDescriptor.getConfiguration());

            // Remove any derived data provider instances stored in the manager
            allDerivedDps.forEach(desc -> manager.removeDataProvider(experiment, desc.getId()));

            // Clean-up configuration
            configurator.removeDataProviderDescriptor(experiment, derivedDescriptor);

            return Response.ok(derivedDescriptor).build();
        } catch (TmfConfigurationException e) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, e.getMessage());
        }
    }

    private static Response validateParameters(String outputId, QueryParameters queryParameters) {
        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID);
        }
        if (queryParameters == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_PARAMETERS);
        }
        return null;
    }

    private static Response validateOutputConfigParameters(String outputId, ConfigurationQueryParameters queryParameters) {
        if (outputId == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_OUTPUTID);
        }
        if (queryParameters == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_PARAMETERS);
        }

        return null;
    }

    private @Nullable IDataProviderDescriptor getDescriptor(@NonNull ITmfTrace experiment, @NonNull String outputId) {
        List<IDataProviderDescriptor> list = manager.getAvailableProviders(experiment);
        list.addAll(xmlManager.getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.TIME_GRAPH)));
        list.addAll(xmlManager.getXmlDataProviderDescriptors(experiment, EnumSet.of(OutputType.XY)));

        Optional<IDataProviderDescriptor> provider = list.stream().filter(p -> p.getId().equals(outputId)).findFirst();
        if (provider.isPresent()) {
            return provider.get();
        }
        return null;
    }

}
