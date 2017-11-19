/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import java.util.Map;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;

import com.google.common.collect.ImmutableList;

/**
 * Aspects for Trace Compass Logs
 *
 * @author Matthew Khouzam
 */
public class TraceEventAspects {

    /**
     * An ID aspect, can be used to identify flows.
     */
    public static final TraceCompassScopeLogIdAspect ID_ASPECT = new TraceCompassScopeLogIdAspect();

    /**
     * Apects of a trace
     */
    private static Iterable<@NonNull ITmfEventAspect<?>> aspects;

    /**
     * Get the event aspects
     *
     * @return get the event aspects
     */
    @SuppressWarnings("null")
    public static @NonNull Iterable<@NonNull ITmfEventAspect<?>> getAspects() {
        Iterable<@NonNull ITmfEventAspect<?>> aspectSet = aspects;
        if (aspectSet == null) {
            aspectSet = ImmutableList.of(
                    new TraceCompassScopeLogLabelAspect(),
                    TmfBaseAspects.getTimestampAspect(),
                    new TraceCompassLogPhaseAspect(),
                    new TraceCompassScopeLogLevel(),
                    new TraceCompassScopeLogTidAspect(),
                    new TraceCompassScopeLogPidAspect(),
                    new TraceCompassScopeLogCategoryAspect(),
                    new TraceCompassScopeLogDurationAspect(),
                    ID_ASPECT,
                    new TraceCompassScopeLogArgsAspect(),
                    new TraceCompassScopeLogCallsiteAspect());
            aspects = aspectSet;
        }
        return aspectSet;
    }

    private static class TraceCompassLogPhaseAspect implements ITraceEventAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Phase);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_PhaseD);
        }

        @Override
        public @Nullable String resolveTCL(TraceEventEvent event) {
            return String.valueOf(event.getField().getPhase());
        }
    }

    private static class TraceCompassScopeLogLevel implements ITraceEventAspect<Level> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_LogLevel);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_LogLevelD);
        }

        @Override
        public @Nullable Level resolveTCL(@NonNull TraceEventEvent event) {
            return event.getLevel();
        }
    }

    private static class TraceCompassScopeLogCallsiteAspect implements ITraceEventAspect<ITmfCallsite> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Callsite);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_CallsiteD);
        }

        @Override
        public @Nullable ITmfCallsite resolveTCL(@NonNull TraceEventEvent event) {
            return event.getCallsite();
        }
    }

    private static class TraceCompassScopeLogLabelAspect implements ITraceEventAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Name);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_NameD);
        }

        @Override
        public @Nullable String resolveTCL(@NonNull TraceEventEvent event) {
            return event.getName();
        }
    }

    private static class TraceCompassScopeLogTidAspect implements ITraceEventAspect<Integer> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_ThreadId);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_ThreadIdD);
        }

        @Override
        public @Nullable Integer resolveTCL(@NonNull TraceEventEvent event) {
            return event.getField().getTid();
        }
    }

    private static class TraceCompassScopeLogPidAspect implements ITraceEventAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceEventAspects_Pid);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(
                    Messages.TraceEventAspects_PidD);
        }

        @Override
        public @Nullable String resolveTCL(@NonNull TraceEventEvent event) {
            Object field = event.getField().getPid();
            if (field != null) {
                return String.valueOf(field);
            }
            return null;
        }
    }

    private static class TraceCompassScopeLogDurationAspect implements ITraceEventAspect<Long> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceEventAspects_Duration);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceEventAspects_DurationD);
        }

        @Override
        public @Nullable Long resolveTCL(@NonNull TraceEventEvent event) {
            return event.getField().getDuration();
        }
    }

    private static class TraceCompassScopeLogCategoryAspect implements ITraceEventAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Category);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_CategoryD);
        }

        @Override
        public @Nullable String resolveTCL(@NonNull TraceEventEvent event) {
            return event.getField().getCategory();
        }
    }

    private static class TraceCompassScopeLogIdAspect implements ITraceEventAspect<String> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Id);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_IdD);
        }

        @Override
        public @Nullable String resolveTCL(@NonNull TraceEventEvent event) {
            return event.getField().getId();
        }
    }

    private static class TraceCompassScopeLogArgsAspect implements ITraceEventAspect<Map<String, Object>> {

        @Override
        public @NonNull String getName() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_Args);
        }

        @Override
        public @NonNull String getHelpText() {
            return String.valueOf(Messages.TraceCompassScopeLogAspects_ArgsD);
        }

        @Override
        public @Nullable Map<String, Object> resolveTCL(@NonNull TraceEventEvent event) {
            return event.getField().getArgs();
        }
    }
}
