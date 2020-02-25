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

package org.eclipse.tracecompass.incubator.scripting.ui.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ease.IExecutionListener;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.Script;
import org.eclipse.ease.service.EngineDescription;
import org.eclipse.ease.service.IScriptService;
import org.eclipse.ease.service.ScriptService;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;

/**
 * Test that all example scripts in the core plugin work well with their
 * respective engine. It only makes sure they do not throw exception with
 * default trace, or use scripted methods that are not available anymore.
 * Methods themselves should be unit tested on their own.
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class ScriptTest {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    // TODO Some scripts don't work with rhino: org.eclipse.ease.javascript.rhino
    private static final Map<String, String> ENGINES = ImmutableMap.of(
            "nashorn", "org.eclipse.ease.javascript.nashorn",
            "py4j", "org.eclipse.ease.lang.python.py4j.engine");

    private static final @NonNull String SOME_PROJECT_NAME = "myProject";
    private static final @NonNull IProgressMonitor PROGRESS_MONITOR = new NullProgressMonitor();

    private static final String SCRIPT_FOLDER = "scripts/tracecompass-ease-scripting/";

    // Final fields for the test
    private final IScriptEngine fScriptEngine;
    private final Path fScriptFile;

    // Environment variables
    private IProject fProject;
    private ITmfTrace fTrace;

    private @Nullable Script fScript;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------

    /**
     * For each engine, get all the example files. Parameters are an engine and
     * file
     *
     * @return The arrays of parameters
     * @throws IOException
     *             Exception thrown by getting files
     */
    @Parameters(name = "{0}")
    public static Iterable<Object[]> getParameters() throws IOException {
        List<Object[]> parameters = new ArrayList<>();
        final IScriptService scriptService = ScriptService.getService();
        for (Entry<String, String> entry : ENGINES.entrySet()) {
            final EngineDescription engineDescription = scriptService.getEngineByID(entry.getValue());
            if (engineDescription == null) {
                throw new NullPointerException("Script engine not found " + entry.getKey());
            }

            List<Path> filesForEngine = getFilesForEngine(engineDescription);
            assertFalse(filesForEngine.isEmpty());
            for (Path file : filesForEngine) {
                /*
                 * A separate engine needs to be created for each script,
                 * otherwise, only the first script works
                 */
                IScriptEngine engine = engineDescription.createEngine();
                Object[] engineValues = { entry.getKey() + ", " + file.getFileName(), engine, file };
                parameters.add(engineValues);
            }

        }
        return parameters;
    }

    /**
     * Constructor
     *
     * @param testName
     *            The name of the test
     * @param scriptEngine
     *            The engine to use
     * @param scriptFile
     *            The file to test
     */
    public ScriptTest(String testName, IScriptEngine scriptEngine, Path scriptFile) {
        fScriptEngine = scriptEngine;
        fScriptFile = scriptFile;
    }

    /**
     * Setup a project with traces
     *
     * @throws CoreException
     *             Exception thrown during project creation
     */
    @Before
    public void setUpEnvironment() throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // Create a project inside workspace location to store script files
        IWorkspaceRoot wsRoot = workspace.getRoot();
        IProject project = wsRoot.getProject(SOME_PROJECT_NAME);
        if (!project.exists()) {
            project.create(null);
        }
        if (!project.isOpen()) {
            project.open(null);
        }

        fProject = project;

        /*
         * Get a trace and make sure there is an active trace, as script may
         * need to get the active trace
         */
        ITmfTrace trace = ScriptingTestUtils.getTrace();
        fTrace = trace;
        TmfTraceManager.getInstance().traceOpened(new TmfTraceOpenedSignal(this, trace, null));
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
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    private static List<Path> getFilesForEngine(EngineDescription engineDescription) throws IOException {
        // Get the extension to look for
        String extension = engineDescription.getSupportedScriptTypes().get(0).getDefaultExtension();
        List<Path> files = new ArrayList<>();

        // Get the files from the scripting core plugin's documentation folder
        org.eclipse.core.runtime.IPath path = ActivatorTest.getAbsoluteFilePath(SCRIPT_FOLDER);
        Files.walkFileTree(Paths.get(path.toOSString()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Add the file if it has the correct extension
                if (file.toString().endsWith(extension)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    private static final IFile createFile(String name, Path scriptPath, IContainer parent) throws CoreException, IOException {
        List<String> readAllLines = Files.readAllLines(scriptPath, Charset.forName("UTF-8"));
        String code = String.join("\n", readAllLines);
        final IFile file = parent.getFile(new org.eclipse.core.runtime.Path(name));
        if (!file.exists()) {
            file.create(new ByteArrayInputStream(code.getBytes("UTF-8")), false, null);
        } else {
            file.setContents(new ByteArrayInputStream(code.getBytes("UTF-8")), false, false, null);
        }

        return file;
    }

    /**
     * Test executing the test script with the given engine and make sure it
     * runs
     *
     * @throws CoreException
     *             Exception thrown by executing the script
     * @throws IOException
     *             Exception thrown by executing the script
     */
    @Ignore
    @Test
    public void testScriptExecution() throws CoreException, IOException {

        /*
         * Save the execution script in a field so that listener can assign it
         */
        fScript = null;

        /* Execution listener to access the script and its result */
        fScriptEngine.addExecutionListener(new IExecutionListener() {

            @Override
            public void notify(IScriptEngine engine, Script script, int status) {
                if (status == IExecutionListener.SCRIPT_END && script != null) {
                    fScript = script;
                }
            }

        });

        /*
         * Create the file for the script. It needs to be in a IFile so that
         * EASE module loading can get bootstrapped and modules are loadable
         */
        IFile file = createFile("testScript." + fScriptEngine.getDescription().getSupportedScriptTypes().get(0).getDefaultExtension(), fScriptFile, fProject);
        fScriptEngine.executeAsync(file);
        runUntilTerminated(fScriptEngine);

        // Verify the script result
        Script script = fScript;
        assertNotNull(fScriptFile.getFileName().toString(), script);
        assertNull("Result of " + fScriptFile.getFileName(), script.getResult().getException());
    }

    private static void runUntilTerminated(IScriptEngine engine) {
        // Schedule the script and wait for it to terminate
        engine.schedule();
        while (!engine.isFinished()) {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
