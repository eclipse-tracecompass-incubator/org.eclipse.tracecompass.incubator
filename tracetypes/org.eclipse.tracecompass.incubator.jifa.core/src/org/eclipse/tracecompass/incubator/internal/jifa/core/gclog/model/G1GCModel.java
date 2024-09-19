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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventBooleanType.GC_AT_END_OF_OLD_CYCLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.ARCHIVE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.EDEN;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.HEAP;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.HUMONGOUS;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.OLD;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea.SURVIVOR;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.FULL_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_CONCURRENT_CYCLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_CONCURRENT_MARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_CONCURRENT_MARK_ABORT;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_CONCURRENT_REBUILD_REMEMBERED_SETS;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_CONCURRENT_UNDO_CYCLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_MIXED_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_PAUSE_CLEANUP;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.G1_REMARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.YOUNG_GC;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

import java.util.Arrays;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCEventBooleanType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.GCMemoryItem;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCLogStyle;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.LongData;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.TimeRange;

/**
 * Gen1 gc model
 */
public class G1GCModel extends GCModel {
    private long heapRegionSize = UNKNOWN_INT; // in b
    private boolean regionSizeExact = false;
    private static GCCollectorType collector = GCCollectorType.G1;

    /**
     * Sets if this is an "exact" size.
     *
     * @param b
     *            true if exact
     */
    public void setRegionSizeExact(boolean b) {
        regionSizeExact = b;
    }

    /**
     * is this region size exact?
     *
     * @return true if size exact
     */
    public boolean isRegionSizeExact() {
        return regionSizeExact;
    }

    /**
     * set the heap region size, in bytes
     *
     * @param heapRegionSize
     *            the heap region size
     */
    public void setHeapRegionSize(long heapRegionSize) {
        this.heapRegionSize = heapRegionSize;
    }

    /**
     * Get the heap region size in bytes
     *
     * @return the heap region size
     */
    public long getHeapRegionSize() {
        return heapRegionSize;
    }

    /**
     * Default constructor
     */
    public G1GCModel() {
        super(collector);
    }

    private static List<GCEventType> allEventTypes = GCModel.calcAllEventTypes(collector);
    private static List<GCEventType> pauseEventTypes = GCModel.calcPauseEventTypes(collector);
    private static List<GCEventType> mainPauseEventTypes = GCModel.calcMainPauseEventTypes(collector);
    private static List<GCEventType> parentEventTypes = GCModel.calcParentEventTypes(collector);
    private static List<GCEventType> importantEventTypes = List.of(YOUNG_GC, G1_MIXED_GC, FULL_GC, G1_CONCURRENT_CYCLE,
            G1_CONCURRENT_MARK, G1_REMARK, G1_CONCURRENT_REBUILD_REMEMBERED_SETS, G1_PAUSE_CLEANUP, G1_CONCURRENT_UNDO_CYCLE);

    @Override
    protected List<GCEventType> getAllEventTypes() {
        return allEventTypes;
    }

    @Override
    protected List<GCEventType> getPauseEventTypes() {
        return pauseEventTypes;
    }

    @Override
    protected List<GCEventType> getMainPauseEventTypes() {
        return mainPauseEventTypes;
    }

    @Override
    protected List<GCEventType> getImportantEventTypes() {
        return importantEventTypes;
    }

    @Override
    protected List<GCEventType> getParentEventTypes() {
        return parentEventTypes;
    }

    private static boolean collectionResultUsingRegion(GCEvent event) {
        GCEventType type = event.getEventType();
        return (type == YOUNG_GC || type == FULL_GC || type == G1_MIXED_GC) && event.getMemoryItems() != null;
    }

    private static List<MemoryArea> AREAS_COUNTED_BY_REGION = List.of(EDEN, SURVIVOR, OLD, HUMONGOUS, ARCHIVE);

    private void inferHeapRegionSize() {
        if (heapRegionSize != UNKNOWN_INT) {
            return;
        }
        for (int i = getGcEvents().size() - 1; i >= 0; i--) {
            GCEvent event = getGcEvents().get(i);
            if (!collectionResultUsingRegion(event)) {
                continue;
            }
            if (event.getMemoryItem(HEAP).getPreUsed() == UNKNOWN_INT) {
                continue;
            }
            long regionCount = Arrays.stream(event.getMemoryItems())
                    .filter(item -> item != null && AREAS_COUNTED_BY_REGION.contains(item.getArea())
                            && item.getPreUsed() != UNKNOWN_INT)
                    .mapToLong(GCMemoryItem::getPreUsed)
                    .sum();
            if (regionCount < 3) {
                continue;
            }
            double bytesPerRegion = event.getMemoryItem(HEAP).getPreUsed() / (double) regionCount;
            heapRegionSize = (long) Math.pow(2, Math.ceil(Math.log(bytesPerRegion) / Math.log(2)));
            return;
        }
    }

