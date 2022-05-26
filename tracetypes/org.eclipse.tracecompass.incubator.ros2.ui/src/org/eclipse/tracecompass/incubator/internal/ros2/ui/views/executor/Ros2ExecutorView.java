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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.executor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.executor.Ros2ExecutorDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.AbstractRos2DataProviderTimeGraphView;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2ObjectTreeLabelProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * View for ROS 2 Executor.
 *
 * @author Christophe Bedard
 */
public class Ros2ExecutorView extends AbstractRos2DataProviderTimeGraphView {

    /** Tree columns for this provider */
    private static final String[] TREE_COLUMNS = new String[] { StringUtils.EMPTY, "Hostname", "Host ID" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String ID_SUFFIX = ".executor"; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_MACHINE = "🤖 "; //$NON-NLS-1$

    private static class Ros2ExecutorViewFilterLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            TimeGraphEntry entry = (TimeGraphEntry) element;
            ITmfTreeDataModel model = entry.getEntryModel();
            List<@NonNull String> labels = model.getLabels();
            if (columnIndex >= 0 && columnIndex < TREE_COLUMNS.length) {
                if (labels.size() <= 1) {
                    if (0 == columnIndex) {
                        return entry.getName();
                    }
                } else if (0 == columnIndex) {
                    // Prepend emoji
                    return COLUMN_TEXT_PREFIX_MACHINE + entry.getName();
                } else if (1 == columnIndex) {
                    // Host name
                    return Ros2ObjectTreeLabelProvider.hostnameToString(labels.get(1));
                } else if (2 == columnIndex) {
                    // Host ID
                    return Ros2ObjectTreeLabelProvider.hostIdToString(labels.get(2));
                }
            } else if (0 == columnIndex) {
                return entry.getName();
            }

            return StringUtils.EMPTY;
        }
    }

    /**
     * Constructor
     */
    public Ros2ExecutorView() {
        super(getFullViewId(), new Ros2ExecutorPresentationProvider(), Ros2ExecutorDataProvider.getFullDataProviderId());
        setTreeColumns(TREE_COLUMNS);
        setTreeLabelProvider(new Ros2ExecutorViewFilterLabelProvider());
        setFilterColumns(TREE_COLUMNS);
        setFilterLabelProvider(new Ros2ExecutorViewFilterLabelProvider());
    }

    /**
     * @return the full ID of this view
     */
    public static @NonNull String getFullViewId() {
        return AbstractRos2DataProviderTimeGraphView.getViewIdFromSuffix(ID_SUFFIX);
    }
}
