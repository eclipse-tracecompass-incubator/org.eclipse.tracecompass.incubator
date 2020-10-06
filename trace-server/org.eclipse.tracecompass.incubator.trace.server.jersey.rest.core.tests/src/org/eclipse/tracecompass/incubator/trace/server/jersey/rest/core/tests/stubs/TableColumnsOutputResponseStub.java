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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stub class for the response to a table columns' request. It contains
 * the generic response, as well as a list of {@link ColumnHeaderEntryStub}
 *
 * @author Geneviève Bastien
 */
public class TableColumnsOutputResponseStub extends OutputResponseStub {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = -2740460350665524620L;
    private final List<ColumnHeaderEntryStub> fColumns;

    /**
     * {@link JsonCreator} Constructor from json
     *
     * @param columns
     *            The model for this response
     * @param status
     *            The status of the response
     * @param statusMessage
     *            The custom status message of the response
     */
    public TableColumnsOutputResponseStub(@JsonProperty("model") List<ColumnHeaderEntryStub> columns,
            @JsonProperty("status") String status,
            @JsonProperty("statusMessage") String statusMessage) {
        super(status, statusMessage);
        fColumns = columns;
    }

    /**
     * Get the model for this response
     *
     * @return The model for the response
     */
    public List<ColumnHeaderEntryStub> getModel() {
        return fColumns;
    }

}
