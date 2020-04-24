/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.ui.tests.trace.marker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.tracemarker.*;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.incubator.scripting.ui.tracemarker.TraceMarkerScriptingModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Multimap;

/**
 * Test the following classes {@link TraceMarker}
 * {@link ScriptingMarkerSourceFactory} {@link TraceMarkerScriptingModule}
 * {@link ScriptingMarkerSource}
 *
 * @author Ibrahima Sega Sangare
 */
public class TraceMarkerTest {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final @NonNull IProgressMonitor PROGRESS_MONITOR = new NullProgressMonitor();
    /** The constant ALPHA for the marker transparency. */
    private static final int DEFAULT_ENDTIME = -1;
    private static final String DEFAULT_COLOR = "Red";
    private static final @NonNull String[] TEST_COLORS = { "Dark Orange", "Black", "Burlywood", "Cyan", "Chartreuse", "Azure", "Yellow Green", "Silver" };
    private static final String[] TEST_LABELS = { "Default Marker", "Test", "Ericsson Marker", "" };
    private static final @NonNull String[] TEST_CATEGORIES = { "Default", "Analysis", "Performance" };
    private ITmfTrace fTrace;
    private ScriptingMarkerSource fScriptingMarkerSource;
    private ScriptingMarkerSourceFactory fScriptingMarkerSourceFactory;
    private TraceMarkerScriptingModule fTraceMarkerScriptingModule;

    // ------------------------------------------------------------------------

    // Initialization
    // ------------------------------------------------------------------------
    /**
     * Setup a project with a trace.
     */
    @SuppressWarnings("null")
    @Before
    public void setUpEnvironment() {
        // Initialize TraceMarkerScriptingModule. Should not be able to add
        // markers without a trace

        fTraceMarkerScriptingModule = new TraceMarkerScriptingModule();
        Exception exception = null;
        try {
            fTraceMarkerScriptingModule.addTraceMarker(fTrace, 0, DEFAULT_ENDTIME, null, null, DEFAULT_COLOR);
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);

        // Initialize trace
        fTrace = ScriptingTestUtils.getTrace();
        assertTrue(fTrace instanceof TmfXmlTraceStub);

        fTraceMarkerScriptingModule = new TraceMarkerScriptingModule();

        // Initialize scriptingMarkerSource
        assertNotNull(fTrace);
        fScriptingMarkerSource = new ScriptingMarkerSource(fTrace);
        fScriptingMarkerSource.initializeAdapterMarkersLists();
        assertTrue(fTrace.equals(fScriptingMarkerSource.getTrace()));
        assertNotNull(fScriptingMarkerSource);
        assertNotNull(fScriptingMarkerSource.getMarkerCategories());
        assertTrue(fScriptingMarkerSource.getMarkerCategories().isEmpty());

        // Initialize scriptingMarkerSourceFactory
        fScriptingMarkerSourceFactory = ScriptingMarkerSourceFactory.getInstance();
        assertNotNull(fScriptingMarkerSourceFactory);
        assertNotNull(fScriptingMarkerSourceFactory.getAdapterList()[0]);
    }

    /**
     * Delete the project after tests and dispose of test trace.
     */
    @After
    public void cleanUpEnvironment() {
        if (fTrace != null) {
            fTrace.dispose();
            fTrace = null;
        }
        if (fScriptingMarkerSource != null) {
            fScriptingMarkerSource.dispose();
            fScriptingMarkerSource = null;
        }
        if (fScriptingMarkerSourceFactory != null) {
            fScriptingMarkerSourceFactory.dispose();
            fScriptingMarkerSourceFactory = null;
        }
    }

    // ------------------------------------------------------------------------
    // Test Cases
    // ------------------------------------------------------------------------

