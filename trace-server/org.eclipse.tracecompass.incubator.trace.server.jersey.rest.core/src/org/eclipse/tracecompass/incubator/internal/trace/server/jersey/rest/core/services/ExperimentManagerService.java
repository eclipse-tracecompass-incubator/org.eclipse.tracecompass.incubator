/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * Service to manage experiments
 *
 * @author Loic Prieur-Drevon
 */
@Path("/experiments")
public class ExperimentManagerService {

    private static final Map<UUID, TmfExperiment> EXPERIMENTS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, List<UUID>> TRACE_UUIDS = Collections.synchronizedMap(new HashMap<>());

    private static final String EXPERIMENTS_FOLDER = "Experiments"; //$NON-NLS-1$
    private static final String TRACES_FOLDER = "Traces"; //$NON-NLS-1$
    private static final String SUFFIX = "_exp"; //$NON-NLS-1$

    /**
     * Getter for the list of experiments from the trace manager
     *
     * @return The set of opened experiments
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExperiments() {
        synchronized (EXPERIMENTS) {
            List<Experiment> experiments = Lists.transform(new ArrayList<>(EXPERIMENTS.entrySet()),
                    e -> Experiment.from(e.getValue(), e.getKey()));
            return Response.ok(experiments).build();
        }
    }

    /**
     * Getter for an experiment by {@link UUID}.
     *
     * @param expUUID
     *            UUID of the experiment to search for
     *
     * @return The experiment with the queried {@link UUID} if it exists.
     */
    @GET
    @Path("/{expUUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExperiment(@PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = EXPERIMENTS.get(expUUID);
        if (experiment != null) {
            return Response.ok(Experiment.from(experiment, expUUID)).build();        }
        return Response.status(Status.NOT_FOUND).build();
    }

    /**
     * Get the outputs for an experiment
     *
     * @param expUUID
     *            UUID of the experiment to get the outputs for
     * @return The outputs for the experiment
     */
    @GET
    @Path("/{expUUID}/outputs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutputs(@PathParam("expUUID") UUID expUUID) {
        return Response.status(Status.NOT_IMPLEMENTED).entity("Not implemented for " + expUUID).build(); //$NON-NLS-1$
    }


    /**
     * Delete an experiment by {@link UUID}.
     *
     * @param expUUID
     *            UUID of the experiment to delete
     *
     * @return The experiment with the queried {@link UUID} if it exists.
     */
    @DELETE
    @Path("/{expUUID}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response deleteExperiment(@PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = EXPERIMENTS.remove(expUUID);
        if (experiment == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        Experiment entity = Experiment.from(experiment, expUUID);
        TRACE_UUIDS.remove(expUUID);

        TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(this, experiment));
        experiment.dispose();

        IResource resource = experiment.getResource();
        boolean deleteResources = true;
        synchronized (EXPERIMENTS) {
            for (TmfExperiment e : EXPERIMENTS.values()) {
                if (resource.equals(e.getResource())) {
                    deleteResources = false;
                    break;
                }
            }
        }
        if (deleteResources) {
            try {
                ResourcesPlugin.getWorkspace().run(mon -> {
                    // Delete supplementary files
                    TmfTraceManager.deleteSupplementaryFolder(experiment);
                    // Finally, delete the experiment
                    resource.delete(true, null);
                }, experiment.getResource().getProject(), IWorkspace.AVOID_UPDATE, null);
            } catch (CoreException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
            }
        }
        return Response.ok(entity).build();
    }

