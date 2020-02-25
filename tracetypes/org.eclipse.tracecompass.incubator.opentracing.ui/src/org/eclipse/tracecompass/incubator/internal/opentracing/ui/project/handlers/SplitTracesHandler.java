/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.handlers;

import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
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
                SplitImportTracesOperation.splitAndImport(object, null, newTracesDestination, destinationFolder);
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
}
