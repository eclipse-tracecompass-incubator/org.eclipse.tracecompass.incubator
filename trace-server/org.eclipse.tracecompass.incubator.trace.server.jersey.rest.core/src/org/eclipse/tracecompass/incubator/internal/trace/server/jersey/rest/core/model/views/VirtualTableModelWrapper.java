/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.List;

import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.ITmfVirtualTableModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;

/**
 * A non-generic class to wrap the generic {@link ITmfVirtualTableModel}, for
 * easier serialization
 *
 * @author Geneviève Bastien
 */
public class VirtualTableModelWrapper {

    private final ITmfVirtualTableModel fWrapper;

    /**
     * Constructor
     *
     * @param model
     *            The model to wrap
     */
    public VirtualTableModelWrapper(ITmfVirtualTableModel model) {
        fWrapper = model;
    }

    /**
     * Gets columns IDs
     *
     * @return The list of column IDs in order that they are sorted
     */
    public List<Long> getColumnIds() {
        return fWrapper.getColumnIds();
    }

    /**
     * Gets the data associated with the model. The list represent the lines for
     * this table model. The data in a {@link IVirtualTableLine} are in the same
     * order as the column IDs order
     *
     * @return The list of lines
     */
    public List<IVirtualTableLine> getLines() {
        return fWrapper.getLines();
    }

    /**
     * Gets the index of the first table entry in the model
     *
     * @return The top index
     */
    public long getLowIndex() {
        return fWrapper.getIndex();
    }

    /**
     * Gets the number of table entries that matches a filter. If there was no
     * filter applied, it will return simply the the total number of table
     * entries.
     *
     * @return The total number of table entries that matches a filter
     */
    public long getSize() {
        return fWrapper.getSize();
    }

}
