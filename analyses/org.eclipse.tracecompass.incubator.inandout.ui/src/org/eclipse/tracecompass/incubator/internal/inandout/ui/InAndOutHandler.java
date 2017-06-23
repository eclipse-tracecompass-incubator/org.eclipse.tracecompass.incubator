/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.ui;

import java.io.File;
import java.net.URI;
import java.util.Objects;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.InAndOutAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfAnalysisElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfViewsElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * In and out configurator
 *
 * TODO: move to tmf.ui.
 *
 * @author Matthew Khouzam
 */
public class InAndOutHandler extends AbstractHandler {

    private @Nullable TmfAnalysisElement fAnalysis;

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

        // Make sure there is only one selection and that it is a trace
        fAnalysis = null;
        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
            // There should be only one item selected as per the plugin.xml
            Object element = sel.getFirstElement();
            if (element instanceof TmfAnalysisElement) {
                fAnalysis = (TmfAnalysisElement) element;
            }
        }

        if (fAnalysis != null) {
            return Objects.equals(fAnalysis.getAnalysisId(), InAndOutAnalysisModule.ID);
        }
        return false;
    }

    @Override
    public @Nullable Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        Shell activeShellChecked = HandlerUtil.getActiveShellChecked(event);
        TmfAnalysisElement analysis = fAnalysis;
        if (analysis != null && activeShellChecked != null) {
            TmfViewsElement parent = analysis.getParent();
            TmfCommonProjectElement traceElement = parent.getParent();
            // Trace can be null if not initialized.
            String elementPath = traceElement.getElementPath();
            if (traceElement instanceof TmfExperimentElement) {
                elementPath += ((TmfExperimentElement) traceElement).getSuffix();
            }
            IFolder traceSupplementaryFolder = analysis.getTraceSupplementaryFolder(elementPath);
            URI rawLocationURI = traceSupplementaryFolder.getRawLocationURI();
            String osString = rawLocationURI.getRawPath();
            InAndOutConfigDialog dialog = new InAndOutConfigDialog(activeShellChecked, new File(osString), traceElement.getTrace());
            dialog.open();
            if (dialog.isChanged()) {
                traceElement.closeEditors();
            }
            parent.refresh();
            if (dialog.isChanged()) {
                TmfOpenTraceHelper.openFromElement(traceElement);
            }
        }
        return null;
    }
}
