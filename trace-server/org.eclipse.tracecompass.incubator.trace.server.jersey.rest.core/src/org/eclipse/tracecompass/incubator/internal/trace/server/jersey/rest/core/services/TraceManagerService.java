/*******************************************************************************
 * Copyright (c) 2017, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

/**
 * Service to manage traces.
 *
 * @author Loic Prieur-Drevon
 */
@Path("/traces")
public class TraceManagerService {

    private static final Map<UUID, IResource> TRACES = Collections.synchronizedMap(new HashMap<>());

    private static final String TRACES_FOLDER = "Traces"; //$NON-NLS-1$

    /**
     * Getter method to access the list of traces
     *
     * @return a response containing the list of traces
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
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
    public Response putTrace(QueryParameters queryParameters) {
        Map<String, Object> parameters = queryParameters.getParameters();
        String name = (String) parameters.get("name");
        String path = (String) parameters.get("uri");
        Object typeIDObject = parameters.get("typeID");
        String typeID = typeIDObject != null ? (String) typeIDObject : "";

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
            return Response.status(Status.NOT_IMPLEMENTED).entity("Trace type not supported").build(); //$NON-NLS-1$
        }
        String traceType = traceTypes.get(0).getTraceTypeId();

        UUID uuid = UUID.nameUUIDFromBytes(Objects.requireNonNull((path + name).getBytes(Charset.defaultCharset())));

        IResource resource = getResource(path, name);
        if (!resource.exists()) {
            if (!createResource(path, resource)) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Trace resource creation failed").build(); //$NON-NLS-1$
            }
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, traceType);
        } else {
            IPath targetLocation = org.eclipse.core.runtime.Path.forPosix(path).removeTrailingSeparator();
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
        TRACES.put(uuid, resource);
        return Response.ok(createTraceModel(uuid)).build();
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
            TraceTypeHelper helper = TmfTraceType.getTraceType(typeID);
            ITmfTrace trace = helper.getTraceClass().getDeclaredConstructor().newInstance();
            String path = Objects.requireNonNull(ResourceUtil.getLocation(resource)).removeTrailingSeparator().toOSString();
            String name = resource.getName();
            trace.initTrace(resource, path, ITmfEvent.class, name, typeID);
            trace.indexTrace(false);
            // read first event to make sure start time is initialized
            ITmfContext ctx = trace.seekEvent(0);
            trace.getNext(ctx);
            ctx.dispose();
            return trace;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | CoreException | TmfTraceException e) {
            Activator.getInstance().logError("Failed to create trace instance for " + uuid, e); //$NON-NLS-1$
            return null;
        }
    }

    private static Trace createTraceModel(UUID uuid) {
        ITmfTrace traceInstance = createTraceInstance(uuid);
        if (traceInstance == null) {
            return null;
        }
        Trace trace = Trace.from(traceInstance, uuid);
        traceInstance.dispose();
        return trace;
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
        IPath targetLocation = org.eclipse.core.runtime.Path.forPosix(path);
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
    private static boolean createResource(String path, IResource resource) throws CoreException {
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
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getTrace(@PathParam("uuid") @NotNull UUID uuid) {
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
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteTrace(@PathParam("uuid") @NotNull UUID uuid) {
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
            ResourcesPlugin.getWorkspace().getRoot()
            .getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME)
            .refreshLocal(Integer.MAX_VALUE, null);
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

    private static void cleanupFolders(File folder, File root) {
        File current = folder;
        while (current.isDirectory() && !current.equals(root) && current.listFiles().length == 0) {
            current.delete();
            current = current.getParentFile();
        }
    }
}
