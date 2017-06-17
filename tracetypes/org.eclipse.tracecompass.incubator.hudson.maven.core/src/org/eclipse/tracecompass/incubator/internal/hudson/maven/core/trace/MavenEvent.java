/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.trace;

import java.util.function.Function;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.text.TextTraceEvent;

import com.google.common.collect.ImmutableList;

/**
 * Maven Trace Event, these are content-less, they use aspects for their data.
 *
 * @author Matthew Khouzam
 */
public class MavenEvent extends TextTraceEvent {

    @NonNullByDefault
    private static final class MavenEventAspect<T> implements ITmfEventAspect<T> {
        private String fDescription;
        private String fName;
        private Function<ITmfEvent, T> fResolver;

        public MavenEventAspect(String name, String description, Function<ITmfEvent, T> resolver) {
            fName = name;
            fDescription = description;
            fResolver = resolver;
        }

        @Override
        public String getHelpText() {
            return fDescription;
        }

        @Override
        public String getName() {
            return fName;
        }

        @Override
        public @Nullable T resolve(@NonNull ITmfEvent event) {
            return fResolver.apply(event);
        }

    }

    /**
     * Duration of the event
     */
    public static final @NonNull ITmfEventAspect<Double> DURATION_ASPECT = new MavenEventAspect<>("Duration", "Time taken by the event", event -> { //$NON-NLS-1$ //$NON-NLS-2$
        if (event instanceof MavenEvent) {
            return ((MavenEvent) event).fDuration;
        }
        return null;
    });

    private static final @NonNull ITmfEventAspect<String> ELEMENT_ASPECT = new MavenEventAspect<>("Element", "Element of the event", event -> { //$NON-NLS-1$ //$NON-NLS-2$
        if (event instanceof MavenEvent) {
            return ((MavenEvent) event).fElement;
        }
        return null;
    });

    /**
     * Full group aspect, the group in cannonical form
     */
    public static final @NonNull ITmfEventAspect<String> FULL_GROUP_ASPECT = new MavenEventAspect<>("Full Group", "Full group of the event", event -> { //$NON-NLS-1$ //$NON-NLS-2$
        if (event instanceof MavenEvent) {
            return ((MavenEvent) event).fFullGroup;
        }
        return null;
    });

    /**
     * Maven Goal type
     */
    public static final TmfEventType GOAL_TYPE = new TmfEventType("Goal", null); //$NON-NLS-1$

    /**
     * Group aspect, the group of the event
     */
    public static final @NonNull ITmfEventAspect<String> GROUP_ASPECT = new MavenEventAspect<>("Group", "Group of the event", event -> { //$NON-NLS-1$ //$NON-NLS-2$
        if (event instanceof MavenEvent) {
            return ((MavenEvent) event).fGroup;
        }
        return null;
    });

    private static final @NonNull ITmfEventAspect<Level> LEVEL_ASPECT = new MavenEventAspect<>("Log Level", "Log levle of the event", event -> { //$NON-NLS-1$ //$NON-NLS-2$
        if (event instanceof MavenEvent) {
            return ((MavenEvent) event).fLevel;
        }
        return null;
    });

    /**
     * Maven summary, typically a test group
     */
    public static final TmfEventType SUMMARY_TYPE = new TmfEventType("Summary", null); //$NON-NLS-1$

    /**
     * Maven test, an individual test
     */
    public static final TmfEventType TEST_TYPE = new TmfEventType("Test", null); //$NON-NLS-1$

    /**
     * Aspects of the event.
     */
    public static final @NonNull Iterable<@NonNull ITmfEventAspect<?>> EVENT_ASPECTS = ImmutableList.of(
            TmfBaseAspects.getTimestampAspect(),
            TmfBaseAspects.getEventTypeAspect(),
            LEVEL_ASPECT,
            GROUP_ASPECT,
            DURATION_ASPECT,
            ELEMENT_ASPECT,
            FULL_GROUP_ASPECT);

    /**
     * Create a goal
     *
     * @param mavenTrace
     *            the parent trace
     * @param timestamp
     *            the timestamp
     * @param level
     *            the loglevel
     * @param group
     *            the group
     * @param fullGroup
     *            the full group
     * @param element
     *            the source, e.g. org.eclipse.cdt.core
     * @return a goal event
     */
    public static MavenEvent createGoal(MavenTrace mavenTrace, @NonNull ITmfTimestamp timestamp, String level, String group, String fullGroup, String element) {
        return new MavenEvent(mavenTrace, timestamp, GOAL_TYPE, Level.parse(level), fullGroup, group, element);
    }

    /**
     * Create a summary event, a maven event representing a specific plug-in
     * being tested
     *
     * @param mavenTrace
     *            the parent trace
     * @param timestamp
     *            the timestamp
     * @param group
     *            the group
     * @param fullGroup
     *            the full group
     * @param duration
     *            time duration in seconds
     * @return a summary event
     */
    public static MavenEvent createSummary(MavenTrace mavenTrace, @NonNull ITmfTimestamp timestamp, String group, String fullGroup, double duration) {
        return new MavenEvent(mavenTrace, timestamp, SUMMARY_TYPE, group, fullGroup, duration);
    }

    /**
     * Create a Test event, a maven event representing a specific unit test
     * being run
     *
     * @param mavenTrace
     *            the parent trace
     * @param timestamp
     *            the timestamp
     * @param group
     *            the group
     * @param fullGroup
     *            the full group
     * @param duration
     *            time duration in seconds
     * @return a summary event
     */
    public static MavenEvent createTest(MavenTrace mavenTrace, @NonNull ITmfTimestamp timestamp, String group, String fullGroup, double duration) {
        return new MavenEvent(mavenTrace, timestamp, TEST_TYPE, group, fullGroup, duration);
    }

    private final Double fDuration;
    private final String fElement;
    private final String fFullGroup;
    private final String fGroup;
    private final Level fLevel;

    /**
     * Default Constructor
     */
    public MavenEvent() {
        super(null, null, null, null);
        fGroup = null;
        fFullGroup = null;
        fDuration = null;
        fLevel = null;
        fElement = null;
    }

    private MavenEvent(MavenTrace mavenTrace, @NonNull ITmfTimestamp timestamp, TmfEventType eventType, Level level, String fullGroup, String group, String element) {
        super(mavenTrace, timestamp, eventType, null);
        fGroup = group;
        fFullGroup = fullGroup;
        fElement = element;
        fLevel = level;
        fDuration = null;

    }

    private MavenEvent(MavenTrace mavenTrace, @NonNull ITmfTimestamp timestamp, TmfEventType eventType, String group, String fullGroup, double duration) {
        super(mavenTrace, timestamp, eventType, null);
        fGroup = group;
        fFullGroup = fullGroup;
        fDuration = duration;
        fElement = null;
        fLevel = Level.FINE;
    }
}