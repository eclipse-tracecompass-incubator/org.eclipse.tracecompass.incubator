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

package org.eclipse.tracecompass.incubator.internal.ros.ui.actions;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tracecompass.incubator.internal.ros.ui.Activator;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

/**
 * Action to hide rosout-related information, such as the rosout publishers
 * (which relay a node's logs to the rosout node) and the rosout node (which may
 * redirect logs to stdout/stderr and a file).
 *
 * TODO support namespaces
 *
 * @see <a href="http://wiki.ros.org/roscpp/Overview/Logging">ROS wiki: roscpp
 *      logging</a>
 *
 * @author Christophe Bedard
 */
public class HideRosoutAction extends Action {

    private static final @NonNull String IMG_UI = "icons/elcl16/hide_rosout.gif"; //$NON-NLS-1$
    private static final String HIDE_ROSOUT_KEY_PREFIX = "hide.rosout."; //$NON-NLS-1$

    /** The time graph viewer */
    private final TimeGraphViewer fTimeGraphViewer;
    /** The dialog settings for persistence */
    private final IDialogSettings fDialogSettings;
    /** The suffix for the dialog setting key */
    private final String fHideRosoutKeySufix;

    /**
     * This filter simply keeps a list of elements that should be filtered out.
     * All the other elements will be shown. By default and when the list is set
     * to null, all elements are shown.
     */
    private class RosoutViewerFilter extends ViewerFilter {

        private static final String ROSOUT_NODE = "rosout/rosout"; //$NON-NLS-1$
        private static final String ROSOUT_TOPIC = "/rosout"; //$NON-NLS-1$

        private boolean fIsEnabled;

        public RosoutViewerFilter(boolean isEnabled) {
            fIsEnabled = isEnabled;
        }

        public void setFilterEnabled(boolean isEnabled) {
            fIsEnabled = isEnabled;
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (!fIsEnabled) {
                return true;
            }
            ITimeGraphEntry entry = (ITimeGraphEntry) element;
            String entryName = entry.getName();
            // Filter out rosout node and /rosout publishers
            if (entryName.equals(ROSOUT_NODE) || entryName.equals(ROSOUT_TOPIC)) {
                return false;
            }
            // Filter out list entry (e.g. Publishers, TransportLink) if rosout is the only topic
            if (entry.getChildren().size() == 1 && entry.getChildren().get(0).getName().equals(ROSOUT_TOPIC)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Constructor
     *
     * @param hideRosoutKeySuffix
     *            the suffix to the dialog setting key for the HideRosoutAction
     *            state
     * @param timeGraphViewer
     *            the time graph viewer
     * @param dialogSettings
     *            the dialog settings for persistence
     */
    public HideRosoutAction(String hideRosoutKeySuffix, TimeGraphViewer timeGraphViewer, final IDialogSettings dialogSettings) {
        fHideRosoutKeySufix = hideRosoutKeySuffix;
        fTimeGraphViewer = timeGraphViewer;
        fDialogSettings = dialogSettings;
        setText(Messages.HideRosoutAction_NameText);
        setToolTipText(Messages.HideRosoutAction_ToolTipText);
        setImageDescriptor(Objects.requireNonNull(Activator.getDefault()).getImageDescripterFromPath(IMG_UI));

        // FIXME make this work on initialization
        if (fDialogSettings != null) {
            boolean hideRosout = fDialogSettings.getBoolean(HIDE_ROSOUT_KEY_PREFIX + fHideRosoutKeySufix);
            update(hideRosout);
            setChecked(hideRosout);
        }
    }

    private void update(boolean hideRosout) {
        RosoutViewerFilter rosoutViewerFilter = null;
        for (ViewerFilter filter : fTimeGraphViewer.getFilters()) {
            if (filter instanceof RosoutViewerFilter) {
                rosoutViewerFilter = (RosoutViewerFilter) filter;
            }
        }
        if (rosoutViewerFilter == null) {
            rosoutViewerFilter = new RosoutViewerFilter(hideRosout);
            addFilter(rosoutViewerFilter);
        } else {
            rosoutViewerFilter.setFilterEnabled(hideRosout);
            changeFilter(rosoutViewerFilter);
        }
    }

    @Override
    public void run() {
        ITimeGraphEntry[] topInput = fTimeGraphViewer.getTimeGraphContentProvider().getElements(fTimeGraphViewer.getInput());
        if (topInput != null) {
            boolean hideRosout = isChecked();
            update(hideRosout);
            if (fDialogSettings != null) {
                fDialogSettings.put(HIDE_ROSOUT_KEY_PREFIX + fHideRosoutKeySufix, hideRosout);
            }
        }
    }

    /**
     * Add a viewer filter.
     *
     * @param filter
     *            The filter object to be added to the viewer
     */
    private void addFilter(@NonNull ViewerFilter filter) {
        fTimeGraphViewer.addFilter(filter);
    }

    /**
     * Update a viewer filter.
     *
     * @param filter
     *            The updated filter object
     */
    private void changeFilter(@NonNull ViewerFilter filter) {
        fTimeGraphViewer.changeFilter(filter);
    }
}
