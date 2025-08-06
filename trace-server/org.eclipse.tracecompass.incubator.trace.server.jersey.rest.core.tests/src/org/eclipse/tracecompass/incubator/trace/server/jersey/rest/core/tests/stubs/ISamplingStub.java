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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.ISampling;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Stub version of {@link ISampling}.
 *
 * @author Siwei Zhang
 */
@JsonSerialize(using = SamplingStubSerializer.class)
@JsonDeserialize(using = SamplingStubDeserializer.class)
public sealed interface ISamplingStub extends Serializable permits
        ISamplingStub.TimestampsStub,
        ISamplingStub.CategoriesStub,
        ISamplingStub.RangesStub {

    /**
     * Get the number of sampling points
     *
     * @return number of points
     */
    int size();

    /**
     * Timestamp-based sampling.
     */
    final class TimestampsStub implements ISamplingStub {
        private static final long serialVersionUID = -8242136490356720296L;

        private final long[] fTimestamps;

        public TimestampsStub(long[] timestamps) {
            this.fTimestamps = Objects.requireNonNull(timestamps);
        }

        public long[] getTimestamps() {
            return fTimestamps;
        }

        @Override
        public int size() {
            return fTimestamps.length;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof TimestampsStub other &&
                    Arrays.equals(this.fTimestamps, other.fTimestamps));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(fTimestamps);
        }

        @Override
        public String toString() {
            return "Timestamps" + Arrays.toString(fTimestamps); //$NON-NLS-1$
        }
    }

    /**
     * Categorical sampling (e.g., names).
     */
    final class CategoriesStub implements ISamplingStub {
        private static final long serialVersionUID = 3751152643508688051L;

        private final List<String> fCategories;

        public CategoriesStub(List<String> categories) {
            this.fCategories = Objects.requireNonNull(categories);
        }

        public List<String> getCategories() {
            return fCategories;
        }

        @Override
        public int size() {
            return fCategories.size();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof CategoriesStub other &&
                    Objects.equals(this.fCategories, other.fCategories));
        }

        @Override
        public int hashCode() {
            return Objects.hash(fCategories);
        }

        @Override
        public String toString() {
            return "Categories" + fCategories.toString(); //$NON-NLS-1$
        }
    }

    /**
     * Range sampling, representing start-end pairs.
     */
    final class RangesStub implements ISamplingStub {
        private static final long serialVersionUID = 3434126540189939098L;

        private final List<RangeStub> fRanges;

        public RangesStub(List<RangeStub> ranges) {
            this.fRanges = Objects.requireNonNull(ranges);
        }

        public List<RangeStub> getRanges() {
            return fRanges;
        }

        @Override
        public int size() {
            return fRanges.size();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return (this == obj) || (obj instanceof RangesStub other &&
                    Objects.equals(this.fRanges, other.fRanges));
        }

        @Override
        public int hashCode() {
            return Objects.hash(fRanges);
        }

        @Override
        public String toString() {
            return "Ranges" + fRanges.toString(); //$NON-NLS-1$
        }

        /**
         * Stub representing a range.
         */
        public static final class RangeStub implements Serializable {
            private static final long serialVersionUID = 1L;

            private final long fStart;
            private final long fEnd;

            public RangeStub(long start, long end) {
                this.fStart = start;
                this.fEnd = end;
            }

            public long getStart() {
                return fStart;
            }

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
    }
}
