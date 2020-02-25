/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * File Entry Model, a time graph entry mapped to a file's life
 *
 * @author Matthew Khouzam
 */
public class FileEntryModel extends TimeGraphEntryModel {
    private Type fType;

    /**
     * Type of file
     *
     * @author Matthew Khouzam
     */
    public enum Type {
        /**
         * In Memory File
         */
        InRam,
        /**
         * Folder or directory
         */
        Directory,
        /**
         * Actual file
         */
        File
    }

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param name
     *            Entry name to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param hasRowModel
     *            has a row model
     * @param type
     *            the type
     */
    public FileEntryModel(long id, long parentId, String name, long startTime, long endTime, boolean hasRowModel, Type type) {
        super(id, parentId, name, startTime, endTime, hasRowModel);
        fType = type;
    }

    /**
     * Get the type of the file entry
     *
     * @return the type
     */
    public Type getType() {
        return fType;
    }

}
