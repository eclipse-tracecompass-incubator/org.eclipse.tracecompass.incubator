/**********************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * {@link TimeGraphEntryModel} for the Flame chart data
 *
 * @author Geneviève Bastien
 */
public class FlameChartEntryModel extends TimeGraphEntryModel {

    /**
     * An enumeration for the type of flame chart entries
     */
    public enum EntryType {
        /**
         * A descriptive entry, for example the one for the trace
         */
        TRACE,
        /**
         * Represent a group of the callstack analysis
         */
        LEVEL,
        /**
         * Represent an entry with function data, the actual callstack data
         */
        FUNCTION,
        /**
         * This entry will show the kernel statuses for the TID running the callstack.
         * Will not always be present
         */
        KERNEL
    }

    private final EntryType fEntryType;
    private final int fDepth;

    /**
     * Constructor
     *
     * @param id
     *            unique ID for this {@link FlameChartEntryModel}
     * @param parentId
     *            parent's ID to build the tree
     * @param name
     *            entry's name
     * @param startTime
     *            entry's start time
     * @param endTime
     *            entry's end time
     * @param entryType
     *            The type of this entry
     */
    public FlameChartEntryModel(long id, long parentId, String name, long startTime, long endTime, EntryType entryType) {
        super(id, parentId, name, startTime, endTime);
        fEntryType = entryType;
        fDepth = -1;
    }

    /**
     * Constructor
     *
     * @param elementId
     *            unique ID for this {@link FlameChartEntryModel}
     * @param parentId
     *            parent's ID to build the tree
     * @param name
     *            entry's name
     * @param startTime
     *            entry's start time
     * @param endTime
     *            entry's end time
     * @param entryType
     *            The type of this entry
     * @param depth
     *            entry's PID or TID if is a thread
     */
    public FlameChartEntryModel(long elementId, long parentId, String name, long startTime, long endTime, EntryType entryType, int depth) {
        super(elementId, parentId, name, startTime, endTime);
        fEntryType = entryType;
        fDepth = depth;
    }

    /**
     * Getter for the entry type
     *
     * @return The type of entry.
     */
    public EntryType getEntryType() {
        return fEntryType;
    }

    /**
     * Get the depth of this entry
     *
     * @return The depth of this entry
     */
    public int getDepth() {
        return fDepth;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!super.equals(obj)) {
            // nullness, class, name, ids
            return false;
        }
        if (!(obj instanceof FlameChartEntryModel)) {
            return false;
        }
        FlameChartEntryModel other = (FlameChartEntryModel) obj;
        return fEntryType == other.fEntryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fEntryType);
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + fEntryType.toString();
    }

}
