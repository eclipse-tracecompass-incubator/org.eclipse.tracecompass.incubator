/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.tracex.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.tracecompass.incubator.internal.tracex.core.config.TraceXClockConfigurationSource;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests configuration requests against real persisted workspace trace resources. */
public class TraceXClockConfigurationSourceTest {

    private IProject fProject;
    private IFile fFile;

    /** Create an imported trace independently from the clock implementation. */
    @Before
    public void setUp() throws Exception {
        fProject = ResourcesPlugin.getWorkspace().getRoot().getProject("TraceXClockSource"); //$NON-NLS-1$
        fProject.create(null);
        fProject.open(null);
        fFile = fProject.getFile("clock.trx"); //$NON-NLS-1$
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x70000000L, 8, 0, 2, 0);
        byte[] bytes = fixture.fileBytes();
        fixture.putEvent(bytes, 0, 0x1000, 6, 8);
        fixture.putEvent(bytes, 1, 0x1000, 6, 4);
        fFile.create(new ByteArrayInputStream(bytes), true, null);
        fFile.setPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER,
                fProject.getLocation().append("clock-supplementary").toOSString()); //$NON-NLS-1$
        fFile.setPersistentProperty(TmfCommonConstants.TRACETYPE,
                "org.eclipse.tracecompass.incubator.tracex.core.trace"); //$NON-NLS-1$
    }

    /** Remove the disposable imported trace and its persistent properties. */
    @After
    public void tearDown() throws Exception {
        fProject.delete(true, true, null);
    }

    /** Tests source discovery, persisted settings, updates, and reset. */
    @Test
    public void testPersistedConfigurationLifecycle() throws Exception {
        TraceXClockConfigurationSource source = new TraceXClockConfigurationSource();
        assertEquals(TraceXClockConfigurationSource.ID, source.getConfigurationSourceType().getId());
        Map<String, Object> parameters = parameters();
        ITmfConfiguration configured = source.create(request(parameters));
        String id = configured.getId();
        assertTrue(source.contains(id));
        assertEquals("2", Objects.requireNonNull(source.get(id)).getParameters().get("ticks_per_second")); //$NON-NLS-1$ //$NON-NLS-2$
        TraceXClockConfigurationSource restarted = new TraceXClockConfigurationSource();
        assertTrue(restarted.contains(id));
        assertEquals(1, restarted.getConfigurations().size());

        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(fFile, fFile.getLocation().toOSString(), null);
            assertEquals(2_000_000_000L, trace.getEndTime().toNanos());
            assertEquals(TraceXTimerDirection.DECREASING, trace.getTimerDirection());
        } finally {
            trace.dispose();
        }

        parameters.remove("ticks_per_second"); //$NON-NLS-1$
        restarted.update(id, request(parameters));
        assertFalse(Objects.requireNonNull(restarted.get(id)).getParameters().containsKey("ticks_per_second")); //$NON-NLS-1$
        assertEquals(id, Objects.requireNonNull(restarted.remove(id)).getId());
        assertFalse(restarted.contains(id));
        assertNull(restarted.remove(id));
        source.dispose();
        assertTrue(source.getConfigurations().isEmpty());
    }

    /** Tests malformed requests and failed edits retaining the valid clock. */
    @Test
    public void testRequestValidation() throws Exception {
        TraceXClockConfigurationSource source = new TraceXClockConfigurationSource();
        String id = source.create(request(parameters())).getId();
        for (Map<String, Object> invalid : List.of(
                Map.<String, Object>of(),
                Map.<String, Object>of("trace_uri", location(), "timer_direction", "bad"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                Map.<String, Object>of("trace_uri", location(), "timer_direction", "increasing", "ticks_per_second", 2), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                Map.<String, Object>of("trace_uri", location(), "timer_direction", "increasing", "ticks_per_second", "0"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                Map.<String, Object>of("trace_uri", location(), "timer_direction", "increasing", "extra", "value"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            assertThrows(TmfConfigurationException.class, () -> source.update(id, request(invalid)));
            assertEquals("decreasing", Objects.requireNonNull(source.get(id)).getParameters().get("timer_direction")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("2", Objects.requireNonNull(source.get(id)).getParameters().get("ticks_per_second")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        assertThrows(TmfConfigurationException.class, () -> source.update("missing", request(parameters()))); //$NON-NLS-1$
        Map<String, Object> unknown = parameters();
        unknown.put("trace_uri", location() + ".missing"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThrows(TmfConfigurationException.class, () -> source.create(request(unknown)));
    }

    /** Ignore the empty typed marker files used to represent experiment members. */
    @Test
    public void testExperimentMarkersAreNotConfigurations() throws Exception {
        IFolder experiment = fProject.getFolder("experiment"); //$NON-NLS-1$
        experiment.create(true, true, null);
        IFile marker = experiment.getFile("clock.trx"); //$NON-NLS-1$
        marker.create(new ByteArrayInputStream(new byte[0]), true, null);
        marker.setPersistentProperty(TmfCommonConstants.TRACETYPE,
                "org.eclipse.tracecompass.incubator.tracex.core.trace"); //$NON-NLS-1$
        TraceXClockConfigurationSource source = new TraceXClockConfigurationSource();
        String id = source.create(request(parameters())).getId();
        assertEquals(1, source.getConfigurations().size());
        assertTrue(source.contains(id));
        Map<String, Object> invalid = parameters();
        invalid.put("trace_uri", marker.getLocation().toOSString()); //$NON-NLS-1$
        assertThrows(TmfConfigurationException.class, () -> source.create(request(invalid)));
    }

    /** Tests rejection while a live trace owns the imported resource. */
    @Test
    public void testOpenTraceIsProtected() throws Exception {
        TraceXClockConfigurationSource source = new TraceXClockConfigurationSource();
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(fFile, location(), null);
            TmfTraceManager.getInstance().traceOpened(new TmfTraceOpenedSignal(this, trace, fFile));
            assertThrows(TmfConfigurationException.class, () -> source.create(request(parameters())));
            assertEquals(TraceXTimerDirection.INCREASING, trace.getTimerDirection());
            assertFalse(trace.getTicksPerSecond().isPresent());
        } finally {
            TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(this, trace));
            trace.dispose();
        }
    }

    /** Build a request using the current configuration interface. */
    private static ITmfConfiguration request(Map<String, Object> parameters) {
        return new TmfConfiguration.Builder().setParameters(parameters).build();
    }

    /** Return a valid request for this imported trace. */
    private Map<String, Object> parameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("trace_uri", location()); //$NON-NLS-1$
        parameters.put("timer_direction", "decreasing"); //$NON-NLS-1$ //$NON-NLS-2$
        parameters.put("ticks_per_second", "2"); //$NON-NLS-1$ //$NON-NLS-2$
        return parameters;
    }

    /** Return the file path used by the framework import resource. */
    private String location() {
        return fFile.getLocation().toOSString();
    }
}
