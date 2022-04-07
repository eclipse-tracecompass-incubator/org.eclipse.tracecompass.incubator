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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.messages;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.AbstractRos2DataProviderTimeGraphView;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2ObjectTreeLabelProvider;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for ROS 2 Messages.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesView extends AbstractRos2DataProviderTimeGraphView {

    private static final String ID_SUFFIX = ".messages"; //$NON-NLS-1$

    private class Ros2MessagesViewTreeLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            return Ros2ObjectTreeLabelProvider.getColumnText(element, columnIndex);
        }
    }

    /**
     * Constructor
     */
    public Ros2MessagesView() {
        super(getFullViewId(), new Ros2MessagesPresentationProvider(), Ros2MessagesDataProvider.getFullDataProviderId());
        setTreeColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setTreeLabelProvider(new Ros2MessagesViewTreeLabelProvider());
        setFilterColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setFilterLabelProvider(new Ros2MessagesViewTreeLabelProvider());
    }

    /**
     * @return the full ID of this view
     */
    public static @NonNull String getFullViewId() {
        return AbstractRos2DataProviderTimeGraphView.getViewIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        IDialogSettings settings = Objects.requireNonNull(Activator.getDefault()).getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        // Hide arrows button
        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideArrowsAction);

        // Add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
    }
}
