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
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the entry model. It matches the trace server protocol's
 * <code>XYModel</code> schema
 *
 * @author Geneviève Bastien
 */
public class XyModelStub implements Serializable {

    private static final long serialVersionUID = 6027193074532379770L;

    private final Set<XySeriesStub> fSeries;
    private final String fTitle;
    private final @Nullable Boolean fCommonXAxis;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param series
     *            The set of series for this model
     * @param title
     *            The title of this model
     * @param commonXAxis
     *            Whether it uses a common X axis
     */
    @JsonCreator
    public XyModelStub(@JsonProperty("series") Set<XySeriesStub> series,
            @JsonProperty("title") String title,
            @JsonProperty("commonXAxis") Boolean commonXAxis) {
        fSeries = Objects.requireNonNull(series, "The 'series' json field was not set");
        fTitle = Objects.requireNonNull(title, "The 'title' json field was not set");
        fCommonXAxis = commonXAxis;
    }

    /**
     * Get the series described by this model
     *
     * @return The series in this model
     */
    public Set<XySeriesStub> getSeries() {
        return fSeries;
    }

    /**
     * Get the title that describe this model
     *
     * @return The title of this model
     */
    public String getTitle() {
        return fTitle;
    }

    /**
     * Get whether this model uses a common x axis
     *
     * @return Whether this model uses a common x axis
     */
    public @Nullable Boolean getCommonXAxis() {
        return fCommonXAxis;
    }

}
