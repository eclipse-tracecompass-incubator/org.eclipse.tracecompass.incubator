/*******************************************************************************
 * Copyright (c) 2018, 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.handlers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.trace.SplitImportTracesOperation;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceTypeUIUtils;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * Handler used to separate multiple open tracing traces in a same .json file
 *
 * @author Katherine Nadeau
 *
 */
public class SplitTracesHandler extends AbstractHandler {

    private enum ImportConflictOptions {
        RENAME, OVERWRITE, SKIP;
    }

    private static final String TRACE_TYPE_ID = "org.eclipse.tracecompass.incubator.opentracing.core"; //$NON-NLS-1$

    private static final String DATA_KEY = "data"; //$NON-NLS-1$

    @Override
    public boolean isEnabled() {
        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return false;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return false;
        }
        ISelection selection = selectionProvider.getSelection();
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        final TmfCommonProjectElement traceElem = (firstElement instanceof TmfTraceElement) ? ((TmfTraceElement) firstElement).getElementUnderTraceFolder() : (TmfCommonProjectElement) firstElement;

        if (!traceElem.getTraceType().equals(TRACE_TYPE_ID)) {
            return false;
        }

        int arraySize = 0;

        try (FileReader fileReader = new FileReader(traceElem.getLocation().getPath())) {
            try (JsonReader reader = new JsonReader(fileReader)) {
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(reader, JsonObject.class);
                arraySize = object.get(DATA_KEY).getAsJsonArray().size();
            }
        } catch (IOException e) {
            return false;
        }

        return arraySize > 1;
    }

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        final TmfCommonProjectElement traceElem = (firstElement instanceof TmfTraceElement) ? ((TmfTraceElement) firstElement).getElementUnderTraceFolder() : (TmfCommonProjectElement) firstElement;

        TmfTraceFolder destinationFolder = traceElem.getProject().getTracesFolder();
        if (destinationFolder == null) {
            return null;
        }

        // Path for the new folder under Traces for the subTraces
        IPath traceElemPath = traceElem.getPath().removeFileExtension();
        String newTracesDestination = traceElemPath.makeRelativeTo(destinationFolder.getPath()).toString();
        if (ResourcesPlugin.getWorkspace().getRoot().findMember(traceElemPath) != null) {
            String index = startImportConflictDialog(traceElemPath);
            if (index == null) {
                return null;
            }
            newTracesDestination += index;
        }

        // We read, split and import the new traces
        try (FileReader fileReader = new FileReader(traceElem.getLocation().getPath())) {
            try (JsonReader reader = new JsonReader(fileReader)) {
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(reader, JsonObject.class);
                String newFolderPath = destinationFolder.getLocation().getPath() + newTracesDestination;
                SplitImportTracesOperation.splitAndImport(object, null, newFolderPath,
                        (tracesFolder, traceFile) -> SplitTracesHandler.refreshAndSetTraceType(tracesFolder, traceFile));
            }
        } catch (IOException e) {
        }

        return null;
    }

    private static String startImportConflictDialog(IPath tracePath) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        Shell shell = window.getShell();
        final MessageDialog dialog = new MessageDialog(shell,
                "Confirmation", //$NON-NLS-1$
                null,
                NLS.bind("Folder with name {0} already exists in project. Do you want to rename, overwrite or skip?", tracePath.lastSegment()), //$NON-NLS-1$
                MessageDialog.QUESTION, new String[] {
                        "Rename", //$NON-NLS-1$
                        "Overwrite", //$NON-NLS-1$
                        "Skip" //$NON-NLS-1$
                }, 2) {
            @Override
            protected int getShellStyle() {
                return super.getShellStyle() | SWT.SHEET;
            }
        };

        final int[] returnValue = new int[1];
        shell.getDisplay().syncExec(new Runnable() {

            @Override
            public void run() {
                returnValue[0] = dialog.open();
            }

        });

        switch (ImportConflictOptions.values()[returnValue[0]]) {
        case RENAME:
            return nextIndex(tracePath);
        case OVERWRITE:
            return ""; //$NON-NLS-1$
        case SKIP:
        default:
            return null;
        }
    }

    private static String nextIndex(IPath tracePath) {
        IResource existingResource = ResourcesPlugin.getWorkspace().getRoot().findMember(tracePath);
        IContainer folder = existingResource.getParent();

        for (int i = 2; true; i++) {
            String index = '(' + Integer.toString(i) + ')';
            if (folder.findMember(existingResource.getName() + index) == null) {
                return index;
            }
        }
    }

    /**
     * Refresh resource hierarchy (to make the newly imported traces visible),
     * and set the newly imported traces with the right trace type.
     *
     * @param tracesFolderAbsolutePath
     *            absolute path indicating the folder where the jaeger trace is
     *            stored
     * @param jaegerTraceFileName
     *            name of the (json) file containing the jaeger traces
     * @return false if the operation failed, true otherwise
     */
    public static boolean refreshAndSetTraceType(String tracesFolderAbsolutePath, String jaegerTraceFileName) {
        try {

            IWorkspace ws = ResourcesPlugin.getWorkspace();
            if (ws == null) {
                return false;
            }

            /*
             * Get the file representing the trace
             */
            File subTrace = new File(tracesFolderAbsolutePath + '/' + jaegerTraceFileName);
            URI location = subTrace.toURI();
            IFile[] files = ws.getRoot().findFilesForLocationURI(location);
            /*
             * Get the folder containing the trace
             */
            File tracesFolder = new File(tracesFolderAbsolutePath);
            location = tracesFolder.toURI();
            IContainer[] folders = ws.getRoot().findContainersForLocationURI(location);

            if ((files.length == 0) || (folders.length==0) ) {
                return false;
            }

            /*
             * Refresh resource hierarchy, to make the newly imported traces
             * visible.
             */
            folders[0].refreshLocal(IResource.DEPTH_INFINITE, null);
            /*
             * Set the newly imported traces with the right trace type.
             */
            TraceTypeHelper traceTypeHelper = TmfTraceType.getTraceType(TRACE_TYPE_ID);
            TmfTraceTypeUIUtils.setTraceType(files[0], traceTypeHelper);
        } catch (CoreException e) {
            return false;
        }
        return true;
    }

}
