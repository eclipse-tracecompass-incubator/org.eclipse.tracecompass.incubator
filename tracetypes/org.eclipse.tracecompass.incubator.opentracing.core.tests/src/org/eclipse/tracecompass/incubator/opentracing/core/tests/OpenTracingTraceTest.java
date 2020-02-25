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

package org.eclipse.tracecompass.incubator.opentracing.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.trace.OpenTracingTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test reading real traces
 *
 * @author Katherine Nadeau
 *
 */
public class OpenTracingTraceTest {

    /**
     * Test reading an event and get the aspects
     *
     * @throws TmfTraceException
     *             file error
     */
    @Test
    public void testEvent() throws TmfTraceException {
        String path = "traces/simple.json";
        Map<String, ITmfEventAspect<?>> eventAspects = getEventAspects(path);
        ITmfEvent event = getFirstEvent(path);
        assertNotNull(event);
        ImmutableSet<String> aspectNames = ImmutableSet.of("Duration", "Process", "ID", "Timestamp", "Process tags", "Tags", "Name");

        assertEquals(aspectNames, eventAspects.keySet());
        testAspect(eventAspects.get("Name"), event, "say-hello");

        Double ts = 1526674498419000.0;
        testAspect(eventAspects.get("Timestamp"), event, TmfTimestamp.fromMicros(ts.longValue()));
        testAspect(eventAspects.get("Duration"), event, "16.961 ms");
        testAspect(eventAspects.get("ID"), event, "cf46871fbf4f262b");
        testAspect(eventAspects.get("Process"), event, "hello-world");

        Map<@NonNull String, @NonNull Object> processTagsMap = new HashMap<>();
        processTagsMap.put("hostname", "mystery");
        processTagsMap.put("jaeger.version", "Java-0.26.0");
        processTagsMap.put("ip", "127.0.1.1");
        testAspect(eventAspects.get("Process tags"), event, processTagsMap);

        Map<@NonNull String, @NonNull Object> tagsMap = new HashMap<>();
        tagsMap.put("sampler.type", "const");
        tagsMap.put("hello-to", "Kath");
        tagsMap.put("sampler.param", "true");
        testAspect(eventAspects.get("Tags"), event, tagsMap);
    }

    private static void testAspect(ITmfEventAspect<?> eventAspect, ITmfEvent event, Object expected) {
        assertNotNull(event);
        assertNotNull(eventAspect);
        assertEquals(expected, eventAspect.resolve(event));
    }

    /**
     * Test a simple in order json trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testSimple() throws TmfTraceException {
        String path = "traces/simple.json";
        int nbEvents = 3;
        Double start = 1526674498419000.0;
        Double end = 1526674498436163.0;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(start.longValue());
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(end.longValue());
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test a multiple services trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testMultipleServices() throws TmfTraceException {
        String path = "traces/multiple_services.json";
        int nbEvents = 5;
        Double start = 1527684461617000.0;
        Double end = 1527684462133362.0;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(start.longValue());
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(end.longValue());
        testTrace(path, nbEvents, startTime, endTime);
    }

    private static Map<String, String> testTrace(String path, int nbEvents, ITmfTimestamp startTime, ITmfTimestamp endTime) throws TmfTraceException {
        ITmfTrace trace = new OpenTracingTrace();
        try {
            IStatus validate = trace.validate(null, path);
            assertTrue(validate.getMessage(), validate.isOK());
            trace.initTrace(null, path, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            long count = 0;
            long prevTs = -1;
            while (event != null) {
                count++;
                @NonNull
                ITmfTimestamp currentTime = event.getTimestamp();
                assertNotNull("Event has a timestamp", currentTime);
                assertTrue("Monotonic events", currentTime.toNanos() >= prevTs);
                prevTs = currentTime.toNanos();
                event = trace.getNext(context);
            }
            assertEquals(nbEvents, count);
            assertEquals(nbEvents, trace.getNbEvents());
            assertEquals(startTime.toNanos(), trace.getStartTime().toNanos());
            assertEquals(endTime.toNanos(), trace.getEndTime().toNanos());
            assertTrue(trace instanceof ITmfPropertiesProvider);
            return ((ITmfPropertiesProvider) trace).getProperties();
        } finally {
            trace.dispose();
        }
    }

    private static Map<String, ITmfEventAspect<?>> getEventAspects(String path) {
        ITmfTrace trace = new OpenTracingTrace();
        Map<String, ITmfEventAspect<?>> returnValues = new HashMap<>();
        for (@NonNull
        ITmfEventAspect<?> aspect : trace.getEventAspects()) {
            returnValues.put(aspect.getName(), aspect);
        }
        trace.dispose();
        return returnValues;
    }

    private static ITmfEvent getFirstEvent(String path) throws TmfTraceException {
        ITmfTrace trace = new OpenTracingTrace();
        try {
            trace.initTrace(null, path, ITmfEvent.class);
            return trace.getNext(trace.seekEvent(0.0));
        } finally {
            trace.dispose();
        }
    }
}
