/**********************************************************************
 * Copyright (c) 2019 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views;

import java.util.Objects;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.tracecompass.incubator.internal.ros.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.ui.actions.HideRosoutAction;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Abstract ROS view, used for common settings and actions
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRosView extends BaseDataProviderTimeGraphView {

    /** The ID prefix of ROS views */
    public static final String ID_PREFIX = "org.eclipse.tracecompass.incubator.ros.ui.views."; //$NON-NLS-1$

    /** The ID suffix for the concrete ROS view */
    private final String fIdSuffix;

    /**
     * Constructor
     *
     * @param idSuffix the view ID suffix
     * @param pres the presentation provider
     * @param providerId the data provider ID
     */
    public AbstractRosView(String idSuffix, TimeGraphPresentationProvider pres, String providerId) {
        super(ID_PREFIX + idSuffix, pres, providerId);
        fIdSuffix = idSuffix;
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        IDialogSettings settings = Objects.requireNonNull(Activator.getDefault()).getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        IAction hideRosoutAction = new HideRosoutAction(fIdSuffix, getTimeGraphViewer(), section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideRosoutAction);

        // add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
    }
}
