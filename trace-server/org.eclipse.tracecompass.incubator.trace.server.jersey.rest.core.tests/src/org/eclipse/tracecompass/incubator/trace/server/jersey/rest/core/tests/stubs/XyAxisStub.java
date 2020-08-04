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

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * A Stub class for the xy series elements. It matches the trace server
 * protocol's <code>XYSeries</code> schema
 *
 * @author Geneviève Bastien
 */
public class XyAxisStub implements Serializable {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = -6936976499776716440L;
    private static final List<String> DATA_TYPES = ImmutableList.of("NUMBER", "BINARY_NUMBER", "TIMESTAMP", "DURATION", "STRING");
    private final String fLabel;
    private final String fUnit;
    private final String fDataType;

    /**
     * Constructor
     *
     * @param label
     *            The label for this axis
     * @param unit
     *            The unit
     * @param dataType
     *            The data type
     */
    @JsonCreator
    public XyAxisStub(@JsonProperty("label") String label,
            @JsonProperty("unit") String unit,
            @JsonProperty("dataType") String dataType) {
        fLabel = label;
        fUnit = unit;
        fDataType = dataType;
        if (dataType != null) {
            assertTrue(DATA_TYPES.contains(dataType));
        }
    }

    /**
     * Get the label for this axis
     *
     * @return The label for the axis
     */
    public String getLabel() {
        return fLabel;
    }

    /**
     * Get the unit for this axis
     *
     * @return The unit
     */
    public String getUnit() {
        return fUnit;
    }

    /**
     * Get the data type for this axis
     *
     * @return The data type
     */
    public String getDataType() {
        return fDataType;
    }

}
