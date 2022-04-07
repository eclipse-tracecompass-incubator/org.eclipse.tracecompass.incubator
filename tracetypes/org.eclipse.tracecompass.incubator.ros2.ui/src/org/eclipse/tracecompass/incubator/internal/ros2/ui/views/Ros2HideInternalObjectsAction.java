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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.Activator;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

/**
 * Action to hide internal ROS 2 objects in the time graph viewer.
 *
 * @author Christophe Bedard
 */
public class Ros2HideInternalObjectsAction extends Action {

    private static final @NonNull String IMG_UI = "icons/elcl16/hide_internal.gif"; //$NON-NLS-1$
    private static final @NonNull String ACTION_TEXT = "Hide ROS 2 internal"; //$NON-NLS-1$
    private static final @NonNull String ACTION_TOOLTIP_TEXT = "Hide internal ROS 2 objects"; //$NON-NLS-1$

    private static final List<String> HIDDEN_TOPICS = Arrays.asList("/parameter_events", "/rosout"); //$NON-NLS-1$ //$NON-NLS-2$

    private final TimeGraphViewer fTimeGraphViewer;
    private final @NonNull Ros2HideInternalViewerFilter fFilter;

    private class Ros2HideInternalViewerFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (!(element instanceof ITimeGraphEntry)) {
                return true;
            }
            ITimeGraphEntry entry = (ITimeGraphEntry) element;
            return !HIDDEN_TOPICS.contains(entry.getName());
        }
    }

    /**
     * Constructor
     *
     * @param timeGraphViewer
     *            the time graph viewer
     */
    public Ros2HideInternalObjectsAction(TimeGraphViewer timeGraphViewer) {
        super(ACTION_TEXT, IAction.AS_CHECK_BOX);
        fTimeGraphViewer = timeGraphViewer;
        fFilter = new Ros2HideInternalViewerFilter();
        setToolTipText(ACTION_TOOLTIP_TEXT);
        setImageDescriptor(Objects.requireNonNull(Activator.getDefault()).getImageDescripterFromPath(IMG_UI));

        update();
    }

    @Override
    public void run() {
        update();
    }

    private void update() {
        if (isChecked()) {
            fTimeGraphViewer.addFilter(fFilter);
        } else {
            fTimeGraphViewer.removeFilter(fFilter);
        }
        fTimeGraphViewer.refresh();
    }
}
