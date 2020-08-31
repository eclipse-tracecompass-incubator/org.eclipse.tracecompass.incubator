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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swtchart.IPlotArea;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.internal.tmf.ui.views.xychart.LockRangeDialog;
import org.eclipse.ui.IViewSite;

/**
 * Successor of the {@link ChartMultiViewer} with actions added as context menu
 * items. Context menu is activated with a right-click on the plot area. These
 * actions are the only difference from {@link ChartMultiViewer}.
 *
 * @author Ivan Grinenko
 *
 */
public class ActionsChartMultiViewer extends ChartMultiViewer implements ISelectionProvider, MenuDetectListener {

    private final IViewSite fViewSite;
    private List<ISelectionChangedListener> fSelectionChangedListeners;
    private MenuManager fMenuManager;

    /**
     * Constructor.
     *
     * @param parent
     *            parent for the viewer
     * @param providerId
     *            data provider's ID
     * @param viewSite
     *            MultiView' site
     */
    public ActionsChartMultiViewer(Composite parent, String providerId, IViewSite viewSite) {
        super(parent, providerId);
        fViewSite = viewSite;
        getChartViewer().setMouseDragZoomProvider(new MouseDragZoomProvider(this));

        fMenuManager = createContextMenus();
        fillContextMenu(fMenuManager);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        if (listener != null && !fSelectionChangedListeners.contains(listener)) {
            fSelectionChangedListeners.add(listener);
        }
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        if (listener != null) {
            fSelectionChangedListeners.remove(listener);
        }
    }

    @Override
    public ISelection getSelection() {
        return () -> false;
    }

    @Override
    public void setSelection(ISelection selection) {
        // Nothing needed here
    }

    /**
     * Cancels the context menu by default since we want to detect absence of
     * dragging.
     */
    @Override
    public void menuDetected(MenuDetectEvent e) {
        e.doit = false;
    }

    /**
     * Can used to change the context menu thus changing the set of actions.
     *
     * @return The menu manager for the viewer.
     */
    public MenuManager getMenuManager() {
        return fMenuManager;
    }

    /**
     * Shows the context menu for the PlotArea.
     */
    public void showMenu() {
        getChartViewer().getSwtChart().getMenu().setVisible(true);
    }

    /**
     * Selection changed callback
     */
    public void fireSelectionChanged() {
        if (null != fSelectionChangedListeners) {
            for (ISelectionChangedListener listener : fSelectionChangedListeners) {
                listener.selectionChanged(new SelectionChangedEvent(this, getSelection()));
            }
        }
    }

    private void fillContextMenu(MenuManager menuManager) {
        menuManager.add(createClampAction());
    }

    private MenuManager createContextMenus() {
        MenuManager menuManager = new MenuManager();
        IPlotArea plotArea = getChartViewer().getSwtChart().getPlotArea();
        Menu menu = menuManager.createContextMenu((Composite) plotArea);
        getChartViewer().getSwtChart().setMenu(menu);
        getChartViewer().getSwtChart().addMenuDetectListener(this);
        fViewSite.registerContextMenu(menuManager, this);
        return menuManager;
    }

    private IAction createClampAction() {
        Action action = new Action(Messages.TmfChartView_LockYAxis, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                LockRangeDialog rangeDialog = new LockRangeDialog(fViewSite.getShell(), getChartViewer());
                rangeDialog.open();
            }
        };
        action.setChecked(false);
        return action;
    }

}
