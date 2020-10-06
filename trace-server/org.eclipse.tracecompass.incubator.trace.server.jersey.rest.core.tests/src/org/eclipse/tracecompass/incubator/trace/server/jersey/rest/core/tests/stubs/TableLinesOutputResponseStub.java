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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stub class for the response to a table columns' request. It contains
 * the generic response, as well as a {@link TableModelStub}
 *
 * @author Geneviève Bastien
 */
public class TableLinesOutputResponseStub extends OutputResponseStub {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = 406541774243541075L;

    private final TableModelStub fModel;

    /**
     * {@link JsonCreator} Constructor from json
     *
     * @param model
     *            The model for this response
     * @param status
     *            The status of the response
     * @param statusMessage
     *            The custom status message of the response
     */
    public TableLinesOutputResponseStub(@JsonProperty("model") TableModelStub model,
            @JsonProperty("status") String status,
            @JsonProperty("statusMessage") String statusMessage) {
        super(status, statusMessage);
        fModel = Objects.requireNonNull(model, "Model object has not been set");
    }

    /**
     * Get the model for this response
     *
     * @return The model for the response
     */
    public TableModelStub getModel() {
        return fModel;
    }

}
