/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal
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
    private final ISamplingStub fSampling;
    private final List<Double> fYValues;
    private final OutputElementStyleStub fStyle;
    private final TmfXYAxisDescriptionStub fXAxisDescription;
    private final TmfXYAxisDescriptionStub fYAxisDescription;

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
     * @param style
     *            The style for this series
     * @param xAxisDescription
     *            The description for x axis
     * @param yAxisDescription
     *            The description for y axis
     */
    @JsonCreator
    public XySeriesStub(@JsonProperty("seriesName") String name,
            @JsonProperty("seriesId") Integer id,
            @JsonProperty("xValues") ISamplingStub xValues,
            @JsonProperty("yValues") List<Double> yValues,
            @JsonProperty("style") OutputElementStyleStub style,
            @JsonProperty("xValuesDescription") TmfXYAxisDescriptionStub xAxisDescription,
            @JsonProperty("yValuesDescription") TmfXYAxisDescriptionStub yAxisDescription) {
        fName = Objects.requireNonNull(name, "The 'seriesName' json field was not set");
        fId = Objects.requireNonNull(id, "The 'seriesId' json field was not set");
        fSampling = Objects.requireNonNull(xValues, "The 'xValues' json field was not set");
        fYValues = Objects.requireNonNull(yValues, "The 'yValues' json field was not set");
        fStyle = Objects.requireNonNull(style, "The 'style' json field was not set");
        fXAxisDescription = xAxisDescription;
        fYAxisDescription = yAxisDescription;
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
    public ISamplingStub getXValues() {
        return fSampling;
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
    public OutputElementStyleStub getStyle() {
        return fStyle;
    }

    /**
     * Get the description for x axis
     *
     * @return The description for x axis
     */
    public TmfXYAxisDescriptionStub getXAxisDescription() {
        return fXAxisDescription;
    }

    /**
     * Get the description for y axis
     *
     * @return The description for y axis
     */
    public TmfXYAxisDescriptionStub getYAxisDescription() {
        return fYAxisDescription;
    }
}
