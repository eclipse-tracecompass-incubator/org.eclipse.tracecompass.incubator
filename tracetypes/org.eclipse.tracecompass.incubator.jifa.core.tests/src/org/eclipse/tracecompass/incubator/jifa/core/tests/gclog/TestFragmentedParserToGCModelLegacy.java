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

import java.util.HashMap;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.Safepoint;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCCause;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventBooleanType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCMemoryItem;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment.GCLogAnalyzer;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.CMSGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.G1GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.ParallelGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.PauseStatistics;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test borrowed Assert from TestParser, and expects fragment.GCLogAnalyzer
 * to produce the same result as normal GCLogAnalyzer. Be advised that Assert
 * about GCEvent.getPhases are excluded, since fragment.GCLogAnalyzer doesn't
 * guarantee the same order. Also, gclog of jdk11 and 17 and 8 with serialGC are
 * excluded for the time being.
 */
public class TestFragmentedParserToGCModelLegacy {
    public static final double DELTA = 1e-6;
    private static final double UNKNOWN_DOUBLE = Constant.UNKNOWN_DOUBLE;

    private static GCModel parse(List<String> context) {
        return new GCLogAnalyzer().parseToGCModel(context, new HashMap<String, String>());
    }

    @Test
    public void testJDK11CMSGCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("11CMSGCParser.log");
        Assert.assertEquals(log.size(), 38);

