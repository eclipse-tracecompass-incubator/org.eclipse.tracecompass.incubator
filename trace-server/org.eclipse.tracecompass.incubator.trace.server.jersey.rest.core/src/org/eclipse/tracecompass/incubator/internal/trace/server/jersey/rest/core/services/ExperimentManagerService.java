/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXPERIMENT_NAME_EXISTS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXPERIMENT_NAME_EXISTS_DETAIL;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.EXP_UUID;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INVALID_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_EXPERIMENT;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.NO_SUCH_TRACE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.Activator;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ExperimentErrorResponse;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.ExperimentQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.annotations.TraceAnnotationProvider;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service to manage experiments
 *
 * @author Loic Prieur-Drevon
 */
@Path("/experiments")
@Tag(name = EndpointConstants.EXP)
public class ExperimentManagerService {

    private static final Map<UUID, List<UUID>> TRACE_UUIDS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, Map<UUID, ITmfTrace>> TRACE_INSTANCES = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, IResource> EXPERIMENT_RESOURCES = Collections.synchronizedMap(initExperimentResources());
    private static final Map<UUID, TmfExperiment> EXPERIMENTS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, TraceAnnotationProvider> TRACE_ANNOTATION_PROVIDERS = Collections.synchronizedMap(new HashMap<>());

    private static final String EXPERIMENTS_FOLDER = "Experiments"; //$NON-NLS-1$
    private static final String TRACES_FOLDER = "Traces"; //$NON-NLS-1$
    private static final String SUFFIX = "_exp"; //$NON-NLS-1$
    private static final String BOOKMARKS_HIDDEN_FILE = ".bookmarks"; //$NON-NLS-1$
    private static final String EXPERIMENT_EDITOR_INPUT_TYPE = "editorInputType.experiment"; //$NON-NLS-1$

