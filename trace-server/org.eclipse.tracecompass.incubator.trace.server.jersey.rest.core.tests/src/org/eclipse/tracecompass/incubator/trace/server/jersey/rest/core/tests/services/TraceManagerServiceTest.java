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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.NewRestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment.IndexingStatusEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Trace;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TraceQueryParameters;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.junit.Test;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Loic Prieur-Drevon
 * @author Bend Hufmann
 */
@SuppressWarnings("null")
public class TraceManagerServiceTest extends NewRestServerTest {

    private static final String TEST = "test";
    private static final @NonNull List<Trace> CONTEXT_SWITCH_SET = List.of(sfContextSwitchesUstStub);
    private static final @NonNull Experiment EXPECTED = new Experiment()
            .name(TEST)
            .traces(CONTEXT_SWITCH_SET)
            .uuid(getExperimentUUID(TEST))
            .end(1450193745774189602L)
            .start(1450193697034689597L)
            .nbEvents(3934L)
            .indexingStatus(IndexingStatusEnum.COMPLETED);

    private static final String NAME_EXISTS = "The trace (name) already exists and both differ"; //$NON-NLS-1$
    private static final String NAME_EXISTS_DETAIL = "The trace with same name already exists with conflicting parameters. Use a different name to avoid the conflict. See error details for conflicting trace."; //$NON-NLS-1$
    private static final String TRACE_IN_USE = "The trace is in use by at least one experiment thus cannot be deleted."; //$NON-NLS-1$
    private static final String TRACE_IN_USE_DETAIL = "Delete all experiements using this trace before deleting the trace."; //$NON-NLS-1$

