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

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_CONCURRENT_MARK;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_CONCURRENT_NONREF;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_CONCURRENT_PREPARE_RELOC_SET;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_CONCURRENT_RELOCATE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_CONCURRENT_SELECT_RELOC_SET;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_GARBAGE_COLLECTION;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_PAUSE_MARK_END;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_PAUSE_MARK_START;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType.ZGC_PAUSE_RELOCATE_START;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.TimedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.GCCollectorType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;

public class ZGCModel extends GCModel {

    // key of maps here should include unit like
    // "Memory: Allocation Rate MB/s" to deduplicate
    private List<ZStatistics> statistics = new ArrayList<>();
    private List<GCEvent> allocationStalls = new ArrayList<>();
    private long recommendMaxHeapSize = UNKNOWN_INT;


    private static GCCollectorType collector = GCCollectorType.ZGC;

    public ZGCModel() {
        super(collector);
        this.setMetaspaceCapacityReliable(true);
    }

    private static List<GCEventType> allEventTypes = GCModel.calcAllEventTypes(collector);
    private static List<GCEventType> pauseEventTypes = GCModel.calcPauseEventTypes(collector);
    private static List<GCEventType> mainPauseEventTypes = GCModel.calcMainPauseEventTypes(collector);
    private static List<GCEventType> parentEventTypes = GCModel.calcParentEventTypes(collector);
    private static List<GCEventType> importantEventTypes = List.of(ZGC_GARBAGE_COLLECTION, ZGC_PAUSE_MARK_START,
            ZGC_PAUSE_MARK_END, ZGC_PAUSE_RELOCATE_START, ZGC_CONCURRENT_MARK, ZGC_CONCURRENT_NONREF,
            ZGC_CONCURRENT_SELECT_RELOC_SET, ZGC_CONCURRENT_PREPARE_RELOC_SET, ZGC_CONCURRENT_RELOCATE);

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

    public List<GCEvent> getAllocationStalls() {
        return allocationStalls;
    }

    public void addAllocationStalls(GCEvent allocationStall) {
        this.allocationStalls.add(allocationStall);
    }


    public List<ZStatistics> getStatistics() {
        return statistics;
    }

    @Override
    public long getRecommendMaxHeapSize() {
        if (recommendMaxHeapSize == UNKNOWN_INT && !statistics.isEmpty()) {
            // used at marking start + garbage collection cycle * allocation rate
            int statisticIndex = 0;
            for (GCEvent collection : getGcCollectionEvents()) {
                if (collection.getEventType() != ZGC_GARBAGE_COLLECTION) {
                    continue;
                }
                if (collection.getMemoryItem(MemoryArea.HEAP).getPreUsed() == UNKNOWN_INT) {
                    continue;
                }
                while (statisticIndex < statistics.size() &&
                        statistics.get(statisticIndex).getStartTime() < collection.getEndTime()) {
                    statisticIndex++;
                }
                if (statisticIndex >= statistics.size()) {
                    break;
                }
                double collectionCycleMs = statistics.get(statisticIndex).get("Collector: Garbage Collection Cycle ms").getMax10s();
                double allocationRateMBps = statistics.get(statisticIndex).get("Memory: Allocation Rate MB/s").getMax10s();
                double size = collection.getMemoryItem(MemoryArea.HEAP).getPreUsed() +
                        (collectionCycleMs / Constant.MS2S) * (allocationRateMBps * Constant.KB2MB);
                recommendMaxHeapSize = Math.max(recommendMaxHeapSize, (long) size);
            }
        }
        return recommendMaxHeapSize;
    }

    public static class ZStatistics extends TimedEvent {
        private Map<String, ZStatisticsItem> items = new HashMap<>();

        public ZStatisticsItem get(String key) {
            return items.getOrDefault(key, null);
        }

        public void put(String key, ZStatisticsItem item) {
            items.put(key, item);
        }

        public Map<String, ZStatisticsItem> getStatisticItems() {
            return items;
        }
    }


    public static class ZStatisticsItem {
        private double avg10s;
        private double max10s;
        private double avg10m;
        private double max10m;
        private double avg10h;
        private double max10h;
        private double avgTotal;
        private double maxTotal;
        public ZStatisticsItem(double double1, double double2, double double3, double double4,
                double double5, double double6, double double7, double double8) {
            avg10s=double1;
            max10s = double2;
            avg10m = double3;
            max10m = double4;
            avg10h = double5;
            max10h = double6;
            avgTotal = double7;
            maxTotal = double8;

        }
        /**
         * @return the avg10s
         */
        public double getAvg10s() {
            return avg10s;
        }
        /**
         * @param avg10s the avg10s to set
         */
        public void setAvg10s(double avg10s) {
            this.avg10s = avg10s;
        }
        /**
         * @return the max10s
         */
        public double getMax10s() {
            return max10s;
        }
        /**
         * @param max10s the max10s to set
         */
        public void setMax10s(double max10s) {
            this.max10s = max10s;
        }
        /**
         * @return the avg10m
         */
        public double getAvg10m() {
            return avg10m;
        }
        /**
         * @param avg10m the avg10m to set
         */
        public void setAvg10m(double avg10m) {
            this.avg10m = avg10m;
        }
        /**
         * @return the max10m
         */
        public double getMax10m() {
            return max10m;
        }
        /**
         * @param max10m the max10m to set
         */
        public void setMax10m(double max10m) {
            this.max10m = max10m;
        }
        /**
         * @return the avg10h
         */
        public double getAvg10h() {
            return avg10h;
        }
        /**
         * @param avg10h the avg10h to set
         */
        public void setAvg10h(double avg10h) {
            this.avg10h = avg10h;
        }
        /**
         * @return the max10h
         */
        public double getMax10h() {
            return max10h;
        }
        /**
         * @param max10h the max10h to set
         */
        public void setMax10h(double max10h) {
            this.max10h = max10h;
        }
        /**
         * @return the avgTotal
         */
        public double getAvgTotal() {
            return avgTotal;
        }
        /**
         * @param avgTotal the avgTotal to set
         */
        public void setAvgTotal(double avgTotal) {
            this.avgTotal = avgTotal;
        }
        /**
         * @return the maxTotal
         */
        public double getMaxTotal() {
            return maxTotal;
        }
        /**
         * @param maxTotal the maxTotal to set
         */
        public void setMaxTotal(double maxTotal) {
            this.maxTotal = maxTotal;
        }
    }
}
