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

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.CANNOT_READ;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NAME_EXISTS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NOT_SUPPORTED;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_TRACE;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TRACE_CREATION_FAILED;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.TRACE_UUID;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.validation.constraints.NotNull;
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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.TraceQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to manage traces.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces")
@Tag(name = EndpointConstants.TRA)
public class TraceManagerService {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").contains("Windows"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final Map<UUID, IResource> TRACES = Collections.synchronizedMap(initTraces());

    private static final String TRACES_FOLDER = "Traces"; //$NON-NLS-1$

    /**
     * Getter method to access the list of traces
     *
     * @return a response containing the list of traces
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of physical traces imported on the server", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of traces", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Trace.class))))
    })
    public Response getTraces() {
        synchronized (TRACES) {
            List<Trace> traces = new ArrayList<>();
            for (UUID uuid : TRACES.keySet()) {
                Trace trace = createTraceModel(uuid);
                if (trace != null) {
                    traces.add(trace);
                }
            }
            return Response.ok(traces).build();
        }
    }

    private static Map<UUID, IResource> initTraces() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        Map<UUID, IResource> traces = new HashMap<>();
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            IFolder tracesFolder = project.getFolder(TRACES_FOLDER);
            tracesFolder.accept(resource -> {
                if (ResourceUtil.isSymbolicLink(resource)) {
                    traces.put(getTraceUUID(resource), resource);
                    return false;
                }
                return true;
            });
        } catch (CoreException e) {
        }
        return traces;
    }

    /**
     * Method to create the trace resources and add it to the trace manager.
     *
     * @param queryParameters
     *            Parameters to post a trace as described by
     *            {@link QueryParameters}
     * @return the new trace model object or the exception if it failed to load.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Import a trace", description = "Import a trace to the trace server. Return some base information once imported.", responses = {
            @ApiResponse(responseCode = "200", description = "The trace has been successfully added to the trace server", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Trace.class))),
            @ApiResponse(responseCode = "400", description = MISSING_PARAMETERS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_TRACE, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "406", description = CANNOT_READ, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = NAME_EXISTS, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = TRACE_CREATION_FAILED, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "501", description = NOT_SUPPORTED, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response putTrace(@RequestBody(content = {
            @Content(schema = @Schema(implementation = TraceQueryParameters.class))
    }, required = true) QueryParameters queryParameters) {

        if (queryParameters == null) {
            return Response.status(Status.BAD_REQUEST).entity(MISSING_PARAMETERS).build();
        }
        Map<String, Object> parameters = queryParameters.getParameters();
        String errorMessage = QueryParametersUtil.validateTraceQueryParameters(parameters);
        if (errorMessage != null) {
            return Response.status(Status.BAD_REQUEST).entity(errorMessage).build();
        }
        String name = (String) parameters.get("name"); //$NON-NLS-1$
        String path = (String) parameters.get("uri"); //$NON-NLS-1$
        if (IS_WINDOWS && path != null && path.startsWith("/")) { //$NON-NLS-1$
            /*
             * Workaround for path created by the theia-trace-extension, see
             * https://github.com/theia-ide/theia-trace-extension/issues/545.
             * This is caused by
             * https://github.com/eclipse-theia/theia/issues/8098. Once issue
             * #8098 is resolved this workaround can be removed.
             */
             path = path.substring(1);
        }
        Object typeIDObject = parameters.get("typeID"); //$NON-NLS-1$
        String typeID = typeIDObject != null ? (String) typeIDObject : ""; //$NON-NLS-1$

        try {
            return put(path, name, typeID);
        } catch (TmfTraceImportException | CoreException | IllegalArgumentException | SecurityException e) {
            return Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build();
        }
    }

    private static Response put(String path, String name, String typeID)
            throws TmfTraceImportException, CoreException, IllegalArgumentException, SecurityException {

        if (!Paths.get(path).toFile().exists()) {
            return Response.status(Status.NOT_FOUND).entity("No trace at " + path).build(); //$NON-NLS-1$
        }

        List<TraceTypeHelper> traceTypes = TmfTraceType.selectTraceType(path, typeID);
        if (traceTypes.isEmpty()) {
            return Response.status(Status.NOT_IMPLEMENTED).entity(NOT_SUPPORTED).build();
        }
        String traceType = traceTypes.get(0).getTraceTypeId();

        IResource resource = getResource(path, name);
        if (!resource.exists()) {
            if (!createResource(path, resource)) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TRACE_CREATION_FAILED).build();
            }
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, traceType);
        } else {
            IPath targetLocation = getTargetLocation(path);
            IPath oldLocation = ResourceUtil.getLocation(resource);
            if (oldLocation == null || !targetLocation.equals(oldLocation.removeTrailingSeparator()) ||
                    !traceType.equals(resource.getPersistentProperty(TmfCommonConstants.TRACETYPE))) {
                synchronized (TRACES) {
                    Optional<@NonNull Entry<UUID, IResource>> oldEntry = TRACES.entrySet().stream().filter(entry -> resource.equals(entry.getValue())).findFirst();
                    if (!oldEntry.isPresent()) {
                        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Failed to find conflicting trace").build(); //$NON-NLS-1$
                    }
                    UUID oldUUID = oldEntry.get().getKey();
                    return Response.status(Status.CONFLICT).entity(createTraceModel(oldUUID)).build();
                }
            }
        }
        UUID uuid = getTraceUUID(resource);
        TRACES.put(uuid, resource);
        return Response.ok(createTraceModel(uuid)).build();
    }

    /**
     * Get the UUID of a trace by its resource
     *
     * @param resource
     *            the trace resource
     * @return the trace UUID
     */
    public static UUID getTraceUUID(IResource resource) {
        IPath location = ResourceUtil.getLocation(resource);
        IPath path = location != null ? location.append(resource.getName()) : resource.getProjectRelativePath();
        UUID uuid = UUID.nameUUIDFromBytes(Objects.requireNonNull(path.toString().getBytes(Charset.defaultCharset())));
        return uuid;
    }

    /**
     * Get the resource of a trace by its UUID.
     * @param uuid
     *            the trace UUID
     * @return the trace resource, or null if it could not be found
     */
    public static @Nullable IResource getTraceResource(UUID uuid) {
        return TRACES.get(uuid);
    }

    /**
     * Create an instance of a trace by its UUID. The caller is responsible to
     * dispose the instance when it is no longer needed.
     *
     * @param uuid
     *            the trace UUID
     * @return the trace instance, or null if it could not be created
     */
    public static @Nullable ITmfTrace createTraceInstance(UUID uuid) {
        try {
            IResource resource = TRACES.get(uuid);
            if (resource == null) {
                return null;
            }
            String typeID = TmfTraceType.getTraceTypeId(resource);
            if (typeID == null) {
                return null;
            }
            ITmfTrace trace = TmfTraceType.instantiateTrace(typeID);
            if (trace != null) {
                String path = Objects.requireNonNull(ResourceUtil.getLocation(resource)).removeTrailingSeparator().toOSString();
                String name = resource.getName();
                trace.initTrace(resource, path, ITmfEvent.class, name, typeID);
                trace.indexTrace(false);
                // read first event to make sure start time is initialized
                ITmfContext ctx = trace.seekEvent(0);
                trace.getNext(ctx);
                ctx.dispose();
            }
            return trace;
        } catch (CoreException | TmfTraceException e) {
            Activator.getInstance().logError("Failed to create trace instance for " + uuid, e); //$NON-NLS-1$
            return null;
        }
    }

    private static Trace createTraceModel(UUID uuid) {
        IResource resource = TRACES.get(uuid);
        if (resource == null) {
            return null;
        }
        return Trace.from(resource, uuid);
    }

    /**
     * Gets the Eclipse resource from the path and prepares the supplementary
     * directory for this trace.
     *
     * @param path
     *            the absolute path string to the trace
     * @param name
     *            the trace name
     * @return The Eclipse resources
     *
     * @throws CoreException
     *             if an error occurs
     */
    private static IResource getResource(String path, String name) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        IFolder tracesFolder = project.getFolder(TRACES_FOLDER);
        IPath targetLocation = getTargetLocation(path);
        IPath resourcePath = targetLocation.removeLastSegments(1).append(name);

        IResource resource = null;
        // create the resource hierarchy.
        if (new File(path).isFile()) {
            resource = tracesFolder.getFile(resourcePath);
        } else {
            resource = tracesFolder.getFolder(resourcePath);
        }
        return resource;
    }

    /**
     * Create the Eclipse resource from the target location and prepare the
     * supplementary directory for this trace.
     *
     * @param path
     *            the absolute path string to the trace
     * @param name
     *            the trace name
     * @return true if creation was successful
     *
     * @throws CoreException
     *             if an error occurs
     */
    private static synchronized boolean createResource(String path, IResource resource) throws CoreException {
        // create the resource hierarchy.
        IPath targetLocation = org.eclipse.core.runtime.Path.forPosix(path);
        createFolder((IFolder) resource.getParent(), null);
        if (!ResourceUtil.createSymbolicLink(resource, targetLocation, true, null)) {
            return false;
        }

        // create supplementary folder on file system:
        IFolder supplRootFolder = resource.getProject().getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
        IFolder supplFolder = supplRootFolder.getFolder(resource.getProjectRelativePath().removeFirstSegments(1));
        createFolder(supplFolder, null);
        resource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());

        return true;
    }

    /**
     * Getter method to get a trace object
     *
     * @param uuid
     *            Unique trace ID
     * @return a response containing the trace
     */
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the model object for a trace", responses = {
            @ApiResponse(responseCode = "200", description = "Return the trace model", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Trace.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_TRACE, content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response getTrace(@Parameter(description = TRACE_UUID) @PathParam("uuid") @NotNull UUID uuid) {
        Trace trace = createTraceModel(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(trace).build();
    }

    /**
     * Delete a trace from the manager
     *
     * @param uuid
     *            Unique trace ID
     * @return a not found response if there is no such trace or the entity.
     */
    @DELETE
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove a trace from the server and disk", responses = {
            @ApiResponse(responseCode = "200", description = "The trace was successfully deleted", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Trace.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_TRACE, content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "409", description = "The trace is in use by at least one experiment thus cannot be deleted", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public Response deleteTrace(@Parameter(description = TRACE_UUID) @PathParam("uuid") @NotNull UUID uuid) {
        Trace trace = createTraceModel(uuid);
        if (trace == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        if (ExperimentManagerService.isTraceInUse(uuid)) {
            return Response.status(Status.CONFLICT).entity(trace).build();
        }
        IResource resource = TRACES.remove(uuid);
        if (resource == null) {
            return Response.ok(trace).build();
        }
        try {
            // Delete supplementary files and folders
            File supplFolder = new File(resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER));
            FileUtils.cleanDirectory(supplFolder);
            cleanupFolders(supplFolder,
                    resource.getProject().getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME).getLocation().toFile());
            // Delete trace resource
            resource.delete(IResource.FORCE, null);
            cleanupFolders(resource.getParent().getLocation().toFile(),
                    resource.getProject().getFolder(TRACES_FOLDER).getLocation().toFile());
            // Refresh the workspace
            resource.getProject().refreshLocal(Integer.MAX_VALUE, null);
        } catch (CoreException | IOException e) {
            Activator.getInstance().logError("Failed to delete trace", e); //$NON-NLS-1$
        }
        return Response.ok(trace).build();
    }

    private static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
        // Taken from: org.eclipse.tracecompass.tmf.ui.project.model.TraceUtil.java
        // TODO: have a tmf.core util for that.
        if (!folder.exists()) {
            if (folder.getParent() instanceof IFolder) {
                createFolder((IFolder) folder.getParent(), monitor);
            }
            folder.create(true, true, monitor);
        }
    }

    private static synchronized void cleanupFolders(File folder, File root) {
        File current = folder;
        while (current.isDirectory() && !current.equals(root)) {
            File[] listFiles = current.listFiles();
            if (listFiles == null || listFiles.length != 0) {
                break;
            }
            current.delete();
            current = current.getParentFile();
        }
    }

    /**
     * Dispose method to be only called at server shutdown.
     */
    public static void dispose() {
        TRACES.clear();
    }

    private static IPath getTargetLocation(String path) {
        if (IS_WINDOWS) {
            IPath p = org.eclipse.core.runtime.Path.forWindows(path);
            return new org.eclipse.core.runtime.Path(p.toString().replace(":", "")).removeTrailingSeparator(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return org.eclipse.core.runtime.Path.forPosix(path).removeTrailingSeparator();
    }
}
