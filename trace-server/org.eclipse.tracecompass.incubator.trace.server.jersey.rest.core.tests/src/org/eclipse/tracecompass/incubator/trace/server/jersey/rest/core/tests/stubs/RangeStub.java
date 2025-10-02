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
import org.eclipse.tracecompass.tmf.core.model.ISampling.Range;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stub version of {@link Range}.
 *
 * @author Siwei Zhang
 */
public final class RangeStub implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long fStart;
    private final long fEnd;

    /**
     * Constructor
     *
     * @param start
     *            the start value
     * @param end
     *            the end value
     */
    @JsonCreator
    public RangeStub(@JsonProperty("start") Long start, @JsonProperty("end") Long end) {
        this.fStart = start;
        this.fEnd = end;
    }

    /**
     * @return the start value
     */
    public long getStart() {
        return fStart;
    }

    /**
     * @return the end value
     */
    public long getEnd() {
        return fEnd;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return (this == obj) || (obj instanceof RangeStub other &&
                fStart == other.fStart && fEnd == other.fEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fStart, fEnd);
    }

    @Override
    public String toString() {
        return "[" + fStart + ", " + fEnd + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
