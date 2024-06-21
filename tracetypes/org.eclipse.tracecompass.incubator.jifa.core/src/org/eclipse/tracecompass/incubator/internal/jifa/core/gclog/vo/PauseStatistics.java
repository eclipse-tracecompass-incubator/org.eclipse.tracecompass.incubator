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

public class PauseStatistics {
    public PauseStatistics(double tput, double average, double median, double percentile, double percentile2, double max) {
        throughput = tput;
        pauseAvg = average;
        pauseMedian = median;
        pauseP99 = percentile;
        pauseP999 = percentile2;
        pauseMax = max;
    }

    double throughput;
    double pauseAvg;
    double pauseMedian;
    double pauseP99;
    double pauseP999;
    double pauseMax;

    /**
     * @return the throughput
     */
    public double getThroughput() {
        return throughput;
    }

    /**
     * @param throughput
     *            the throughput to set
     */
    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    /**
     * @return the pauseAvg
     */
    public double getPauseAvg() {
        return pauseAvg;
    }

    /**
     * @param pauseAvg
     *            the pauseAvg to set
     */
    public void setPauseAvg(double pauseAvg) {
        this.pauseAvg = pauseAvg;
    }

    /**
     * @return the pauseMedian
     */
    public double getPauseMedian() {
        return pauseMedian;
    }

    /**
     * @param pauseMedian
     *            the pauseMedian to set
     */
    public void setPauseMedian(double pauseMedian) {
        this.pauseMedian = pauseMedian;
    }

    /**
     * @return the pauseP99
     */
    public double getPauseP99() {
        return pauseP99;
    }

    /**
     * @param pauseP99
     *            the pauseP99 to set
     */
    public void setPauseP99(double pauseP99) {
        this.pauseP99 = pauseP99;
    }

    /**
     * @return the pauseP999
     */
    public double getPauseP999() {
        return pauseP999;
    }

    /**
     * @param pauseP999
     *            the pauseP999 to set
     */
    public void setPauseP999(double pauseP999) {
        this.pauseP999 = pauseP999;
    }

    /**
     * @return the pauseMax
     */
    public double getPauseMax() {
        return pauseMax;
    }

    /**
     * @param pauseMax
     *            the pauseMax to set
     */
    public void setPauseMax(double pauseMax) {
        this.pauseMax = pauseMax;
    }
}
