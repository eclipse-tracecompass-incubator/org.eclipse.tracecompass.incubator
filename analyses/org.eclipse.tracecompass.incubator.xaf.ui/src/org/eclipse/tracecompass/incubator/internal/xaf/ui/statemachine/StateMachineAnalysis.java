/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraph;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.StateMachineBackendAnalysis;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.builder.BuilderInstanceGroup;
import org.eclipse.tracecompass.incubator.xaf.ui.handlers.XaFParameterProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;

/**
 * @author Raphaël Beamonte
 *
 */
public class StateMachineAnalysis extends AbstractSegmentStoreAnalysisModule {

    /**
     * The ID of this analysis
     */
    public static final String ANALYSIS_ID = "org.eclipse.tracecompass.xaf.core.statemachine"; //$NON-NLS-1$

    private static final @NonNull Collection<ISegmentAspect> BASE_ASPECTS =
            ImmutableList.of(
                    StateMachineSegment.TidAspect.INSTANCE,
                    StateMachineSegment.StatusAspect.INSTANCE,
                    StateMachineSegment.InvalidConstraintsAspect.INSTANCE);

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    private boolean finishedEditing = false;
    private boolean contentEdited = false;
    private Object finishedEditingLock = new Object();

    @SuppressWarnings("nls")
    @Override
    protected boolean buildAnalysisSegments(@NonNull ISegmentStore<@NonNull ISegment> segmentStore, @NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return false;
        }