        CMSGCModel model = (CMSGCModel) parse(log);
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getParallelThread(), 8);
        Assert.assertEquals(model.getConcurrentThread(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 479, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 31.208, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 69952 * 1024, 8703 * 1024, 78656 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, 24072 * 1024, 174784 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 6531 * 1024, 6530 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 30, DELTA);

        GCEvent cms = model.getGcEvents().get(1);
        Assert.assertEquals(cms.getGcid(), 1);
        Assert.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assert.assertEquals(cms.getStartTime(), 3231, DELTA);
        Assert.assertEquals(cms.getPhases().size(), 6);

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getStartTime(), 8970, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 178.617, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 174 * 1024 * 1024, 166 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 180, DELTA);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
    }

    @Test
    public void testJDK11G1Parser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("11G1Parser.log");
        Assert.assertEquals(log.size(), 64);

        G1GCModel model = (G1GCModel) parse(log);

        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 3);

        // assert model info
        Assert.assertEquals(model.getStartTime(), 0.0 * 1000, DELTA);
        Assert.assertEquals(model.getEndTime(), 7.123 * 1000, DELTA);
        Assert.assertEquals(model.getCollectorType(), GCCollectorType.G1);

        Assert.assertEquals(model.getHeapRegionSize(), 1024 * 1024);
        Assert.assertNull(model.getVmOptions());
        Assert.assertEquals(model.getParallelThread(), 8);
        Assert.assertEquals(model.getConcurrentThread(), 2);

        // assert events correct
        Assert.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assert.assertEquals(safepoint.getStartTime(), 1010 - 10.1229, DELTA);
        Assert.assertEquals(safepoint.getDuration(), 10.1229, DELTA);
        Assert.assertEquals(safepoint.getTimeToEnter(), 0.0077, DELTA);

        List<GCEvent> event = model.getGcEvents();
        GCEvent youngGC = event.get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertTrue(youngGC.isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED));
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getStartTime(), 1.0 * 1000, DELTA);
        Assert.assertEquals(youngGC.getPause(), 10.709, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.709, DELTA);
        Assert.assertEquals(youngGC.getCause(), GCCause.METADATA_GENERATION_THRESHOLD);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 0.01 * 1000, DELTA);
        Assert.assertEquals(youngGC.getPhases().size(), 4);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 0, 3 * 1024 * 1024, 3 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 20679 * 1024, 20679 * 1024, 45056 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 19 * 1024 * 1024, 4 * 1024 * 1024, 64 * 1024 * 1024));
        Assert.assertTrue(youngGC.toString().contains("To-space Exhausted"));

        GCEvent concurrentMark = event.get(1);
        Assert.assertEquals(concurrentMark.getGcid(), 1);
        Assert.assertEquals(concurrentMark.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(concurrentMark.getDuration(), 14.256, DELTA);
        Assert.assertEquals(concurrentMark.getPause(), 2.381 + 0.094, DELTA);

        GCEvent fullGC = event.get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getStartTime(), 7.055 * 1000, DELTA);
        Assert.assertEquals(fullGC.getPause(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertEquals(fullGC.getPhases().size(), 4);

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK8CMSParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSParser.log");
        Assert.assertEquals(log.size(), 30);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 10);
        Assert.assertEquals(model.getSafepoints().size(), 1);

        Safepoint safepoint = model.getSafepoints().get(0);
        Assert.assertEquals(safepoint.getStartTime(), 675110 - 0.1215, DELTA);
        Assert.assertEquals(safepoint.getDuration(), 0.1215, DELTA);
        Assert.assertEquals(safepoint.getTimeToEnter(), 0.0271, DELTA);

        GCEvent fullgc = model.getGcEvents().get(0);
        Assert.assertEquals(fullgc.getStartTime(), 610956, DELTA);
        Assert.assertEquals(fullgc.getDuration(), 1027.7002, DELTA);
        Assert.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullgc.getCause(), GCCause.HEAP_DUMP);
        Assert.assertEquals(fullgc.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 114217 * 1024, 113775 * 1024, 1153024 * 1024));
        Assert.assertEquals(fullgc.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 1537414 * 1024, 1388294 * 1024, 4915200L * 1024));
        Assert.assertEquals(fullgc.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 324459 * 1024, 175339 * 1024, 3072000L * 1024));
        Assert.assertEquals(fullgc.getPhases().size(), 4);
        Assert.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getStartTime(), 611637, DELTA);
        Assert.assertEquals(fullgc.getLastPhaseOfType(GCEventType.WEAK_REFS_PROCESSING).getDuration(), 1.8945, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getUser(), 1710, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getSys(), 50, DELTA);
        Assert.assertEquals(fullgc.getCpuTime().getReal(), 1030, DELTA);

        fullgc = model.getGcEvents().get(8);
        Assert.assertEquals(fullgc.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullgc.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 3956586L * 1024, 1051300 * 1024, 4019584L * 1024));

        GCEvent youngGC = model.getGcEvents().get(9);
        Assert.assertEquals(youngGC.getStartTime(), 813396, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.5137, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 69952 * 1024, 11354 * 1024, 253440 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assert.assertNull(youngGC.getPhases());
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 813404, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferencePauseTime(), 0.0260, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceCount(), 4);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferenceStartTime(), 813405, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferencePauseTime(), 0.0110, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getWeakReferenceCount(), 59);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferenceStartTime(), 813406, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferencePauseTime(), 2.5979, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getFinalReferenceCount(), 1407);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceStartTime(), 813407, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferencePauseTime(), 0.0131, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceCount(), 11);
        Assert.assertEquals(youngGC.getReferenceGC().getPhantomReferenceFreedCount(), 10);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferenceStartTime(), 813408, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0088, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 40, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent cms = model.getGcEvents().get(2);
        Assert.assertEquals(cms.getEventType(), GCEventType.CMS_CONCURRENT_MARK_SWEPT);
        Assert.assertEquals(cms.getStartTime(), 675164, DELTA);
        Assert.assertEquals(cms.getPhases().size(), 12, DELTA);
        for (GCEvent phase : cms.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 675164, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 34415, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 154390, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 770, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 431.5, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 237, DELTA);
    }

    @Test
    public void testJDK8CMSCPUTime() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSCPUTime.log");
        Assert.assertEquals(log.size(), 12);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertNotNull(model);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_INITIAL_MARK).getCpuTime().getReal(), 130, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 10, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_PRECLEAN).getCpuTime().getReal(), 30, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_ABORTABLE_PRECLEAN).getCpuTime().getReal(), 4650, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getReal(), 30, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_SWEEP).getCpuTime().getReal(), 20, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_CONCURRENT_RESET).getCpuTime().getReal(), 40, DELTA);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.YOUNG_GC).getCpuTime().getReal(), 50, DELTA);
    }

    @Test
    public void TestJDK8CMSPromotionFailed() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSPromotionFailed.log");
        Assert.assertEquals(log.size(), 1);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 7689600L * 1024, 0, 7689600L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 9258265L * 1024, 5393434L * 1024, 12582912L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 16878013L * 1024, 5393434L * 1024, 20272512L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 208055 * 1024, 203568 * 1024, 1253376 * 1024));
        Assert.assertEquals(fullGC.getCause(), GCCause.PROMOTION_FAILED);
        Assert.assertTrue(fullGC.isTrue(GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC));
    }

    @Test
    public void TestJDK8CMSScavengeBeforeRemark() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSScavengeBeforeRemark.log");
        Assert.assertEquals(log.size(), 14);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 830, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 1922431L * 1024L, 174720L * 1024L, 1922432L * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 1056998L * 1024L, 151245L * 1024L, 1922432L * 1024L));
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 490, DELTA);
        Assert.assertEquals(youngGC.getCause(), GCCause.CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assert.assertEquals(pause.getPauseAvg(), (226.6431 + 362.9243 + 293.1600) / 3.0, DELTA);
    }

    @Test
    public void testJDK8CMSPrintGC() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8CMSPrintGC.log");
        Assert.assertEquals(log.size(), 14);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 3);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 4);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 3);
        for (GCEvent event : model.getGcEvents()) {
            Assert.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assert.assertTrue(event.getDuration() > 0);
                Assert.assertNotNull(event.getCause());
                Assert.assertNotNull(event.getMemoryItem(MemoryArea.HEAP));
            }
            if (event.isOldGC()) {
                Assert.assertEquals(2, event.getPhases().size());
            }
        }
    }

    @Test
    public void testJDK8G1GCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8G1GCParser.log");
        Assert.assertEquals(log.size(), 1);

        G1GCModel model = (G1GCModel) parse(log);

        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 4);
        Assert.assertEquals(model.getParallelThread(), 4);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getStartTime(), 3960, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 56.3085, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 184 * 1024 * 1024, 3800L * 1024 * 1024, (int) (19.3 * 1024 * 1024), 3800L * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 184 * 1024 * 1024, 184 * 1024 * 1024, 0, 160 * 1024 * 1024));
        Assert.assertNotNull(youngGC.getPhases());
        for (GCEvent phase : youngGC.getPhases()) {
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_GC_REFPROC).getDuration(), 15.1, DELTA);
        Assert.assertEquals(youngGC.getLastPhaseOfType(GCEventType.G1_CODE_ROOT_SCANNING).getDuration(), 0.5, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getSoftReferenceStartTime(), 4000, DELTA);
        Assert.assertEquals(youngGC.getReferenceGC().getJniWeakReferencePauseTime(), 0.0057, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getSys(), 10, DELTA);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 60, DELTA);

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assert.assertEquals(concurrentCycle.getStartTime(), 4230, DELTA);
        Assert.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            if (phase.getEventType() != GCEventType.G1_CONCURRENT_MARK_RESET_FOR_OVERFLOW) {
                Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            }
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getStartTime(), 4230, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS).getDuration(), 160.8430, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getStartTime(), 19078, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getDuration(), 478.5858, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getMemoryItem(MemoryArea.HEAP).getPostUsed(), 9863L * 1024 * 1024, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_REMARK).getCpuTime().getUser(), 1470, DELTA);
        Assert.assertEquals(concurrentCycle.getLastPhaseOfType(GCEventType.G1_PAUSE_CLEANUP).getCpuTime().getSys(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getStartTime(), 23346, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 1924.2692, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.METADATA_GENERATION_THRESHOLD);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 1792694 * 1024, 291615 * 1024, 698368 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, (long) (7521.7 * 1024 * 1024), (long) (46144.0 * 1024 * 1024), (long) (7002.8 * 1024 * 1024), (long) (46144.0 * 1024 * 1024)));
        Assert.assertEquals(fullGC.getCpuTime().getUser(), 2090, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getSys(), 190, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 1920, DELTA);

        GCEvent mixedGC = model.getGcEvents().get(3);
        Assert.assertEquals(mixedGC.getStartTime(), 79619, DELTA);
        Assert.assertEquals(mixedGC.getDuration(), 26.4971, DELTA);
        Assert.assertEquals(mixedGC.getEventType(), GCEventType.G1_MIXED_GC);
        Assert.assertEquals(mixedGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertTrue(mixedGC.isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED));
        Assert.assertEquals(mixedGC.getMemoryItem(MemoryArea.HEAP).getPostCapacity(), (long) (19.8 * 1024 * 1024 * 1024));
        Assert.assertEquals(mixedGC.getMemoryItem(MemoryArea.EDEN).getPreUsed(), 2304L * 1024 * 1024);
        Assert.assertNotNull(mixedGC.getPhases());
        for (GCEvent phase : mixedGC.getPhases()) {
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK8G1GCParserAdaptiveSize() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8G1GCParserAdaptiveSize.log");
        Assert.assertEquals(log.size(), 1);

        G1GCModel model = (G1GCModel) parse(log);

        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getStartTime(), 683, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 8.5898, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP).getPreUsed(), 52224 * 1024);
    }

    @Test
    public void testJDK8G1PrintGC() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8G1PrintGC.log");
        Assert.assertEquals(log.size(), 22);

        G1GCModel model = (G1GCModel) parse(log);

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 8);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 2);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 1);
        Assert.assertEquals(model.getGcEvents().stream().filter(event -> event.isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED)).count(), 2);
        for (GCEvent event : model.getGcEvents()) {
            Assert.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assert.assertTrue(event.getDuration() > 0);
                Assert.assertNotNull(event.getCause());
                Assert.assertNotNull(event.getMemoryItem(MemoryArea.HEAP));
            }
            if (event.isOldGC()) {
                Assert.assertTrue(event.getPhases().size() >= 2);
            }
        }
    }

    @Test
    public void testJDK8ParallelGCParser() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8ParallelGCParser.log");
        Assert.assertEquals(log.size(), 6);

        ParallelGCModel model = (ParallelGCModel) parse(log);

        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 6);

        GCEvent youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getStartTime(), 962, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 46.2864, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(MemoryArea.YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 51200 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 4096 * 1024);
        Assert.assertEquals(youngGen.getPostCapacity(), 77824 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(MemoryArea.HEAP);
        Assert.assertEquals(total.getPreUsed(), 118572 * 1024);
        Assert.assertEquals(total.getPostUsed(), 117625 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 252416 * 1024);
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 290, DELTA);

        GCEvent fullGC = model.getGcEvents().get(5);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ERGONOMICS);
        Assert.assertEquals(fullGC.getStartTime(), 14608, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 4.6781, DELTA);
        youngGen = fullGC.getMemoryItem(MemoryArea.YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 65530 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 0);
        Assert.assertEquals(youngGen.getPostCapacity(), 113664 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(MemoryArea.OLD);
        Assert.assertEquals(oldGen.getPreUsed(), 341228 * 1024);
        Assert.assertEquals(oldGen.getPostUsed(), 720 * 1024);
        Assert.assertEquals(oldGen.getPostCapacity(), 302592 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(MemoryArea.METASPACE);
        Assert.assertEquals(metaspace.getPreUsed(), 3740 * 1024);
        Assert.assertEquals(metaspace.getPostUsed(), 3737 * 1024);
        Assert.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(MemoryArea.HEAP);
        Assert.assertEquals(total.getPreUsed(), 406759 * 1024);
        Assert.assertEquals(total.getPostUsed(), 720 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 416256 * 1024);
        Assert.assertEquals(fullGC.getCpuTime().getUser(), 20, DELTA);
    }

    @Test
    public void testJDK8GenerationalGCInterleave() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8GenerationalGCInterleave.log");
        Assert.assertEquals(log.size(), 1);

        GCModel model = parse(log);

        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getStartTime(), 61988328, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 2016.0411, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 2621440L * 1024, 0, 2883584L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 1341593L * 1024, 1329988L * 1024, 2097152L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 3963033L * 1024, 1329988L * 1024, 4980736L * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 310050L * 1024, 309844L * 1024, 1343488L * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 2010, DELTA);
    }

    @Test
    public void testJDK8ConcurrentPrintDateTimeStamp() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("8ConcurrentPrintDateTimeStamp.log");
        Assert.assertEquals(log.size(), 7);

        G1GCModel model = (G1GCModel) parse(log);

        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getGcEvents().get(0).getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(model.getGcEvents().get(1).getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getStartTime(), 725081, DELTA);
        Assert.assertEquals(model.getGcEvents().get(2).getEventType(), GCEventType.YOUNG_GC);
    }

    @Test
    public void TestIncompleteGCLog() throws Exception {
        List<String> log = TestUtil.generateShuffledGCLog("IncompleteGCLog.log");
        Assert.assertEquals(log.size(), 30);

        CMSGCModel model = (CMSGCModel) parse(log);

        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 2);
        Assert.assertEquals(model.getAllEvents().size(), 8);
    }
}
