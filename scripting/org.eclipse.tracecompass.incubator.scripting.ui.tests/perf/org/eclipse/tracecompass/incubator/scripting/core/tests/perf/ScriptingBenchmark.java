/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.tests.perf;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.ctf.core.tests.shared.CtfBenchmarkTrace;
import org.eclipse.tracecompass.incubator.internal.scripting.core.ScriptExecutionHelper;
import org.eclipse.tracecompass.incubator.scripting.core.trace.ScriptEventsIterator;
import org.eclipse.tracecompass.incubator.scripting.core.trace.TraceScriptingModule;
import org.eclipse.tracecompass.incubator.scripting.ui.tests.ActivatorTest;
import org.eclipse.tracecompass.incubator.scripting.ui.tests.TestModule;
import org.eclipse.tracecompass.internal.tmf.ui.project.model.TmfImportHelper;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.common.TmfXmlTestUtils;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.tests.shared.ProjectModelTestData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;

/**
 * Benchmarks EASE Scripting in native java, javascript (Nashorn and Rhino) and
 * python (Jython and Py4j)
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class ScriptingBenchmark {

    /**
     * Initial value for which to compute the Collatz sequence
     */
    public static final int INITIAL_VALUE = 10;
    /**
     * Last value for which to compute the Collatz sequence
     */
    public static final int LIMIT = 300000;

    private static final String JAVASCRIPT_EXTENSION = ".js";
    private static final String PYTHON_EXTENSION = ".py";
    private static final String XML_EXTENSION = ".xml";

    private static final String JAVA_PREFIX = "Java: ";
    private static final String RHINO_PREFIX = "JS Rhino: ";
    private static final String NASHORN_PREFIX = "JS Nashorn: ";
    private static final String PY4J_PREFIX = "Py4j: ";
    private static final String JYTHON_PREFIX = "Jython: ";
    private static final String XML_PREFIX = "XML: ";

    private static final int LOOP_COUNT = 25;
    private static final int LOOP_COUNT_SMALL = 5;

    private static final String DEFAULT_PROJECT = "Tracing";

    private static final String JAVASCRIPT_PATH = "scripts/perf/javascript/";
    private static final String PYTHON_PATH = "scripts/perf/python/";
    private static final String XML_PATH = "scripts/perf/xml/";

    private static final String RHINO_ENGINE = "org.eclipse.ease.javascript.rhino";
    private static final String NASHORN_ENGINE = "org.eclipse.ease.javascript.nashorn";
    private static final String JYTHON_ENGINE = "org.eclipse.ease.python.jython";
    private static final String PY4J_ENGINE = "org.eclipse.ease.lang.python.py4j.engine";
    private static final String XML_ANALYSIS_ID = "tracecompass.script.benchmark";

    private static final CtfTestTrace SMALL_TRACE = CtfTestTrace.SYNC_DEST;
    private static final CtfBenchmarkTrace LARGE_TRACE = CtfBenchmarkTrace.ALL_OS_ANALYSES;
    private static TmfProjectElement sfProject;

    /**
     * @return The arrays of parameters
     * @throws IOException
     *             Exception thrown initializing the traces
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() throws IOException {
        return Arrays.asList(new Object[][] {
                { "Empty Script", EMPTY, "empty", null, LOOP_COUNT },
                { "Simple Computation", SIMPLE_COMPUTATION, "simpleComputation", null, LOOP_COUNT },
                { "Computation Through Java", JAVA_COMPUTATION, "computationJava", null, LOOP_COUNT },
                { "Computation Through Callback", CALLBACK_COMPUTATION, "computationCallback", null, LOOP_COUNT / 2 },
                { "Compute Values in Java", COMPUTE_EACH_VALUE, "computationEachValue", null, LOOP_COUNT_SMALL },
                { "Read trace events for os-events", READ_LARGE_TRACE, "readTrace", ImmutableList.of(String.valueOf(LARGE_TRACE.getTracePath().getFileName())), LOOP_COUNT_SMALL },
                { "Read trace events for small trace", READ_SMALL_TRACE, "readTrace", ImmutableList.of(String.valueOf(FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getName())), LOOP_COUNT },
                { "TID analysis for Os-events", TID_ANALYSIS_LARGE_TRACE, "tidAnalysis", ImmutableList.of(String.valueOf(LARGE_TRACE.getTracePath().getFileName())), LOOP_COUNT_SMALL },
                { "TID analysis for small trace", TID_ANALYSIS_SMALL_TRACE, "tidAnalysis", ImmutableList.of(String.valueOf(FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getName())), LOOP_COUNT },

        });
    }

    private static final Runnable EMPTY = () -> {
        // Do nothing much, to benchmark script initialization
        int i = 0;
        System.out.println(i);
    };

    private static final Runnable SIMPLE_COMPUTATION = () -> {
        // Compute the Collatz Conjecture sequence for integers between INITIAL_VALUE and LIMIT
        int base = INITIAL_VALUE;
        long value = base;
        while (base < LIMIT) {
            if (value == 1) {
                value = base++;
            }
            if (value % 2 == 0) {
                value = value / 2;
            } else {
                value = 3 * value + 1;
            }
        }
    };

    private static final Runnable JAVA_COMPUTATION = () -> {
        TestModule testModule = new TestModule();
        testModule.doJavaLoop();
    };

    private static final Runnable CALLBACK_COMPUTATION = () -> {
        TestModule testModule = new TestModule();
        testModule.doLoopWithCallback(value -> {
            if (value % 2 == 0) {
                return value / 2;
            }
            return 3 * value + 1;
        });
    };

    private static final Runnable COMPUTE_EACH_VALUE = () -> {
        // Compute the Collatz Conjecture sequence for integers between INITIAL_VALUE and LIMIT
        TestModule testModule = new TestModule();
        testModule.doJavaLoop();
        int base = INITIAL_VALUE;
        long value = base;
        while (base < 100000) {
            if (value == 1) {
                value = base++;
            }
            value = testModule.compute(value);
        }
    };

    private static void readTrace(String absolutePathToTrace) {
        LttngKernelTrace trace = new LttngKernelTrace();
        try {
            trace.initTrace(null, absolutePathToTrace, CtfTmfEvent.class);
            TraceScriptingModule module = new TraceScriptingModule();
            ScriptEventsIterator eventIterator = module.getEventIterator(trace);
            eventIterator.addEvent("sched_switch");
            int schedSwitchCnt = 0;
            while (eventIterator.hasNext()) {
                ITmfEvent event = eventIterator.next();
                if (event.getName().equals("sched_switch")) {
                    schedSwitchCnt++;
                }
            }
            System.out.println("Count sched switch: " + schedSwitchCnt);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        } finally {
            trace.dispose();
        }
    }

    private static final Runnable READ_LARGE_TRACE = () -> {
        readTrace(LARGE_TRACE.getTracePath().toString());
    };

    private static final Runnable READ_SMALL_TRACE = () -> {
        try {
            readTrace(FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getAbsolutePath());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    };

    private static void deleteSupplementaryFiles(@NonNull ITmfTrace trace) {
        /*
         * Delete the supplementary files at the end of the benchmarks
         */
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        for (File file : suppDir.listFiles()) {
            file.delete();
        }
    }

    private static void runTidAnalysis(String absolutePathtoFile) {
        LttngKernelTrace trace = new LttngKernelTrace();
        TidAnalysisModule analysisModule = null;
        try {

            trace.initTrace(null, absolutePathtoFile, CtfTmfEvent.class);

            analysisModule = new TidAnalysisModule();
            analysisModule.setTrace(trace);

            TmfTestHelper.executeAnalysis(analysisModule);

        } catch (TmfTraceException | TmfAnalysisException e) {
            fail(e.getMessage());
        } finally {
            deleteSupplementaryFiles(trace);
            if (analysisModule != null) {
                analysisModule.dispose();
            }
            trace.dispose();
        }
    }

    private static final Runnable TID_ANALYSIS_SMALL_TRACE = () -> {
        try {
            runTidAnalysis(FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getAbsolutePath());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    };

    private static final Runnable TID_ANALYSIS_LARGE_TRACE = () -> {
        runTidAnalysis(LARGE_TRACE.getTracePath().toString());
    };

    private final String fName;
    private final Runnable fJavaMethod;
    private final String fScript;
    private final @Nullable List<@NonNull String> fArguments;
    private final int fLoopCount;

    /**
     * Constructor
     *
     * @param name
     *            The name of the test
     * @param javaMethod
     *            The java runnable method to benchmark the java part
     * @param script
     *            The name of the files for this test. Language-specific
     *            extension will be appended
     * @param arguments
     *            The list of arguments to pass to the script
     * @param loopCount
     *            The number of times the benchmark should run
     */
    public ScriptingBenchmark(String name, Runnable javaMethod, String script, @Nullable List<@NonNull String> arguments, int loopCount) {
        fName = name;
        fJavaMethod = javaMethod;
        fScript = script;
        fArguments = arguments;
        fLoopCount = loopCount;
    }

    /**
     * Prepare the workspace by preloading the required traces
     *
     * @throws CoreException
     *             Exception preparing the traces
     * @throws IOException
     *             Exception preparing the traces
     */
    @BeforeClass
    public static void prepareWorkspace() throws CoreException, IOException {
        IProject project = TmfProjectRegistry.createProject(DEFAULT_PROJECT, null, null);
        final TmfProjectElement projectElement = TmfProjectRegistry.getProject(project, true);
        TmfTraceFolder tracesFolder = projectElement.getTracesFolder();
        if (tracesFolder != null) {
            IFolder traceFolder = tracesFolder.getResource();

            /* Add the all os events trace from benchmark */
            Path tracePath = LARGE_TRACE.getTracePath();
            IPath pathString = new org.eclipse.core.runtime.Path(tracePath.toString());
            IResource linkedTrace = TmfImportHelper.createLink(traceFolder, pathString, pathString.lastSegment());
            if (!(linkedTrace != null && linkedTrace.exists())) {
                throw new NullPointerException("Trace cannot be created");
            }
            linkedTrace.setPersistentProperty(TmfCommonConstants.TRACETYPE,
                    "org.eclipse.linuxtools.lttng2.kernel.tracetype");

            /* Add the django test trace */
            String absolutePath = FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getAbsolutePath();
            pathString = new org.eclipse.core.runtime.Path(absolutePath);
            linkedTrace = TmfImportHelper.createLink(traceFolder, pathString, pathString.lastSegment());
            if (!(linkedTrace != null && linkedTrace.exists())) {
                throw new NullPointerException("Trace cannot be created");
            }
            linkedTrace.setPersistentProperty(TmfCommonConstants.TRACETYPE,
                    "org.eclipse.linuxtools.lttng2.kernel.tracetype");

            // Refresh the project model
            tracesFolder.refresh();

            for (TmfTraceElement traceElement : tracesFolder.getTraces()) {
                traceElement.refreshTraceType();
            }
        }
        projectElement.refresh();
        sfProject = projectElement;
    }

    /**
     * Delete project and traces at the end
     */
    @AfterClass
    public static void deleteProject() {
        TmfProjectElement project = sfProject;
        if (project != null) {
            Display.getDefault().syncExec(() -> ProjectModelTestData.deleteProject(project));
        }
    }

    /**
     * Benchmark the java runnable
     */
    @Test
    public void javaTest() {

        Performance perf = Performance.getDefault();
        PerformanceMeter pmJava = perf.createPerformanceMeter(JAVA_PREFIX + fName);
        perf.tagAsSummary(pmJava, JAVA_PREFIX + fName, Dimension.CPU_TIME);

        for (int i = 0; i < fLoopCount; i++) {
            pmJava.start();
            fJavaMethod.run();
            pmJava.stop();
        }
        pmJava.commit();

    }

    /**
     * Benchmark the javascript rhino engine
     */
    @Test
    public void javaScriptRhinoTest() {

        IPath absoluteFilePath = ActivatorTest.getAbsoluteFilePath(JAVASCRIPT_PATH + fScript + JAVASCRIPT_EXTENSION);

        Performance perf = Performance.getDefault();
        PerformanceMeter pmJavaScript = perf.createPerformanceMeter(RHINO_PREFIX + fName);
        perf.tagAsSummary(pmJavaScript, RHINO_PREFIX + fName, Dimension.CPU_TIME);

        for (int i = 0; i < fLoopCount; i++) {
            pmJavaScript.start();
            ScriptExecutionHelper.executeScript(Objects.requireNonNull(absoluteFilePath.toOSString()), RHINO_ENGINE, fArguments);
            pmJavaScript.stop();
        }
        pmJavaScript.commit();
    }

    /**
     * Benchmark the javascript nashorn engine
     */
    @Test
    public void javaScriptNashornTest() {
        IPath absoluteFilePath = ActivatorTest.getAbsoluteFilePath(JAVASCRIPT_PATH + fScript + JAVASCRIPT_EXTENSION);

        Performance perf = Performance.getDefault();
        PerformanceMeter pmJavaScript = perf.createPerformanceMeter(NASHORN_PREFIX + fName);
        perf.tagAsSummary(pmJavaScript, NASHORN_PREFIX + fName, Dimension.CPU_TIME);

        for (int i = 0; i < fLoopCount; i++) {
            pmJavaScript.start();
            ScriptExecutionHelper.executeScript(Objects.requireNonNull(absoluteFilePath.toOSString()), NASHORN_ENGINE, fArguments);
            pmJavaScript.stop();
        }
        pmJavaScript.commit();
    }

    /**
     * Benchmark the python py4j engine
     */
    @Test
    public void py4jTest() {
        // See if a specific file for py4j exists, otherwise, use the python
        // script
        IPath absoluteFilePath;
        try {
            absoluteFilePath = ActivatorTest.getAbsoluteFilePath(PYTHON_PATH + "py4j_" + fScript + PYTHON_EXTENSION);
        } catch (NullPointerException e) {
            absoluteFilePath = ActivatorTest.getAbsoluteFilePath(PYTHON_PATH + fScript + PYTHON_EXTENSION);
        }

        Performance perf = Performance.getDefault();
        PerformanceMeter pmPython = perf.createPerformanceMeter(PY4J_PREFIX + fName);
        perf.tagAsSummary(pmPython, PY4J_PREFIX + fName, Dimension.CPU_TIME);

        for (int i = 0; i < fLoopCount; i++) {
            pmPython.start();
            ScriptExecutionHelper.executeScript(Objects.requireNonNull(absoluteFilePath.toOSString()), PY4J_ENGINE, fArguments);
            pmPython.stop();
        }
        pmPython.commit();
    }

    /**
     * Benchmark the python jython engine
     */
    @Test
    public void jythonTest() {
        // See if a specific file for py4j exists, otherwise, use the python
        // script
        IPath absoluteFilePath;
        try {
            absoluteFilePath = ActivatorTest.getAbsoluteFilePath(PYTHON_PATH + "jython_" + fScript + PYTHON_EXTENSION);
        } catch (NullPointerException e) {
            absoluteFilePath = ActivatorTest.getAbsoluteFilePath(PYTHON_PATH + fScript + PYTHON_EXTENSION);
        }

        Performance perf = Performance.getDefault();
        PerformanceMeter pmPython = perf.createPerformanceMeter(JYTHON_PREFIX + fName);
        perf.tagAsSummary(pmPython, JYTHON_PREFIX + fName, Dimension.CPU_TIME);

        for (int i = 0; i < fLoopCount; i++) {
            pmPython.start();
            ScriptExecutionHelper.executeScript(Objects.requireNonNull(absoluteFilePath.toOSString()), JYTHON_ENGINE, fArguments);
            pmPython.stop();
            System.out.println("Did iteration " + i);
        }
        pmPython.commit();
    }

    private static @NonNull ITmfTrace getTraceForXml(String traceName) {
        String tracePath = null;
        try {
            if (String.valueOf(LARGE_TRACE.getTracePath().getFileName()).equals(traceName)) {
                tracePath = LARGE_TRACE.getTracePath().toString();
            } else if (String.valueOf(FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getName()).equals(traceName)) {
                tracePath = FileUtils.toFile(FileLocator.toFileURL(SMALL_TRACE.getTraceURL())).getAbsolutePath();
            }
        } catch (IOException e1) {
            throw new NullPointerException("Cannot initialize trace: " + e1.getMessage());
        }
        if (tracePath == null) {
            throw new NullPointerException("Cannot find trace: " + traceName);
        }

        // The trace is one of the 2 traces small or large
        LttngKernelTrace trace = new LttngKernelTrace();
        try {
            trace.initTrace(null, tracePath, CtfTmfEvent.class);
        } catch (TmfTraceException e) {
            trace.dispose();
            throw new NullPointerException("can't open the trace: " + e.getMessage());
        }
        return trace;
    }

    /**
     * Benchmark the xml analysis that does the same
     *
     * @throws TmfAnalysisException
     *             Exceptions thrown by setting the trace to the module
     */
    @Test
    public void xmlTest() throws TmfAnalysisException {
        // See if an XML file exists for this script
        IPath absoluteFilePath;
        try {
            absoluteFilePath = ActivatorTest.getAbsoluteFilePath(XML_PATH + fScript + XML_EXTENSION);
        } catch (NullPointerException e) {
            // There was no file, don't fail the test.
            return;
        }

        Performance perf = Performance.getDefault();
        PerformanceMeter pmXmlcript = perf.createPerformanceMeter(XML_PREFIX + fName);
        perf.tagAsSummary(pmXmlcript, XML_PREFIX + fName, Dimension.CPU_TIME);

        List<@NonNull String> arguments = fArguments;
        if (arguments == null || arguments.isEmpty()) {
            throw new NullPointerException("XML analysis set to run, but the arguments do not contain the path to the trace");
        }

        String tracePath = arguments.get(0);

        ITmfTrace trace = getTraceForXml(tracePath);
        try {
            for (int i = 0; i < fLoopCount; i++) {
                TmfAbstractAnalysisModule module = null;
                try {
                    module = TmfXmlTestUtils.getModuleInFile(Objects.requireNonNull(absoluteFilePath.toOSString()), XML_ANALYSIS_ID);
                    module.setTrace(trace);
                    pmXmlcript.start();
                    TmfTestHelper.executeAnalysis(module);
                    pmXmlcript.stop();
                } finally {
                    if (module != null) {
                        module.dispose();
                    }
                }
                deleteSupplementaryFiles(trace);
            }
            pmXmlcript.commit();
        } finally {
            deleteSupplementaryFiles(trace);
            trace.dispose();
        }

    }

}
