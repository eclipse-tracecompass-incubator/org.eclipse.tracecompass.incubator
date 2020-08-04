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
 * A stub class for the response to a tree request for xy charts and data trees.
 * It contains the generic response, as well as an {@link XyModelStub}
 *
 * @author Geneviève Bastien
 */
public class XyOutputResponseStub extends OutputResponseStub {

    private static final long serialVersionUID = -2273261726401144959L;

    private final XyModelStub fModel;

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
    public XyOutputResponseStub(@JsonProperty("model") XyModelStub model,
            @JsonProperty("status") String status,
            @JsonProperty("statusMessage") String statusMessage) {
        super(status, statusMessage);
        fModel = Objects.requireNonNull(model, "The 'model' json field was not set");
    }

    /**
     * Get the model for this response
     *
     * @return The model for the response
     */
    public XyModelStub getModel() {
        return fModel;
    }

}
