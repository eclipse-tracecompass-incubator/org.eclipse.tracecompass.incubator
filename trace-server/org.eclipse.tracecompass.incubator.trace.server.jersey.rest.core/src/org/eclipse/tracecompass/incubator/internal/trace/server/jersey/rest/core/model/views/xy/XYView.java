/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.xy;

import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.analysis.AnalysisModel;

/**
 * Object to encapsulate the values returned by a query for an XY view
 *
 * @author Loic Prieur-Drevon
 */
@XmlRootElement
public class XYView {
    private final AnalysisModel fModel;
    private final long[] fTimes;
    private final Map<String, long[]> fValues;

    /**
     * Parameterized Constructor
     *
     * @param model
     *            the Analysis model that this XYView represents
     * @param xSeries
     *            the series for the x axis
     * @param yValues
     *            Map of y series
     */
    public XYView(AnalysisModel model, long[] xSeries, Map<String, long[]> yValues) {
        fModel = model;
        fTimes = xSeries;
        fValues = yValues;
    }

    /**
     * Getter for the analysis model
     *
     * @return the analysis model
     */
    public AnalysisModel getModel() {
        return fModel;
    }

    /**
     * Getter for the time values in this XY view
     *
     * @return the time values
     */
    @XmlElement
    public long[] getTimes() {
        return fTimes;
    }

    /**
     * Getter for the Y values in this XY view
     *
     * @return a map of series name to values
     */
    @XmlElement
    public Map<String, long[]> getValues() {
        return fValues;
    }
}
