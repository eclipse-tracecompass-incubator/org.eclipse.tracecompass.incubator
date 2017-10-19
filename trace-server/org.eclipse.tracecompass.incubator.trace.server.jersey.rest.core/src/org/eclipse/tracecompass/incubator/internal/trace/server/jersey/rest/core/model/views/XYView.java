/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace.TraceModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfCommonXAxisModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;

/**
 * Object to encapsulate the values returned by a query for an XY view
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@XmlRootElement
public class XYView {
    private final TraceModel fModel;
    private final TmfModelResponse<@NonNull ITmfCommonXAxisModel> fResponse;

    /**
     * Compose the {@link TraceModel} and {@link ITmfCommonXAxisModel} in an
     * {@link XYView}
     *
     * @param traceModel
     *            trace model object for the queried trace
     * @param response
     *            XY model response for the query
     */
    public XYView(@Nullable TraceModel traceModel, TmfModelResponse<@NonNull ITmfCommonXAxisModel> response) {
        fModel = traceModel;
        fResponse = response;
    }

    /**
     * Getter for the trace model
     *
     * @return the trace model
     */
    public TraceModel getTrace() {
        return fModel;
    }

    /**
     * Getter for the time values in this XY view
     *
     * @return the time values
     */
    @XmlElement
    public TmfModelResponse<@NonNull ITmfCommonXAxisModel> getResponse() {
        return fResponse;
    }
}
