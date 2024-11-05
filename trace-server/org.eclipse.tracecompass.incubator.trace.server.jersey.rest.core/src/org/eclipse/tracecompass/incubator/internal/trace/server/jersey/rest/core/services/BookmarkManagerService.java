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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.BookmarkQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.Lists;

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
 * @since 10.1
 */
@Path("/experiments/{expUUID}/bookmarks")
@Tag(name = BKM)
public class BookmarkManagerService {

    private static final Map<UUID, Map<UUID, Bookmark>> EXPERIMENT_BOOKMARKS = Collections.synchronizedMap(initBookmarkResources());
    private static final String BOOKMARKS_FOLDER = "Bookmarks"; //$NON-NLS-1$

    private static Map<UUID, Map<UUID, Bookmark>> initBookmarkResources() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        Map<UUID, Map<UUID, Bookmark>> experimentBookmarks = new HashMap<>();
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            IFolder bookmarksFolder = project.getFolder(BOOKMARKS_FOLDER);
            // Check if the folder exists. If not, create it
            if (!bookmarksFolder.exists()) {
                bookmarksFolder.create(true, true, null);
            }
            bookmarksFolder.accept((IResourceVisitor) resource -> {
                if (resource.equals(bookmarksFolder)) {
                    return true;
                }
                if (resource instanceof IFolder) {
                    UUID expUUID = UUID.fromString(Objects.requireNonNull(resource.getName()));
                    Map<UUID, Bookmark> bookmarks = loadBookmarks((IFolder) resource);
                    if (!bookmarks.isEmpty()) {
                        experimentBookmarks.put(expUUID, bookmarks);
                    }
                }
                return false;
            }, IResource.DEPTH_ONE, IResource.NONE);
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to load bookmarks", e); //$NON-NLS-1$
        }
        return experimentBookmarks;
    }

    private static Map<UUID, Bookmark> loadBookmarks(IFolder experimentFolder) throws CoreException {
        Map<UUID, Bookmark> bookmarks = new HashMap<>();
        experimentFolder.accept(resource -> {
            if (resource instanceof IFile && resource.getName().endsWith(".bookmark")) { //$NON-NLS-1$
                try (ObjectInputStream ois = new ObjectInputStream(((IFile) resource).getContents(true))) {
                    Bookmark bookmark = (Bookmark) ois.readObject();
                    bookmarks.put(bookmark.getUUID(), bookmark);
                } catch (Exception e) {
                    Activator.getInstance().logError("Failed to load bookmark", e); //$NON-NLS-1$
                }
            }
            return true;
        });
        return bookmarks;
    }

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
            @ApiResponse(responseCode = "200", description = "Returns the list of bookmarks", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Bookmark.class)))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_EXPERIMENT, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getBookmarks(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        synchronized (EXPERIMENT_BOOKMARKS) {
            List<Bookmark> bookmarks = Lists.transform(new ArrayList<>(EXPERIMENT_BOOKMARKS.getOrDefault(expUUID, Collections.emptyMap()).values()), bookmark -> bookmark);
            return Response.ok(bookmarks).build();
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
            @ApiResponse(responseCode = "200", description = "Returns the bookmark", content = @Content(schema = @Schema(implementation = Bookmark.class))),
            @ApiResponse(responseCode = "404", description = "Experiment or bookmark not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = "Bookmark UUID") @PathParam("bookmarkUUID") UUID bookmarkUUID) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        Map<UUID, Bookmark> bookmarks = EXPERIMENT_BOOKMARKS.get(expUUID);
        if (bookmarks == null || !bookmarks.containsKey(bookmarkUUID)) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        return Response.ok(bookmarks.get(bookmarkUUID)).build();
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
            @ApiResponse(responseCode = "200", description = "Bookmark created successfully", content = @Content(schema = @Schema(implementation = Bookmark.class))),
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

        String name = Objects.requireNonNull((String) parameters.get("name")); //$NON-NLS-1$
        long start = Objects.requireNonNull((Number) parameters.get("start")).longValue(); //$NON-NLS-1$
        long end = Objects.requireNonNull((Number) parameters.get("end")).longValue(); //$NON-NLS-1$

        try {
            IFolder bookmarkFolder = getBookmarkFolder(expUUID);
            UUID bookmarkUUID = UUID.nameUUIDFromBytes(Objects.requireNonNull(name.getBytes(Charset.defaultCharset())));

            // Check if bookmark already exists
            Map<UUID, Bookmark> existingBookmarks = EXPERIMENT_BOOKMARKS.get(expUUID);
            if (existingBookmarks != null && existingBookmarks.containsKey(bookmarkUUID)) {
                Bookmark existingBookmark = Objects.requireNonNull(existingBookmarks.get(bookmarkUUID));
                // Check if it's the same bookmark (same start and end times)
                if (existingBookmark.getStart() != start || existingBookmark.getEnd() != end) {
                    // It's a different bookmark with the same name, return conflict
                    return Response.status(Status.CONFLICT).entity(existingBookmark).build();
                }
                // It's the same bookmark, return it
                return Response.ok(existingBookmark).build();
            }

            createFolder(bookmarkFolder);

            Bookmark bookmark = new Bookmark(bookmarkUUID, name, start, end);

            // Save to file system
            IFile bookmarkFile = bookmarkFolder.getFile(bookmarkUUID.toString() + ".bookmark"); //$NON-NLS-1$
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(baos)) {

                oos.writeObject(bookmark);
                oos.flush();

                if (bookmarkFile.exists()) {
                    bookmarkFile.setContents(new ByteArrayInputStream(baos.toByteArray()), IResource.FORCE, null);
                } else {
                    bookmarkFile.create(new ByteArrayInputStream(baos.toByteArray()), true, null);
                }
            } catch (IOException e) {
                Activator.getInstance().logError("Failed to create bookmark", e); //$NON-NLS-1$
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }

            // Add to memory
            Map<UUID, Bookmark> bookmarks = EXPERIMENT_BOOKMARKS.computeIfAbsent(expUUID, k -> new HashMap<>());
            bookmarks.put(bookmarkUUID, bookmark);

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
            @ApiResponse(responseCode = "200", description = "Bookmark updated successfully", content = @Content(schema = @Schema(implementation = Bookmark.class))),
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

        Map<UUID, Bookmark> bookmarks = EXPERIMENT_BOOKMARKS.get(expUUID);
        if (bookmarks == null || !bookmarks.containsKey(bookmarkUUID)) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        String name = Objects.requireNonNull((String) parameters.get("name")); //$NON-NLS-1$
        long start = Objects.requireNonNull((Number) parameters.get("start")).longValue(); //$NON-NLS-1$
        long end = Objects.requireNonNull((Number) parameters.get("end")).longValue(); //$NON-NLS-1$

        try {
            IFolder bookmarkFolder = getBookmarkFolder(expUUID);
            Bookmark updatedBookmark = new Bookmark(bookmarkUUID, name, start, end);

            // Update file system
            IFile bookmarkFile = bookmarkFolder.getFile(bookmarkUUID.toString() + ".bookmark"); //$NON-NLS-1$
            if (bookmarkFile.exists()) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(baos)) {

                    oos.writeObject(updatedBookmark);
                    oos.flush();

                    bookmarkFile.setContents(new ByteArrayInputStream(baos.toByteArray()), IResource.FORCE, null);
                    // Update memory
                    bookmarks.put(bookmarkUUID, updatedBookmark);
                    return Response.ok(updatedBookmark).build();
                }
            }

            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to update bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
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
            @ApiResponse(responseCode = "200", description = "Bookmark deleted successfully", content = @Content(schema = @Schema(implementation = Bookmark.class))),
            @ApiResponse(responseCode = "404", description = "Experiment or bookmark not found", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response deleteBookmark(
            @Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID,
            @Parameter(description = "Bookmark UUID") @PathParam("bookmarkUUID") UUID bookmarkUUID) {

        TmfExperiment experiment = ExperimentManagerService.getExperimentByUUID(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).entity(NO_SUCH_EXPERIMENT).build();
        }

        Map<UUID, Bookmark> bookmarks = EXPERIMENT_BOOKMARKS.get(expUUID);
        if (bookmarks == null || !bookmarks.containsKey(bookmarkUUID)) {
            return Response.status(Status.NOT_FOUND).entity(EndpointConstants.BOOKMARK_NOT_FOUND).build();
        }

        try {
            IFolder bookmarkFolder = getBookmarkFolder(expUUID);
            IFile bookmarkFile = bookmarkFolder.getFile(bookmarkUUID.toString() + ".bookmark"); //$NON-NLS-1$
            Bookmark deletedBookmark = bookmarks.remove(bookmarkUUID);

            if (bookmarkFile.exists()) {
                bookmarkFile.delete(true, null);
            }

            if (bookmarks.isEmpty()) {
                EXPERIMENT_BOOKMARKS.remove(expUUID);
                if (bookmarkFolder.exists()) {
                    bookmarkFolder.delete(true, null);
                }
            }

            return Response.ok(deletedBookmark).build();
        } catch (CoreException e) {
            Activator.getInstance().logError("Failed to delete bookmark", e); //$NON-NLS-1$
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets the Eclipse resource folder for the bookmark.
     *
     * @param expUUID
     *            UUID of the experiment
     * @return The Eclipse resource folder
     *
     * @throws CoreException
     *             if an error occurs
     */
    private static @NonNull IFolder getBookmarkFolder(UUID expUUID) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        IFolder bookmarksFolder = project.getFolder(BOOKMARKS_FOLDER);
        return Objects.requireNonNull(bookmarksFolder.getFolder(expUUID.toString()));
    }

    private static void createFolder(IFolder folder) throws CoreException {
        if (!folder.exists()) {
            if (folder.getParent() instanceof IFolder) {
                createFolder((IFolder) folder.getParent());
            }
            folder.create(true, true, null);
        }
    }

    /**
     * Dispose method to be only called at server shutdown.
     */
    public static void dispose() {
        EXPERIMENT_BOOKMARKS.clear();
    }
}