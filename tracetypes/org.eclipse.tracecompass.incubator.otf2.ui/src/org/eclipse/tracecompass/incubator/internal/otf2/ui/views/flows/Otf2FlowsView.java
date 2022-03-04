/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.ui.views.flows;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows.Otf2FlowsDataProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.ui.views.AbstractOtf2View;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for OTF2 flows
 *
 * @author Yoann Heitz
 */
@SuppressWarnings("restriction")
public class Otf2FlowsView extends AbstractOtf2View {

    /** View ID suffix */
    public static final String ID_SUFFIX = "flows"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Otf2FlowsView() {
        super(ID_SUFFIX, new BaseDataProviderTimeGraphPresentationProvider(), Otf2FlowsDataProvider.getFullDataProviderId());
        setAutoExpandLevel(1);
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        TimeGraphViewer timeGraphViewer = getTimeGraphViewer();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getResetScaleAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getPreviousEventAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getNextEventAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getToggleBookmarkAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getPreviousMarkerAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getNextMarkerAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getPreviousItemAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getNextItemAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getZoomInAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, timeGraphViewer.getZoomOutAction());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
    }
}
