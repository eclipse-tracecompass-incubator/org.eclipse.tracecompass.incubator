/*******************************************************************************
 * Copyright (c) 2020 Ecole Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.scripting.ui.tracemarker;

import java.util.HashMap;
import java.util.Objects;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.tracemarker.TraceMarker;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.tracemarker.ScriptingMarkerSource;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceAdapterManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;

/**
 * Trace marker module that adds trace marking capabilities to Trace Compass
 * scripting.
 * <p>
 * With this module, a script can iterate through all the events of a trace and
 * call the
 * {@link #addTraceMarker(ITmfTrace, long, long, String, String, String)} method
 * to apply highlighting markers on specific events that are visible on all
 * views of Trace Compass.
 * </p>
 * <p>
 * The markers are completely configurable through the use of optional
 * parameters, each of which has a default value if left untouched.
 * </p>
 *
 * @author Maxime Thibault
 * @author Ibrahima Sega Sangare
 */
public class TraceMarkerScriptingModule {

    /** The DEFAULT_ENDTIME for the marker if it was left unspecified. */
    private static final String DEFAULT_ENDTIME = "-1"; //$NON-NLS-1$

    /** The DEFAULT_LABEL for the marker if it was left unspecified. */
    private static final String DEFAULT_LABEL = "TraceMarker"; //$NON-NLS-1$

    /** The DEFAULT_CATEGORY for the marker if it was left unspecified. */
    private static final String DEFAULT_CATEGORY = "Generals markers"; //$NON-NLS-1$

    /** The DEFAULT_COLOR for the marker if it was left unspecified. */
    private static final String DEFAULT_COLOR = "Red"; //$NON-NLS-1$

    /**
     * The message INVALID_START_TIMESTAMP to print if the marker was set with
     * an bad start time stamp.
     */
    private static final String INVALID_START_TIMESTAMP = "Invalid start time. Start time before end of trace (marker start time %d, start of trace %d)"; //$NON-NLS-1$

    /**
     * The message INVALID_END_TIMESTAMP to print if the marker was set with an
     * end timestamp is less than the start time.
     */
    private static final String INVALID_END_TIMESTAMP = "Invalid end time. End time before start time (start %d, end %d)"; //$NON-NLS-1$

    /**
     * The message INVALID_END_TIMESTAMP to print if the marker was set with an
     * end timestamp greater than the end time of the trace.
     */
    private static final String INVALID_END_TIMESTAMP_2 = "Invalid end time. End time beyond end of trace (marker end time %d, end of trace %d)"; //$NON-NLS-1$

    /**
     * The message INVALID_TRACE to print if the active trace is invalid or
     * non-existing.
     */
    private static final String INVALID_TRACE = "Non-existing or invalid trace: open or reaload the trace!"; //$NON-NLS-1$

    /**
     * The scripting marker source map. Each trace is associated with a source
     * (adapter).
     */
    private final HashMap<ITmfTrace, ScriptingMarkerSource> fScriptingMarkerSourceMap = new HashMap<>();

    /**
     * Adds a trace marker to the time graph view. Except the startTime, all the
     * other parameters are optional and it's up to the user to decide which
     * ones he wants to override. In case a parameter is left untouched, a
     * default value will be assigned.
     *
     * @param trace
     *            the trace to which the marker is added
     * @param startTime
     *            the starting time stamp of the marker in ns (required)
     * @param endTime
     *            the ending time stamp of the marker in ns (optional - default
     *            : startTime)
     * @param label
     *            the marker's label to show (it's name) (optional - default :
     *            TraceMarker)
     * @param category
     *            the marker's group category (can be anything and is used to
     *            group markers together in sets that can be enabled/disabled)
     *            (optional - default : General markers)
     * @param color
     *            the marker's highlighting color in X11 format (ex: "Red",
     *            "Green", "Cyan", "Gold", more at
     *            https://en.wikipedia.org/wiki/X11_color_names) (optional -
     *            default : Red)
     * @throws NullPointerException
     *             Exception thrown if the trace is null. Script developers can
     *             choose to catch this exception or let the script fail. This
     *             is present mostly for testing purposes.
     * @throws IllegalArgumentException
     *             Exception thrown if the parameters for the start time or end
     *             time are incorrect. Script developers can choose to catch
     *             this exception or let the script fail.
     */
    @WrapToScript
    public void addTraceMarker(ITmfTrace trace, long startTime, @ScriptParameter(defaultValue = DEFAULT_ENDTIME) long endTime, @ScriptParameter(defaultValue = DEFAULT_LABEL) @Nullable String label,
            @ScriptParameter(defaultValue = DEFAULT_CATEGORY) @Nullable String category,
            @ScriptParameter(defaultValue = DEFAULT_COLOR) @Nullable String color) throws NullPointerException, IllegalArgumentException {
        ScriptingMarkerSource source = getMarkerSource(trace);
        if (source == null) {
            try {
                source = findMarkerSourceFromAdapters(trace);
                if (source == null) {
                    source = createMarkerSource(trace);
                }
            } catch (NullPointerException e) {
                throw new NullPointerException(INVALID_TRACE);
            }
        }
        try {
            TraceMarker traceMarker = createTraceMarker(trace, label, category, startTime, endTime, color);
            source.configureMarker(traceMarker);
        } catch (IllegalArgumentException e) {
            String markerName = label == null ? DEFAULT_LABEL : label;
            String exceptionMessage = e + " for marker : " + markerName + " at " + startTime; //$NON-NLS-1$ //$NON-NLS-2$
            throw new IllegalArgumentException(exceptionMessage, e);
        }
    }

