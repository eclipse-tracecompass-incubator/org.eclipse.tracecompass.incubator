/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otlp.core.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Native OTLP span data model that preserves all OTLP-specific fields beyond
 * what the OpenTracing/Jaeger model supports.
 *
 * @author Matthew Khouzam
 */
public class OtlpSpan {

    // OpenTracing-compatible fields
    private final @NonNull String fTraceId;
    private final @NonNull String fSpanId;
    private final @Nullable String fParentSpanId;
    private final @NonNull String fOperationName;
    private final long fStartTimeNanos;
    private final long fDurationNanos;
    private final @NonNull String fServiceName;

    // OTLP-specific fields
    private final @NonNull OtlpSpanKind fKind;
    private final @NonNull OtlpSpanStatus fStatus;
    private final @NonNull List<@NonNull OtlpSpanEvent> fEvents;
    private final @NonNull List<@NonNull OtlpSpanLink> fLinks;
    private final @Nullable String fInstrumentationScopeName;
    private final @Nullable String fInstrumentationScopeVersion;
    private final @NonNull Map<@NonNull String, @NonNull String> fAttributes;
    private final @NonNull Map<@NonNull String, @NonNull String> fResourceAttributes;
    private final int fDroppedAttributesCount;
    private final int fDroppedEventsCount;
    private final int fDroppedLinksCount;

    /**
     * Constructor
     *
     * @param builder
     *            the builder to construct this span from
     */
    private OtlpSpan(Builder builder) {
        fTraceId = builder.fTraceId;
        fSpanId = builder.fSpanId;
        fParentSpanId = builder.fParentSpanId;
        fOperationName = builder.fOperationName;
        fStartTimeNanos = builder.fStartTimeNanos;
        fDurationNanos = builder.fDurationNanos;
        fServiceName = builder.fServiceName;
        fKind = builder.fKind;
        fStatus = builder.fStatus;
        fEvents = Collections.unmodifiableList(builder.fEvents);
        fLinks = Collections.unmodifiableList(builder.fLinks);
        fInstrumentationScopeName = builder.fInstrumentationScopeName;
        fInstrumentationScopeVersion = builder.fInstrumentationScopeVersion;
        fAttributes = Collections.unmodifiableMap(builder.fAttributes);
        fResourceAttributes = Collections.unmodifiableMap(builder.fResourceAttributes);
        fDroppedAttributesCount = builder.fDroppedAttributesCount;
        fDroppedEventsCount = builder.fDroppedEventsCount;
        fDroppedLinksCount = builder.fDroppedLinksCount;
    }

    /**
     * Get the trace ID
     *
     * @return the trace ID
     */
    public @NonNull String getTraceId() {
        return fTraceId;
    }

    /**
     * Get the span ID
     *
     * @return the span ID
     */
    public @NonNull String getSpanId() {
        return fSpanId;
    }

    /**
     * Get the parent span ID
     *
     * @return the parent span ID, or null if this is a root span
     */
    public @Nullable String getParentSpanId() {
        return fParentSpanId;
    }

    /**
     * Get the operation name
     *
     * @return the operation name
     */
    public @NonNull String getOperationName() {
        return fOperationName;
    }

    /**
     * Get the start time in nanoseconds since Unix epoch
     *
     * @return the start time in nanoseconds
     */
    public long getStartTimeNanos() {
        return fStartTimeNanos;
    }

