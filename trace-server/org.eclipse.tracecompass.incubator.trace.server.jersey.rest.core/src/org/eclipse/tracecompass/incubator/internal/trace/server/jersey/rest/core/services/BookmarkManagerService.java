/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXP_UUID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INVALID_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_EXPERIMENT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.BKM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.BookmarkQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.resources.ITmfMarker;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to manage bookmarks for experiments
 *
 * @author Kaveh Shahedi
 */
@Path("/experiments/{expUUID}/bookmarks")
@Tag(name = BKM)
public class BookmarkManagerService {

    // Bookmark attribute constants
    private static final String BOOKMARK_UUID = "uuid"; //$NON-NLS-1$
    private static final String BOOKMARK_NAME = "name"; //$NON-NLS-1$
    private static final String BOOKMARK_START = "start"; //$NON-NLS-1$
    private static final String BOOKMARK_END = "end"; //$NON-NLS-1$

    private static final String BOOKMARK_DEFAULT_COLOR = "RGBA {255, 0, 0, 128}"; //$NON-NLS-1$

    /**
     * Retrieve all bookmarks for a specific experiment
     *
     * @param expUUID
     *            UUID of the experiment
     * @return Response containing the list of bookmarks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all bookmarks for an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Returns the list of bookmarks", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Bookmark.class)))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_EXPERIMENT, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getBookmarks(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        IFile editorFile = TmfTraceManager.getInstance().getTraceEditorFile(experiment);
        if (editorFile == null) {
            return Response.ok(Collections.emptyList()).build();
        }

        try {
            IMarker[] markers = findBookmarkMarkers(editorFile);
            List<Bookmark> bookmarks = markersToBookmarks(markers);
            return Response.ok(bookmarks).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to get bookmarks", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific bookmark from an experiment
     *
     * @param expUUID
     *            UUID of the experiment
     * @param bookmarkUUID
     *            UUID of the bookmark to retrieve
     * @return Response containing the bookmark
     */
    @GET
    @Path("/{bookmarkUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a specific bookmark from an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Returns the bookmark", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Bookmark.class))),
            @ApiResponse(responseCode = "404", description = "Experiment or bookmark not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = "Bookmark UUID") @PathParam("bookmarkUUID") UUID bookmarkUUID) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        IFile editorFile = TmfTraceManager.getInstance().getTraceEditorFile(experiment);
        if (editorFile == null) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        try {
            IMarker[] markers = findBookmarkMarkers(editorFile);
            IMarker marker = findMarkerByUUID(markers, bookmarkUUID);
            if (marker == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
            }

            Bookmark bookmark = markerToBookmark(marker);
            if (bookmark == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
            }

            return Response.ok(bookmark).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to get bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new bookmark in an experiment
     *
     * @param expUUID
     *            UUID of the experiment
     * @param queryParameters
     *            Parameters for creating the bookmark
     * @return Response containing the created bookmark
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new bookmark in an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Bookmark created successfully", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Bookmark.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_EXPERIMENT, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response createBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @RequestBody(content = {
                    @Content(schema = @Schema(implementation = BookmarkQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        if (queryParameters == null) {
            return Response.status(Status.BAD_REQUEST).entity(MISSING_PARAMETERS).build();
        }

        Map<String, Object> parameters = queryParameters.getParameters();
        String errorMessage = QueryParametersUtil.validateBookmarkQueryParameters(parameters);
        if (errorMessage != null) {
            return Response.status(Status.BAD_REQUEST).entity(errorMessage).build();
        }

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        IFile editorFile = TmfTraceManager.getInstance().getTraceEditorFile(experiment);
        if (editorFile == null) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        try {
            String name = Objects.requireNonNull((String) parameters.get(BOOKMARK_NAME));
            long start = Objects.requireNonNull((Number) parameters.get(BOOKMARK_START)).longValue();
            long end = Objects.requireNonNull((Number) parameters.get(BOOKMARK_END)).longValue();
            UUID uuid = generateUUID(editorFile);

            Bookmark bookmark = new Bookmark(uuid, name, start, end);

            IMarker marker = editorFile.createMarker(IMarker.BOOKMARK);
            setMarkerAttributes(marker, bookmark);
            return Response.ok(bookmark).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to create bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing bookmark in an experiment
     *
     * @param expUUID
     *            UUID of the experiment
     * @param bookmarkUUID
     *            UUID of the bookmark to update
     * @param queryParameters
     *            Parameters for updating the bookmark
     * @return Response containing the updated bookmark
     */
    @PUT
    @Path("/{bookmarkUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update an existing bookmark in an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Bookmark updated successfully", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Bookmark.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Experiment or bookmark not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response updateBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = "Bookmark UUID") @PathParam("bookmarkUUID") UUID bookmarkUUID,
            @RequestBody(content = {
                    @Content(schema = @Schema(implementation = BookmarkQueryParameters.class))
            }, required = true) QueryParameters queryParameters) {

        if (queryParameters == null) {
            return Response.status(Status.BAD_REQUEST).entity(MISSING_PARAMETERS).build();
        }

        Map<String, Object> parameters = queryParameters.getParameters();
        String errorMessage = QueryParametersUtil.validateBookmarkQueryParameters(parameters);
        if (errorMessage != null) {
            return Response.status(Status.BAD_REQUEST).entity(errorMessage).build();
        }

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        IFile editorFile = TmfTraceManager.getInstance().getTraceEditorFile(experiment);
        if (editorFile == null) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        String name = Objects.requireNonNull((String) parameters.get(BOOKMARK_NAME));
        long start = Objects.requireNonNull((Number) parameters.get(BOOKMARK_START)).longValue();
        long end = Objects.requireNonNull((Number) parameters.get(BOOKMARK_END)).longValue();

        Bookmark bookmark = new Bookmark(bookmarkUUID, name, start, end);

        try {
            IMarker[] markers = findBookmarkMarkers(editorFile);
            IMarker marker = findMarkerByUUID(markers, bookmarkUUID);
            if (marker == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
            }

            setMarkerAttributes(marker, bookmark);
            return Response.ok(bookmark).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to update bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a bookmark from an experiment
     *
     * @param expUUID
     *            UUID of the experiment
     * @param bookmarkUUID
     *            UUID of the bookmark to delete
     * @return Response containing the deleted bookmark
     */
    @DELETE
    @Path("/{bookmarkUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a bookmark from an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Bookmark deleted successfully", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Bookmark.class))),
            @ApiResponse(responseCode = "404", description = "Experiment or bookmark not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response deleteBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = "Bookmark UUID") @PathParam("bookmarkUUID") UUID bookmarkUUID) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        IFile editorFile = TmfTraceManager.getInstance().getTraceEditorFile(experiment);
        if (editorFile == null) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        try {
            IMarker[] markers = findBookmarkMarkers(editorFile);
            IMarker marker = findMarkerByUUID(markers, bookmarkUUID);
            if (marker == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
            }

            Bookmark bookmark = markerToBookmark(marker);
            if (bookmark == null) {
                return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
            }

            marker.delete();
            return Response.ok(bookmark).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to delete bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate a random UUID for a new bookmark
     */
    private static UUID generateUUID(IFile editorFile) throws CoreException {
        IMarker[] markers = findBookmarkMarkers(editorFile);
        while (true) {
            UUID uuid = UUID.randomUUID();

            // Check if the UUID hasn't been used yet
            if (findMarkerByUUID(markers, uuid) == null) {
                return uuid;
            }
        }
    }

    /**
     * Find all bookmark markers in the editor file
     */
    private static IMarker[] findBookmarkMarkers(IFile editorFile) throws CoreException {
        return editorFile.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
    }

    /**
     * Convert a marker to a Bookmark object
     */
    private static Bookmark markerToBookmark(IMarker marker) {
        String uuid = marker.getAttribute(BOOKMARK_UUID, (String) null);
        if (uuid == null) {
            return null;
        }

        String name = marker.getAttribute(IMarker.MESSAGE, (String) null);
        if (name == null) {
            return null;
        }

        String startStr = marker.getAttribute(ITmfMarker.MARKER_TIME, (String) null);
        if (startStr == null) {
            return null;
        }
        long start;
        try {
            start = Long.parseLong(startStr);
        } catch (NumberFormatException e) {
            return null;
        }

        long duration = 0;
        String durationStr = marker.getAttribute(ITmfMarker.MARKER_DURATION, (String) null);
        if (durationStr != null) {
            try {
                duration = Long.parseLong(durationStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        long end = start + duration;

        return new Bookmark(UUID.fromString(uuid), name, start, end);
    }

    /**
     * Find a specific bookmark marker by UUID
     */
    private static IMarker findMarkerByUUID(IMarker[] markers, UUID bookmarkUUID) {
        for (IMarker marker : markers) {
            String uuid = marker.getAttribute(BOOKMARK_UUID, (String) null);
            if (uuid != null && UUID.fromString(uuid).equals(bookmarkUUID)) {
                return marker;
            }
        }
        return null;
    }

    /**
     * Create a new marker with bookmark attributes
     */
    private static void setMarkerAttributes(IMarker marker, Bookmark bookmark) throws CoreException {
        Long duration = bookmark.getEnd() - bookmark.getStart();

        marker.setAttribute(BOOKMARK_UUID, bookmark.getUUID().toString());
        marker.setAttribute(IMarker.MESSAGE, bookmark.getName());
        marker.setAttribute(ITmfMarker.MARKER_TIME, Long.toString(bookmark.getStart()));

        if (duration > 0) {
            marker.setAttribute(ITmfMarker.MARKER_DURATION, Long.toString(duration));
            marker.setAttribute(IMarker.LOCATION,
                    NLS.bind("timestamp [{0}, {1}]", //$NON-NLS-1$
                            TmfTimestamp.fromNanos(bookmark.getStart()),
                            TmfTimestamp.fromNanos(bookmark.getEnd())));
        } else {
            marker.setAttribute(IMarker.LOCATION,
                    NLS.bind("timestamp [{0}]", //$NON-NLS-1$
                            TmfTimestamp.fromNanos(bookmark.getStart())));
        }

        marker.setAttribute(ITmfMarker.MARKER_COLOR, BOOKMARK_DEFAULT_COLOR);
    }

    /**
     * Convert list of markers to list of bookmarks
     */
    private static List<Bookmark> markersToBookmarks(IMarker[] markers) {
        List<Bookmark> bookmarks = new ArrayList<>();
        for (IMarker marker : markers) {
            Bookmark bookmark = markerToBookmark(marker);
            if (bookmark != null) {
                bookmarks.add(bookmark);
            }
        }
        return bookmarks;
    }
}