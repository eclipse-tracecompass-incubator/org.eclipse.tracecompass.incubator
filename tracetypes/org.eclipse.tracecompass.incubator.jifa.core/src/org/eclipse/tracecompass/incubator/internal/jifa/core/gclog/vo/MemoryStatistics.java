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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo;

import java.util.Objects;

public class MemoryStatistics {
    private MemoryStatisticsItem young;
    private MemoryStatisticsItem old;
    private MemoryStatisticsItem humongous;
    private MemoryStatisticsItem heap;
    private MemoryStatisticsItem metaspace;

    public static class MemoryStatisticsItem {
        private long capacityAvg;
        private long usedMax;
        private long usedAvgAfterFullGC;
        private long usedAvgAfterOldGC;

        public MemoryStatisticsItem(long average, long max, long avgOldGC, long avgPostGC) {
            capacityAvg = average;
            usedMax = max;
            usedAvgAfterFullGC = avgPostGC;
            usedAvgAfterOldGC = avgOldGC;
        }

        /**
         * @return the capacityAvg
         */
        public long getCapacityAvg() {
            return capacityAvg;
        }

        /**
         * @param capacityAvg
         *            the capacityAvg to set
         */
        public void setCapacityAvg(long capacityAvg) {
            this.capacityAvg = capacityAvg;
        }

        /**
         * @return the usedMax
         */
        public long getUsedMax() {
            return usedMax;
        }

        /**
         * @param usedMax
         *            the usedMax to set
         */
        public void setUsedMax(long usedMax) {
            this.usedMax = usedMax;
        }

        /**
         * @return the usedAvgAfterFullGC
         */
        public long getUsedAvgAfterFullGC() {
            return usedAvgAfterFullGC;
        }

        /**
         * @param usedAvgAfterFullGC
         *            the usedAvgAfterFullGC to set
         */
        public void setUsedAvgAfterFullGC(long usedAvgAfterFullGC) {
            this.usedAvgAfterFullGC = usedAvgAfterFullGC;
        }

        /**
         * @return the usedAvgAfterOldGC
         */
        public long getUsedAvgAfterOldGC() {
            return usedAvgAfterOldGC;
        }

        /**
         * @param usedAvgAfterOldGC
         *            the usedAvgAfterOldGC to set
         */
        public void setUsedAvgAfterOldGC(long usedAvgAfterOldGC) {
            this.usedAvgAfterOldGC = usedAvgAfterOldGC;
        }

        @Override
        public int hashCode() {
            return Objects.hash(capacityAvg, usedAvgAfterFullGC, usedAvgAfterOldGC, usedMax);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MemoryStatisticsItem other = (MemoryStatisticsItem) obj;
            return capacityAvg == other.capacityAvg && usedAvgAfterFullGC == other.usedAvgAfterFullGC && usedAvgAfterOldGC == other.usedAvgAfterOldGC && usedMax == other.usedMax;
        }
    }

    /**
     * @return the young
     */
    public MemoryStatisticsItem getYoung() {
        return young;
    }

    /**
     * @param young
     *            the young to set
     */
    public void setYoung(MemoryStatisticsItem young) {
        this.young = young;
    }

    /**
     * @return the old
     */
    public MemoryStatisticsItem getOld() {
        return old;
    }

    /**
     * @param old
     *            the old to set
     */
    public void setOld(MemoryStatisticsItem old) {
        this.old = old;
    }

    /**
     * @return the humongous
     */
    public MemoryStatisticsItem getHumongous() {
        return humongous;
    }

    /**
     * @param humongous
     *            the humongous to set
     */
    public void setHumongous(MemoryStatisticsItem humongous) {
        this.humongous = humongous;
    }

    /**
     * @return the heap
     */
    public MemoryStatisticsItem getHeap() {
        return heap;
    }

    /**
     * @param heap
     *            the heap to set
     */
    public void setHeap(MemoryStatisticsItem heap) {
        this.heap = heap;
    }

    /**
     * @return the metaspace
     */
    public MemoryStatisticsItem getMetaspace() {
        return metaspace;
    }

    /**
     * @param metaspace
     *            the metaspace to set
     */
    public void setMetaspace(MemoryStatisticsItem metaspace) {
        this.metaspace = metaspace;
    }
}