    /**
     * Getter for the list of experiments from the trace manager
     *
     * @return The set of opened experiments
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the list of experiments on the server", responses = {
            @ApiResponse(responseCode = "200", description = "Returns a list of experiments", content = @Content(array = @ArraySchema(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Experiment.class))))
    })
    public Response getExperiments() {
        synchronized (EXPERIMENT_RESOURCES) {
            List<Experiment> experiments = Lists.transform(new ArrayList<>(EXPERIMENT_RESOURCES.entrySet()), e -> {
                UUID expUUID = e.getKey();
                TmfExperiment experiment = EXPERIMENTS.get(expUUID);
                if (experiment != null) {
                    return Experiment.from(experiment, expUUID);
                }
                IResource experimentResource = e.getValue();
                return Experiment.from(experimentResource, expUUID);
            });
            return Response.ok(experiments).build();
        }
    }

    private static Map<UUID, IResource> initExperimentResources() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        Map<UUID, IResource> experimentResources = new HashMap<>();
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            IFolder experimentsFolder = project.getFolder(EXPERIMENTS_FOLDER);
            experimentsFolder.accept((IResourceVisitor) resource -> {
                if (resource.equals(experimentsFolder)) {
                    return true;
                }
                if (resource instanceof IFolder) {
                    UUID expUUID = UUID.nameUUIDFromBytes(Objects.requireNonNull(resource.getName().getBytes(Charset.defaultCharset())));
                    experimentResources.put(expUUID, resource);
                    List<UUID> traceUUIDs = getTraceUUIDs((IFolder) resource);
                    TRACE_UUIDS.put(expUUID, traceUUIDs);
                }
                return false;
            }, IResource.DEPTH_ONE, IResource.NONE);
        } catch (CoreException e) {
        }
        return experimentResources;
    }

    private static List<UUID> getTraceUUIDs(IFolder experimentResource) throws CoreException {
        List<UUID> traceUUIDs = new ArrayList<>();
        experimentResource.accept(resource -> {
            if (resource instanceof IFile) {
                IPath path = resource.getProjectRelativePath().makeRelativeTo(experimentResource.getProjectRelativePath());
                IResource traceResource = experimentResource.getProject().getFolder(TRACES_FOLDER).findMember(path);
                if (traceResource != null) {
                    traceUUIDs.add(TraceManagerService.getTraceUUID(traceResource));
                }
                return false;
            }
            return true;
        });
        return traceUUIDs;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the model object for an experiment", responses = {
            @ApiResponse(responseCode = "200", description = "Return the experiment model", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Experiment.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_EXPERIMENT, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getExperiment(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {
        TmfExperiment experiment = getExperimentByUUID(expUUID);
        if (experiment != null) {
            return Response.ok(Experiment.from(experiment, expUUID)).build();
        }
        return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, "No experiment found with uuid " + expUUID); //$NON-NLS-1$
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove an experiment from the server", responses = {
            @ApiResponse(responseCode = "200", description = "The trace was successfully deleted, return the deleted experiment.", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Experiment.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_EXPERIMENT, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteExperiment(@Parameter(description = EXP_UUID) @PathParam("expUUID") UUID expUUID) {
        IResource resource = EXPERIMENT_RESOURCES.remove(expUUID);
        if (resource == null) {
            return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, "No experiment found with uuid " + expUUID); //$NON-NLS-1$
        }
        TmfExperiment experiment = EXPERIMENTS.remove(expUUID);
        Experiment experimentModel = experiment != null ? Experiment.from(experiment, expUUID) : Experiment.from(resource, expUUID);
        if (experiment != null) {
            TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(this, experiment));
            experiment.dispose();
        }
        TRACE_ANNOTATION_PROVIDERS.remove(expUUID);
        TRACE_UUIDS.remove(expUUID);
        TRACE_INSTANCES.remove(expUUID);
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
                // Delete supplementary files and folders
                File supplFolder = new File(resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER));
                FileUtils.cleanDirectory(supplFolder);
                supplFolder.delete();
                // Delete experiment resource
                resource.delete(true, null);
                // Refresh the workspace
                resource.getProject().refreshLocal(Integer.MAX_VALUE, null);
            } catch (CoreException | IOException e) {
                Activator.getInstance().logError("Failed to delete experiment", e); //$NON-NLS-1$
            }
        }
        return Response.ok(experimentModel).build();
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
    @Operation(summary = "Create a new experiment on the server", responses = {
            @ApiResponse(responseCode = "200", description = "The experiment was successfully created", content = @Content(schema = @Schema(implementation = org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.Experiment.class))),
            @ApiResponse(responseCode = "204", description = "The experiment has at least one trace which hasn't been created yet", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = INVALID_PARAMETERS, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = NO_SUCH_TRACE, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = EXPERIMENT_NAME_EXISTS, content = @Content(schema = @Schema(implementation = ExperimentErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal trace-server error while trying to post experiment", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response postExperiment(@RequestBody(content = {
            @Content(schema = @Schema(implementation = ExperimentQueryParameters.class))
    }, required = true) QueryParameters queryParameters) {

        if (queryParameters == null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, MISSING_PARAMETERS);
        }
        Map<String, Object> parameters = queryParameters.getParameters();
        String errorMessage = QueryParametersUtil.validateExperimentQueryParameters(parameters);
        if (errorMessage != null) {
            return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, errorMessage);
        }
        String name = Objects.requireNonNull((String) parameters.get("name")); //$NON-NLS-1$
        List<String> tracesObj = Objects.requireNonNull((List<String>) parameters.get("traces")); //$NON-NLS-1$
        List<UUID> traceUUIDs = new ArrayList<>();

        List<IResource> traceResources = new ArrayList<>();
        for (Object uuidObj : tracesObj) {
            if (!(uuidObj instanceof String)) {
                return ErrorResponseUtil.newErrorResponse(Status.BAD_REQUEST, "UUID object is not instance of String"); //$NON-NLS-1$
            }
            UUID uuid = UUID.fromString((String) uuidObj);
            IResource traceResource = TraceManagerService.getTraceResource(uuid);
            if (traceResource == null) {
                // The trace should have been created first
                return ErrorResponseUtil.newErrorResponse(Status.NOT_FOUND, "No trace found with UUID " + uuid); //$NON-NLS-1$
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
                    TmfExperiment oldExperiment = EXPERIMENTS.get(expUUID);
                    boolean dispose = false;
                    if (oldExperiment == null) {
                        // should not happen
                        dispose = true;
                        oldExperiment = new TmfExperiment(ITmfEvent.class, resource.getLocation().toOSString(), new ITmfTrace[0], TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, resource);
                    }
                    Experiment entity = Experiment.from(oldExperiment, expUUID);
                    if (dispose) {
                        oldExperiment.dispose();
                    }
                    return ErrorResponseUtil.newErrorResponse(Status.CONFLICT, EXPERIMENT_NAME_EXISTS, EXPERIMENT_NAME_EXISTS_DETAIL, entity);
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
        } catch (CoreException e) {
            return ErrorResponseUtil.newErrorResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        TRACE_UUIDS.put(expUUID, traceUUIDs);
        EXPERIMENT_RESOURCES.put(expUUID, resource);
        TmfExperiment experiment = createExperimentInstance(expUUID);
        if (experiment == null) {
            return ErrorResponseUtil.newErrorResponse(Status.INTERNAL_SERVER_ERROR, "Failed to instantiate experiment"); //$NON-NLS-1$
        }

        return Response.ok(Experiment.from(experiment, expUUID)).build();
    }

    private static @Nullable TmfExperiment createExperimentInstance(UUID expUUID) {
        List<UUID> traceUUIDs = TRACE_UUIDS.get(expUUID);
        IResource resource = EXPERIMENT_RESOURCES.get(expUUID);
        if (traceUUIDs == null || resource == null) {
            return null;
        }
        // Create and set the supplementary folder
        createSupplementaryFolder(resource);

        // Instantiate the experiment and return it
        Map<UUID, ITmfTrace> uuidToTraceInstances = new LinkedHashMap<>();
        for (UUID uuid : traceUUIDs) {
            ITmfTrace trace = TraceManagerService.createTraceInstance(uuid);
            uuidToTraceInstances.put(uuid, trace);
        }
        // Determine cache size for experiments
        int cacheSize = Integer.MAX_VALUE;
        for (ITmfTrace trace : uuidToTraceInstances.values()) {
            cacheSize = Math.min(cacheSize, trace.getCacheSize());
        }
        TmfExperiment experiment = null;
        try {
            ITmfTrace[] traces = uuidToTraceInstances.values().toArray(new ITmfTrace[0]);
            String experimentTypeId = getOrDetectExerimentType(resource, traces);
            experiment = TmfTraceType.instantiateExperiment(experimentTypeId);
            if (experiment != null) {
                experiment.initExperiment(ITmfEvent.class, resource.getLocation().toOSString(), traces, cacheSize, resource, experimentTypeId);
                experiment.indexTrace(false);
                // read first event to make sure start time is initialized
                ITmfContext ctx = experiment.seekEvent(0);
                experiment.getNext(ctx);
                ctx.dispose();

                TmfSignalManager.dispatchSignal(new TmfTraceOpenedSignal(ExperimentManagerService.class, experiment, createBookmarksFile(resource)));

                EXPERIMENTS.put(expUUID, experiment);
                TRACE_INSTANCES.put(expUUID, uuidToTraceInstances);
                TRACE_ANNOTATION_PROVIDERS.put(expUUID, new TraceAnnotationProvider(experiment));
                return experiment;
            }
        } catch (CoreException e) {
            Activator.getInstance().logWarning("Error instantiating experiment"); //$NON-NLS-1$
        }
        return experiment;
    }

    /**
     * Get experiment type from experiment resource or auto-detect if it has not
     * been detected. It will fall-back to the default experiment if experiment
     * type can't be retrieved or detected.
     *
     * @param resource
     *            the experiment resource
     * @param traces
     *            array for traces for the experiment
     * @return experiment type ID
     * @throws CoreException
     *             in case of error handling Eclipse resource
     */
    private static @NonNull String getOrDetectExerimentType(IResource resource, ITmfTrace[] traces) throws CoreException {
        String experimentTypeId = TmfTraceType.getTraceTypeId(resource);
        if (experimentTypeId == null) {
            // Fall-back experiment type.
            experimentTypeId = TmfTraceType.DEFAULT_EXPERIMENT_TYPE;
            // Auto-detect experiment type
            List<TraceTypeHelper> helpers = TmfTraceType.selectExperimentType(Arrays.asList(traces), null);
            if (!helpers.isEmpty()) {
                experimentTypeId = helpers.get(0).getTraceTypeId();
            }
            resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, experimentTypeId);
        } else if (TmfTraceType.getTraceAttributes(experimentTypeId) == null) {
            // Plug-in for experiment type not available. Fall-back to default
            // experiment type.
            Activator.getInstance().logWarning("Extension for experiment type (" + experimentTypeId + ") not installed. Fall-back to generic experiment."); //$NON-NLS-1$ //$NON-NLS-2$
            experimentTypeId = TmfTraceType.DEFAULT_EXPERIMENT_TYPE;
        }
        return experimentTypeId;
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

