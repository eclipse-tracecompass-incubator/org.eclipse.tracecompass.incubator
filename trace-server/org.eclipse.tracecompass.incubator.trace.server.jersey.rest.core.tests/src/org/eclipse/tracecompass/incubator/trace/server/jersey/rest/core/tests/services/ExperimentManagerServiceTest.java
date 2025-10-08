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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment.IndexingStatusEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ExperimentErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ExperimentParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ExperimentQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Trace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.junit.Test;

/**
 * Test the {@link ExperimentManagerService}
 *
 * @author Loic Prieur-Drevon
 * @author Bernd Hufamnn
 */
@SuppressWarnings("null")
public class ExperimentManagerServiceTest extends RestServerTest {

    private static final String TEST = "test";
    private static final @NonNull List<Trace> CONTEXT_SWITCH_SET = List.of(sfContextSwitchesKernelWithOffsetStub, sfContextSwitchesUstWithOffsetStub);
    private static final @NonNull List<Trace> CONTEXT_SWITCH_NOT_INITIALIZED_SET = List.of(sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);
    private static final @NonNull Experiment EXPECTED = new Experiment()
            .name(TEST)
            .traces(CONTEXT_SWITCH_SET)
            .uuid(getExperimentUUID(TEST))
            .end(sfContextSwitchesKernelWithOffsetStub.getEnd())
            .start(sfContextSwitchesKernelWithOffsetStub.getStart())
            .nbEvents(sfContextSwitchesKernelWithOffsetStub.getNbEvents() + sfContextSwitchesUstWithOffsetStub.getNbEvents())
            .indexingStatus(IndexingStatusEnum.COMPLETED);

    private static final String EXPERIMENT_NAME_EXISTS = "The experiment (name) already exists and both differ."; //$NON-NLS-1$
    private static final String EXPERIMENT_NAME_EXISTS_DETAIL = "The experiment with same name already exists with conflicting parameters. Use a different name to avoid the conflict."; //$NON-NLS-1$

