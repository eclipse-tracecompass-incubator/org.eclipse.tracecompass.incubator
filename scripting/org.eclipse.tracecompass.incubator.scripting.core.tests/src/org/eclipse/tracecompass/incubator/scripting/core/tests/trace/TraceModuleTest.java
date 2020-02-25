/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.tests.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.Messages;
import org.eclipse.tracecompass.incubator.scripting.core.tests.ActivatorTest;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.incubator.scripting.core.trace.TraceScriptingModule;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test the {@link TraceScriptingModule} class
 *
 * @author Geneviève Bastien
 */
public class TraceModuleTest {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final @NonNull String SOME_PROJECT_NAME = "myProject";
    private static final @NonNull String TRACE_PATH = "testfiles/traces/callstack.xml";
    private static final @NonNull String TRACE_FILE = "callstack.xml";
    private static final @NonNull String TRACE_FOLDER_PATH = "folder";
    private static final @NonNull String NONEXISTENT_FILE_TRACE = "NotARealFile.xml";
    private static final @NonNull String NONEXISTENT_TRACE = "UnexistentTrace";
    private static final @NonNull String NONEXISTENT_PROJECT = "NotARealProject";
    private static final @NonNull String NONEXISTENT_TRACE_PATH = "Not/A/Real/Path/NotARealFile.xml";
    private static final @NonNull String NONESISTENT_TRACE_IN_EXISTENT_PATH = "testfiles/traces/NotARealFile.xml";
    private static final @NonNull IProgressMonitor PROGRESS_MONITOR = new NullProgressMonitor();
    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private IProject fProject;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------
    /**
     * Setup a project with traces
     *
     * @throws CoreException
     *             Exception thrown during project creation
     * @throws IOException
     *             Exception thrown by supplementary file folder
     */
    @Before
    public void setUpEnvironment() throws CoreException, IOException {

        TEMPORARY_FOLDER.create();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // Create a project inside workspace location
        IWorkspaceRoot wsRoot = workspace.getRoot();
        IProject project = wsRoot.getProject(SOME_PROJECT_NAME);
        project.create(PROGRESS_MONITOR);
        project.open(PROGRESS_MONITOR);

        fProject = project;

        // Create the traces and experiments folder
        IFolder folder = project.getFolder("Experiments");
        if (!folder.exists()) {
            folder.create(true, true, PROGRESS_MONITOR);
        }
        folder = project.getFolder("Traces");
        if (!folder.exists()) {
            folder.create(true, true, PROGRESS_MONITOR);
        }

        // Add the trace
        IPath filePath = ActivatorTest.getAbsoluteFilePath(TRACE_PATH);
        IResource resource = folder.getFile(TRACE_FILE);
        assertNotNull(resource);
        assertTrue(ResourceUtil.createSymbolicLink(resource, filePath, true, PROGRESS_MONITOR));

        // Create the supplementary files folder for this trace
        File supplementaryFile = TEMPORARY_FOLDER.newFolder(TRACE_FILE);
        resource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplementaryFile.getPath());

        // Add the folder and the trace inside it
        IFolder traceFolder = folder.getFolder(TRACE_FOLDER_PATH);
        if (!traceFolder.exists()) {
            traceFolder.create(true, true, PROGRESS_MONITOR);
        }
        IResource extraTraceResource = traceFolder.getFile(TRACE_FILE);
        assertNotNull(extraTraceResource);
        assertTrue(ResourceUtil.createSymbolicLink(extraTraceResource, filePath, true, PROGRESS_MONITOR));

