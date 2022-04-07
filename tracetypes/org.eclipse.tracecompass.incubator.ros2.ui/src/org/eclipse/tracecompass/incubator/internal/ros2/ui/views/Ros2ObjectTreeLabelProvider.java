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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
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
    public static final String[] TREE_COLUMNS = new String[] { StringUtils.EMPTY, "Handle", "PID", "Hostname", "Host ID" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static final String COLUMN_TEXT_PREFIX_MACHINE = "🤖 "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_NODE = "🔲 "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_PUBLISHER = "↗️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_SUBSCRIPTION = "↘️ "; //$NON-NLS-1$
    private static final String COLUMN_TEXT_PREFIX_TIMER = "⏳ "; //$NON-NLS-1$

    private Ros2ObjectTreeLabelProvider() {
        // Do nothing
    }

    private static String handleToHex(Ros2ObjectHandle handle) {
        return "0x" + Long.toHexString(handle.getHandle()); //$NON-NLS-1$
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
                    String handle = getHandle(entryModel, type);
                    if (null != handle) {
                        return handle;
                    }
                } else if (2 == columnIndex) {
                    @Nullable
                    String pid = getPid(entryModel, type);
                    if (null != pid) {
                        return pid;
                    }
                } else if (3 == columnIndex) {
                    @Nullable
                    String hostId = getHostId(entryModel, type);
                    if (null != hostId) {
                        return hostId;
                    }
                } else if (4 == columnIndex) {
                    @Nullable
                    String hostname = getHostname(entryModel, type);
                    if (null != hostname) {
                        return hostname;
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
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            return COLUMN_TEXT_PREFIX_TIMER + entry.getName();
        }
        return null;
    }

    private static @Nullable String getHandle(Ros2ObjectTimeGraphEntryModel entryModel, Ros2ObjectTimeGraphEntryModelType type) {
        // Get handle
        if (Ros2ObjectTimeGraphEntryModelType.NODE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) entryModel.getObject();
            return handleToHex(nodeObject.getHandle());
        } else if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
            Ros2PublisherObject publisherObject = (Ros2PublisherObject) entryModel.getObject();
            return handleToHex(publisherObject.getHandle());
        } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
            Ros2SubscriptionObject subscriptionObject = (Ros2SubscriptionObject) entryModel.getObject();
            return handleToHex(subscriptionObject.getHandle());
        } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
            Ros2TimerObject timerObject = (Ros2TimerObject) entryModel.getObject();
            return handleToHex(timerObject.getHandle());
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

    private static @Nullable String getHostId(Ros2ObjectTimeGraphEntryModel entryModel, Ros2ObjectTimeGraphEntryModelType type) {
        // Get host ID only for traces, just to keep it simple
        if (Ros2ObjectTimeGraphEntryModelType.TRACE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) entryModel.getObject();
            return hostnameToString(nodeObject.getHandle().getHostProcess().getHostId().getHostname());
        }
        return null;
    }

    private static @Nullable String getHostname(Ros2ObjectTimeGraphEntryModel entryModel, Ros2ObjectTimeGraphEntryModelType type) {
        // Get hostname only for traces, just to keep it simple
        if (Ros2ObjectTimeGraphEntryModelType.TRACE == type) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) entryModel.getObject();
            return hostIdToString(nodeObject.getHandle().getHostProcess().getHostId().getId());
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