    private static void createSupplementaryFolder(IResource experimentResource) {
        try {
            IFolder supplRootFolder = experimentResource.getProject().getFolder(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER_NAME);
            IFolder supplFolder = supplRootFolder.getFolder(experimentResource.getName() + SUFFIX);
            createFolder(supplFolder);
            experimentResource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplFolder.getLocation().toOSString());
        } catch (CoreException e) {
        }
    }

    private static IFile createBookmarksFile(IResource resource) {
        IFile file = ((IFolder) resource).getFile(resource.getName() + '_');
        try {
            if (!file.exists()) {
                final IFile bookmarksFile = ((IFolder) resource.getParent()).getFile(BOOKMARKS_HIDDEN_FILE);
                if (!bookmarksFile.exists()) {
                    final InputStream source = new ByteArrayInputStream(new byte[0]);
                    bookmarksFile.create(source, IResource.FORCE | IResource.HIDDEN, new NullProgressMonitor());
                }
                file.createLink(bookmarksFile.getLocation(), IResource.REPLACE | IResource.HIDDEN, new NullProgressMonitor());
            }
            file.setPersistentProperty(TmfCommonConstants.TRACETYPE, EXPERIMENT_EDITOR_INPUT_TYPE);
        } catch (CoreException e) {
            Activator.getInstance().logWarning("Error creating bookmarks file", e); //$NON-NLS-1$
        }
        return file;
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
    public static synchronized @Nullable TmfExperiment getExperimentByUUID(UUID expUUID) {
        TmfExperiment experiment = EXPERIMENTS.get(expUUID);
        if (experiment == null) {
            experiment = createExperimentInstance(expUUID);
        }
        return experiment;
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
     * Get the list of trace instances of an experiment from the experiment manager.
     *
     * @param expUUID
     *            queried {@link UUID}
     * @return the map from of trace UUID to trace instance.
     */
    public static Map<UUID, ITmfTrace> getTraceInstances(UUID expUUID) {
        return TRACE_INSTANCES.getOrDefault(expUUID, Collections.emptyMap());
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

    /**
     * Returns a {@link TraceAnnotationProvider} for a given experiment
     *
     * @param uuid
     *  the trace UUID
     * @return {@link TraceAnnotationProvider}
     */
    public static TraceAnnotationProvider getTraceAnnotationProvider(UUID uuid) {
        return TRACE_ANNOTATION_PROVIDERS.get(uuid);
    }

    /**
     * Dispose method to be only called at server shutdown. It disposes experiments, traces etc.
     */
    public static void dispose() {
        for (TmfExperiment experiment : EXPERIMENTS.values()) {
            if (experiment != null) {
                TmfSignalManager.dispatchSignal(new TmfTraceClosedSignal(experiment, experiment));
                // Experiment dispose() will dispose its traces as well.
                experiment.dispose();
            }
        }
        EXPERIMENTS.clear();
        TRACE_UUIDS.clear();
        TRACE_INSTANCES.clear();
        EXPERIMENT_RESOURCES.clear();
        TRACE_ANNOTATION_PROVIDERS.clear();
    }
}
