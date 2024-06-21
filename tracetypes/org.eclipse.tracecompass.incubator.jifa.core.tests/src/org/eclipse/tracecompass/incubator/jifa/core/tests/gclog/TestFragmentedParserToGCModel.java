/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.jifa.core.tests.gclog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment.GCLogAnalyzer;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParserFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.junit.Assert;
import org.junit.Test;

/*
This test aims the same as TestFragmentedParserToGCModelLegacy.
Instead of checking each field with so many Assert.assertEquals, this test barely compares both GC Models
produced by normal parser and fragmented parser. However, considering the large amount of fields in GC Event,
compareGCModel() only takes fields related with Metric into account.
Also, gclog of jdk11 and 17 and 8 with serialGC are excluded for the time being. Furthermore, by design,
IncompleteGCLog and 11_CMS gclogs are not supported.
 */

public class TestFragmentedParserToGCModel {
    public static final double DELTA = 1e-6;

    @Test
    public void test() throws Exception {
        Set<String> gcLogSet = Set.of("11G1Parser.log",
                        "8CMSParser.log", "8CMSPrintGC.log", "8G1PrintGC.log", "8ParallelGCParser.log", "8G1GCParser.log",
                        "8G1GCParserAdaptiveSize.log", "8ConcurrentPrintDateTimeStamp.log",
                        "8CMSCPUTime.log", "8CMSPromotionFailed.log",
                        "8CMSScavengeBeforeRemark.log", "8GenerationalGCInterleave.log");
        for (String gclog : gcLogSet) {
            compareGCModel(parseByNormalParser(gclog), parseByFragmentedParser(TestUtil.generateShuffledGCLog(gclog)));
        }
    }

    private static void compareGCModel(GCModel left, GCModel right) {
        assertEquals(left.getGcEvents().size(), right.getGcEvents().size());
        for (int i = 0; i < left.getGcEvents().size(); i++) {
            GCEvent l = left.getGcEvents().get(i), r = right.getGcEvents().get(i);
            assertEquals(l.getGcid(), r.getGcid());
            assertEquals(l.getStartTime(), r.getStartTime(), DELTA);
            assertEquals(l.getDuration(), r.getDuration(), DELTA);
            assertEquals(l.getPromotion(), r.getPromotion());
            assertEquals(l.getPause(), r.getPause(), DELTA);
            assertEquals(l.toString(), r.toString());
            compareCpuTime(l, r);
            compareCause(l, r);
            compareEventType(l, r);
            compareMemoryArea(l, r);
            compareSubPhase(l, r);
            compareReferenceGC(l, r);
        }
    }

    private static void compareSubPhase(GCEvent l, GCEvent r) {
        if (l.getPhases() == null) {
            Assert.assertNull(r.getPhases());
            return;
        }
        assertEquals(l.getPhases().size(), r.getPhases().size());
        for (GCEvent gcEvent : l.getPhases()) {
            boolean find = false;
            for (GCEvent another : r.getPhases()) {
                if (gcEvent.getEventType().getName().equals(another.getEventType().getName())) {
                    find = true;
                    assertEquals(gcEvent.getDuration(), another.getDuration(), DELTA);
                    break;
                }
            }
            Assert.assertTrue(find);
        }
    }

    private static void compareMemoryArea(GCEvent l, GCEvent r) {
        for (MemoryArea memoryArea : new MemoryArea[]{MemoryArea.YOUNG, MemoryArea.OLD, MemoryArea.HEAP, MemoryArea.METASPACE}) {
            if (l.getMemoryItem(memoryArea) == null) {
                Assert.assertNull(r.getMemoryItem(memoryArea));
                continue;
            }
            assertEquals(l.getMemoryItem(memoryArea), r.getMemoryItem(memoryArea));
        }
    }

    private static void compareReferenceGC(GCEvent l, GCEvent r) {
        if (l.getReferenceGC() == null) {
            assertNull(r.getReferenceGC());
            return;
        }
        assertEquals(l.getReferenceGC().getSoftReferenceCount(), r.getReferenceGC().getSoftReferenceCount());
        assertEquals(l.getReferenceGC().getSoftReferencePauseTime(), r.getReferenceGC().getSoftReferencePauseTime(), DELTA);
        assertEquals(l.getReferenceGC().getWeakReferenceCount(), r.getReferenceGC().getWeakReferenceCount());
        assertEquals(l.getReferenceGC().getWeakReferencePauseTime(), r.getReferenceGC().getWeakReferencePauseTime(), DELTA);
        assertEquals(l.getReferenceGC().getFinalReferenceCount(), r.getReferenceGC().getFinalReferenceCount());
        assertEquals(l.getReferenceGC().getFinalReferencePauseTime(), r.getReferenceGC().getFinalReferencePauseTime(), DELTA);
        assertEquals(l.getReferenceGC().getPhantomReferenceCount(), r.getReferenceGC().getPhantomReferenceCount());
        assertEquals(l.getReferenceGC().getPhantomReferencePauseTime(), r.getReferenceGC().getPhantomReferencePauseTime(), DELTA);
        assertEquals(l.getReferenceGC().getPhantomReferenceFreedCount(), r.getReferenceGC().getPhantomReferenceFreedCount());
        assertEquals(l.getReferenceGC().getJniWeakReferencePauseTime(), r.getReferenceGC().getJniWeakReferencePauseTime(), DELTA);
    }

    private static void compareCpuTime(GCEvent l, GCEvent r) {
        if (l.getCpuTime() == null) {
            assertNull(r.getCpuTime());
            return;
        }
        assertEquals(l.getCpuTime().toString(), r.getCpuTime().toString());
    }

    private static void compareCause(GCEvent l, GCEvent r) {
        if (l.getCause() == null) {
            assertNull(r.getCause());
            return;
        }
        assertEquals(l.getCause().getName(), r.getCause().getName());
    }

    private static void compareEventType(GCEvent l, GCEvent r) {
        if (l.getEventType() == null) {
            assertNull(r.getEventType());
            return;
        }
        assertEquals(l.getEventType().getName(), r.getEventType().getName());
    }

    private static GCModel parseByFragmentedParser(List<String> context) {
        return new GCLogAnalyzer().parseToGCModel(context, new HashMap<String, String>());
    }

    @SuppressWarnings("resource")
    private static GCModel parseByNormalParser(String name) throws TmfTraceException, IOException {
        GCLogParser gcLogParser = new GCLogParserFactory().getParser(TestUtil.getGCLog(name));
        GCModel model = gcLogParser.parse(TestUtil.getGCLog(name));
        model.calculateDerivedInfo(new NullProgressMonitor());
        return model;
    }
}