    private void adjustMemoryInfo() {
        if (heapRegionSize == UNKNOWN_INT) {
            return;
        }
        for (GCEvent event : getGcEvents()) {
            if (!collectionResultUsingRegion(event)) {
                continue;
            }
            for (GCMemoryItem item : event.getMemoryItems()) {
                if (item != null && AREAS_COUNTED_BY_REGION.contains(item.getArea())) {
                    item.multiply(heapRegionSize);
                }
            }
        }
    }

    @Override
    protected void doBeforeCalculatingDerivedInfo() {
        if (getLogStyle() == GCLogStyle.UNIFIED) {
            inferHeapRegionSize();
            adjustMemoryInfo();
        }
    }

    @Override
    protected void doAfterCalculatingDerivedInfo() {
        decideGCsAfterOldGC();
    }

    private void decideGCsAfterOldGC() {
        GCEvent lastGCInCycle = null;
        double lastRemarkEndTime = Double.MAX_VALUE;
        double lastConcCycleEndTime = Double.MAX_VALUE;
        for (GCEvent event : getGcEvents()) {
            GCEventType type = event.getEventType();
            if (type == G1_CONCURRENT_UNDO_CYCLE) {
                continue;
            }
            if (type == G1_CONCURRENT_CYCLE) {
                if (event.containPhase(G1_CONCURRENT_MARK_ABORT)) {
                    return;
                }
                lastConcCycleEndTime = event.getEndTime();
                GCEvent remark = event.getLastPhaseOfType(G1_REMARK);
                if (remark != null) {
                    lastRemarkEndTime = remark.getEndTime();
                }
            }
            if (type == FULL_GC || type == YOUNG_GC || type == G1_MIXED_GC) {
                if (event.getStartTime() > lastRemarkEndTime) {
                    event.setTrue(GCEventBooleanType.GC_AFTER_REMARK);
                    lastRemarkEndTime = Double.MAX_VALUE;
                }
                if (event.getStartTime() >= lastConcCycleEndTime) {
                    if (type == FULL_GC) {
                        // a full gc interrupts mixed gcs
                        event.setTrue(GC_AT_END_OF_OLD_CYCLE);
                        lastGCInCycle = null;
                        lastConcCycleEndTime = Double.MAX_VALUE;
                    } else if (lastGCInCycle == null) {
                        lastGCInCycle = event;
                        if (type == YOUNG_GC && getLogStyle() == GCLogStyle.PRE_UNIFIED) {
                            // jdk8 does not print Prepare Mixed, add this sign
                            // for easier
                            // future analysis
                            event.setTrue(GCEventBooleanType.PREPARE_MIXED);
                        }
                    } else if (type == YOUNG_GC) {
                        // we have found the end of mixed gcs
                        lastGCInCycle.setTrue(GC_AT_END_OF_OLD_CYCLE);
                        lastGCInCycle = null;
                        lastConcCycleEndTime = Double.MAX_VALUE;
                    } else if (type == G1_MIXED_GC) {
                        lastGCInCycle = event;
                    }
                }
            }
        }
    }

    @Override
    protected void calculateUsedAvgAfterOldGC(TimeRange range, LongData[][] data) {
        iterateEventsWithinTimeRange(getGcEvents(), range, event -> {
            // read old from the last gc of old gc cycle
            if (event.isTrue(GC_AT_END_OF_OLD_CYCLE) && event.getMemoryItem(OLD) != null) {
                data[1][3].add(event.getMemoryItem(OLD).getPostUsed());
            }
            // read humongous and metaspace from the gc after remark
            if (event.isTrue(GCEventBooleanType.GC_AFTER_REMARK)) {
                if (event.getMemoryItem(HUMONGOUS) != null) {
                    data[2][3].add(event.getMemoryItem(HUMONGOUS).getPreUsed());

                }
                if (event.getMemoryItem(MemoryArea.METASPACE) != null) {
                    data[4][3].add(event.getMemoryItem(MemoryArea.METASPACE).getPreUsed());
                }
            }
        });
    }

}
