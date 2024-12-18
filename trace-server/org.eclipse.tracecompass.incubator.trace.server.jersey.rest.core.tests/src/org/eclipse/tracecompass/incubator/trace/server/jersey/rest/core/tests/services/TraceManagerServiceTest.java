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

import java.io.IOException;
import java.util.Collections;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.TraceManagerService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TraceModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.TmfProjectNature;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link TraceManagerService}
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("null")
public class TraceManagerServiceTest extends RestServerTest {

    /**
     * Test basic operations on the {@link TraceManagerService}.
     */
    @Test
    public void testWithOneTrace() {
        WebTarget traces = getApplicationEndpoint().path(TRACES);

        assertTrue("Expected empty set of traces", getTraces(traces).isEmpty());

        TraceModelStub kernelStub = assertPost(traces, sfContextSwitchesKernelNotInitializedStub);

        assertEquals(sfContextSwitchesKernelNotInitializedStub, traces.path(kernelStub.getUUID().toString()).request().get(TraceModelStub.class));

        assertEquals("Expected set of traces to contain trace2 stub",
                Collections.singleton(sfContextSwitchesKernelNotInitializedStub), getTraces(traces));

        String kernelStubUUUID = kernelStub.getUUID().toString();

        try (Response deleteResponse = traces.path(kernelStub.getUUID().toString()).request().delete()) {
            int deleteCode = deleteResponse.getStatus();
            assertEquals("Failed to DELETE trace2, error code=" + deleteCode, 200, deleteCode);
            assertEquals(sfContextSwitchesKernelNotInitializedStub, deleteResponse.readEntity(TraceModelStub.class));
        }
        try (Response response = traces.path(kernelStubUUUID).request(MediaType.APPLICATION_JSON).get()) {
            assertEquals("Trace should have been deleted", 404, response.getStatus());
        }
        assertEquals("Trace should have been deleted and trace set should be empty", Collections.emptySet(), getTraces(traces));
    }

    /**
     * Test the server with two traces, to eliminate the server trace manager bug
     */
    @Test
    public void testWithTwoTraces() {
        WebTarget traces = getApplicationEndpoint().path(TRACES);

        assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertPost(traces, sfContextSwitchesUstNotInitializedStub);

        assertEquals(ImmutableSet.of(sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub), getTraces(traces));
    }

    /**
     * Test conflicting traces
     */
    @Test
    public void testConflictingTraces() {
        WebTarget traces = getApplicationEndpoint().path(TRACES);

        assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertEquals(ImmutableSet.of(sfContextSwitchesKernelNotInitializedStub), getTraces(traces));

        // Post the trace a second time
        assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertEquals(ImmutableSet.of(sfContextSwitchesKernelNotInitializedStub), getTraces(traces));

        // Post a trace with the same name but another path, the name does not
        // matter if the path is different, the trace will be added
        assertPost(traces, sfArm64KernelNotIntitialzedStub);
        assertEquals(ImmutableSet.of(sfContextSwitchesKernelNotInitializedStub, sfArm64KernelNotIntitialzedStub), getTraces(traces));
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
        WebTarget traces = getApplicationEndpoint().path(TRACES);

        assertPost(traces, sfContextSwitchesKernelNotInitializedStub);
        assertPost(traces, sfContextSwitchesUstNotInitializedStub);

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
}
