/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.ITableColumnDescriptor;

/**
 * An object that describes a table column header for table column queries
 *
 * @author Geneviève Bastien
 */
public class TableColumnHeader {

    private final long fId;
    private final String fName;
    private final @Nullable String fDescription;
    private final DataType fType;

    /**
     * Constructor
     *
     * @param descriptor
     *            the column descriptor
     */
    public TableColumnHeader(ITableColumnDescriptor descriptor) {
        fId = descriptor.getId();
        fName = descriptor.getText();
        fDescription = descriptor.getTooltip();
        fType = descriptor.getDataType();
    }

    /**
     * Get the ID of this column
     *
     * @return The ID of the column
     */
    public long getId() {
        return fId;
    }

    /**
     * Get the name of this column
     *
     * @return The name of the column
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the description of the column
     *
     * @return The description of the column
     */
    public String getDescription() {
        return fDescription;
    }

    /**
     * Get the type of the column
     *
     * @return The type of the column
     */
    public DataType getType() {
        return fType;
    }

}
