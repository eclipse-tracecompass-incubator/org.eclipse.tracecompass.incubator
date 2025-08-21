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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A stub interface for AxisDomain which can be either Categorical or TimeRange.
 * Matches the trace server protocol schema for <code>AxisDomain</code>.
 *
 * @author Siwei Zhang
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IAxisDomainStub.CategoricalStub.class, name = "categorical"),
    @JsonSubTypes.Type(value = IAxisDomainStub.RangeStub.class, name = "range")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface IAxisDomainStub extends Serializable permits
        IAxisDomainStub.CategoricalStub,
        IAxisDomainStub.RangeStub {

    /**
     * Stub for AxisDomain.Categorical
     */
    final class CategoricalStub implements IAxisDomainStub {

        private static final long serialVersionUID = 2L;

        private final List<String> fCategories;

        /**
         * Constructor
         *
         * @param categories
         *            the set of category labels for the axis domain
         */
        @JsonCreator
        public CategoricalStub(@JsonProperty("categories") List<String> categories) {
            fCategories = categories == null ? Collections.emptyList() : categories;
        }

        /**
         * Get the categories for this categorical axis domain.
         *
         * @return the set of category labels
         */
        public List<String> getCategories() {
            return fCategories;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof CategoricalStub other &&
                   Objects.equals(fCategories, other.fCategories);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fCategories);
        }

        @Override
        public String toString() {
            return "CategoricalStub{" + "categories=" + fCategories + '}';
        }
    }

    /**
     * Stub for AxisDomain.Range
     */
    final class RangeStub implements IAxisDomainStub {

        private static final long serialVersionUID = 3L;

        private final long fStart;
        private final long fEnd;

        /**
         * Constructor
         *
         * @param start
         *            start of the time range
         * @param end
         *            end of the time range
         */
        @JsonCreator
        public RangeStub(@JsonProperty("start") long start,
                         @JsonProperty("end") long end) {
            fStart = start;
            fEnd = end;
        }

        /**
         * Get the start of range.
         *
         * @return the start
         */
        public long getStart() {
            return fStart;
        }

        /**
         * Get the end of range.
         *
         * @return the end
         */
        public long getEnd() {
            return fEnd;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof RangeStub other &&
                   fStart == other.fStart &&
                   fEnd == other.fEnd;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fStart, fEnd);
        }

        @Override
        public String toString() {
            return "RangeStub{" + "start=" + fStart + ", end=" + fEnd + '}';
        }
    }
}