        // We will create two more experiments: one with only the
        // kernel traces, and one with only the other traces
        List<ITmfTrace> kernelTraces = new ArrayList<>();
        List<ITmfTrace> otherTraces = new ArrayList<>();
        for (ITmfTrace t : getAllTraces(trace)) {
            if (t instanceof IKernelTrace) {
                kernelTraces.add(t);
            } else {
                otherTraces.add(t);
            }
        }
        TmfExperiment expKernel = new TmfExperiment(CtfTmfEvent.class, trace.getName()+" (Kernel only)", kernelTraces.toArray(new CtfTmfTrace[kernelTraces.size()]), TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
        TmfExperiment expOther = new TmfExperiment(CtfTmfEvent.class, trace.getName()+" (No kernel)", otherTraces.toArray(new CtfTmfTrace[otherTraces.size()]), TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);

        // Get the environment variables
        Map<String, String> env = System.getenv();
        String envv;

        // Get the analysis properties
        Properties xafproperties = null;
        boolean ENV_use_env = Boolean.parseBoolean(env.get("PARAMETER_USE_ENV"));
        if (ENV_use_env) {
            xafproperties = new Properties();
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_MODEL_PROVIDED,
                    new Boolean(Boolean.parseBoolean(env.get("PARAMETER_MODEL_PROVIDED"))).toString());
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION,
                    env.getOrDefault("MODEL", StringUtils.EMPTY));
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_SELECTED_VARIABLES,
                    env.getOrDefault("PARAMETER_SELECTED_VARIABLES", StringUtils.EMPTY));
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES,
                    env.getOrDefault("PARAMETER_SELECTED_TIMERANGES", StringUtils.EMPTY));
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION_HISTORY,
                    StringUtils.EMPTY);
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_CHECK_MODEL,
                    new Boolean(Boolean.parseBoolean(env.get("PARAMETER_CHECK_MODEL"))).toString());
            xafproperties.setProperty(XaFParameterProvider.PROPERTY_ALL_INSTANCES_VALID,
                    new Boolean(Boolean.parseBoolean(env.get("PARAMETER_ALL_INSTANCES_VALID"))).toString());
        } else {
            xafproperties = (Properties) getParameter("parameters");
        }
        if (xafproperties == null) {
            // No available properties, or the user cancelled
            return false;
        }


        for (ITmfTrace kernelTrace : expKernel.getTraces()) {
            KernelAnalysisModule kernelAnalysisModule = TmfTraceUtils.getAnalysisModuleOfClass(kernelTrace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
            if (kernelAnalysisModule != null) {
                IStatus status = kernelAnalysisModule.schedule();
                if (!status.isOK()) {
                    System.out.println("kernelAnalysisModule status is not ok");
                }
            } else {

                System.out.println("kernelAnalysisModule is null");
            }
        }

        StateMachineBenchmark benchmarkObject = new StateMachineBenchmark("State system build");

        List<StateMachineBackendAnalysis> stateMachineBackendAnalysisList = new ArrayList<>();
        for (ITmfTrace kernelTrace : expKernel.getTraces()) {
            StateMachineBackendAnalysis stateMachineBackendAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(kernelTrace, StateMachineBackendAnalysis.class, StateMachineBackendAnalysis.ID);
            stateMachineBackendAnalysisList.add(stateMachineBackendAnalysis);
            if (stateMachineBackendAnalysis != null) {
                IStatus status = stateMachineBackendAnalysis.schedule();
                if (status.isOK()) {
                    stateMachineBackendAnalysis.waitForCompletion();
                } else {
                    System.out.println("stateMachineAnalysisModule status is not ok");
                }
            } else {
                System.out.println("stateMachineAnalysisModule is null");
            }
        }

        benchmarkObject.stop();
        benchmarkObject = new StateMachineBenchmark("Critical path build");

        List<OsExecutionGraph> criticalPathModulesList = new ArrayList<>();
        for (ITmfTrace kernelTrace : expKernel.getTraces()) {
            OsExecutionGraph criticalPathAnalysisModule = TmfTraceUtils.getAnalysisModuleOfClass(kernelTrace, OsExecutionGraph.class, OsExecutionGraph.ANALYSIS_ID);
            criticalPathModulesList.add(criticalPathAnalysisModule);
            if (criticalPathAnalysisModule != null) {
                IStatus status = criticalPathAnalysisModule.schedule();
                if (!status.isOK()) {
                    System.out.println("fModuleCriticalPath status is not ok");
                }
            } else {
                System.out.println("fModuleCriticalPath is null");
            }
        }

        benchmarkObject.stop();


        // Either load the initial transitions from the file or generate them from the trace
        List<StateMachineTransition> initialTransitions = null;
        BuilderInstanceGroup builderInstanceGroup = null;
        boolean modelProvided = Boolean.parseBoolean(xafproperties.getProperty(XaFParameterProvider.PROPERTY_MODEL_PROVIDED, Boolean.TRUE.toString()));
        boolean allInstancesAsValid = Boolean.parseBoolean(xafproperties.getProperty(XaFParameterProvider.PROPERTY_ALL_INSTANCES_VALID, Boolean.FALSE.toString()));
        String model = xafproperties.getProperty(XaFParameterProvider.PROPERTY_MODEL_LOCATION);
        if (!modelProvided) {
            benchmarkObject = new StateMachineBenchmark("Building model");

            Set<String> variablesTypes = new HashSet<>(Arrays.asList(
                    xafproperties.getProperty(XaFParameterProvider.PROPERTY_SELECTED_VARIABLES)
                                 .split(XaFParameterProvider.PROPERTY_SEPARATOR)));

            Set<TimestampInterval> timestampIntervals = null;
            String timestampIntervalsStr = xafproperties.getProperty(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES);
            if (timestampIntervalsStr != null && !timestampIntervalsStr.isEmpty()) {
                timestampIntervals = new TreeSet<>();
                for (String intervalStr : timestampIntervalsStr.split(XaFParameterProvider.PROPERTY_SEPARATOR)) {
                    String[] intervalStrVal = intervalStr.split(XaFParameterProvider.PROPERTY_SELECTED_TIMERANGES_SEPARATOR);
                    long startTime = Long.parseLong(intervalStrVal[0]);
                    long endTime = Long.parseLong(intervalStrVal[1]);
                    timestampIntervals.add(new TimestampInterval(startTime, endTime));
                }
            }

            builderInstanceGroup = new BuilderInstanceGroup(stateMachineBackendAnalysisList, criticalPathModulesList, variablesTypes, timestampIntervals);
            builderInstanceGroup.buildOn(expOther);
            initialTransitions = builderInstanceGroup.getBasicInitialTransitions();
            benchmarkObject.stop();
        } else {
            try {
                initialTransitions = StateMachineUtils.getModelFromXML(model);
                if (initialTransitions == null) {
                    throw new RuntimeException("No initial transition found");
                }
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }



        benchmarkObject = new StateMachineBenchmark("Instances construction and constraint verification");

        StateMachineInstanceGroup smig = new StateMachineInstanceGroup(initialTransitions, stateMachineBackendAnalysisList, criticalPathModulesList, allInstancesAsValid);

        smig.buildOn(expOther);
        /*ITmfContext ctx = expOther.seekEvent(0);
        ITmfEvent event = null;

        event = expOther.getNext(ctx);
        while (event != null) {
            smig.receivedEvent(event);
            event = expOther.getNext(ctx);
        }*/

        benchmarkObject.stop();

        if (builderInstanceGroup != null) {
            benchmarkObject = new StateMachineBenchmark("Clean up the model built");
            builderInstanceGroup.cleanUnusedVariablesAndConstraints(initialTransitions);
            benchmarkObject.stop();

            try (PrintWriter writer = new PrintWriter("/tmp/sm.dot", "UTF-8")) { // FIXME: DEBUG PRINT SM
                System.out.println("Saving the completed state machine..."); // FIXME: DEBUG PRINT SM
                writer.write(StateMachineUtils.StateMachineToDot.drawStateMachine(initialTransitions));
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (model != null) {
                // Save the newly created state machine
                try (PrintWriter writer = new PrintWriter(model, "UTF-8")) {
                    writer.write(StateMachineUtils.getXMLFromModel(initialTransitions));
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                boolean checkModel = Boolean.parseBoolean(xafproperties.getProperty(XaFParameterProvider.PROPERTY_CHECK_MODEL, Boolean.TRUE.toString()));
                if (!checkModel) {
                    smig.cleanUpAdaptive();
                } else {
                    // Open the editor so the user can change stuff in the generated state machine
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                            contentEdited = false;

                            if (page != null) {
                                IWorkspace ws = ResourcesPlugin.getWorkspace();
                                IProject uniqueProject;
                                do {
                                    uniqueProject = ws.getRoot().getProject(
                                            String.format("XaFModels_%s", RandomStringUtils.randomAlphanumeric(8)));
                                } while (uniqueProject.exists());
                                final IProject modelProject = uniqueProject;

                                try {
                                    modelProject.create(null);
                                    modelProject.open(null);

                                    IPath location = new Path(model);
                                    IFile file = modelProject.getFile(location.lastSegment());
                                    file.createLink(location, IResource.NONE, null);
                                    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());

                                    IEditorPart editorPart = IDE.openEditor(page, file, desc.getId(), true);

                                    editorPart.addPropertyListener(new IPropertyListener() {
                                        @Override
                                        public void propertyChanged(@Nullable Object source, int propId) {
                                            if (ISaveablePart.PROP_DIRTY == propId) {
                                                if (!editorPart.isDirty()) {
                                                    try {
                                                        List<StateMachineTransition> trans = StateMachineUtils.getModelFromXML(model);
                                                        if (trans == null) {
                                                            MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR);
                                                            messageBox.setMessage("No initial transition found");
                                                            messageBox.open();
                                                        }
                                                    } catch (SAXException | IOException | ParserConfigurationException e) {
                                                        MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR);
                                                        messageBox.setMessage(e.getMessage());
                                                        messageBox.open();
                                                    }
                                                    contentEdited = true;
                                                }
                                            }
                                        }
                                    });
                                    page.addPartListener(new IPartListener2() {
                                        @Override
                                        public void partVisible(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partOpened(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partInputChanged(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partHidden(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partDeactivated(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partClosed(@Nullable IWorkbenchPartReference partRef) {
                                            if (partRef instanceof IEditorReference) {
                                                IEditorPart ieditorPart = ((IEditorReference)partRef).getEditor(true);
                                                if (ieditorPart != null && ieditorPart.equals(editorPart)) {
                                                    // Signal that we finished editing the XML file
                                                    synchronized(finishedEditingLock) {
                                                        finishedEditing = true;
                                                        finishedEditingLock.notifyAll();
                                                    }
                                                }

                                                // Delete the temporary project we created to edit this file
                                                try {
                                                    modelProject.delete(true, true, null);
                                                } catch (CoreException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        @Override
                                        public void partBroughtToTop(@Nullable IWorkbenchPartReference partRef) {
                                        }

                                        @Override
                                        public void partActivated(@Nullable IWorkbenchPartReference partRef) {
                                        }
                                    });
                                } catch (CoreException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    // Wait that the user finishes editing the model
                    synchronized(finishedEditingLock) {
                        while(!finishedEditing) {
                            try {
                                finishedEditingLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }

                    // If the model has been changed, load it back
                    if (contentEdited) {
                        try {
                            initialTransitions = StateMachineUtils.getModelFromXML(model);
                            if (initialTransitions == null) {
                                MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR);
                                messageBox.setMessage("No initial transition found");
                                messageBox.open();
                                return false;
                            }

                            // We need to rebuild the state machine instance group as everything could have changed...
                            smig = new StateMachineInstanceGroup(initialTransitions, stateMachineBackendAnalysisList, criticalPathModulesList, allInstancesAsValid);
                            smig.buildOn(expOther);
                        } catch (SAXException | IOException | ParserConfigurationException e) {
                            MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR);
                            messageBox.setMessage(e.getMessage());
                            messageBox.open();
                            return false;
                        }
                    } else {
                        smig.cleanUpAdaptive();
                    }
                }
            }
        }

        benchmarkObject = new StateMachineBenchmark("Printing checked instances");

        boolean print = true;
        envv = env.get("PRINTCHECK");
        if (envv != null) {
            if (Boolean.parseBoolean(envv)) {
                print = true;
            } else {
                print = false;
            }
        }
        if (print) {
            System.out.println(smig.toString());
        }

        benchmarkObject.stop();

        // All instances must be considered valid, there's no analysis to be ran
        if (!modelProvided && allInstancesAsValid) {
            return true;
        }

        boolean analyze = true;
        envv = env.get("ANALYZE");
        if (envv != null) {
            if (Boolean.parseBoolean(envv)) {
                analyze = true;
            } else {
                analyze = false;
            }
        }

        if (analyze) {
            StateMachineReport.debug("\nVérification des instances:");
            StateMachineReport.debug("===");

            benchmarkObject = new StateMachineBenchmark("Analyze");
            smig.analyze(expKernel);
            benchmarkObject.stop();
        }

        StateMachineBenchmark.printBenchmarks();

        // TODO: Takes a lot of time !!!
        segmentStore.addAll(smig.getSegmentStore());

        return true;
    }

    private static Set<ITmfTrace> getAllTraces(ITmfTrace trace) {
        Set<ITmfTrace> traces = new HashSet<>();

        if (trace instanceof TmfExperiment) {
            TmfExperiment exp = (TmfExperiment)trace;
            traces.addAll(exp.getTraces());
        } else {
            traces.add(trace);
        }

        return traces;
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        final String vtidContext = "context._vtid"; //$NON-NLS-1$
        for (ITmfTrace t : getAllTraces(trace)) {
            // Check if vtid is available
            ITmfEvent event = t.getNext(t.seekEvent(0));

            if (event != null && !event.getContent().getFieldNames().contains(vtidContext)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void canceling() {
        // TODO Auto-generated method stub

    }

    @Override
    protected Object @NonNull [] readObject(@NonNull ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

}
