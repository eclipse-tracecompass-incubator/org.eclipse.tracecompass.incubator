/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.ui.views;

import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.views.timegraph.ScriptedTimeGraphView;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Scripting module that allow to interact with views using EASE in the Trace
 * Compass UI.
 * <p>
 * Example scripts using views can be found here:
 * <ul>
 * <li><a href="../../core/analysis/doc-files/scriptedDataProvider.js">A
 * scripted time graph data provider</a> with script-defined entries and arrows,
 * in javascript</li>
 * <li><a href="../../core/analysis/doc-files/basicAnalysis.py">A basic
 * analysis,</a> building an state system and showing its data in a time graph,
 * in python</li>
 * </ul>
 *
 * @author Geneviève Bastien
 */
public class ViewModule {

    /** Module identifier. */
    public static final String MODULE_ID = "/TraceCompass/Views"; //$NON-NLS-1$

    /**
     * Open a time graph view with a data provider
     *
     * @param dataProvider
     *            The data provider used to populate the view
     */
    @WrapToScript
    public void openTimeGraphView(ITimeGraphDataProvider<TimeGraphEntryModel> dataProvider) {

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IViewPart view = openView(dataProvider.getId());
                    if (view instanceof ScriptedTimeGraphView) {
                        ((ScriptedTimeGraphView) view).refreshIfNeeded();
                    }
                } catch (final PartInitException e) {
                    // Do nothing
                }
            }
        });
    }

    private static @Nullable IViewPart openView(String name) throws PartInitException {
        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

        return activePage.showView(ScriptedTimeGraphView.ID, name.replace(":", ScriptedTimeGraphView.COLON), IWorkbenchPage.VIEW_ACTIVATE); //$NON-NLS-1$
    }
}
