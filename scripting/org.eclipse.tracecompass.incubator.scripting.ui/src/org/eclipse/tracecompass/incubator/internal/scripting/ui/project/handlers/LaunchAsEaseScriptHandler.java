/*******************************************************************************
 * Copyright (c) 2019 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.project.handlers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.OpenDebugConfigurations;
import org.eclipse.debug.internal.ui.actions.OpenRunConfigurations;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Command Handler to run scripts.
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings({ "restriction", "null" })
public class LaunchAsEaseScriptHandler extends AbstractHandler {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    private static final String TYPE_PARAMETER = "org.eclipse.tracecompass.incubator.scripting.ui.commandparameter.launch_as_ease_script.type"; //$NON-NLS-1$
    private static final String MODE_PARAMETER = "org.eclipse.tracecompass.incubator.scripting.ui.commandparameter.launch_as_ease_script.mode"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    @Nullable
    private TreeSelection fSelection = null;

    // ------------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------------

    @Override
    public boolean isEnabled() {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }

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

        // Make sure selection contains only traces
        fSelection = null;
        if (selection instanceof TreeSelection) {
            fSelection = (TreeSelection) selection;
            Iterator<Object> iterator = fSelection.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();
                if (!(element instanceof TmfCommonProjectElement) && !(element instanceof IFile)) {
                    return false;
                }
            }
        }

        // If we get here, either nothing is selected or everything is a trace
        return !selection.isEmpty();
    }

    // ------------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------------

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {

        TreeSelection selection = fSelection;
        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null || event == null || selection == null) {
            return null;
        }

        List<LaunchShortcutExtension> allShortCuts = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchShortcuts();
        if (allShortCuts == null) {
            return null;
        }
        for (Object element : selection.toList()) {
            // Get the file to execute
            IResource resource = null;
            if (element instanceof IFile) {
                resource = (IFile) element;
            } else if (element instanceof TmfCommonProjectElement) {
                TmfCommonProjectElement trace = (TmfCommonProjectElement) element;
                if (trace instanceof TmfTraceElement) {
                    trace = ((TmfTraceElement) trace).getElementUnderTraceFolder();
                }
                resource = trace.getResource();
            }
            String type = event.getParameter(TYPE_PARAMETER);
            String mode = event.getParameter(MODE_PARAMETER);
            if (resource != null) {
                IStructuredSelection s = new StructuredSelection(Arrays.asList(resource));
                if (type.equals(LaunchElementTypeContributionItem.LAUNCH_DIALOG_CONFIG_ID)) {
                    if (mode.equals(ILaunchManager.RUN_MODE)) {
                        OpenRunConfigurations r = new OpenRunConfigurations();
                        r.run(null);
                    }

                    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                        OpenDebugConfigurations r = new OpenDebugConfigurations();
                        r.run(null);
                    }
                } else {
                    for (LaunchShortcutExtension launchShortcutExtension : allShortCuts) {
                        if (type.equals(launchShortcutExtension.getId())) {
                            launchShortcutExtension.launch(s, mode);
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

}