    /**
     * Test: assign different colors to a marker at its creation
     */
    @Test
    public void testMarkerColor() {
        RGBAColor rgbaColor;
        // Test: Marker has correct color
        for (int i = 0; i < TEST_COLORS.length; i++) {
            TraceMarker traceMarker = new TraceMarker("Marker" + String.valueOf(i), null, fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(),
                    TEST_COLORS[i]);
            String hexColor = X11ColorUtils.toHexColor(TEST_COLORS[i]);
            assertNotNull(hexColor);

            rgbaColor = new RGBAColor(Integer.parseInt(hexColor.substring(1), 16));
            assertEquals(rgbaColor, traceMarker.getRGBAColor());
        }

        // Test: Marker has default color when color parameter is an empty
        // string
        TraceMarker traceMarker = new TraceMarker("", "", fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "");
        String hexColor = X11ColorUtils.toHexColor(DEFAULT_COLOR);
        assertNotNull(hexColor);

        rgbaColor = new RGBAColor(Integer.parseInt(hexColor.substring(1), 16));
        assertEquals(rgbaColor, traceMarker.getRGBAColor());

        // Test: Marker has default color when invalid color is passed as a
        // parameter
        traceMarker = new TraceMarker("", "", fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "invalid");
        assertEquals(rgbaColor, traceMarker.getRGBAColor());

        // Test: Marker has default color when null color is passed as a
        // parameter
        traceMarker = new TraceMarker("", "", fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), null);
        assertEquals(rgbaColor, traceMarker.getRGBAColor());
    }

    /**
     * Test: assign correct timestamps to a marker
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     */
    @SuppressWarnings("null")
    @Test
    public void testCorrectMarkerTimestamp() throws Exception {
        // Test: startTime equal to endTime
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, 1, 1, null, null, null);
    }

    /**
     * Test: negative value for startTime
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp1() throws Exception {
        assertNotNull(fTrace);
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, -10, fTrace.getEndTime().toNanos(), "", "", DEFAULT_COLOR);
    }

    /**
     * Test: negative value for end time
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp2() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, fTrace.getStartTime().toNanos(), -2, "", "", "");
    }

    /**
     * Test: start time and end time outside of trace time range
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp3() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, fTrace.getStartTime().toNanos() - 1, fTrace.getEndTime().toNanos() + 1, "", "", "");
    }

    /**
     * Test: negative values for both startTime and endTime
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp4() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, -10, -2, "", "", "");
    }

    /**
     * Test: start time greater than end time
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp5() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, fTrace.getEndTime().toNanos(), fTrace.getStartTime().toNanos() + 1, "", "", "");
    }

    /**
     * Test: start time below start time of trace
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp6() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, fTrace.getStartTime().toNanos() - 1, fTrace.getStartTime().toNanos(), "", "", "");
    }

    /**
     * Test: end time above end time of trace
     *
     * @throws Exception
     *             IllegalArgumentException thrown by incorrect trace marker
     *             timestamp values
     *
     */
    @SuppressWarnings("null")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectMarkerTimestamp7() throws Exception {
        fTraceMarkerScriptingModule.addTraceMarker(fTrace, fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos() + 1, "", "", "");
    }

    /**
     * Test: marker label is correct
     */
    @Test
    public void testMarkerLabel() {

        for (int i = 0; i < TEST_LABELS.length; i++) {
            TraceMarker traceMarker = new TraceMarker(TEST_LABELS[i], null, fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "");
            assertTrue(traceMarker.getLabel().equals(TEST_LABELS[i]));
        }

        TraceMarker traceMarker = new TraceMarker(null, "", fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "");
        assertTrue(traceMarker.getLabel() == null);
    }

    /**
     * Test: marker category is correct
     */
    @Test
    public void testMarkerCategory() {

        for (int i = 0; i < TEST_CATEGORIES.length; i++) {
            TraceMarker traceMarker = new TraceMarker("", TEST_CATEGORIES[i], fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "");
            assertTrue(traceMarker.getCategory().equals(TEST_CATEGORIES[i]));
        }

        TraceMarker traceMarker = new TraceMarker("", null, fTrace.getStartTime().toNanos(), fTrace.getEndTime().toNanos(), "");
        assertTrue(traceMarker.getCategory() == null);
    }

    /**
     * Test: verify marker configuration via added categories and added
     * MarkerEvent objects
     */
    @Test
    public void testConfigureTraceMarker() {
        fScriptingMarkerSource.initializeAdapterMarkersLists();
        assertTrue(fScriptingMarkerSource.getMarkerEventMap().isEmpty());
        assertTrue(fScriptingMarkerSource.getMarkerCategories().isEmpty());
        assertTrue(fScriptingMarkerSource.getMarkerEvents().isEmpty());

        // Test: configure markers
        long startTime = fTrace.getStartTime().toNanos();
        long endTime = fTrace.getEndTime().toNanos();
        ArrayList<@NonNull TraceMarker> traceMarkers = new ArrayList<>();
        for (int i = 0; i < TEST_CATEGORIES.length; i++) {
            traceMarkers.add(new TraceMarker("", TEST_CATEGORIES[i], startTime, endTime, ""));
            assertFalse(fScriptingMarkerSource.getMarkerCategories().contains(TEST_CATEGORIES[i]));

            fScriptingMarkerSource.configureMarker(traceMarkers.get(i));
            assertTrue(fScriptingMarkerSource.getMarkerCategories().contains(traceMarkers.get(i).getCategory()));
        }

        // Test: Make sure the correct amount of markers has been created
        int markerEventCount = 0;
        @NonNull
        Multimap<@NonNull String, @NonNull IMarkerEvent> markerEventMap = fScriptingMarkerSource.getMarkerEventMap();
        for (String category : fScriptingMarkerSource.getMarkerCategories()) {
            markerEventCount += markerEventMap.get(category).size();
        }
        assertTrue(markerEventCount == fScriptingMarkerSource.getMarkerEvents().size());
        assertTrue(markerEventCount == fScriptingMarkerSource.getMarkerList(startTime, endTime, 0, PROGRESS_MONITOR).size());

        // Test: Add marker in existing category
        TraceMarker marker = new TraceMarker("", TEST_CATEGORIES[0], startTime, endTime, "");
        fScriptingMarkerSource.configureMarker(marker);
        assertTrue(fScriptingMarkerSource.getMarkerCategories().contains(marker.getCategory()));

        // Test: Get markers from a specific category with no markers outside
        // the time range
        @NonNull
        List<@NonNull IMarkerEvent> markerEvents = fScriptingMarkerSource.getMarkerList(
                TEST_CATEGORIES[0],
                startTime,
                endTime,
                0,
                PROGRESS_MONITOR);

        ArrayList<IMarkerEvent> markersInFirstCategory = new ArrayList<>();
        markersInFirstCategory.addAll(fScriptingMarkerSource.getMarkerEventMap().get(TEST_CATEGORIES[0]));
        assertNotNull(markersInFirstCategory);
        assertFalse(markerEvents.isEmpty());
        assertTrue(markerEvents.size() == markersInFirstCategory.size());
    }

    /**
     * Test: Insert markers with surrounding neighbors
     */
    @Test
    public void testgetMarkerList() {
        long startTime = fTrace.getStartTime().toNanos();
        long endTime = fTrace.getEndTime().toNanos();
        long duration = endTime - startTime;

        // Test: Get marker list when no marker has been added
        @NonNull
        List<@NonNull IMarkerEvent> EmptyMarkerEvents = fScriptingMarkerSource.getMarkerList(
                TEST_CATEGORIES[0],
                startTime,
                endTime, 0,
                PROGRESS_MONITOR);
        assertTrue(EmptyMarkerEvents.isEmpty());

        // Test: Get markers from a specific category including nearest previous
        // and next markers.
        fScriptingMarkerSource.initializeAdapterMarkersLists();

        // Marker at the start
        TraceMarker markerAtStart = new TraceMarker("", TEST_CATEGORIES[0], startTime, startTime + (duration / 4), "");
        fScriptingMarkerSource.configureMarker(markerAtStart);

        // Marker at the end
        TraceMarker markerAtEnd = new TraceMarker("", TEST_CATEGORIES[0], startTime + (duration * 3 / 4), endTime, "");
        fScriptingMarkerSource.configureMarker(markerAtEnd);

        // Marker in the middle
        TraceMarker markerInMiddle = new TraceMarker("", TEST_CATEGORIES[0], startTime + (duration / 3), endTime - (duration / 3), "");
        fScriptingMarkerSource.configureMarker(markerInMiddle);

        @NonNull
        List<@NonNull IMarkerEvent> splitMarkerEvents = fScriptingMarkerSource.getMarkerList(
                TEST_CATEGORIES[0],
                markerInMiddle.getStartTime(),
                markerInMiddle.getEndTime(), 0,
                PROGRESS_MONITOR);
        assertFalse(splitMarkerEvents.isEmpty());
        assertTrue(splitMarkerEvents.size() == fScriptingMarkerSource.getMarkerEvents().size());

        // Test: Get markers from a specific category including nearest previous
        // only
        fScriptingMarkerSource.initializeAdapterMarkersLists();

        // Marker on the left
        TraceMarker leftMarker = new TraceMarker("", TEST_CATEGORIES[0], startTime, startTime + (duration / 3), "");
        fScriptingMarkerSource.configureMarker(leftMarker);

        // Marker on the right
        TraceMarker rightMarker = new TraceMarker("", TEST_CATEGORIES[0], startTime + (duration * 2 / 3), endTime, "");
        fScriptingMarkerSource.configureMarker(rightMarker);

        @NonNull
        List<@NonNull IMarkerEvent> markerEvents = fScriptingMarkerSource.getMarkerList(
                TEST_CATEGORIES[0],
                rightMarker.getStartTime(),
                rightMarker.getEndTime(), 0,
                PROGRESS_MONITOR);
        assertFalse(markerEvents.isEmpty());
        assertTrue(markerEvents.size() == fScriptingMarkerSource.getMarkerEvents().size());

        // Test: Get markers from a specific category including nearest next
        // only
        markerEvents = fScriptingMarkerSource.getMarkerList(
                TEST_CATEGORIES[0],
                leftMarker.getStartTime(),
                leftMarker.getEndTime(), 0,
                PROGRESS_MONITOR);
        assertFalse(markerEvents.isEmpty());
        assertTrue(markerEvents.size() == fScriptingMarkerSource.getMarkerEvents().size());

    }

    /**
     * Test: verify marker timestamp getters
     */
    @Test
    public void testMarkerTimeStampGetters() {
        TraceMarker marker = new TraceMarker(TEST_LABELS[0], TEST_CATEGORIES[0], fTrace.getStartTime().toNanos(), fTrace.getStartTime().toNanos(), DEFAULT_COLOR);
        assertTrue(marker.getEndTime() == fTrace.getEndTime().toNanos());
        assertTrue(marker.getStartTime() == fTrace.getStartTime().toNanos());
        assertTrue(marker.getDuration() == (fTrace.getEndTime().toNanos() - fTrace.getStartTime().toNanos()));
    }

}