        // Create the supplementary files folder for the trace in a folder
        extraTraceResource.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER, supplementaryFile.getPath());

        // Add the trace element pointing to an unexistent trace
        resource = folder.getFile(NONEXISTENT_FILE_TRACE);
        assertNotNull(resource);
        assertTrue(ResourceUtil.createSymbolicLink(resource, new Path(NONESISTENT_TRACE_IN_EXISTENT_PATH), true, PROGRESS_MONITOR));
    }

    /**
     * Delete the project after tests
     *
     * @throws CoreException
     *             Exception thrown by project deletion
     */
    @After
    public void cleanUpEnvironment() throws CoreException {
        IProject project = fProject;
        if (project != null) {
            project.delete(true, PROGRESS_MONITOR);
        }
        TEMPORARY_FOLDER.delete();
    }

    // ------------------------------------------------------------------------
    // Test Cases
    // ------------------------------------------------------------------------

    /**
     * Test opening minimal file with existing trace
     *
     * @throws FileNotFoundException
     *             Exception thrown by the module
     */
    @Test
    public void testExistingTraces() throws FileNotFoundException {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();

        ITmfTrace trace = null;
        try {
            trace = traceScriptingModule.openMinimalTrace(SOME_PROJECT_NAME, TRACE_FILE, false);
            assertNotNull(trace);
            assertTrue(trace instanceof TmfXmlTraceStub);
        } finally {
            if (trace != null) {
                trace.dispose();
            }
        }
    }

    /**
     * Test opening from a project that does not exist
     */
    @Test
    public void testNonexistentProject() {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();
        boolean fileExist = true;
        try {
            traceScriptingModule.openMinimalTrace(NONEXISTENT_PROJECT, TRACE_FILE, false);
        } catch (FileNotFoundException e) {
            assertEquals(Messages.projectDoesNotExist, e.getMessage());
            fileExist = false;
        }
        assertTrue(!fileExist);
    }

    /**
     * Opening a trace whose name does not exist in the project
     */
    @Test
    public void testNonexistingTrace() {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();
        boolean fileExist = true;
        try {
            traceScriptingModule.openMinimalTrace(SOME_PROJECT_NAME, NONEXISTENT_TRACE, false);
        } catch (FileNotFoundException e) {
            assertEquals(Messages.traceDoesNotExist, e.getMessage());
            fileExist = false;
        }
        assertTrue(!fileExist);
    }

    /**
     * Opening a trace in a folder
     *
     * @throws FileNotFoundException
     *             Exception thrown by the module
     */
    @Test
    public void testTraceInFolder() throws FileNotFoundException {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();

        ITmfTrace trace = null;
        try {
            trace = traceScriptingModule.openMinimalTrace(SOME_PROJECT_NAME, TRACE_FOLDER_PATH + '/' + TRACE_FILE, false);
            assertNotNull(trace);
            assertTrue(trace instanceof TmfXmlTraceStub);
        } finally {
            if (trace != null) {
                trace.dispose();
            }
        }
    }

    /**
     * Opening a trace in a folder that does not exist
     */
    @Test
    public void testTraceInNonexistentFolder() {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();
        boolean fileExist = true;
        try {
            traceScriptingModule.openMinimalTrace(SOME_PROJECT_NAME, NONEXISTENT_TRACE_PATH, false);
        } catch (FileNotFoundException e) {
            assertEquals(Messages.folderDoesNotExist, e.getMessage());
            fileExist = false;
        }
        assertTrue(!fileExist);
    }

    /**
     * Opening a trace whose file underneath does not exist
     */
    @Test
    public void testNonexistentTraceFile() {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();
        boolean fileExist = true;
        try {
            traceScriptingModule.openMinimalTrace(SOME_PROJECT_NAME, NONEXISTENT_FILE_TRACE, false);
        } catch (FileNotFoundException e) {
            assertEquals(Messages.traceDoesNotExist, e.getMessage());
            fileExist = false;
        }
        assertTrue(!fileExist);
    }

    /**
     * Test the event iterator
     */
    @Test
    public void testEventIterator() {
        TraceScriptingModule traceScriptingModule = new TraceScriptingModule();

        ITmfTrace trace = ScriptingTestUtils.getTrace();
        try {

            Iterator<ITmfEvent> eventIterator = traceScriptingModule.getEventIterator(trace);
            assertNotNull(eventIterator);

            int count = 0;
            while (eventIterator.hasNext()) {
                eventIterator.next();
                count++;
            }
            // Make sure it parsed the whole trace
            assertEquals(36, count);

        } finally {
            trace.dispose();
        }
    }

}
