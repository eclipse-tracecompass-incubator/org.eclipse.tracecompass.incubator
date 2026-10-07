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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.tracex.core.Activator;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXAnalysis;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProvider;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProviderFactory;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.Test;

/** Tests per-trace clock configuration, persistence, and rebuilt consumers. */
public class TraceXClockConfigurationTest {

    private static final String TICKS_PROPERTY = "ticks_per_second"; //$NON-NLS-1$
    private static final String DIRECTION_PROPERTY = "timer_direction"; //$NON-NLS-1$

    /** Tests ordering ticks, configured time, raw fields, and invalid changes. */
    @Test
    public void testConfigurationAndEventTime() throws Exception {
        Path path = twoEventTrace(ByteOrder.LITTLE_ENDIAN, 10, 12, 0xffff);
        CountingTrace trace = new CountingTrace();
        try {
            assertThrows(Exception.class,
                    () -> trace.setTicksPerSecond("1")); //$NON-NLS-1$
            trace.initTrace(null, path.toString(), null);
            assertFalse(trace.getTicksPerSecond().isPresent());
            assertEquals(TraceXTimerDirection.INCREASING,
                    trace.getTimerDirection());
            assertEquals("unavailable", trace.getProperties().get(TICKS_PROPERTY)); //$NON-NLS-1$
            assertEquals("ordering-ticks", trace.getProperties().get("time_unit")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("unavailable", trace.getProperties().get("physical_time")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(2, trace.getEndTime().toNanos());

            ITmfContext context = trace.seekEvent(1);
            ITmfEvent event = trace.getNext(context);
            assertNotNull(event);
            assertEquals(2, event.getTimestamp().toNanos());
            TraceXEvent raw = (TraceXEvent) event.getContent().getValue();
            assertEquals(12, raw.getRawTimestamp());
            assertEquals(2, raw.getRelativeTicks().getAsLong());
            assertEquals(12L,
                    raw.getFields().get(TraceXEvent.FIELD_RAW_TIMESTAMP));
            assertEquals(2L,
                    raw.getFields().get(TraceXEvent.FIELD_RELATIVE_TICKS));

            trace.setTicksPerSecond("2"); //$NON-NLS-1$
            assertEquals(1, trace.fInvalidations);
            assertEquals(2, trace.getTicksPerSecond().getAsLong());
            assertEquals(1_000_000_000L, trace.getEndTime().toNanos());
            assertEquals("nanoseconds", trace.getProperties().get("time_unit")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("available-rounded-down", //$NON-NLS-1$
                    trace.getProperties().get("physical_time")); //$NON-NLS-1$
            trace.setTicksPerSecond("2"); //$NON-NLS-1$
            assertEquals(1, trace.fInvalidations);

            String[] invalid = { "0", "-1", "1.5", "1000000001" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            for (String value : invalid) {
                assertThrows(IllegalArgumentException.class,
                        () -> trace.setTicksPerSecond(value));
                assertEquals(2, trace.getTicksPerSecond().getAsLong());
                assertEquals(1, trace.fInvalidations);
            }

            trace.clearTicksPerSecond();
            assertEquals(2, trace.fInvalidations);
            assertFalse(trace.getTicksPerSecond().isPresent());
            trace.clearTicksPerSecond();
            assertEquals(2, trace.fInvalidations);
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests decreasing normalization, non-all-ones masks, and byte orders. */
    @Test
    public void testDirectionMaskAndByteOrderEquivalence() throws Exception {
        for (ByteOrder order : new ByteOrder[] { ByteOrder.LITTLE_ENDIAN,
                ByteOrder.BIG_ENDIAN }) {
            Path path = twoEventTrace(order, 1, 14, 0xf);
            CountingTrace trace = new CountingTrace();
            try {
                trace.initTrace(null, path.toString(), null);
                assertEquals(13, trace.getEndTime().toNanos());
                trace.setTimerDirection(TraceXTimerDirection.DECREASING);
                assertEquals(3, trace.getEndTime().toNanos());
                assertEquals("decreasing", //$NON-NLS-1$
                        trace.getProperties().get(DIRECTION_PROPERTY));
                assertEquals(1, trace.fInvalidations);
                trace.setTimerDirection(TraceXTimerDirection.DECREASING);
                assertEquals(1, trace.fInvalidations);
            } finally {
                trace.dispose();
                Files.deleteIfExists(path);
            }
        }
    }

    /** Tests persistence and rejection without replacing the valid setting. */
    @Test
    public void testPersistenceAndFailureAtomicity() throws Exception {
        Map<QualifiedName, String> properties = new HashMap<>();
        IResource resource = resource(properties, false);
        Path path = twoEventTrace(ByteOrder.LITTLE_ENDIAN, 4, 8, 0xffff);
        CountingTrace first = new CountingTrace();
        CountingTrace second = new CountingTrace();
        try {
            first.initTrace(resource, path.toString(), null);
            first.setTicksPerSecond("4"); //$NON-NLS-1$
            first.setTimerDirection(TraceXTimerDirection.DECREASING);
            assertTrue(properties.containsValue("4")); //$NON-NLS-1$
            assertTrue(properties.containsValue("decreasing")); //$NON-NLS-1$
            first.dispose();

            second.initTrace(resource, path.toString(), null);
            assertEquals(4, second.getTicksPerSecond().getAsLong());
            assertEquals(TraceXTimerDirection.DECREASING,
                    second.getTimerDirection());
            assertEquals(16_383_000_000_000L,
                    second.getEndTime().toNanos());

            CountingTrace failing = new CountingTrace();
            try {
                failing.initTrace(resource(properties, true), path.toString(),
                        null);
                assertThrows(Exception.class,
                        () -> failing.setTicksPerSecond("8")); //$NON-NLS-1$
                assertEquals(4, failing.getTicksPerSecond().getAsLong());
                assertEquals(0, failing.fInvalidations);
            } finally {
                failing.dispose();
            }
        } finally {
            first.dispose();
            second.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests malformed persisted values and conversion overflow rejection. */
    @Test
    public void testPersistedAndEndpointFailures() throws Exception {
        Map<QualifiedName, String> invalid = new HashMap<>();
        invalid.put(new QualifiedName(Activator.PLUGIN_ID, TICKS_PROPERTY), "0"); //$NON-NLS-1$
        Path ordinary = twoEventTrace(ByteOrder.LITTLE_ENDIAN, 1, 2, 0xffff);
        CountingTrace malformed = new CountingTrace();
        try {
            assertThrows(Exception.class, () -> malformed.initTrace(
                    resource(invalid, false), ordinary.toString(), null));
        } finally {
            malformed.dispose();
            Files.deleteIfExists(ordinary);
        }

        Path overflow = overflowingTrace();
        CountingTrace trace = new CountingTrace();
        try {
            trace.initTrace(null, overflow.toString(), null);
            assertThrows(Exception.class, () -> trace.setTicksPerSecond("1")); //$NON-NLS-1$
            assertFalse(trace.getTicksPerSecond().isPresent());
            assertEquals(0, trace.fInvalidations);
        } finally {
            trace.dispose();
            Files.deleteIfExists(overflow);
        }
    }

    /** Tests the server-facing provider against a changed time base. */
    @Test
    public void testDataProviderUsesRebuiltTimeBase() throws Exception {
        Path path = twoEventTrace(ByteOrder.LITTLE_ENDIAN, 20, 22, 0xffff);
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), ITmfEvent.class,
                    "server.trx", //$NON-NLS-1$
                    "org.eclipse.tracecompass.incubator.tracex.core.trace"); //$NON-NLS-1$
            TraceXDataProviderFactory factory = new TraceXDataProviderFactory();
            try (AnalysisHandle initial = analyze(trace)) {
                TraceXDataProvider provider = initial.fProvider;
                assertEquals(TraceXDataProviderFactory.ID, provider.getId());
                assertEquals(2, providerEnd(provider));
                assertTrue(Objects.requireNonNull(provider.fetchArrows(
                        new HashMap<>(), null).getModel()).isEmpty());
            }

            trace.setTicksPerSecond("2"); //$NON-NLS-1$
            try (AnalysisHandle rebuilt = analyze(trace)) {
                assertEquals(1_000_000_000L,
                        providerEnd(rebuilt.fProvider));
            }

            ITmfTrace other = (ITmfTrace) Proxy.newProxyInstance(
                    ITmfTrace.class.getClassLoader(),
                    new Class<?>[] { ITmfTrace.class },
                    (proxy, method, arguments) -> "getChildren" //$NON-NLS-1$
                            .equals(method.getName()) ? Collections.emptyList()
                                    : null);
            assertNull(factory.createDataProvider(other));
            assertTrue(factory.getDescriptors(other).isEmpty());

            TraceXAnalysis serverAnalysis = new TraceXAnalysis();
            serverAnalysis.setId(TraceXAnalysis.ID);
            assertTrue(serverAnalysis.setTrace(trace));
            assertNull(trace.addAnalysisModule(serverAnalysis));
            TmfExperiment experiment = new TmfExperiment(ITmfEvent.class,
                    "TraceX server experiment", new ITmfTrace[] { trace }, //$NON-NLS-1$
                    TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
            TmfTraceOpenedSignal opened = new TmfTraceOpenedSignal(this,
                    experiment, null);
            try {
                experiment.traceOpened(opened);
                TmfTraceManager.getInstance().traceOpened(opened);
                Collection<IDataProviderDescriptor> descriptors = factory
                        .getDescriptors(experiment);
                assertEquals(1, descriptors.size());
                IDataProviderDescriptor descriptor = descriptors.iterator().next();
                assertEquals(TraceXDataProviderFactory.ID, descriptor.getId());
                assertEquals("TraceX Execution", descriptor.getName()); //$NON-NLS-1$
                assertEquals(ProviderType.TIME_GRAPH, descriptor.getType());

                ITmfDataProvider provider = factory.createDataProvider(experiment);
                assertNotNull(provider);
                assertEquals(TraceXDataProviderFactory.ID, provider.getId());
                provider.dispose();
            } finally {
                TmfTraceManager.getInstance().traceClosed(
                        new TmfTraceClosedSignal(this, experiment));
                experiment.dispose();
            }
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests the framework index and supplementary-data invalidation path. */
    @Test
    public void testFrameworkInvalidation() throws Exception {
        Path path = twoEventTrace(ByteOrder.LITTLE_ENDIAN, 30, 32, 0xffff);
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), null);
            trace.setTicksPerSecond("2"); //$NON-NLS-1$
            assertEquals(1_000_000_000L, trace.getEndTime().toNanos());
            assertEquals(2, trace.getNbEvents());
            ITmfContext context = trace.seekEvent(1);
            ITmfEvent event = trace.getNext(context);
            assertNotNull(event);
            assertEquals(1_000_000_000L, event.getTimestamp().toNanos());
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    private static long providerEnd(TraceXDataProvider provider) {
        TmfTreeModel<@NonNull TimeGraphEntryModel> model = Objects
                .requireNonNull(provider.fetchTree(new HashMap<>(), null)
                        .getModel());
        return model.getEntries().get(0).getEndTime();
    }

    private static AnalysisHandle analyze(TraceXTrace trace) throws Exception {
        TraceXAnalysis analysis = new TraceXAnalysis();
        analysis.setId(TraceXAnalysis.ID);
        assertTrue(analysis.setTrace(trace));
        analysis.schedule();
        assertTrue(analysis.waitForCompletion());
        return new AnalysisHandle(analysis,
                new TraceXDataProvider(trace, analysis));
    }

    private static final class AnalysisHandle implements AutoCloseable {
        private final TraceXAnalysis fAnalysis;
        private final TraceXDataProvider fProvider;

        private AnalysisHandle(TraceXAnalysis analysis,
                TraceXDataProvider provider) {
            fAnalysis = analysis;
            fProvider = provider;
        }

        @Override
        public void close() {
            fAnalysis.dispose();
        }
    }

    private static Path twoEventTrace(ByteOrder order, long first, long second,
            long mask) throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(order,
                0x70000000L, 8, 0, 2, 0);
        fixture.putInt(4, mask);
        byte[] data = fixture.fileBytes();
        fixture.putEvent(data, 0, 0x1000, 1, first);
        fixture.putEvent(data, 1, 0x2000, 1, second);
        Path path = Files.createTempFile("tracex-clock-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        return path;
    }

    private static Path overflowingTrace() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x70000000L, 8, 0, 4, 0);
        fixture.putInt(4, 0xffffffffL);
        byte[] data = fixture.fileBytes();
        for (int physical = 0; physical < 4; physical++) {
            fixture.putEvent(data, physical, 0x1000 + physical, 1,
                    3 - physical);
        }
        Path path = Files.createTempFile("tracex-clock-overflow-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        return path;
    }

    private static IResource resource(Map<QualifiedName, String> properties,
            boolean failWrites) {
        return (IResource) Proxy.newProxyInstance(IResource.class.getClassLoader(),
                new Class<?>[] { IResource.class }, (proxy, method, arguments) -> {
                    switch (method.getName()) {
                    case "getName": //$NON-NLS-1$
                        return "clock.trx"; //$NON-NLS-1$
                    case "getPersistentProperty": //$NON-NLS-1$
                        return properties.get(arguments[0]);
                    case "setPersistentProperty": //$NON-NLS-1$
                        if (failWrites) {
                            throw new CoreException(new org.eclipse.core.runtime.Status(IStatus.ERROR,
                                    Activator.PLUGIN_ID, "synthetic persistence failure")); //$NON-NLS-1$
                        }
                        QualifiedName key = (QualifiedName) arguments[0];
                        String value = (String) arguments[1];
                        if (value == null) {
                            properties.remove(key);
                        } else {
                            properties.put(key, value);
                        }
                        return null;
                    default:
                        Class<?> type = method.getReturnType();
                        if (!type.isPrimitive()) {
                            return null;
                        }
                        if (type == boolean.class) {
                            return false;
                        }
                        return 0;
                    }
                });
    }

    private static final class CountingTrace extends TraceXTrace {
        private int fInvalidations;

        @Override
        protected void invalidateTimeDependentData() {
            fInvalidations++;
        }
    }
}
