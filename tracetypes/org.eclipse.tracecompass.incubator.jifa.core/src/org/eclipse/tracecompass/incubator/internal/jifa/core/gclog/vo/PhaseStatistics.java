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

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

public class PhaseStatistics {
    public static class ParentStatisticsInfo {
        private List<PhaseStatisticItem> causes;

        private List<PhaseStatisticItem> phases;
        private PhaseStatisticItem self;
        public ParentStatisticsInfo(PhaseStatisticItem phaseStatisticItem, @NonNull List<PhaseStatisticItem> collect, @NonNull List<PhaseStatisticItem> collect2) {
            self = phaseStatisticItem;
            phases = collect;
            causes = collect2;
        }

        /**
         * @return the causes
         */
        public List<PhaseStatisticItem> getCauses() {
            return causes;
        }

        /**
         * @return the phases
         */
        public List<PhaseStatisticItem> getPhases() {
            return phases;
        }

        /**
         * @return the self
         */
        public PhaseStatisticItem getSelf() {
            return self;
        }

        /**
         * @param causes
         *            the causes to set
         */
        public void setCauses(List<PhaseStatisticItem> causes) {
            this.causes = causes;
        }

        /**
         * @param phases
         *            the phases to set
         */
        public void setPhases(List<PhaseStatisticItem> phases) {
            this.phases = phases;
        }

        /**
         * @param self
         *            the self to set
         */
        public void setSelf(PhaseStatisticItem self) {
            this.self = self;
        }
    }

    public static class PhaseStatisticItem {
        private int count;

        private double durationAvg;

        private double durationMax;
        private double durationTotal;
        private double intervalAvg;
        private double intervalMin;
        private String name;
        public PhaseStatisticItem(String name2, int n, double average, double min, double average2, double max, double sum) {
            name = name2;
            count = n;
            intervalAvg =average;
            intervalMin = min;
            durationAvg = average2;
            durationMax = max;
            durationTotal = sum;
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
            PhaseStatisticItem other = (PhaseStatisticItem) obj;
            return count == other.count && Double.doubleToLongBits(durationAvg) == Double.doubleToLongBits(other.durationAvg) && Double.doubleToLongBits(durationMax) == Double.doubleToLongBits(other.durationMax)
                    && Double.doubleToLongBits(durationTotal) == Double.doubleToLongBits(other.durationTotal) && Double.doubleToLongBits(intervalAvg) == Double.doubleToLongBits(other.intervalAvg)
                    && Double.doubleToLongBits(intervalMin) == Double.doubleToLongBits(other.intervalMin) && Objects.equals(name, other.name);
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @return the durationAvg
         */
        public double getDurationAvg() {
            return durationAvg;
        }

        /**
         * @return the durationMax
         */
        public double getDurationMax() {
            return durationMax;
        }

        /**
         * @return the durationTotal
         */
        public double getDurationTotal() {
            return durationTotal;
        }

        /**
         * @return the intervalAvg
         */
        public double getIntervalAvg() {
            return intervalAvg;
        }

        /**
         * @return the intervalMin
         */
        public double getIntervalMin() {
            return intervalMin;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(count, durationAvg, durationMax, durationTotal, intervalAvg, intervalMin, name);
        }

        /**
         * @param count
         *            the count to set
         */
        public void setCount(int count) {
            this.count = count;
        }

        /**
         * @param durationAvg
         *            the durationAvg to set
         */
        public void setDurationAvg(double durationAvg) {
            this.durationAvg = durationAvg;
        }

        /**
         * @param durationMax
         *            the durationMax to set
         */
        public void setDurationMax(double durationMax) {
            this.durationMax = durationMax;
        }

        /**
         * @param durationTotal
         *            the durationTotal to set
         */
        public void setDurationTotal(double durationTotal) {
            this.durationTotal = durationTotal;
        }

        /**
         * @param intervalAvg
         *            the intervalAvg to set
         */
        public void setIntervalAvg(double intervalAvg) {
            this.intervalAvg = intervalAvg;
        }

        /**
         * @param intervalMin
         *            the intervalMin to set
         */
        public void setIntervalMin(double intervalMin) {
            this.intervalMin = intervalMin;
        }

        /**
         * @param name
         *            the name to set
         */
        public void setName(String name) {
            this.name = name;
        }
    }

    private List<ParentStatisticsInfo> parents;

    public PhaseStatistics(List<ParentStatisticsInfo> result) {
        parents = result;
    }

    /**
     * @return the parents
     */
    public List<ParentStatisticsInfo> getParents() {
        return parents;
    }

    /**
     * @param parents
     *            the parents to set
     */
    public void setParents(List<ParentStatisticsInfo> parents) {
        this.parents = parents;
    }
}
