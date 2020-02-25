/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.event;

import java.text.Format;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.format.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;

import com.google.common.collect.ImmutableList;

/**
 * Aspects for Trace Compass Logs
 *
 * @author Katherine Nadeau
 */
public class OpenTracingAspects {

    /**
     * Apects of a trace
     */
    private static @Nullable Iterable<@NonNull ITmfEventAspect<?>> aspects;

    /**
     * Get the event aspects
     *
     * @return get the event aspects
     */
    public static Iterable<@NonNull ITmfEventAspect<?>> getAspects() {
        Iterable<@NonNull ITmfEventAspect<?>> aspectSet = aspects;
        if (aspectSet == null) {
            aspectSet = ImmutableList.of(
                    new OpenTracingLabelAspect(),
                    TmfBaseAspects.getTimestampAspect(),
                    new OpenTracingDurationAspect(),
                    new OpenTracingSpanIdAspect(),
                    new OpenTracingProcessAspect(),
                    new OpenTracingProcessTagsAspect(),
                    new OpenTracingTagsAspect());
            aspects = aspectSet;
        }
        return aspectSet;
    }

    private static class OpenTracingLabelAspect implements IOpenTracingAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.OpenTracingAspects_Name);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_NameD);
        }

        @Override
        public String resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            return event.getName();
        }
    }

    private static class OpenTracingProcessAspect implements IOpenTracingAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.OpenTracingAspects_Process);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_ProcessD);
        }

        @Override
        public String resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            Object field = event.getField().getProcessName();
            return String.valueOf(field);
        }
    }

    private static class OpenTracingDurationAspect implements IOpenTracingAspect<@Nullable String> {
        private static final Format FORMATTER = SubSecondTimeWithUnitFormat.getInstance();

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.OpenTracingAspects_Duration);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_DurationD);
        }

        @Override
        public @Nullable String resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            return FORMATTER.format(event.getField().getDuration());
        }
    }

    private static class OpenTracingSpanIdAspect implements IOpenTracingAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.OpenTracingAspects_SpanId);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_SpanIdD);
        }

        @Override
        public String resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            return event.getField().getSpanId();
        }
    }

    private static class OpenTracingProcessTagsAspect implements IOpenTracingAspect<Map<String, Object>> {

        @Override
        public String getName() {
            return String.valueOf(Messages.OpenTracingAspects_ProcessTags);
        }

        @Override
        public String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_ProcessTagsD);
        }

        @Override
        public Map<String, Object> resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            Map<String, Object> processTags = event.getField().getProcessTags();
            return processTags == null ? Collections.emptyMap() : processTags;
        }
    }

    private static class OpenTracingTagsAspect implements IOpenTracingAspect<Map<String, Object>> {

        @Override
        public String getName() {
            return String.valueOf(Messages.OpenTracingAspects_Tags);
        }

        @Override
        public String getHelpText() {
            return String.valueOf(Messages.OpenTracingAspects_TagsD);
        }

        @Override
        public Map<String, Object> resolveOpenTracingLogs(@NonNull OpenTracingEvent event) {
            Map<String, Object> tags = event.getField().getTags();
            return tags == null ? Collections.emptyMap() : tags;
        }
    }
}
