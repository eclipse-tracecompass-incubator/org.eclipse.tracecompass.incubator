/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Object to encapsulate the values returned by a query for a Tree view
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@XmlRootElement
public class TreeView {
    private final ITmfTrace fTrace;
    private final TmfModelResponse<@NonNull List<@NonNull TmfTreeDataModel>> fResponse;

    /**
     * Compose the {@link ITmfTrace} and list of {@link TmfTreeDataModel} in an
     * {@link TreeView}
     *
     * @param trace
     *            queried trace
     * @param response
     *            Flattened tree model response for the query
     */
    public TreeView(@Nullable ITmfTrace trace, TmfModelResponse<@NonNull List<@NonNull TmfTreeDataModel>> response) {
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
     * Getter for the encapsulated tree response
     *
     * @return the flattened tree model.
     */
    @XmlElement
    public TmfModelResponse<@NonNull List<@NonNull TmfTreeDataModel>> getResponse() {
        return fResponse;
    }
}
