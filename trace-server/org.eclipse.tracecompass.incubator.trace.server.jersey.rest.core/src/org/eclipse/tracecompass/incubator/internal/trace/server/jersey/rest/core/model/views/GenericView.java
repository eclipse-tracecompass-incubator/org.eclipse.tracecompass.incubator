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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Object to encapsulate the values returned by a data provider query
 *
 * @param <R> Type of model to return
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@XmlRootElement
public class GenericView<R> {
    private final ITmfTrace fTrace;
    private final TmfModelResponse<R> fResponse;

    /**
     * Compose the {@link ITmfTrace} and response in an
     * {@link GenericView}
     *
     * @param trace
     *            the queried trace
     * @param response
     *            XY model response for the query
     */
    public GenericView(@Nullable ITmfTrace trace, TmfModelResponse<R> response) {
        fTrace = trace;
        fResponse = response;
    }

    /**
     * Getter for the trace
     *
     * @return the trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Getter for the ModelResponse in this XY view
     *
     * @return the time values
     */
    @XmlElement
    public TmfModelResponse<R> getResponse() {
        return fResponse;
    }
}
