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

package org.eclipse.tracecompass.incubator.scripting.ui.callstack;

import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.callstack.core.flamegraph.FlameGraphDataProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph.FlameGraphView;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.views.timegraph.ScriptedTimeGraphView;
import org.eclipse.tracecompass.incubator.scripting.core.callstack.CallStackScriptingModule;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A UI extension to the {@link CallStackScriptingModule}. It provides methods
 * to visualize callstack data providers in the Trace Compass UI.
 *
 * @author Geneviève Bastien
 */
public class CallStackUiScriptingModule {

    /**
     * Open a time graph view with a data provider
     *
     * @param dataProvider
     *            The data provider used to populate the view
     */
    @WrapToScript
    public void openFlameGraphView(FlameGraphDataProvider<?, ?, ?> dataProvider) {

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

        String secondaryId = name.replace(":", "[COLON]"); //$NON-NLS-1$ //$NON-NLS-2$
        // Hide the view first so it is refreshed when showing again
        // FIXME: It works, even though it does not close the view. how?
        IViewReference viewRef = activePage.findViewReference(FlameGraphView.ID, secondaryId);
        if (viewRef != null) {
            activePage.hideView(viewRef);
        }

        return activePage.showView(FlameGraphView.ID, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
    }

}