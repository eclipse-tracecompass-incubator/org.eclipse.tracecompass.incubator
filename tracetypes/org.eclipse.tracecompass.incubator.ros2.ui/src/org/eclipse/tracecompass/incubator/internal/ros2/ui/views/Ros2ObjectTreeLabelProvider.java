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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModelType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * TreeLabelProvider-like class for ROS 2 views using
 * {@link Ros2ObjectTimeGraphEntryModel}.
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectTreeLabelProvider {

    /** Tree columns for this provider */
    public static final String[] TREE_COLUMNS = new String[] { StringUtils.EMPTY, "PID", "Hostname" }; //$NON-NLS-1$ //$NON-NLS-2$

    private static final String COLUMN_TEXT_PREFIX_MACHINE = "🤖 "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_NODE = "🔲 "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_PUBLISHER = "↗️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_SUBSCRIPTION = "↘️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_CLIENT = "S↗️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_SERVICE = "S↘️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_TIMER = "⏳ "; //$NON-NLS-1$

    private Ros2ObjectTreeLabelProvider() {
        // Do nothing
    }

    /**
     * Get column text.
     *
     * @param element
     *            the entry element
     * @param columnIndex
     *            the column index
     * @return the column text
     */
    public static String getColumnText(Object element, int columnIndex) {
        TimeGraphEntry entry = (TimeGraphEntry) element;

        ITmfTreeDataModel model = entry.getEntryModel();
        if (model instanceof Ros2ObjectTimeGraphEntryModel) {
            Ros2ObjectTimeGraphEntryModel entryModel = (Ros2ObjectTimeGraphEntryModel) model;
            Ros2ObjectTimeGraphEntryModelType type = entryModel.getType();
            if (columnIndex >= 0 && columnIndex < TREE_COLUMNS.length) {
                if (0 == columnIndex) {
                    @Nullable
                    String name = getName(entry, type);
                    if (null != name) {
                        return name;
                    }
                } else if (1 == columnIndex) {
                    @Nullable
                    String pid = getPid(entryModel, type);
                    if (null != pid) {
                        return pid;
                    }
                } else if (2 == columnIndex) {
                    @Nullable
                    String hostId = getHostname(entryModel, type);
                    if (null != hostId) {
                        return hostId;
                    }
                }
            }
        } else if (0 == columnIndex) {
            return entry.getName();
        }

        return StringUtils.EMPTY;
    }

    private static @Nullable String getName(TimeGraphEntry entry, Ros2ObjectTimeGraphEntryModelType type) {
        // Prepend emoji to entry name
        if (Ros2ObjectTimeGraphEntryModelType.TRACE == type) {
            return COLUMN_TEXT_PREFIX_MACHINE + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            return COLUMN_TEXT_PREFIX_NODE + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            return COLUMN_TEXT_PREFIX_PUBLISHER + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            return COLUMN_TEXT_PREFIX_SUBSCRIPTION + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.CLIENT == type) {
            return COLUMN_TEXT_PREFIX_CLIENT + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.SERVICE == type) {
            return COLUMN_TEXT_PREFIX_SERVICE + entry.getName();
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            return COLUMN_TEXT_PREFIX_TIMER + entry.getName();
        }
        return null;
    }

    private static @Nullable String getPid(Ros2ObjectTimeGraphEntryModel entryModel, Ros2ObjectTimeGraphEntryModelType type) {
        // Get PID only for nodes, just to keep it simple
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) entryModel.getObject();
            return Long.toString(nodeObject.getHandle().getPid());
        }
        return null;
    }

    private static @Nullable String getHostname(Ros2ObjectTimeGraphEntryModel entryModel, Ros2ObjectTimeGraphEntryModelType type) {
        // Get hostname only for traces, just to keep it simple
        if (Ros2ObjectTimeGraphEntryModelType.TRACE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) entryModel.getObject();
            return hostnameToString(nodeObject.getHandle().getHostProcess().getHostId().getHostname());
        }
        return null;
    }

    /**
     * @param hostname
     *            the hostname
     * @return the hostname as a string
     */
    public static @NonNull String hostnameToString(@NonNull String hostname) {
        // For some reason, the hostname string has quotes around it
        return removeQuotes(hostname);
    }

    /**
     * @param hostId
     *            the host ID
     * @return the host ID as a string
     */
    public static @NonNull String hostIdToString(@NonNull String hostId) {
        // For some reason, the host ID string has quotes around it
        return removeQuotes(hostId);
    }

    private static @NonNull String removeQuotes(@NonNull String quotedString) {
        return Objects.requireNonNull(quotedString.replace("\"", "")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