    /**
     * Get the duration in nanoseconds
     *
     * @return the duration in nanoseconds
     */
    public long getDurationNanos() {
        return fDurationNanos;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    public @NonNull String getServiceName() {
        return fServiceName;
    }

    /**
     * Get the span kind
     *
     * @return the span kind
     */
    public @NonNull OtlpSpanKind getKind() {
        return fKind;
    }

    /**
     * Get the span status
     *
     * @return the span status
     */
    public @NonNull OtlpSpanStatus getStatus() {
        return fStatus;
    }

    /**
     * Get the span events (timestamped logs)
     *
     * @return an unmodifiable list of span events
     */
    public @NonNull List<@NonNull OtlpSpanEvent> getEvents() {
        return fEvents;
    }

    /**
     * Get the span links
     *
     * @return an unmodifiable list of span links
     */
    public @NonNull List<@NonNull OtlpSpanLink> getLinks() {
        return fLinks;
    }

    /**
     * Get the instrumentation scope name
     *
     * @return the scope name, or null if not set
     */
    public @Nullable String getInstrumentationScopeName() {
        return fInstrumentationScopeName;
    }

    /**
     * Get the instrumentation scope version
     *
     * @return the scope version, or null if not set
     */
    public @Nullable String getInstrumentationScopeVersion() {
        return fInstrumentationScopeVersion;
    }

    /**
     * Get the span attributes
     *
     * @return an unmodifiable map of attributes
     */
    public @NonNull Map<@NonNull String, @NonNull String> getAttributes() {
        return fAttributes;
    }

    /**
     * Get the resource attributes
     *
     * @return an unmodifiable map of resource attributes
     */
    public @NonNull Map<@NonNull String, @NonNull String> getResourceAttributes() {
        return fResourceAttributes;
    }

    /**
     * Get the number of dropped attributes
     *
     * @return the dropped attributes count
     */
    public int getDroppedAttributesCount() {
        return fDroppedAttributesCount;
    }

    /**
     * Get the number of dropped events
     *
     * @return the dropped events count
     */
    public int getDroppedEventsCount() {
        return fDroppedEventsCount;
    }

    /**
     * Get the number of dropped links
     *
     * @return the dropped links count
     */
    public int getDroppedLinksCount() {
        return fDroppedLinksCount;
    }

    /**
     * Builder for {@link OtlpSpan}
     */
    public static class Builder {
        private @NonNull String fTraceId = ""; //$NON-NLS-1$
        private @NonNull String fSpanId = ""; //$NON-NLS-1$
        private @Nullable String fParentSpanId;
        private @NonNull String fOperationName = ""; //$NON-NLS-1$
        private long fStartTimeNanos;
        private long fDurationNanos;
        private @NonNull String fServiceName = ""; //$NON-NLS-1$
        private @NonNull OtlpSpanKind fKind = OtlpSpanKind.INTERNAL;
        private @NonNull OtlpSpanStatus fStatus = new OtlpSpanStatus(OtlpSpanStatus.STATUS_CODE_UNSET, null);
        private @NonNull List<@NonNull OtlpSpanEvent> fEvents = Collections.emptyList();
        private @NonNull List<@NonNull OtlpSpanLink> fLinks = Collections.emptyList();
        private @Nullable String fInstrumentationScopeName;
        private @Nullable String fInstrumentationScopeVersion;
        private @NonNull Map<@NonNull String, @NonNull String> fAttributes = Collections.emptyMap();
        private @NonNull Map<@NonNull String, @NonNull String> fResourceAttributes = Collections.emptyMap();
        private int fDroppedAttributesCount;
        private int fDroppedEventsCount;
        private int fDroppedLinksCount;

        /**
         * Set the trace ID
         *
         * @param traceId
         *            the trace ID
         * @return this builder
         */
        public Builder traceId(@NonNull String traceId) {
            fTraceId = traceId;
            return this;
        }

        /**
         * Set the span ID
         *
         * @param spanId
         *            the span ID
         * @return this builder
         */
        public Builder spanId(@NonNull String spanId) {
            fSpanId = spanId;
            return this;
        }

        /**
         * Set the parent span ID
         *
         * @param parentSpanId
         *            the parent span ID
         * @return this builder
         */
        public Builder parentSpanId(@Nullable String parentSpanId) {
            fParentSpanId = parentSpanId;
            return this;
        }

        /**
         * Set the operation name
         *
         * @param operationName
         *            the operation name
         * @return this builder
         */
        public Builder operationName(@NonNull String operationName) {
            fOperationName = operationName;
            return this;
        }

        /**
         * Set the start time in nanoseconds
         *
         * @param startTimeNanos
         *            the start time in nanoseconds since Unix epoch
         * @return this builder
         */
        public Builder startTimeNanos(long startTimeNanos) {
            fStartTimeNanos = startTimeNanos;
            return this;
        }

        /**
         * Set the duration in nanoseconds
         *
         * @param durationNanos
         *            the duration in nanoseconds
         * @return this builder
         */
        public Builder durationNanos(long durationNanos) {
            fDurationNanos = durationNanos;
            return this;
        }

        /**
         * Set the service name
         *
         * @param serviceName
         *            the service name
         * @return this builder
         */
        public Builder serviceName(@NonNull String serviceName) {
            fServiceName = serviceName;
            return this;
        }

        /**
         * Set the span kind
         *
         * @param kind
         *            the span kind
         * @return this builder
         */
        public Builder kind(@NonNull OtlpSpanKind kind) {
            fKind = kind;
            return this;
        }

        /**
         * Set the span status
         *
         * @param status
         *            the span status
         * @return this builder
         */
        public Builder status(@NonNull OtlpSpanStatus status) {
            fStatus = status;
            return this;
        }

        /**
         * Set the span events
         *
         * @param events
         *            the span events
         * @return this builder
         */
        public Builder events(@NonNull List<@NonNull OtlpSpanEvent> events) {
            fEvents = events;
            return this;
        }

        /**
         * Set the span links
         *
         * @param links
         *            the span links
         * @return this builder
         */
        public Builder links(@NonNull List<@NonNull OtlpSpanLink> links) {
            fLinks = links;
            return this;
        }

        /**
         * Set the instrumentation scope name
         *
         * @param name
         *            the scope name
         * @return this builder
         */
        public Builder instrumentationScopeName(@Nullable String name) {
            fInstrumentationScopeName = name;
            return this;
        }

        /**
         * Set the instrumentation scope version
         *
         * @param version
         *            the scope version
         * @return this builder
         */
        public Builder instrumentationScopeVersion(@Nullable String version) {
            fInstrumentationScopeVersion = version;
            return this;
        }

        /**
         * Set the span attributes
         *
         * @param attributes
         *            the attributes map
         * @return this builder
         */
        public Builder attributes(@NonNull Map<@NonNull String, @NonNull String> attributes) {
            fAttributes = attributes;
            return this;
        }

        /**
         * Set the resource attributes
         *
         * @param resourceAttributes
         *            the resource attributes map
         * @return this builder
         */
        public Builder resourceAttributes(@NonNull Map<@NonNull String, @NonNull String> resourceAttributes) {
            fResourceAttributes = resourceAttributes;
            return this;
        }

        /**
         * Set the dropped attributes count
         *
         * @param count
         *            the dropped attributes count
         * @return this builder
         */
        public Builder droppedAttributesCount(int count) {
            fDroppedAttributesCount = count;
            return this;
        }

        /**
         * Set the dropped events count
         *
         * @param count
         *            the dropped events count
         * @return this builder
         */
        public Builder droppedEventsCount(int count) {
            fDroppedEventsCount = count;
            return this;
        }

        /**
         * Set the dropped links count
         *
         * @param count
         *            the dropped links count
         * @return this builder
         */
        public Builder droppedLinksCount(int count) {
            fDroppedLinksCount = count;
            return this;
        }

        /**
         * Build the OtlpSpan
         *
         * @return the constructed OtlpSpan
         */
        public OtlpSpan build() {
            return new OtlpSpan(this);
        }
    }
}
