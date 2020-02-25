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

package org.eclipse.tracecompass.incubator.scripting.ui.trace;

import java.io.FileNotFoundException;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.Messages;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.project.model.ITmfProjectModelElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;

/**
 * Trace scripting module that allows to interact with the Trace Compass UI to
 * open an import traces.
 *
 * @author Benjamin Saint-Cyr
 */
public class TraceScriptingUI {

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
     * @throws FileNotFoundException
     *             Exception thrown if the file or the trace doesn't exist
     */
    @WrapToScript
    public ITmfTrace openTrace(String projectName, String traceName, @ScriptParameter(defaultValue = "false") boolean isExperiment) throws FileNotFoundException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject rootProject = root.getProject(projectName);
        TmfProjectElement rootElement = TmfProjectRegistry.getProject(Objects.requireNonNull(rootProject));
        if (rootElement == null) {
            throw new FileNotFoundException(Messages.projectDoesNotExist);
        }
        String folderName = isExperiment ? "Experiments" : "Traces"; //$NON-NLS-1$ //$NON-NLS-2$
        ITmfProjectModelElement traceFolder = rootElement.getChild(folderName);
        for (String name : traceName.split("/")) { //$NON-NLS-1$
            String folderPath = traceFolder.getPath().toOSString();
            traceFolder = traceFolder.getChild(name);
            if (traceFolder == null) {
                throw new FileNotFoundException("This folder could not be found: " + folderPath + '/' + name); //$NON-NLS-1$
            }
        }
        ITmfProjectModelElement traceChild = traceFolder;

        if (!(traceChild instanceof TmfCommonProjectElement)) {
            throw new FileNotFoundException();
        }
        // Open the trace
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                TmfOpenTraceHelper.openFromElement((TmfCommonProjectElement) traceChild);
            }
        });
        // Return the trace
        TmfCommonProjectElement projectElement = (TmfCommonProjectElement) traceChild;

        ITmfTrace trace = projectElement.getTrace();
        Integer tryCounter = 0;
        final Integer numberOfTry = 20;
        /*
         * Need to wait until the job inside openTraceFromElement is done and
         * the trace is open. Otherwise there will be a race condition.
         */
        while (trace == null && tryCounter < numberOfTry) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
            trace = projectElement.getTrace();
            tryCounter++;
        }
        if (trace != null) {
            return trace;
        }
        throw new FileNotFoundException("The trace did not open correctly"); //$NON-NLS-1$
    }
}
