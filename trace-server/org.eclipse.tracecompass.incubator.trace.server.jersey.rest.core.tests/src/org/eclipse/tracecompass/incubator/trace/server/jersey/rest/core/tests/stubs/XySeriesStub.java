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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the xy series elements. It matches the trace server
 * protocol's <code>XYSeries</code> schema
 *
 * @author Geneviève Bastien
 */
public class XySeriesStub implements Serializable {

    private static final long serialVersionUID = 3428838268294534414L;

    private final String fName;
    private final int fId;
    private final List<Long> fXValues;
    private final List<Double> fYValues;
    private final @Nullable OutputElementStyleStub fStyle;
    private final XyAxisStub fXAxis;
    private final XyAxisStub fYAxis;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            The name of this series
     * @param id
     *            The unique ID of the series
     * @param xValues
     *            The values for the x axis of this series
     * @param yValues
     *            The values for the y axis of this series
     * @param xAxis
     *            The x axis
     * @param yAxis
     *            The y axis
     * @param style
     *            The style for this series
     */
    @JsonCreator
    public XySeriesStub(@JsonProperty("seriesName") String name,
            @JsonProperty("seriesId") Integer id,
            @JsonProperty("xValues") List<Long> xValues,
            @JsonProperty("yValues") List<Double> yValues,
            @JsonProperty("xAxis") XyAxisStub xAxis,
            @JsonProperty("yAxis") XyAxisStub yAxis,
            @JsonProperty("style") OutputElementStyleStub style) {
        fName = Objects.requireNonNull(name, "The 'seriesName' json field was not set");
        fId = Objects.requireNonNull(id, "The 'seriesId' json field was not set");
        fXValues = Objects.requireNonNull(xValues, "The 'xValues' json field was not set");
        fYValues = Objects.requireNonNull(yValues, "The 'yValues' json field was not set");
        fStyle = style;
        fXAxis = xAxis;
        fYAxis = yAxis;
    }

    /**
     * Get the name of this entry
     *
     * @return The name of the entry
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the ID of this entry
     *
     * @return The ID of the entry
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the x values of the series
     *
     * @return The values on the x axis
     */
    public List<Long> getXValues() {
        return fXValues;
    }

    /**
     * Get the y values of the series
     *
     * @return The values on the y axis
     */
    public List<Double> getYValues() {
        return fYValues;
    }

    /**
     * Get the style for this series
     *
     * @return The style
     */
    public @Nullable OutputElementStyleStub getStyle() {
        return fStyle;
    }

    /**
     * Get the X axis description for this series, if provided
     *
     * @return The x axis
     */
    public @Nullable XyAxisStub getXAxis() {
        return fXAxis;
    }

    /**
     * Get the X axis description for this series, if provided
     *
     * @return The y axis
     */
    public @Nullable XyAxisStub getYAxis() {
        return fYAxis;
    }

}