    /**
     * Basic test for the {@link ExperimentManagerService}
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testExperiment() throws ApiException {
        Trace ustStub = assertPost(sfContextSwitchesUstNotInitializedStub);
        Trace kernelStub = assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertEquals(CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());

        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());

        Experiment expStub = assertPostExperiment(TEST, ustStub, kernelStub);

        assertEquals("Failed to POST the experiment", EXPECTED, expStub);
        assertEquals("Failed to add experiment to set of experiments", List.of(EXPECTED), getExperiments());
        assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, getExperiment(expStub.getUUID()));

        Experiment deletedExperiment = deleteExperiment(expStub.getUUID());
        assertEquals("Failed to DELETE the experiment", EXPECTED, deletedExperiment);
        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());
        assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());
    }

    /**
     * Test posting an experiment twice, it should be OK
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testRePost() throws ApiException {
        Trace ustStub = assertPost(sfContextSwitchesUstNotInitializedStub);
        Trace kernelStub = assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertEquals(CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());

        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());

        Experiment expStub = assertPostExperiment(TEST, ustStub, kernelStub);
        // Make a second post with the same name and traces, should return the experiment
        Experiment expStub2 = assertPostExperiment(TEST, ustStub, kernelStub);

        assertEquals("Failed to POST the experiment a second time", EXPECTED, expStub2);
        assertEquals("There should still be only one experiment", List.of(EXPECTED), getExperiments());
        assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, getExperiment(expStub.getUUID()));

        Experiment deletedExperiment = deleteExperiment(expStub.getUUID());
        assertEquals("Failed to DELETE the experiment", EXPECTED, deletedExperiment);
        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());
        assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces());
    }

    /**
     * Test posting an experiment with different conflicting situations
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testPostConflicts() throws ApiException {
        Trace ustStub = assertPost(sfContextSwitchesUstNotInitializedStub);
        Trace kernelStub = assertPost(sfContextSwitchesKernelNotInitializedStub);
        Trace arm64Stub = assertPost(sfArm64KernelNotIntitialzedStub);
        List<Trace> traceList = new ArrayList<>();
        traceList.add(sfContextSwitchesUstNotInitializedStub);
        traceList.add(sfContextSwitchesKernelNotInitializedStub);
        traceList.add(sfArm64KernelNotIntitialzedStub);
        traceList.sort(TRACE_COMPARATOR);
        assertEquals(null, traceList, getTraces());

        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());

        Experiment expStub = assertPostExperiment(TEST, ustStub, kernelStub);

        assertEquals("Failed to POST the experiment", EXPECTED, expStub);
        assertEquals("Failed to add experiment to set of experiments", List.of(EXPECTED), getExperiments());
        assertEquals("Adding an experiment should not change the trace set", traceList, getTraces());
        assertEquals("Failed to get the experiment by its UUID", EXPECTED, getExperiment(expStub.getUUID()));

        // Post same name experiment, but different traces, should return a conflict
        List<UUID> traceUUIDs = List.of(kernelStub.getUUID());
        ExperimentParameters params = new ExperimentParameters().name(EXPECTED.getName()).traces(traceUUIDs);
        ExperimentQueryParameters experimentQueryParameters = new ExperimentQueryParameters().parameters(params);
        try {
            sfExpApi.postExperiment(experimentQueryParameters);
        } catch (ApiException e) {
            assertEquals(Status.CONFLICT.getStatusCode(), e.getCode());
            ExperimentErrorResponse errorResponse = deserializeErrorResponse(e.getResponseBody(), ExperimentErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Conflict name should be returned", EXPERIMENT_NAME_EXISTS, errorResponse.getTitle());
            assertEquals("Conflict detail should be returned", EXPERIMENT_NAME_EXISTS_DETAIL, errorResponse.getDetail());
            Experiment experimentObj = errorResponse.getExperiment();
            sortExperiment(experimentObj);
            assertEquals("Conflict should return original experiment", EXPECTED, experimentObj);
            assertEquals("There should still be only one experiment", List.of(EXPECTED), getExperiments());
            assertEquals("Failing to add an experiment should not change the trace set", traceList, getTraces());
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, getExperiment(expStub.getUUID()));
        }

        // Post same experiment name, but with traces with the same names, but not the same traces
        traceUUIDs = List.of(arm64Stub.getUUID(), ustStub.getUUID());
        params = new ExperimentParameters().name(EXPECTED.getName()).traces(traceUUIDs);
        experimentQueryParameters = new ExperimentQueryParameters().parameters(params);

        try {
            sfExpApi.postExperiment(experimentQueryParameters);
        } catch (ApiException e) {
            assertEquals(Status.CONFLICT.getStatusCode(), e.getCode());
            ExperimentErrorResponse errorResponse = deserializeErrorResponse(e.getResponseBody(), ExperimentErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals("Conflict name should be returned", EXPERIMENT_NAME_EXISTS, errorResponse.getTitle());
            assertEquals("Conflict detail should be returned", EXPERIMENT_NAME_EXISTS_DETAIL, errorResponse.getDetail());
            Experiment experimentObj = errorResponse.getExperiment();
            sortExperiment(experimentObj);
            assertEquals("Conflict should return original experiment", EXPECTED, experimentObj);
            assertEquals("There should still be only one experiment", List.of(EXPECTED), getExperiments());
            assertEquals("Failing to add an experiment should not change the trace set", traceList, getTraces());
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, getExperiment(expStub.getUUID()));
        }

        Experiment deletedExperiment = deleteExperiment(expStub.getUUID());
        assertEquals("Failed to DELETE the experiment", EXPECTED, deletedExperiment);
        assertEquals("experiment set should be empty at this point", Collections.emptyList(), getExperiments());
        assertEquals("Deleting an experiment should not change the trace set", traceList, getTraces());
    }

    /**
     * Test workspace structure for experiments
     *
     * @throws CoreException
     *             when a CoreException occurs
     * @throws IOException
     *             Exception thrown by getting trace path
     */
    @Test
    public void testWorkspaceStructure() throws CoreException, IOException {
        Trace ustStub = assertPost(sfContextSwitchesUstNotInitializedStub);
        Trace kernelStub = assertPost(sfContextSwitchesKernelNotInitializedStub);

        assertPostExperiment(TEST, ustStub, kernelStub);

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        // Make sure that workspace is refreshed
        root.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Check for Tracing project with name "Tracing"
        IProject tracingProject = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        assertTrue(tracingProject.exists());
        assertTrue(tracingProject.isNatureEnabled(TmfProjectNature.ID));

        IFolder experimentsFolder = tracingProject.getFolder("Experiments");
        assertTrue(experimentsFolder.exists());

        // Check for experiment folder
        IFolder expFolder = experimentsFolder.getFolder(TEST);
        assertTrue(expFolder.exists());

        // Check for Trace in the Experiments/test directory
        String contextSwitchesKernelPath = FileLocator.toFileURL(CtfTestTrace.CONTEXT_SWITCHES_KERNEL.getTraceURL()).getPath().replaceAll("/$", "");
        IPath path = Path.fromOSString(contextSwitchesKernelPath);

        // Check if trace parent folder was created
        IFolder traceParent = expFolder.getFolder(path.removeLastSegments(1));
        assertTrue(traceParent.exists());

        /*
         * Check for trace with name "kernel" under the trace parent directory.
         * Note that the trace is a dummy file (even if the trace is a directory trace)
         * which is not a symbolic link.
         */
        IFile trace = traceParent.getFile(CONTEXT_SWITCHES_KERNEL_NAME);
        assertTrue(trace.exists());
        assertTrue(!ResourceUtil.isSymbolicLink(trace));

        String traceType = trace.getPersistentProperty(TmfCommonConstants.TRACETYPE);
        assertNotNull(traceType);

        assertEquals("org.eclipse.linuxtools.lttng2.kernel.tracetype", traceType);
    }
}
