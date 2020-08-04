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

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * A tree model wrapper to serialize the headers as expected by the trace server
 * protocol
 *
 * @author Geneviève Bastien
 */
public class TreeModelWrapper {

    private final TmfTreeModel<@NonNull ITmfTreeDataModel> fModel;
    private final @NonNull List<@NonNull TreeColumnHeader> fHeaders;

    /**
     * Tree column header class
     */
    public static class TreeColumnHeader {
        private final String fName;

        /**
         * Constructor with only the name
         *
         * @param name The name of the column
         */
        public TreeColumnHeader(String name) {
            fName =name;
        }

        /**
         * Get the name of the column
         *
         * @return The name of the column
         */
        public String getName() {
            return fName;
        }
    }

    /**
     * @param model
     *            The model to wrap
     */
    public TreeModelWrapper(TmfTreeModel<@NonNull ITmfTreeDataModel> model) {
        fModel = model;
        List<@NonNull String> headers = model.getHeaders();
        Builder<@NonNull TreeColumnHeader> builder = ImmutableList.builder();
        headers.forEach(header -> builder.add(new TreeColumnHeader(header)));
        fHeaders = builder.build();
    }

    /**
     * Get the TSP-ready headers for this model
     *
     * @return The headers
     */
    public @NonNull List<@NonNull TreeColumnHeader> getHeaders() {
        return fHeaders;
    }

    /**
     * Wrapper to the {@link TmfTreeModel#getEntries()} method
     *
     * @return The entries
     */
    public @NonNull List<@NonNull ITmfTreeDataModel> getEntries() {
        return fModel.getEntries();
    }

}
