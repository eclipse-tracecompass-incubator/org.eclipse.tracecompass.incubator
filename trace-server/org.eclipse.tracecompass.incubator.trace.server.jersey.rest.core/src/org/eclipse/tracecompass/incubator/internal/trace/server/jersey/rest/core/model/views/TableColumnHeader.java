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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;

/**
 * An object that describes a table column header for table column queries
 *
 * @author Geneviève Bastien
 */
public class TableColumnHeader {

    private final long fId;
    private final String fName;
    private final @Nullable String fDescription;
    private final @Nullable String fType;

    /**
     * Constructor
     *
     * @param dataModel
     *            The tree model that serves as base for this column
     */
    public TableColumnHeader(ITmfTreeDataModel dataModel) {
        fId = dataModel.getId();
        List<@NonNull String> labels = dataModel.getLabels();
        fName = dataModel.getLabels().get(0);
        fDescription = labels.size() >= 2 ? dataModel.getLabels().get(1) : null;
        fType = null;
    }

    /**
     * Get the ID of this column header
     *
     * @return The ID of the column header
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
    public String getType() {
        return fType;
    }

}