    /**
     * Create a trace marker object and verify validity of every constructor
     * parameters. This method will throw an IllegalArgumentException exception
     * if the marker's parameters are not valid. The exception is caught by the
     * addTraceMarker method.
     *
     * @param trace
     *            the trace to which the marker is added
     * @param label
     *            the marker's label to show
     * @param category
     *            the marker's group category
     * @param startTime
     *            the start of the marker in ns
     * @param endTime
     *            the end of the marker in ns
     * @param color
     *            the marker's highlighting color
     * @return the created trace marker
     */
    private static TraceMarker createTraceMarker(ITmfTrace trace, @Nullable String label, @Nullable String category, long startTime, long endTime, @Nullable String color) {
        long tStartTime = startTime;
        long tEndTime = endTime;
        String tLabel = label;
        String tCategory = category;
        String tColor = color;

        if (tEndTime == Long.parseLong(DEFAULT_ENDTIME)) {
            tEndTime = tStartTime;
        }

        if (tStartTime > tEndTime) {
            throw new IllegalArgumentException(String.format(INVALID_END_TIMESTAMP, tStartTime, tEndTime));
        }

        if (tEndTime > trace.getEndTime().toNanos()) {
            throw new IllegalArgumentException(String.format(INVALID_END_TIMESTAMP_2, tEndTime, trace.getEndTime().toNanos()));
        }

        if (tStartTime < trace.getStartTime().toNanos()) {
            throw new IllegalArgumentException(String.format(INVALID_START_TIMESTAMP, tStartTime, trace.getStartTime().toNanos()));
        }

        if (tLabel == null) {
            tLabel = DEFAULT_LABEL;
        }

        if (tCategory == null) {
            tCategory = DEFAULT_CATEGORY;
        }

        if (tColor == null) {
            tColor = DEFAULT_COLOR;
        }

        return new TraceMarker(tLabel, tCategory, tStartTime, tEndTime, tColor);
    }

    /**
     * Get the trace marker adapter (source) of the trace.
     *
     * @param trace
     *            the trace associated to the marker source
     * @return the trace's marker source
     */
    private @Nullable ScriptingMarkerSource getMarkerSource(ITmfTrace trace) {
        return fScriptingMarkerSourceMap.get(trace);
    }

    /**
     * Find the marker source from existing adapters.
     *
     * @param trace
     *            the trace associated to the marker source
     * @return the trace's marker source
     */
    private @Nullable ScriptingMarkerSource findMarkerSourceFromAdapters(ITmfTrace trace) {
        for (IMarkerEventSource source : TmfTraceAdapterManager.getAdapters(trace, IMarkerEventSource.class)) {
            if (source instanceof ScriptingMarkerSource) {
                ScriptingMarkerSource markerSource = (ScriptingMarkerSource) source;
                if (Objects.equals(markerSource.getTrace(), trace)) {
                    markerSource.initializeAdapterMarkersLists();
                    fScriptingMarkerSourceMap.put(trace, markerSource);
                    return markerSource;
                }
            }
        }
        return null;
    }

    /**
     * Create a new marker source for the trace.
     *
     * @param trace
     *            the trace associated to the marker source
     * @return the trace's marker source
     */
    private ScriptingMarkerSource createMarkerSource(ITmfTrace trace) {
        ScriptingMarkerSource markerSource = new ScriptingMarkerSource(trace);
        fScriptingMarkerSourceMap.put(trace, markerSource);
        return markerSource;
    }
}
