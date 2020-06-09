/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * The entry model for scripted analyses. The constructor will take care of
 * generating a unique ID for this entry. Users can then retrieve this ID by
 * calling the {@link #getId()} method if they need to identify their entry by
 * ID, eg. for arrows and row data.
 *
 * @author Geneviève Bastien
 */
public class ScriptedEntryDataModel extends TimeGraphEntryModel {

    private static final AtomicLong sfId = new AtomicLong();
    private final int fQuark;

    /**
     * Constructor
     *
     * @param name
     *            Name of the entry model
     * @param parentId
     *            The ID of the parent entry, or negative for no parent
     * @param quark
     *            The quark in the state system containing the data to display
     */
    public ScriptedEntryDataModel(String name, long parentId, int quark) {
        super(sfId.getAndIncrement(), parentId, name, Long.MIN_VALUE, Long.MAX_VALUE);
        fQuark = quark;
    }

    /**
     * Get the quark in the state system containing the data to display
     *
     * @return The quark to display
     */
    public int getQuark() {
        return fQuark;
    }

}
