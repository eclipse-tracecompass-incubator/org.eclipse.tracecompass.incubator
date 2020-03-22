/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * The Multiview.
 *
 * @author Ivan Grinenko
 *
 */
public class MultiView extends AbstractMultiView {
    /**
     * The view's ID.
     */
    public static final String VIEW_ID = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.MultiView"; //$NON-NLS-1$

    private List<IDataProviderDescriptor> fDescriptors = new ArrayList<>();

    /**
     * Constructor.
     */
    public MultiView() {
        super(VIEW_ID);
    }

    @Override
    public void setFocus() {
        // Nothing yet
    }

    @Override
    public void resetStartFinishTime(boolean notify) {
        TmfWindowRangeUpdatedSignal signal = new TmfWindowRangeUpdatedSignal(this, TmfTimeRange.ETERNITY, getTrace());
        broadcast(signal);
    }

    @Override
    protected void partControlCreated(Composite mainComposite, SashForm sashForm) {
        // Don't show time scales at the very beginning, since there are no
        // lanes
        hideTimeScales();
    }

    @Override
    protected void createMenuItems() {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
        menuManager.add(createSetLaneAction());
    }

    private Action createSetLaneAction() {
        return new Action(Messages.Action_Set, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                ITmfTrace trace = getTrace();
                Shell shell = getSite().getShell();
                SetProviderDialog dialog = new SetProviderDialog(shell, trace, fDescriptors);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    // Clear
                    for (IMultiViewer lane : getLanes()) {
                        removeLane(lane);
                    }

                    // Fill
                    fDescriptors = dialog.getDescriptors();
                    for (IDataProviderDescriptor descriptor : fDescriptors) {
                        ProviderType type = descriptor.getType();
                        if (type == ProviderType.TREE_TIME_XY) {
                            addChartViewer(descriptor.getId(), true);
                        }
                        if (type == ProviderType.TIME_GRAPH) {
                            addTimeGraphViewer(descriptor.getId(), true);
                        }
                    }
                    alignViewers(true);
                    if (fDescriptors.isEmpty()) {
                        hideTimeScales();
                    }
                }
            }

        };
    }

}
