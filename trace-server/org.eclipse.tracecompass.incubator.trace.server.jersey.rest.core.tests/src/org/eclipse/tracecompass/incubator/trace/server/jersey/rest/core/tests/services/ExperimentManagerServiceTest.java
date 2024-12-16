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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.ExperimentManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link ExperimentManagerService}
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("null")
public class ExperimentManagerServiceTest extends RestServerTest {

    private static final String TEST = "test";
    private static final @NonNull ImmutableSet<TraceModelStub> CONTEXT_SWITCH_SET = ImmutableSet.of(sfContextSwitchesKernelStub, sfContextSwitchesUstStub);
    private static final @NonNull ImmutableSet<TraceModelStub> CONTEXT_SWITCH_NOT_INITIALIZED_SET = ImmutableSet.of(sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);
    private static final @NonNull ExperimentModelStub EXPECTED = new ExperimentModelStub(TEST, CONTEXT_SWITCH_SET);

    /**
     * Basic test for the {@link ExperimentManagerService}
     */
    @Test
    public void testExperiment() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, sfContextSwitchesUstNotInitializedStub);
        TraceModelStub kernelStub = assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertEquals(CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(ustStub.getUUID().toString());
        traceUUIDs.add(kernelStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        ExperimentModelStub expStub = null;
        try (Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            expStub = response.readEntity(ExperimentModelStub.class);
            assertEquals("Failed to POST the experiment", EXPECTED, expStub);
            assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
            assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        }
        try (Response deleteResponse = expTarget.path(expStub.getUUID().toString()).request().delete()) {
            assertEquals("Failed to DELETE the experiment", EXPECTED, deleteResponse.readEntity(ExperimentModelStub.class));
            assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
            assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));
        }
    }

    /**
     * Test posting an experiment twice, it should be OK
     */
    @Test
    public void testRePost() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, sfContextSwitchesUstNotInitializedStub);
        TraceModelStub kernelStub = assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertEquals(CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(ustStub.getUUID().toString());
        traceUUIDs.add(kernelStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        ExperimentModelStub expStub = null;
        try (Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            expStub = response.readEntity(ExperimentModelStub.class);
            assertEquals("Failed to POST the experiment", EXPECTED, expStub);
            assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
            assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        }

        // Make a second post with the same name and traces, should return the experiment
        try (Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            ExperimentModelStub expStub2 = response.readEntity(ExperimentModelStub.class);
            assertEquals("Status of second post", Status.OK.getStatusCode(), response.getStatus());
            assertEquals("Failed to POST the experiment a second time", EXPECTED, expStub2);
            assertEquals("There should still be only one experiment", Collections.singleton(EXPECTED), getExperiments(expTarget));
            assertEquals("Adding an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub2.getUUID().toString()).request().get(ExperimentModelStub.class));

        }

        try (Response deleteResponse = expTarget.path(expStub.getUUID().toString()).request().delete()) {
            assertEquals("Failed to DELETE the experiment", EXPECTED, deleteResponse.readEntity(ExperimentModelStub.class));
        }
        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
        assertEquals("Deleting an experiment should not change the trace set", CONTEXT_SWITCH_NOT_INITIALIZED_SET, getTraces(traces));

    }

    /**
     * Test posting an experiment with different conflicting situations
     */
    @Test
    public void testPostConflicts() {
        WebTarget application = getApplicationEndpoint();
        WebTarget traces = application.path(TRACES);
        WebTarget expTarget = application.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(traces, sfContextSwitchesUstNotInitializedStub);
        TraceModelStub kernelStub = assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        TraceModelStub arm64Stub = assertPost(traces, sfArm64KernelNotIntitialzedStub);
        ImmutableSet<TraceModelStub> traceSet = ImmutableSet.of(sfContextSwitchesUstNotInitializedStub, sfContextSwitchesKernelNotInitializedStub, sfArm64KernelNotIntitialzedStub);
        assertEquals(null, traceSet, getTraces(traces));

        assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(kernelStub.getUUID().toString());
        traceUUIDs.add(ustStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);
        ExperimentModelStub expStub = null;

        try (Response response = expTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            expStub = response.readEntity(ExperimentModelStub.class);
            assertEquals("Failed to POST the experiment", EXPECTED, expStub);
            assertEquals("Failed to add experiment to set of experiments", Collections.singleton(EXPECTED), getExperiments(expTarget));
            assertEquals("Adding an experiment should not change the trace set", traceSet, getTraces(traces));
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        }

        // Post same name experiment, but different traces, should return a conflict
        List<String> traceUUIDs2 = new ArrayList<>();
        traceUUIDs2.add(kernelStub.getUUID().toString());
        Map<String, Object> parameters2 = new HashMap<>();
        parameters2.put(NAME, EXPECTED.getName());
        parameters2.put(TRACES, traceUUIDs2);
        try (Response response2 = expTarget.request().post(Entity.json(new QueryParameters(parameters2, Collections.emptyList())))) {
            assertEquals("Expected a conflict for posting different experiment", Status.CONFLICT.getStatusCode(), response2.getStatus());
            assertEquals("Conflict should return original experiment name", EXPECTED.getName(), response2.readEntity(ExperimentModelStub.class).getName());
            assertEquals("There should still be only one experiment", ImmutableSet.of(EXPECTED), getExperiments(expTarget));
            assertEquals("Failing to add an experiment should not change the trace set", traceSet, getTraces(traces));
            assertEquals("Failed to get the experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        }
        // Post same experiment name, but with traces with the same names, but not the same traces
        List<String> traceUUIDs3 = new ArrayList<>();
        traceUUIDs3.add(arm64Stub.getUUID().toString());
        traceUUIDs3.add(ustStub.getUUID().toString());
        Map<String, Object> parameters3 = new HashMap<>();
        parameters3.put(NAME, EXPECTED.getName());
        parameters3.put(TRACES, traceUUIDs3);
        try (Response response3 = expTarget.request().post(Entity.json(new QueryParameters(parameters3, Collections.emptyList())))) {
            assertEquals("Expected a conflict for posting different experiment", Status.CONFLICT.getStatusCode(), response3.getStatus());
            assertEquals("Conflict should return original experiment name", EXPECTED.getName(), response3.readEntity(ExperimentModelStub.class).getName());
            assertEquals("There should still be only one experiment", ImmutableSet.of(EXPECTED), getExperiments(expTarget));
            assertEquals("Failing to add an experiment should not change the trace set", traceSet, getTraces(traces));
            assertEquals("Failed to get the new experiment by its UUID", EXPECTED, expTarget.path(expStub.getUUID().toString()).request().get(ExperimentModelStub.class));
        }
        try (Response deleteResponse = expTarget.path(expStub.getUUID().toString()).request().delete()) {
            assertEquals("Failed to DELETE the experiment", EXPECTED, deleteResponse.readEntity(ExperimentModelStub.class));
            assertEquals("experiment set should be empty at this point", Collections.emptySet(), getExperiments(expTarget));
            assertEquals("Deleting an experiment should not change the trace set", traceSet, getTraces(traces));
        }
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
        WebTarget applicationTarget = getApplicationEndpoint();
        WebTarget tracesTarget = applicationTarget.path(TRACES);
        WebTarget experimentsTarget = applicationTarget.path(EXPERIMENTS);

        TraceModelStub ustStub = assertPost(tracesTarget, sfContextSwitchesUstNotInitializedStub);
        TraceModelStub kernelStub = assertPost(tracesTarget, sfContextSwitchesKernelNotInitializedStub);

        List<String> traceUUIDs = new ArrayList<>();
        traceUUIDs.add(ustStub.getUUID().toString());
        traceUUIDs.add(kernelStub.getUUID().toString());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(NAME, EXPECTED.getName());
        parameters.put(TRACES, traceUUIDs);

        try (Response response = experimentsTarget.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertNotNull(response);
        }
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
