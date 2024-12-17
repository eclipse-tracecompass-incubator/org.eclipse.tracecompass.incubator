/**********************************************************************
 * Copyright (c) 2024 Ericsson
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
 * A stub class for the response to a Marker Sets' request. It contains
 * the generic response, as well as an lsit of {@link MarkerSetStub}
 *
 * @author Bernd Hufmann
 */
public class MarkerSetsOutputResponseStub extends OutputResponseStub {

    private static final long serialVersionUID = -2103307674960234620L;
    private final List<MarkerSetStub> fModel;

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
    public MarkerSetsOutputResponseStub(@JsonProperty("model") List<MarkerSetStub> model,
            @JsonProperty("status") String status,
            @JsonProperty("statusMessage") String statusMessage) {
        super(status, statusMessage);
        fModel = model;
    }

    /**
     * Get the model for this response
     *
     * @return The model for the response
     */
    public List<MarkerSetStub> getModel() {
        return fModel;
    }

}