    /**
     * Post a new experiment encapsulating the traces from the list of
     * {@link UUID}s.
     *
     *  @param queryParameters
     *            Parameters to post a experiment as described by
     *            {@link QueryParameters}
     *            - name -> name for the experiment.
     *            - traces -> List of UUID strings of the traces to add to the experiment
     *
     * @return no content response if one of the trace {@link UUID}s does not map to
     *         any trace.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postExperiment(QueryParameters queryParameters) {
        Map<String, Object> parameters = queryParameters.getParameters();
        Object nameObj = parameters.get("name"); //$NON-NLS-1$
        Object tracesObj = parameters.get("traces"); //$NON-NLS-1$
        if (!(nameObj instanceof String) || !(tracesObj instanceof List<?>)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String name = (String) nameObj;
        List<UUID> traceUUIDs = new ArrayList<>();

        List<IResource> traceResources = new ArrayList<>();
        for (Object uuidObj : (List<?>) tracesObj) {
            if (!(uuidObj instanceof String)) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            UUID uuid = UUID.fromString((String) uuidObj);
            IResource traceResource = TraceManagerService.getTraceResource(uuid);
            if (traceResource == null) {
                // The trace should have been created first
                return Response.noContent().build();
            }
            traceResources.add(traceResource);
            traceUUIDs.add(uuid);
        }

        UUID expUUID = UUID.nameUUIDFromBytes(Objects.requireNonNull(name.getBytes(Charset.defaultCharset())));
        IFolder resource;
        try {
            resource = getExperimentResource(name);
            if (resource.exists()) {
                // An experiment with that name has already been created
                Multiset<IResource> oldTraceResources = HashMultiset.create(getTraceResources(resource));
                Multiset<IResource> newTraceResources = HashMultiset.create(traceResources);
                if (!oldTraceResources.equals(newTraceResources)) {
                    // It's a different experiment, return a conflict
                    TmfExperiment oldExperiment = new TmfExperiment(ITmfEvent.class, resource.getLocation().toOSString(), new ITmfTrace[0], TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, resource);
                    Experiment entity = Experiment.from(oldExperiment, expUUID);
                    oldExperiment.dispose();
                    return Response.status(Status.CONFLICT).entity(entity).build();
                }
                // It's the same experiment, check if it is opened already
                TmfExperiment experiment = EXPERIMENTS.get(expUUID);
                if (experiment != null) {
                    // It's already opened, return it
                    return Response.ok(Experiment.from(experiment, expUUID)).build();
                }
                // It's not opened, continue below to instantiate it
            } else {
                // It's a new experiment, create the experiment resources
                createExperiment(resource, traceResources);
            }
            // Create and set the supplementary folder
            createSupplementaryFolder(resource);
        } catch (CoreException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        // Instantiate the experiment and return it
        ITmfTrace[] traces = Lists.transform(traceUUIDs, uuid -> TraceManagerService.createTraceInstance(uuid)).toArray(new ITmfTrace[0]);
        TmfExperiment experiment = new TmfExperiment(ITmfEvent.class, resource.getLocation().toOSString(), traces, TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, resource);
        experiment.indexTrace(false);
        // read first event to make sure start time is initialized
        ITmfContext ctx = experiment.seekEvent(0);
        experiment.getNext(ctx);
        ctx.dispose();

        TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(this, experiment, null));

        TRACE_UUIDS.put(expUUID, traceUUIDs);
        EXPERIMENTS.put(expUUID, experiment);

        return Response.ok(Experiment.from(experiment, expUUID)).build();
    }

    /**
     * Gets the Eclipse resource from the experiment name.
     *
     * @param name
     *            the experiment name
     * @return The Eclipse resource
     *
     * @throws CoreException
     *             if an error occurs
     */
    private static IFolder getExperimentResource(String name) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        IFolder experimentsFolder = project.getFolder(EXPERIMENTS_FOLDER);
        return experimentsFolder.getFolder(name);
    }

    private static List<IResource> getTraceResources(IFolder folder) {
        final List<IResource> list = new ArrayList<>();
        final IFolder tracesFolder = folder.getProject().getFolder(TRACES_FOLDER);
        try {
            folder.accept(new IResourceProxyVisitor() {
                @Override
                public boolean visit(IResourceProxy resource) throws CoreException {
                    if (resource.getType() == IResource.FILE) {
                        IResource traceResourceUnderExperiment = resource.requestResource();
                        IPath relativePath = traceResourceUnderExperiment.getProjectRelativePath().makeRelativeTo(folder.getProjectRelativePath());
                        IResource traceResource = tracesFolder.findMember(relativePath);
                        if (traceResource != null) {
                            list.add(traceResource);
                        }
                        return false;
                    }
                    return true;
                }
            }, IResource.NONE);
        } catch (CoreException e) {
        }
        list.sort(Comparator.comparing(resource -> resource.getFullPath().toString()));
        return list;
    }

    private static void createExperiment(IFolder folder, List<IResource> traceResources) throws CoreException {
        // create the experiment folder resource
        createFolder(folder);
        // add the traces
        for (IResource traceResource : traceResources) {
            addTrace(folder, traceResource);
        }
        // set the experiment type
        folder.setPersistentProperty(TmfCommonConstants.TRACETYPE, TmfTraceType.DEFAULT_EXPERIMENT_TYPE);
    }

    private static void addTrace(IFolder folder, IResource traceResource) throws CoreException {
        /*
         * Create an empty file to represent the experiment trace. The file's element
         * path relative to the experiment resource corresponds to the trace's element
         * path relative to the Traces folder.
         */
        IPath relativePath = traceResource.getProjectRelativePath().removeFirstSegments(1);
        IFile file = folder.getFile(relativePath);
        createFolder((IFolder) file.getParent());
        file.create(new ByteArrayInputStream(new byte[0]), false, new NullProgressMonitor());
        file.setPersistentProperty(TmfCommonConstants.TRACETYPE, TmfTraceType.getTraceTypeId(traceResource));
    }

    private static void createSupplementaryFolder(IFolder folder) throws CoreException {
        IFolder supplRootFolder = folder.getProject().getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
        IFolder supplFolder = supplRootFolder.getFolder(folder.getName() + SUFFIX);
        createFolder(supplFolder);
        folder.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());
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
     * Try and find an experiment with the queried UUID in the experiment
     * manager.
     *
     * @param expUUID
     *            queried {@link UUID}
     * @return the experiment or null if none match.
     */
    public static @Nullable TmfExperiment getExperimentByUUID(UUID expUUID) {
        return EXPERIMENTS.get(expUUID);
    }

    /**
     * Get the list of trace UUIDs of an experiment from the experiment manager.
     *
     * @param expUUID
     *            queried {@link UUID}
     * @return the list of trace UUIDs.
     */
    public static List<UUID> getTraceUUIDs(UUID expUUID) {
        return TRACE_UUIDS.getOrDefault(expUUID, Collections.emptyList());
    }

    /**
     * Returns true if the given trace is in use by any experiment
     *
     * @param uuid
     *            the trace UUID
     * @return true if the given trace is in use by any experiment
     */
    public static boolean isTraceInUse(UUID uuid) {
        synchronized (TRACE_UUIDS) {
            return TRACE_UUIDS.values().stream().anyMatch(traceUUIDs -> traceUUIDs.contains(uuid));
        }
    }
}
