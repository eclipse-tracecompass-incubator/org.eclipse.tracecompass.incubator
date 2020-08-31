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
 * Thread Entry model... I'm really sure we did this somewhere before couldn't
 * find it.
 *
 * @author Matthew Khouzam
 */
public class ThreadEntryModel extends TimeGraphEntryModel {

    private final int fTid;

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
     * @param tid
     *            the tid
     */
    public ThreadEntryModel(long id, long parentId, String name, long startTime, long endTime, boolean hasRowModel, int tid) {
        super(id, parentId, name, startTime, endTime, hasRowModel);
        fTid = tid;
    }

    /**
     * The tid
     *
     * @return the tid
     */
    public int getTid() {
        return fTid;
    }

}
