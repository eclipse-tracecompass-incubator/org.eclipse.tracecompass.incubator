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

import org.eclipse.jdt.annotation.NonNull;

public class PhaseStatistics {
    private List<ParentStatisticsInfo> parents;

    public static class ParentStatisticsInfo {
        public ParentStatisticsInfo(PhaseStatisticItem phaseStatisticItem, @NonNull List<PhaseStatisticItem> collect, @NonNull List<PhaseStatisticItem> collect2) {
            self = phaseStatisticItem;
            phases = collect;
            causes = collect2;
        }

        private PhaseStatisticItem self;
        private List<PhaseStatisticItem> phases;
        private List<PhaseStatisticItem> causes;

        /**
         * @return the self
         */
        public PhaseStatisticItem getSelf() {
            return self;
        }

        /**
         * @param self
         *            the self to set
         */
        public void setSelf(PhaseStatisticItem self) {
            this.self = self;
        }

        /**
         * @return the phases
         */
        public List<PhaseStatisticItem> getPhases() {
            return phases;
        }

        /**
         * @param phases
         *            the phases to set
         */
        public void setPhases(List<PhaseStatisticItem> phases) {
            this.phases = phases;
        }

        /**
         * @return the causes
         */
        public List<PhaseStatisticItem> getCauses() {
            return causes;
        }

        /**
         * @param causes
         *            the causes to set
         */
        public void setCauses(List<PhaseStatisticItem> causes) {
            this.causes = causes;
        }
    }

    public static class PhaseStatisticItem {
        private String name;
        private int count;
        private double intervalAvg;
        private double intervalMin;
        private double durationAvg;
        private double durationMax;
        private double durationTotal;

        public PhaseStatisticItem(String name2, int n, double average, double min, double average2, double max, double sum) {
            name = name2;
            count = n;
            intervalAvg =average;
            intervalMin = min;
            durationAvg = average2;
            durationMax = max;
            durationTotal = sum;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name
         *            the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @param count
         *            the count to set
         */
        public void setCount(int count) {
            this.count = count;
        }

        /**
         * @return the intervalAvg
         */
        public double getIntervalAvg() {
            return intervalAvg;
        }

        /**
         * @param intervalAvg
         *            the intervalAvg to set
         */
        public void setIntervalAvg(double intervalAvg) {
            this.intervalAvg = intervalAvg;
        }

        /**
         * @return the intervalMin
         */
        public double getIntervalMin() {
            return intervalMin;
        }

        /**
         * @param intervalMin
         *            the intervalMin to set
         */
        public void setIntervalMin(double intervalMin) {
            this.intervalMin = intervalMin;
        }

        /**
         * @return the durationAvg
         */
        public double getDurationAvg() {
            return durationAvg;
        }

        /**
         * @param durationAvg
         *            the durationAvg to set
         */
        public void setDurationAvg(double durationAvg) {
            this.durationAvg = durationAvg;
        }

        /**
         * @return the durationMax
         */
        public double getDurationMax() {
            return durationMax;
        }

        /**
         * @param durationMax
         *            the durationMax to set
         */
        public void setDurationMax(double durationMax) {
            this.durationMax = durationMax;
        }

        /**
         * @return the durationTotal
         */
        public double getDurationTotal() {
            return durationTotal;
        }

        /**
         * @param durationTotal
         *            the durationTotal to set
         */
        public void setDurationTotal(double durationTotal) {
            this.durationTotal = durationTotal;
        }
    }

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
