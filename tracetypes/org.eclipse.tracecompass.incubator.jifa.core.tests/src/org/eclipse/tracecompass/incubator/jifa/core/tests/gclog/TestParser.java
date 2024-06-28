/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.Safepoint;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.ThreadEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCCause;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventBooleanType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCMemoryItem;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.CMSGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.G1GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.ParallelGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.SerialGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.ZGCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCLogMetadata;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParserFactory;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.GCLogParsingMetadata;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.PreUnifiedG1GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.PreUnifiedGenerationalGCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.UnifiedG1GCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.UnifiedGenerationalGCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.parser.UnifiedZGCLogParser;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.PauseStatistics;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.TimeRange;
import org.junit.Assert;
import org.junit.Test;

public class TestParser {

    public static final double DELTA = 1e-6;
    private static final double UNKNOWN_DOUBLE = Constant.UNKNOWN_DOUBLE;
    private static final long UNKNOWN_INT = Constant.UNKNOWN_INT;

    @Test
    public void testJDK11G1Parser() throws Exception {
        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11G1Parser.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1Parser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
        Assert.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_COLLECT_EVACUATION);
        Assert.assertEquals(youngGC.getPhases().get(1).getDuration(), 9.5, DELTA);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 0 * 1024, 3 * 1024 * 1024, 3 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 20679 * 1024, 20679 * 1024, 45056 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 19 * 1024 * 1024, 4 * 1024 * 1024, 64 * 1024 * 1024));
        Assert.assertTrue(youngGC.toString().contains("To-space Exhausted"));

        GCEvent concurrentMark = event.get(1);
        Assert.assertEquals(concurrentMark.getGcid(), 1);
        Assert.assertEquals(concurrentMark.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(concurrentMark.getDuration(), 14.256, DELTA);
        Assert.assertEquals(concurrentMark.getPause(), 2.381 + 0.094, DELTA);
        Assert.assertEquals(concurrentMark.getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_CLEAR_CLAIMED_MARKS);
        Assert.assertEquals(concurrentMark.getPhases().get(0).getDuration(), 0.057, DELTA);

        GCEvent fullGC = event.get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getStartTime(), 7.055 * 1000, DELTA);
        Assert.assertEquals(fullGC.getPause(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 67.806, DELTA);
        Assert.assertEquals(fullGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        Assert.assertEquals(fullGC.getPhases().get(3).getEventType(), GCEventType.G1_COMPACT_HEAP);
        Assert.assertEquals(fullGC.getPhases().get(3).getDuration(), 57.656, DELTA);

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertFalse(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK11G1ParserDetectHeapRegionSize() throws Exception {
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1ParserDetectHeapRegionSize.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        // should infer region size 16m
        Assert.assertEquals(model.getHeapRegionSize(), 16 * 1024 * 1024);
        Assert.assertEquals(model.getGcEvents().get(0).getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 32 * 16 * 1024 * 1024, 37 * 16 * 1024 * 1024, 1952L * 1024 * 1024));
        Assert.assertEquals(model.getGcEvents().get(0).getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 21709 * 1024, 21707 * 1024, 1069056 * 1024));
    }

    @Test
    public void testJDK11ParseDecoration() throws Exception {
        String log = "[2021-05-06T11:25:16.508+0800][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[2021-05-06T11:25:16.510+0800][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        UnifiedG1GCLogParser parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        GCModel model = parser.parse(TestUtil.stringToBufferedReader(log));
        Assert.assertEquals(model.getReferenceTimestamp(), 1620271516508d, DELTA);
        Assert.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);

        log = "[1000000000800ms][info][gc           ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold)\n" +
                "[1000000000802ms][info][gc           ] GC(1) Pause Young (Concurrent Start) (Metadata GC Threshold)\n";
        parser = new UnifiedG1GCLogParser();
        parser.setMetadata(new GCLogParsingMetadata(GCCollectorType.G1, GCLogStyle.UNIFIED));
        model = parser.parse(TestUtil.stringToBufferedReader(log));
        Assert.assertEquals(model.getReferenceTimestamp(), 1000000000800D, DELTA);
        Assert.assertEquals(model.getGcEvents().get(1).getStartTime(), 2, DELTA);
    }

    @Test
    public void testJDK11ZGCParser() throws Exception {
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11ZGCParser.log")));
        ZGCModel model = (ZGCModel) parser.parse(TestUtil.getGCLog("11ZGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent gc = model.getGcEvents().get(0);
        Assert.assertEquals(gc.getGcid(), 374);
        Assert.assertEquals(gc.getStartTime(), 7000, DELTA);
        Assert.assertEquals(gc.getEndTime(), 7356, DELTA);
        Assert.assertEquals(gc.getDuration(), 356, DELTA);
        Assert.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assert.assertEquals(gc.getCause(), GCCause.PROACTIVE);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getEndTime(), 7006, DELTA);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 4.459, DELTA);
        Assert.assertEquals(gc.getMemoryItem(MemoryArea.METASPACE).getPostCapacity(), 128 * 1024 * 1024);
        Assert.assertEquals(gc.getMemoryItem(MemoryArea.METASPACE).getPostUsed(), 125 * 1024 * 1024);
        Assert.assertEquals(gc.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 5614L * 1024 * 1024, 40960L * 1024 * 1024, 1454 * 1024 * 1024, 40960L * 1024 * 1024));
        Assert.assertEquals(gc.getAllocation(), 202 * 1024 * 1024);
        Assert.assertEquals(gc.getReclamation(), 4200L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assert.assertEquals(statistics.size(), 1);
        Assert.assertEquals(72, statistics.get(0).getStatisticItems().size());
        Assert.assertEquals(statistics.get(0).getStartTime(), 7555, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 911, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 913, DELTA);
        List<GCEvent> allocationStalls = model.getAllocationStalls();
        Assert.assertEquals(allocationStalls.size(), 2);
        Assert.assertEquals(allocationStalls.get(1).getEndTime(), 7888, DELTA);
        Assert.assertEquals(allocationStalls.get(1).getDuration(), 0.391, DELTA);
        Assert.assertEquals(((ThreadEvent) (allocationStalls.get(1))).getThreadName(), "NioProcessor-2");

        Assert.assertEquals(model.getOoms().size(), 1);
        ThreadEvent oom = model.getOoms().get(0);
        Assert.assertEquals(oom.getStartTime(), 7889, DELTA);
        Assert.assertEquals(oom.getThreadName(), "thread 8");
    }

    @Test
    public void testJDK8CMSParser() throws Exception {
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSParser.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSCPUTime.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSCPUTime.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
    public void testJDK8G1GCParser() throws Exception {
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1GCParser.log")));

        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1GCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
        // although we don't read anything from options like
        // -XX:+PrintAdaptiveSizePolicy, they should not
        // affect parsing
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1GCParserAdaptiveSize.log")));

        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1GCParserAdaptiveSize.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
    public void testJDK11SerialGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11SerialGCParser.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("11SerialGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 486, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 25.164, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 69952 * 1024, 8704 * 1024, 78656 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, 24185 * 1024, 174784 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 6529 * 1024, 6519 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 68 * 1024 * 1024, 32 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 5614, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 146.617, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 26.097, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.259, DELTA);
    }

    @Test
    public void testJDK11ParallelGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11ParallelGCParser.log")));

        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("11ParallelGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 2);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 455, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 11.081, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 65536 * 1024, 10720 * 1024, 76288 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, 20800 * 1024, 175104 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 6531 * 1024, 6531 * 1024, 1056768 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 64 * 1024 * 1024, 30 * 1024 * 1024, 245 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 2836, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 46.539, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ERGONOMICS);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 10729 * 1024, 0, 76288 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 141664 * 1024, 94858 * 1024, 175104 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 7459 * 1024, 7459 * 1024, 1056768 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 148 * 1024 * 1024, 92 * 1024 * 1024, 245 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 22.465, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 50, DELTA);
    }

    @Test
    public void testJDK11CMSGCParser() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSGCParser.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
        for (GCEvent phase : cms.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
            Assert.assertNotNull(phase.getCpuTime());
            if (phase.getEventType() == GCEventType.CMS_INITIAL_MARK || phase.getEventType() == GCEventType.CMS_FINAL_REMARK) {
                Assert.assertNotNull(phase.getMemoryItem(MemoryArea.HEAP));
            }
        }
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_INITIAL_MARK).getStartTime(), 3231, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getDuration(), 22.229, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_MARK).getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 20, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_FINAL_REMARK).getDuration(), 1.991, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_RESET).getDuration(), 0.386, DELTA);
        Assert.assertEquals(cms.getLastPhaseOfType(GCEventType.CMS_CONCURRENT_SWEEP).getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 142662 * 1024, 92308 * 1024, 174784 * 1024));

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getStartTime(), 8970, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 178.617, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 174 * 1024 * 1024, 166 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 180, DELTA);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.761, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 28.069, DELTA);
    }

    @Test
    public void testJDK8ParallelGCParser() throws Exception {
        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8ParallelGCParser.log")));

        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("8ParallelGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
    public void testJDK8SerialGCParser() throws Exception {

        PreUnifiedGenerationalGCLogParser parser = (PreUnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8SerialGCParser.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("8SerialGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 8);
        Assert.assertEquals(model.getReferenceTimestamp(), 1638847091688.0, DELTA);

        GCEvent youngGC = model.getGcEvents().get(1);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getStartTime(), 68, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 70.1086, DELTA);
        GCMemoryItem youngGen = youngGC.getMemoryItem(MemoryArea.YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 78656 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 8703 * 1024);
        Assert.assertEquals(youngGen.getPostCapacity(), 78656 * 1024);
        GCMemoryItem total = youngGC.getMemoryItem(MemoryArea.HEAP);
        Assert.assertEquals(total.getPreUsed(), 126740 * 1024);
        Assert.assertEquals(total.getPostUsed(), 114869 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 253440 * 1024);
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 70, DELTA);

        GCEvent fullGC = model.getGcEvents().get(6);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getStartTime(), 1472, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 1095.987, DELTA);
        youngGen = fullGC.getMemoryItem(MemoryArea.YOUNG);
        Assert.assertEquals(youngGen.getPreUsed(), 271999 * 1024);
        Assert.assertEquals(youngGen.getPostUsed(), 0);
        Assert.assertEquals(youngGen.getPostCapacity(), 272000 * 1024);
        GCMemoryItem oldGen = fullGC.getMemoryItem(MemoryArea.OLD);
        Assert.assertEquals(oldGen.getPreUsed(), 785946 * 1024);
        Assert.assertEquals(oldGen.getPostUsed(), 756062 * 1024);
        Assert.assertEquals(oldGen.getPostCapacity(), 786120 * 1024);
        GCMemoryItem metaspace = fullGC.getMemoryItem(MemoryArea.METASPACE);
        Assert.assertEquals(metaspace.getPreUsed(), 3782 * 1024);
        Assert.assertEquals(metaspace.getPostUsed(), 3782 * 1024);
        Assert.assertEquals(metaspace.getPostCapacity(), 1056768 * 1024);
        total = fullGC.getMemoryItem(MemoryArea.HEAP);
        Assert.assertEquals(total.getPreUsed(), 823069 * 1024);
        Assert.assertEquals(total.getPostUsed(), 756062 * 1024);
        Assert.assertEquals(total.getPostCapacity(), 1058120 * 1024);
        Assert.assertEquals(fullGC.getCpuTime().getSys(), 70, DELTA);
    }

    @Test
    public void testJDK8GenerationalGCInterleave() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8GenerationalGCInterleave.log"));
        GCModel model = parser.parse(TestUtil.getGCLog("8GenerationalGCInterleave.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
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
    public void testJDK11GenerationalGCInterleave() throws Exception {

        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("11GenerationalGCInterleave.log")));

        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("11GenerationalGCInterleave.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        GCEvent fullGC = model.getGcEvents().get(0);
        Assert.assertEquals(fullGC.getGcid(), 3);
        Assert.assertEquals(fullGC.getStartTime(), 5643, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 146.211, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 78655 * 1024, 0, 78656 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 142112 * 1024, 135957 * 1024, 174784 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 215 * 1024 * 1024, 132 * 1024 * 1024, 247 * 1024 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 7462 * 1024, 7462 * 1024, 1056768 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 24.314, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 17.974, DELTA);
        Assert.assertTrue(fullGC.isTrue(GCEventBooleanType.YOUNG_GC_BECOME_FULL_GC));
    }

    @Test
    public void TestIncompleteGCLog() throws Exception {
        UnifiedGenerationalGCLogParser parser = (UnifiedGenerationalGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("IncompleteGCLog.log")));

        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("IncompleteGCLog.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 2);
        Assert.assertEquals(model.getAllEvents().size(), 8);
    }

    @Test
    public void testJDK8ConcurrentPrintDateTimeStamp() throws Exception {
        /*
         * This test mainly test the line below. The format here is: [DateStamp]
         * [DateStamp] [TimeStamp] [TimeStamp] [Safepoint] [Concurrent cycle
         * phase]
         */
        PreUnifiedG1GCLogParser parser = (PreUnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("8ConcurrentPrintDateTimeStamp.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8ConcurrentPrintDateTimeStamp.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getGcEvents().get(0).getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(model.getGcEvents().get(1).getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getEventType(), GCEventType.G1_CONCURRENT_SCAN_ROOT_REGIONS);
        Assert.assertEquals(model.getGcEvents().get(1).getPhases().get(0).getStartTime(), 725081, DELTA);
        Assert.assertEquals(model.getGcEvents().get(2).getEventType(), GCEventType.YOUNG_GC);
    }

    @Test
    public void TestJDK8CMSPromotionFailed() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSPromotionFailed.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSPromotionFailed.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

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
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSScavengeBeforeRemark.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSScavengeBeforeRemark.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

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
    public void TestJDK11CMSScavengeBeforeRemark() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSScavengeBeforeRemark.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSScavengeBeforeRemark.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        Assert.assertEquals(model.getGcEvents().size(), 3);
        Assert.assertEquals(model.getLastEventOfType(GCEventType.CMS_FINAL_REMARK).getCpuTime().getUser(), 90, DELTA);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 46079 * 1024L, 5120 * 1024L, 46080 * 1024L));

        youngGC = model.getGcEvents().get(2);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 5923 * 1024L, 5921 * 1024L, 46080 * 1024L));
        Assert.assertEquals(youngGC.getCpuTime().getUser(), 70, DELTA);
        Assert.assertEquals(youngGC.getCause(), GCCause.CMS_FINAL_REMARK);

        PauseStatistics pause = model.getPauseStatistics(new TimeRange(0, 99999999));
        Assert.assertEquals(pause.getPauseAvg(), (32.662 + 3.259 + 2.784) / 3.0, DELTA);
    }

    @Test
    public void TestJDK17SerialGCParser() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("17SerialGCParser.log"));
        SerialGCModel model = (SerialGCModel) parser.parse(TestUtil.getGCLog("17SerialGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 173, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 21.766, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 40960 * 1024, 46080 * 1024, 5120 * 1024, 46080 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 40960 * 1024, 40960 * 1024, 0 * 1024, 40960 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 0 * 1024, 5120 * 1024, 5120 * 1024, 5120 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, 51200 * 1024, 14524 * 1024, 51200 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 137 * 1024, 384 * 1024, 138 * 1024, 384 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.NONCLASS), new GCMemoryItem(MemoryArea.NONCLASS, 133 * 1024, 256 * 1024, 134 * 1024, 256 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.CLASS), new GCMemoryItem(MemoryArea.CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 40 * 1024 * 1024, 95 * 1024 * 1024, 19 * 1024 * 1024, 95 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 2616, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 92.137, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 46079 * 1024, 46080 * 1024, 36798 * 1024, 46080 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 40960 * 1024, 40960 * 1024, 36798 * 1024, 40960 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 5119 * 1024, 5120 * 1024, 0 * 1024, 5120 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 51199 * 1024, 51200 * 1024, 51199 * 1024, 51200 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 137 * 1024, 384 * 1024, 137 * 1024, 384 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.NONCLASS), new GCMemoryItem(MemoryArea.NONCLASS, 133 * 1024, 256 * 1024, 133 * 1024, 256 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.CLASS), new GCMemoryItem(MemoryArea.CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 94 * 1024 * 1024, 95 * 1024 * 1024, 85 * 1024 * 1024, 95 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_COMPUTE_NEW_OBJECT_ADDRESSES).getDuration(), 12.103, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.SERIAL_MOVE_OBJECTS).getDuration(), 10.313, DELTA);
    }

    @Test
    public void TestJDK17ParallelGCParser() throws Exception {
        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("17ParallelGCParser.log"));
        ParallelGCModel model = (ParallelGCModel) parser.parse(TestUtil.getGCLog("17ParallelGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 222, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 10.085, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.ALLOCATION_FAILURE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 38912 * 1024, 45056 * 1024, 6137 * 1024, 45056 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 38912 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 0 * 1024, 6144 * 1024, 6137 * 1024, 6144 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, 51200 * 1024, 13208 * 1024, 51200 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.NONCLASS), new GCMemoryItem(MemoryArea.NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.CLASS), new GCMemoryItem(MemoryArea.CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 38 * 1024 * 1024, (45056 + 51200) * 1024, 18 * 1024 * 1024, 94 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 10, DELTA);

        GCEvent fullGC = model.getGcEvents().get(1);
        Assert.assertEquals(fullGC.getGcid(), 1);
        Assert.assertEquals(fullGC.getStartTime(), 547, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 21.046, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.ERGONOMICS);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 6128 * 1024, 45056 * 1024, 0 * 1024, 45056 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 0 * 1024, 38912 * 1024, 0 * 1024, 38912 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 6128 * 1024, 6144 * 1024, 0 * 1024, 6144 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 46504 * 1024, 51200 * 1024, 38169 * 1024, 51200 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 135 * 1024, 384 * 1024, 135 * 1024, 384 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.NONCLASS), new GCMemoryItem(MemoryArea.NONCLASS, 131 * 1024, 256 * 1024, 131 * 1024, 256 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.CLASS), new GCMemoryItem(MemoryArea.CLASS, 4 * 1024, 128 * 1024, 4 * 1024, 128 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 51 * 1024 * 1024, (45056 + 51200) * 1024, 37 * 1024 * 1024, 94 * 1024 * 1024));
        Assert.assertEquals(fullGC.getPhases().size(), 5);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() != UNKNOWN_DOUBLE);
            Assert.assertTrue(phase.getDuration() != UNKNOWN_DOUBLE);
        }
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_SUMMARY).getDuration(), 0.006, DELTA);
        Assert.assertEquals(fullGC.getLastPhaseOfType(GCEventType.PARALLEL_PHASE_COMPACTION).getDuration(), 6.917, DELTA);
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
    }

    @Test
    public void testJDK17ZGCParser() throws Exception {
        UnifiedZGCLogParser parser = (UnifiedZGCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("17ZGCParser.log")));
        ZGCModel model = (ZGCModel) parser.parse(TestUtil.getGCLog("17ZGCParser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        Assert.assertNotNull(model);

        Assert.assertEquals(model.getGcEvents().size(), 1);
        Assert.assertTrue(model.getGcModelMetadata().isMetaspaceCapacityReliable());
        GCEvent gc = model.getGcEvents().get(0);
        Assert.assertEquals(gc.getGcid(), 0);
        Assert.assertEquals(gc.getStartTime(), 918, DELTA);
        Assert.assertEquals(gc.getEndTime(), 950, DELTA);
        Assert.assertEquals(gc.getDuration(), 32, DELTA);
        Assert.assertEquals(gc.getEventType(), GCEventType.ZGC_GARBAGE_COLLECTION);
        Assert.assertEquals(gc.getCause(), GCCause.WARMUP);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getStartTime(), 918 - 0.007, DELTA);
        Assert.assertEquals(gc.getLastPhaseOfType(GCEventType.ZGC_PAUSE_MARK_START).getDuration(), 0.007, DELTA);
        Assert.assertEquals(gc.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, -1, -1, 0, 0));
        Assert.assertEquals(gc.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 104L * 1024 * 1024, 1000L * 1024 * 1024, 88 * 1024 * 1024, 1000L * 1024 * 1024));
        Assert.assertEquals(gc.getAllocation(), 3 * 1024 * 1024);
        Assert.assertEquals(gc.getReclamation(), 19L * 1024 * 1024);

        List<ZGCModel.ZStatistics> statistics = model.getStatistics();
        Assert.assertEquals(statistics.size(), 1);
        Assert.assertEquals(44, statistics.get(0).getStatisticItems().size());
        Assert.assertEquals(statistics.get(0).getStartTime(), 10417, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10s(), 11, DELTA);
        Assert.assertEquals(statistics.get(0).get("System: Java Threads threads").getMax10h(), 11, DELTA);
    }

    @Test
    public void testJDK17G1Parser() throws Exception {

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("17G1Parser.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("17G1Parser.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 4);
        Assert.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);

        GCEvent youngGC = model.getGcEvents().get(0);
        Assert.assertEquals(youngGC.getGcid(), 0);
        Assert.assertEquals(youngGC.getStartTime(), 333, DELTA);
        Assert.assertEquals(youngGC.getDuration(), 20.955, DELTA);
        Assert.assertEquals(youngGC.getEventType(), GCEventType.YOUNG_GC);
        Assert.assertEquals(youngGC.getCause(), GCCause.G1_EVACUATION_PAUSE);
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 50 * 1024 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.EDEN), new GCMemoryItem(MemoryArea.EDEN, 50 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 43 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.SURVIVOR), new GCMemoryItem(MemoryArea.SURVIVOR, 0 * 1024, UNKNOWN_INT, 7 * 1024 * 1024, 7 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.OLD), new GCMemoryItem(MemoryArea.OLD, 0, UNKNOWN_INT, 18 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.ARCHIVE), new GCMemoryItem(MemoryArea.ARCHIVE, 2 * 1024 * 1024, UNKNOWN_INT, 2 * 1024 * 1024, UNKNOWN_INT));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HUMONGOUS), new GCMemoryItem(MemoryArea.HUMONGOUS, 1 * 1024 * 1024, UNKNOWN_INT, 1 * 1024 * 1024, UNKNOWN_INT));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.METASPACE), new GCMemoryItem(MemoryArea.METASPACE, 87 * 1024, 320 * 1024, 87 * 1024, 320 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.NONCLASS), new GCMemoryItem(MemoryArea.NONCLASS, 84 * 1024, 192 * 1024, 84 * 1024, 192 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.CLASS), new GCMemoryItem(MemoryArea.CLASS, 3 * 1024, 128 * 1024, 3 * 1024, 128 * 1024));
        Assert.assertEquals(youngGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 51 * 1024 * 1024, UNKNOWN_INT, 26 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(youngGC.getCpuTime().getReal(), 20, DELTA);
        Assert.assertEquals(youngGC.getPhases().size(), 5);
        Assert.assertEquals(youngGC.getPhases().get(1).getEventType(), GCEventType.G1_MERGE_HEAP_ROOTS);
        Assert.assertEquals(youngGC.getPhases().get(1).getDuration(), 0.1, DELTA);
        for (GCEvent phase : youngGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() >= 0);
            Assert.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentCycle = model.getGcEvents().get(1);
        Assert.assertEquals(concurrentCycle.getEventType(), GCEventType.G1_CONCURRENT_CYCLE);
        Assert.assertEquals(concurrentCycle.getGcid(), 1);
        Assert.assertEquals(concurrentCycle.getStartTime(), 1097, DELTA);
        Assert.assertEquals(concurrentCycle.getDuration(), 25.265, DELTA);
        Assert.assertEquals(concurrentCycle.getPhases().size(), 9);
        for (GCEvent phase : concurrentCycle.getPhases()) {
            Assert.assertTrue(phase.getStartTime() > 0);
            Assert.assertTrue(phase.getDuration() > 0);
        }

        GCEvent fullGC = model.getGcEvents().get(2);
        Assert.assertEquals(fullGC.getGcid(), 2);
        Assert.assertEquals(fullGC.getStartTime(), 1715, DELTA);
        Assert.assertEquals(fullGC.getDuration(), 22.935, DELTA);
        Assert.assertEquals(fullGC.getEventType(), GCEventType.FULL_GC);
        Assert.assertEquals(fullGC.getCause(), GCCause.G1_COMPACTION);
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.YOUNG), new GCMemoryItem(MemoryArea.YOUNG, 0 * 1024 * 1024, UNKNOWN_INT, 0 * 1024 * 1024, 50 * 1024 * 1024));
        Assert.assertEquals(fullGC.getMemoryItem(MemoryArea.HEAP), new GCMemoryItem(MemoryArea.HEAP, 98 * 1024 * 1024, UNKNOWN_INT, 69 * 1024 * 1024, 100 * 1024 * 1024));
        Assert.assertEquals(fullGC.getCpuTime().getReal(), 20, DELTA);
        Assert.assertEquals(fullGC.getPhases().size(), 4);
        for (GCEvent phase : fullGC.getPhases()) {
            Assert.assertTrue(phase.getStartTime() >= 0);
            Assert.assertTrue(phase.getDuration() >= 0);
        }

        GCEvent concurrentUndo = model.getGcEvents().get(3);
        Assert.assertEquals(concurrentUndo.getEventType(), GCEventType.G1_CONCURRENT_UNDO_CYCLE);
        Assert.assertEquals(concurrentUndo.getGcid(), 3);
        Assert.assertEquals(concurrentUndo.getStartTime(), 2145, DELTA);
        Assert.assertEquals(concurrentUndo.getDuration(), 0.125, DELTA);
        Assert.assertEquals(concurrentUndo.getPhases().size(), 1);
        for (GCEvent phase : concurrentUndo.getPhases()) {
            Assert.assertTrue(phase.getStartTime() > 0);
            Assert.assertTrue(phase.getDuration() > 0);
        }

        GCLogMetadata metadata = model.getGcModelMetadata();
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS.getName()));
        Assert.assertTrue(metadata.getImportantEventTypes().contains(GCEventType.G1_CONCURRENT_UNDO_CYCLE.getName()));
    }

    @Test
    public void testJDK17G1InferRegionSize() throws Exception {

        UnifiedG1GCLogParser parser = (UnifiedG1GCLogParser) (new GCLogParserFactory().getParser(TestUtil.getGCLog("17G1InferRegionSize.log")));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("17G1InferRegionSize.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());
        // assert parsing success
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getGcEvents().size(), 1);
        Assert.assertEquals(model.getHeapRegionSize(), 1L * 1024 * 1024);
    }

    @Test
    public void testJDK8G1PrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1PrintGC.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1PrintGC.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

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
    public void testJDK8CMSPrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8CMSPrintGC.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("8CMSPrintGC.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

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
    public void testJDK11G1PrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11G1PrintGC.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1PrintGC.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 7);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 1);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 0);
        Assert.assertEquals(model.getGcEvents().stream().filter(event -> event.isTrue(GCEventBooleanType.TO_SPACE_EXHAUSTED)).count(), 5);
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

        parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11G1PrintGC2.log"));
        model = (G1GCModel) parser.parse(TestUtil.getGCLog("11G1PrintGC2.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 4);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 0);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 0);
        for (GCEvent event : model.getGcEvents()) {
            Assert.assertTrue(event.isYoungGC());
            Assert.assertTrue(event.getDuration() > 0);
            Assert.assertNotNull(event.getCause());
            Assert.assertNotNull(event.getMemoryItem(MemoryArea.HEAP));
        }
    }

    @Test
    public void testJDK11CMSPrintGC() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("11CMSPrintGC.log"));
        CMSGCModel model = (CMSGCModel) parser.parse(TestUtil.getGCLog("11CMSPrintGC.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 5);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 3);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 1);
        for (GCEvent event : model.getGcEvents()) {
            Assert.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assert.assertTrue(event.getDuration() > 0);
                Assert.assertNotNull(event.getCause());
                Assert.assertNotNull(event.getMemoryItem(MemoryArea.HEAP));
            }
            if (event.isOldGC()) {
                Assert.assertEquals(7, event.getPhases().size());
            }
        }
    }

    @Test
    public void testJDK8G1LogConcurrencyProblem() throws Exception {

        GCLogParser parser = new GCLogParserFactory().getParser(TestUtil.getGCLog("8G1LogConcurrencyProblem.log"));
        G1GCModel model = (G1GCModel) parser.parse(TestUtil.getGCLog("8G1LogConcurrencyProblem.log"));
        model.calculateDerivedInfo(new NullProgressMonitor());

        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isYoungGC).count(), 2);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isOldGC).count(), 1);
        Assert.assertEquals(model.getGcEvents().stream().filter(GCEvent::isFullGC).count(), 0);
        for (GCEvent event : model.getGcEvents()) {
            Assert.assertTrue(event.getStartTime() > 0);
            if (event.isYoungGC() || event.isFullGC()) {
                Assert.assertTrue(event.getDuration() > 0);
                Assert.assertNotNull(event.getCause());
                Assert.assertNotNull(event.getMemoryItem(MemoryArea.HEAP));
            }
            if (event.isOldGC()) {
                Assert.assertEquals(8, event.getPhases().size());
            }
        }
    }
}
