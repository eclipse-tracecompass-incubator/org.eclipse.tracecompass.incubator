/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stub class for the XY axis description model. It matches the trace server
 * protocol's <code>TmfXYAxisDescription</code> schema.
 *
 * @author Siwei Zhang
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmfXYAxisDescriptionStub implements Serializable {

    private static final long serialVersionUID = 7302486196351034579L;

    private final String fLabel;
    private final String fUnit;
    private final String fDataType;
    private final @Nullable IAxisDomainStub fAxisDomain;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param label
     *            Label for the axis
     * @param unit
     *            Unit of the axis
     * @param dataType
     *            Type of the data (as a string)
     * @param axisDomain
     *            Optional domain for the axis
     */
    @JsonCreator
    public TmfXYAxisDescriptionStub(
            @JsonProperty("label") String label,
            @JsonProperty("unit") String unit,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("axisDomain") @Nullable IAxisDomainStub axisDomain) {
        fLabel = Objects.requireNonNull(label, "The 'label' json field was not set");
        fUnit = Objects.requireNonNull(unit, "The 'unit' json field was not set");
        fDataType = Objects.requireNonNull(dataType, "The 'dataType' json field was not set");
        fAxisDomain = axisDomain;
    }

    /**
     * Get the axis label
     *
     * @return the label
     */
    public String getLabel() {
        return fLabel;
    }

    /**
     * Get the unit of the axis
     *
     * @return the unit
     */
    public String getUnit() {
        return fUnit;
    }

    /**
     * Get the data type of the axis
     *
     * @return the data type
     */
    public String getDataType() {
        return fDataType;
    }

    /**
     * Get the axis domain
     *
     * @return the axis domain, if any
     */
    public @Nullable IAxisDomainStub getAxisDomain() {
        return fAxisDomain;
    }
}