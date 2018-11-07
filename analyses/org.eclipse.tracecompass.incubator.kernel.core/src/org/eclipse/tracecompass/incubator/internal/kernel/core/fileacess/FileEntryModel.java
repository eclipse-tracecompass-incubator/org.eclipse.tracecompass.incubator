/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.kernel.core.fileacess;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * Tag, you're it
 *
 * @author Matthew Khouzam
 */
public class FileEntryModel extends TimeGraphEntryModel {
    private Type fType;

    public enum Type {
        InRam, Directory, File
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

    public Type getType() {
        return fType;
    }

}
