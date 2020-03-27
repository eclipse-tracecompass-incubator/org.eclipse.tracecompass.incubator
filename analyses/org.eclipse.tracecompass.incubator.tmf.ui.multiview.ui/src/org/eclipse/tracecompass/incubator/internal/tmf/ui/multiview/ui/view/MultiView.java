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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
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

    /**
     * Create necessary items in the menu.
     */
    @Override
    protected void createMenuItems() {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
        menuManager.add(createAddLaneAction());
        menuManager.add(createRemoveLaneAction());

    }

    private Action createAddLaneAction() {
        return new Action(Messages.Action_Add, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                ITmfTrace trace = getTrace();
                Shell shell = getSite().getShell();
                AddProviderDialog dialog = new AddProviderDialog(shell, trace);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    IDataProviderDescriptor descriptor = dialog.getProvider();
                    if (descriptor == null) {
                        return;
                    }
                    if (dialog.getProviderType() == ProviderType.TREE_TIME_XY) {
                        addChartViewer(descriptor.getId(), true);
                    }
                    if (dialog.getProviderType() == ProviderType.TIME_GRAPH) {
                        addTimeGraphViewer(descriptor.getId(), true);
                    }
                    alignViewers(true);
                }
            }

        };
    }

    private Action createRemoveLaneAction() {
        return new Action(Messages.Action_Remove, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                List<@NonNull IMultiViewer> lanes = getLanes();
                if (lanes.isEmpty()) {
                    return;
                }
                IMultiViewer lane = lanes.get(lanes.size() - 1);
                removeLane(lane);
                refreshLayout();
            }
        };
    }


    @Override
    protected void partControlCreated(Composite mainComposite, SashForm sashForm) {
        // Don't show time scales at the very beginning, since there are no
        // lanes
        hideTimeScales();
    }

}