    /**
     * Test basic operations on the {@link TraceManagerService}.
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testWithOneTrace() throws ApiException {
        assertTrue("Expected empty set of traces", getTraces().isEmpty());

        Trace kernelStub = assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertEquals("Expected set of traces to contain trace2 stub",
                List.of(sfContextSwitchesKernelNotInitializedStub), getTraces());

        assertEquals(sfContextSwitchesKernelNotInitializedStub, sfTracesApi.deleteTrace(kernelStub.getUUID()));
        try {
            sfTracesApi.getTrace(kernelStub.getUUID());
        } catch (ApiException e) {
            assertEquals("Trace should have been deleted", 404, e.getCode());
        }
        assertEquals("Trace should have been deleted and trace set should be empty", Collections.emptyList(), getTraces());
    }

    /**
     * Test the server with two traces, to eliminate the server trace manager bug
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testWithTwoTraces() throws ApiException {
        assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertPost(sfContextSwitchesUstNotInitializedStub);

        assertEquals(List.of(sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub), getTraces());
    }

    /**
     * Test conflicting traces
     *
     * @throws ApiException
     *             if an error occurs
     */
    @Test
    public void testConflictingTraces() throws ApiException {
        assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertEquals(List.of(sfContextSwitchesKernelNotInitializedStub), getTraces());

        // Post the trace a second time
        assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertEquals(List.of(sfContextSwitchesKernelNotInitializedStub), getTraces());

        // Post a trace with the same name but another path, the name does not
        // matter if the path is different, the trace will be added
        assertPost(sfArm64KernelNotIntitialzedStub);
        List<Trace> expected = List.of(sfContextSwitchesKernelNotInitializedStub, sfArm64KernelNotIntitialzedStub);
        List<Trace> actual = getTraces();
        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));

        // Verify conflicting parameters (same trace but provided and different trace type)
        TraceParameters params = new TraceParameters();
        params.uri(sfContextSwitchesKernelNotInitializedStub.getPath()).name(sfContextSwitchesKernelNotInitializedStub.getName()).typeID("org.eclipse.linuxtools.tmf.ui.type.ctf");
        TraceQueryParameters traceQueryParameters = new TraceQueryParameters().parameters(params);
        try {
            sfTracesApi.putTrace(traceQueryParameters);
        } catch (ApiException e) {
            assertEquals(409, e.getCode());
            TraceErrorResponse errorResponse = deserializeErrorResponse(e.getResponseBody(), TraceErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(NAME_EXISTS, errorResponse.getTitle());
            assertEquals(NAME_EXISTS_DETAIL, errorResponse.getDetail());
            Trace traceObj = errorResponse.getTrace();
            assertEquals(sfContextSwitchesKernelNotInitializedStub, traceObj);
        }
        params.name(sfContextSwitchesKernelNotInitializedStub.getName() + "(1)");
        traceQueryParameters = new TraceQueryParameters().parameters(params);
        try {
            sfTracesApi.putTrace(traceQueryParameters);
        } catch (ApiException e) {
            fail();
        }
    }

    /**
     * Test workspace structure for traces
     *
     * @throws CoreException
     *             when a CoreException occurs
     * @throws IOException
     *             Exception thrown by getting trace path
     */
    @Test
    public void testWorkspaceStructure() throws CoreException, IOException {

        assertPost(sfContextSwitchesKernelNotInitializedStub);
        assertPost(sfContextSwitchesUstNotInitializedStub);

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        // Make sure that workspace is refreshed
        root.refreshLocal(IResource.DEPTH_INFINITE, null);

        // Check for Tracing project with name "Tracing"
        IProject tracingProject = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        assertTrue(tracingProject.exists());
        assertTrue(tracingProject.isNatureEnabled(TmfProjectNature.ID));

        IFolder tracesFolder = tracingProject.getFolder("Traces");
        assertTrue(tracesFolder.exists());

        String contextSwitchesKernelPath = FileLocator.toFileURL(CtfTestTrace.CONTEXT_SWITCHES_KERNEL.getTraceURL()).getPath().replaceAll("/$", "");
        IPath path = Path.fromOSString(contextSwitchesKernelPath);

        // Check if trace parent folder was created
        IFolder traceParent = tracesFolder.getFolder(path.removeLastSegments(1));
        assertTrue(traceParent.exists());

        // Check for trace with name kernel under the trace parent directory
        IFolder trace = traceParent.getFolder(CONTEXT_SWITCHES_KERNEL_NAME);
        assertTrue(trace.exists());

        // Make sure it's a symbolic link and not file copy
        assertTrue(ResourceUtil.isSymbolicLink(trace));

        // Verify that trace type is set as persistent property
        String traceType = trace.getPersistentProperty(TmfCommonConstants.TRACETYPE);
        assertNotNull(traceType);

        // Verify that the trace type is the kernel trace type
        assertEquals("org.eclipse.linuxtools.lttng2.kernel.tracetype", traceType);
    }

    /**
     * Test error case if trace does not exist
     */
    @Test
    public void testTraceNotExist() {

        TraceParameters params = new TraceParameters().uri("/path/does/not/exist").name("trace-does-not-exist");
        TraceQueryParameters traceQueryParameters = new TraceQueryParameters().parameters(params);
        try {
            sfTracesApi.putTrace(traceQueryParameters);
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(e.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertNotNull(errorResponse.getTitle());
        }
    }

    /**
     * Test delete of trace which is still in use by on experiment
     *             if such error happens
     */
    @Test
    public void testDeleteConflict() {
        Trace ustStub = assertPost(sfContextSwitchesUstNotInitializedStub);
        Experiment expStub = assertPostExperiment(TEST, ustStub);

        // Delete trace (failure)
        try {
            sfTracesApi.deleteTrace(ustStub.getUUID());
        } catch (ApiException e ) {
            assertEquals(409, e.getCode());
            TraceErrorResponse errorResponse = deserializeErrorResponse(e.getResponseBody(), TraceErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(TRACE_IN_USE , errorResponse.getTitle());
            assertEquals(TRACE_IN_USE_DETAIL, errorResponse.getDetail());
            Trace traceObj = errorResponse.getTrace();
            assertEquals(ustStub, traceObj);
        }

        // Delete experiment
        try {
            // Note: Equals works here because list of traces has only one trace
            assertEquals("Failed to DELETE the experiment", EXPECTED, sfExpApi.deleteExperiment(expStub.getUUID()));
        } catch (ApiException e ) {
            fail("Failed to DELETE experiment: " + e.getMessage());
        }

        // Delete trace (success)
        try {
            assertEquals("Failed to DELETE the trace", ustStub, sfTracesApi.deleteTrace(ustStub.getUUID()));
        } catch (ApiException e ) {
            fail("Failed to DELETE trace: " + e.getMessage());
        }
    }
}
