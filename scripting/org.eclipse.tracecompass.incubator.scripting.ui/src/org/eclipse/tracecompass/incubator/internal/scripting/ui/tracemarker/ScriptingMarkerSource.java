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

package org.eclipse.tracecompass.incubator.internal.scripting.ui.tracemarker;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGBA;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.signal.TmfMarkerEventSourceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.AbstractTmfTraceAdapterFactory.IDisposableAdapter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * The Class ScriptingMarkerSource is an "adapter type" class that contains all
 * the required methods to receive a list of TraceMarkers objects, convert it
 * into a list of MarkerEvents and send them to/signals the
 * ScriptedTimeGraphView.
 *
 * @author Maxime Thibault
 * @author Ibrahima Sega Sangare
 */
@NonNullByDefault
public class ScriptingMarkerSource implements IMarkerEventSource, IDisposableAdapter {

    /** The constant ALPHA for the marker transparency. */
    private static final int ALPHA = 70;
    /**
     * The trace marker event map. It represents a list of markers per category.
     */
    private Multimap<String, IMarkerEvent> fTraceMarkerEventMap = HashMultimap.create();

    /** The active trace. */
    private final ITmfTrace fTrace;

    /**
     * Instantiate and register a new trace marker source (adapter) to a trace.
     *
     * @param trace
     *            the active trace
     */
    public ScriptingMarkerSource(ITmfTrace trace) {
        fTrace = trace;
        TmfSignalManager.register(this);
    }

    /**
     * Initialize the market events and categories list for a new script run.
     *
     */
    public void initializeAdapterMarkersLists() {
        fTraceMarkerEventMap.clear();
    }

    /**
     * Convert trace marker object into a marker event and signal the time graph
     * view.
     *
     * @param traceMarker
     *            : the marker object to convert
     */
    public void configureMarker(TraceMarker traceMarker) {
        RGBAColor traceMarkerColor = traceMarker.getRGBAColor();
        RGBA markerEventColor = new RGBA(traceMarkerColor.getRed(),
                traceMarkerColor.getGreen(),
                traceMarkerColor.getBlue(),
                ALPHA);

        String category = traceMarker.getCategory();
        String label = traceMarker.getLabel();
        long start = traceMarker.getStartTime();
        long duration = traceMarker.getDuration();
        MarkerEvent traceMarkerEvent = new MarkerEvent(null, start, duration, category, markerEventColor, label, true);
        if (category != null) {
            fTraceMarkerEventMap.put(category, traceMarkerEvent);
        }
        TmfSignalManager.dispatchSignal(new TmfMarkerEventSourceUpdatedSignal(this));
    }

    /**
     * Get the marker event map, a list of markers (value) per category (key)
     * for each entry
     *
     * @return the marker event map
     */
    public Multimap<String, IMarkerEvent> getMarkerEventMap() {
        return fTraceMarkerEventMap;
    }

    /**
     * Gets the categories list.
     *
     * @return the categories list
     */
    @Override
    public List<String> getMarkerCategories() {
        List<String> markerCategories = new ArrayList<>();
        markerCategories.addAll(fTraceMarkerEventMap.keySet());
        return markerCategories;
    }

    /**
     * Get all the marker events on the trace.
     *
     * @return the marker events
     */
    public List<IMarkerEvent> getMarkerEvents() {
        List<IMarkerEvent> markerEvents = new ArrayList<>();
        markerEvents.addAll(fTraceMarkerEventMap.values());
        return markerEvents;
    }

    /**
     * Gets the list of marker events of a specific category that intersect the
     * given time range (inclusively).
     * <p>
     * The list should also include the nearest previous and next markers that
     * do not intersect the time range as stated in the definition in the
     * IMarkerEventSource interface.
     *
     * @param category
     *            The marker category
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution (not used as for the being.)
     * @param monitor
     *            The progress monitor object
     * @return The list of marker events
     */
    @Override
    public @NonNull List<@NonNull IMarkerEvent> getMarkerList(@NonNull String category, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        List<IMarkerEvent> markerEventsInCategory = new ArrayList<>();
        markerEventsInCategory.addAll(fTraceMarkerEventMap.get(category));
        if (markerEventsInCategory.isEmpty()) {
            return markerEventsInCategory;
        }

        List<IMarkerEvent> markerEvents = new ArrayList<>();
        long distanceFromStart = Long.MAX_VALUE;
        long distanceFromEnd = Long.MAX_VALUE;

        IMarkerEvent nearestNextMarkerEvent = null;
        IMarkerEvent nearestPreviousMarkerEvent = null;

        for (IMarkerEvent markerEvent : markerEventsInCategory) {
            long markerEndTime = markerEvent.getTime() + markerEvent.getDuration();

            // Marker is inside time range inclusively
            if (markerEvent.getTime() >= startTime &&
                    markerEndTime <= endTime) {
                markerEvents.add(markerEvent);
            }

            // Marker comes after time range
            if (markerEvent.getTime() > endTime) {
                long distance = markerEvent.getTime() - endTime;
                if (distance < distanceFromEnd) {
                    distanceFromEnd = distance;
                    nearestNextMarkerEvent = markerEvent;
                }
            }

            // Marker comes before time range
            if (markerEndTime < startTime) {
                long distance = startTime - markerEndTime;
                if (distance < distanceFromStart) {
                    distanceFromStart = distance;
                    nearestPreviousMarkerEvent = markerEvent;
                }

            }
        }
        if (nearestNextMarkerEvent != null) {
            markerEvents.add(nearestNextMarkerEvent);
        }

        if (nearestPreviousMarkerEvent != null) {
            markerEvents.add(nearestPreviousMarkerEvent);
        }

        return markerEvents;
    }

    /**
     * Unregister the adapter.
     */
    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
    }

    /**
     * Get the trace linked to this adapter
     *
     * @return the trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

}
