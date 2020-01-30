/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.trace;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ease.IExecutionListener;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.Script;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.Messages;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.ScriptEventRequest;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceImportException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Scripting modules to open and manipulate traces
 *
 * @author Benjamin Saint-Cyr
 */
public class TraceScriptingModule extends AbstractScriptModule {

    /**
     * Fully open a trace in the Trace Compass application, ie it will open as
     * if the user had opened it in the UI, running all automatic analyses, etc.
     * If the trace is successfully opened, it becomes the currently active
     * trace.
     *
     * @param projectName
     *            The name of the project
     * @param traceName
     *            the trace to open
     * @param isExperiment
     *            is the trace an experiment
     * @return The trace
     * @deprecated This method was never supported, to open a trace in Trace
     *             Compass, use the <code>openTrace</code> method from the
     *             <code>'/TraceCompass/TraceUI'</code> module
     */
    @Deprecated
    @WrapToScript
    public ITmfTrace openTrace(String projectName, String traceName, @ScriptParameter(defaultValue = "false") boolean isExperiment) {
        // TODO may need to be implemented for Theia.
        // Can not do anything without the UI
        throw new UnsupportedOperationException("Load the /TraceCompass/TraceUI module instead"); //$NON-NLS-1$
    }

    /**
     * The trace will be opened, its events can be queried, but the analyses
     * will not have been run on it, they will not be available. The trace
     * content will not be opened in the UI and it won't be able to populate any
     * view. Typical use is for stand-alone scripts who want to run and export
     * content without interacting with the UI. The trace must have been
     * previously imported in trace compass as it needs to be in a project.
     * <p>
     * The trace will not be attached to the workspace, so it is important after
     * using to dispose of it by calling the {@link ITmfTrace#dispose()} method.
     * </p>
     *
     * @param projectName
     *            The name of the project
     * @param traceName
     *            the trace to open
     * @param isExperiment
     *            is the trace an experiment
     * @return The trace
     * @throws FileNotFoundException
     *             if the file or the trace doesn't exist
     */
    @WrapToScript
    public @Nullable ITmfTrace openMinimalTrace(String projectName, String traceName, @ScriptParameter(defaultValue = "false") boolean isExperiment) throws FileNotFoundException {
        // See if project exists
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            throw new FileNotFoundException(Messages.projectDoesNotExist);
        }

        String folderName = isExperiment ? "Experiments" : "Traces"; //$NON-NLS-1$ //$NON-NLS-2$
        IFolder subFolder = project.getFolder(folderName);
        if (!subFolder.exists()) {
            throw new FileNotFoundException(Messages.folderDoesNotExist);
        }
        String[] split = traceName.split("/"); //$NON-NLS-1$
        for (int i = 0; i <= split.length - 2; i++) {
            subFolder = subFolder.getFolder(split[i]);
            if (!subFolder.exists()) {
                throw new FileNotFoundException(Messages.folderDoesNotExist);
            }
        }
        String traceFile = split[split.length - 1];
        IFile file = subFolder.getFile(traceFile);


        IPath location = file.getLocation();
        if (location == null) {
            return null;
        }
        try {
            // open the trace
            String traceFileName = location.toFile().getName();
            return openAndInitializeTrace(file, Objects.requireNonNull(location.toOSString()), traceFileName, ""); //$NON-NLS-1$
        } catch (InstantiationException | IllegalAccessException | TmfTraceException | TmfTraceImportException e) {
            // We cannot differentiate in this method between a file that does
            // not exist in an existing resource or if the resource does not
            // exist. The message contains both possibilities
            throw new FileNotFoundException(Messages.traceDoesNotExist);
        }
    }

    private static ITmfTrace openAndInitializeTrace(IFile file, String location, String name, String typeID) throws TmfTraceException, InstantiationException, IllegalAccessException, FileNotFoundException, TmfTraceImportException {
        List<TraceTypeHelper> traceTypes = TmfTraceType.selectTraceType(location, typeID);
        if (traceTypes.isEmpty()) {
            throw new FileNotFoundException(Messages.noTraceType);
        }

        TraceTypeHelper helper = traceTypes.get(0);
        ITmfTrace trace = helper.getTraceClass().newInstance();
        trace.initTrace(file, location, ITmfEvent.class, name, typeID);
        return trace;
    }

    /**
     * Get the currently active trace, ie the last trace opened in the UI
     *
     * @return The current trace or <code>null</code> if no trace is opened
     */
    @WrapToScript
    public @Nullable ITmfTrace getActiveTrace() {
        return TmfTraceManager.getInstance().getActiveTrace();
    }

    /**
     * Get an iterator to iterate chronologically through the events of the
     * trace. To reduce overhead of passing all events to/from the script when
     * only a subset of events is requested, the
     * {@link ScriptEventsIterator#addEvent(String)} method can be used to set
     * the events to filter.
     *
     * Thus, to iterate through the events of a trace in a scripted analysis,
     * one can just do the following snippet (javascript)
     *
     * <pre>
     * var trace = getActiveTrace();
     * var iter = getEventIterator(trace);
     *
     * var event = null;
     * while (iter.hasNext()) {
     *
     *     event = iter.next();
     *
     *     // Do something with the event
     * }
     * </pre>
     *
     * @param trace
     *            The trace for which to get the event iterator
     *
     * @return The event iterator, starting from the first event
     */
    @WrapToScript
    public ScriptEventsIterator getEventIterator(@Nullable ITmfTrace trace) {
        if (trace == null) {
            throw new NullPointerException("Trace should not be null"); //$NON-NLS-1$
        }

        ScriptEventRequest scriptEventRequest = new ScriptEventRequest();
        trace.sendRequest(scriptEventRequest);
        setupEventIteratorExecutionListener(scriptEventRequest);

        return scriptEventRequest.getEventIterator();
    }

    private void setupEventIteratorExecutionListener(ScriptEventRequest scriptEventRequest) {
        IScriptEngine scriptEngine = getScriptEngine();
        if (scriptEngine == null) {
            return;
        }

        scriptEngine.addExecutionListener(new IExecutionListener() {
            @Override
            public void notify(@Nullable IScriptEngine engine, @Nullable Script script, int status) {
                if (status == SCRIPT_END && !scriptEventRequest.isCompleted()) {
                    scriptEventRequest.cancel();
                }
            }
        });
    }

    /**
     * A wrapper method to get the value of an event field. If the field itself
     * does not exist, it will try to resolve an aspect from the trace the event
     * is from.
     *
     * @param event
     *            The event for which to get the field
     * @param fieldName
     *            The name of the field to fetch
     * @return The field value object, or <code>null</code> if the field is not
     *         found
     */
    @WrapToScript
    public @Nullable Object getEventFieldValue(ITmfEvent event, String fieldName) {

        final ITmfEventField field = event.getContent().getField(fieldName);

        /* If the field does not exist, see if it's a special case */
        if (field == null) {
            // This will allow to use any column as input
            return TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), fieldName, event);
        }
        return field.getValue();

    }
}
