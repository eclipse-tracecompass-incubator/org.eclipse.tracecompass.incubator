/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProviderPreferencePage;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderConfigDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * <b><u>RenameExperimentHandler</u></b>
 * <p>
 */
public class ConfigureSymbolHandler extends AbstractHandler {

    private @Nullable ITmfTrace fTrace;

    // ------------------------------------------------------------------------
    // isEnabled
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

        // Make sure there is only selection and that it is an experiment
        fTrace = null;
        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
            Object element = sel.getFirstElement();
            if (element instanceof TmfCommonProjectElement) {
                TmfCommonProjectElement selected = (TmfCommonProjectElement) element;
                ITmfTrace trace = selected.getTrace();
                if (trace != null) {
                    fTrace = trace;
                }
            }
        }

        return (fTrace != null);
    }

    // ------------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------------

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        // Fire the Rename Experiment dialog
        Shell shell = window.getShell();
        SymbolProviderConfigDialog dialog = new SymbolProviderConfigDialog(shell, getProviderPages());
        if (dialog.open() == IDialogConstants.OK_ID) {
//            fPresentationProvider.resetFunctionNames();
//            refresh();
        }

        return null;
    }

    /**
     * @return an array of {@link ISymbolProviderPreferencePage} that will
     *         configure the current traces
     */
    private ISymbolProviderPreferencePage[] getProviderPages() {
        List<ISymbolProviderPreferencePage> pages = new ArrayList<>();
        ITmfTrace trace = fTrace;
        if (trace != null) {
            for (ITmfTrace subTrace : TmfTraceManager.getTraceSet(trace)) {
                for (ISymbolProvider provider : SymbolProviderManager.getInstance().getSymbolProviders(subTrace)) {
                    if (provider instanceof org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider) {
                        org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider provider2 = (org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider) provider;
                        ISymbolProviderPreferencePage page = provider2.createPreferencePage();
                        if (page != null) {
                            pages.add(page);
                        }
                    }
                }
            }
        }
        return pages.toArray(new ISymbolProviderPreferencePage[pages.size()]);
    }

}
