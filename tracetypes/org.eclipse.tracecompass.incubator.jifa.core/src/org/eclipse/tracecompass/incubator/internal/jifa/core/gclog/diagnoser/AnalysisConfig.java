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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser;

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.TimeRange;

public class AnalysisConfig {
    private TimeRange timeRange;
    private double longPauseThreshold;
    private double longConcurrentThreshold;
    private double youngGCFrequentIntervalThreshold;
    private double oldGCFrequentIntervalThreshold;
    private double fullGCFrequentIntervalThreshold;
    private double highOldUsageThreshold;
    private double highHumongousUsageThreshold;
    private double highHeapUsageThreshold;
    private double highMetaspaceUsageThreshold;
    private double smallGenerationThreshold;
    private double highPromotionThreshold;
    private double badThroughputThreshold;
    private double tooManyOldGCThreshold;
    private double highSysThreshold;
    private double lowUsrThreshold;

    public static AnalysisConfig defaultConfig(GCModel model) {
        AnalysisConfig config = new AnalysisConfig();
        config.setTimeRange(new TimeRange(model.getStartTime(), model.getEndTime()));
        config.setLongPauseThreshold(model.isPauseless() ? 30 : 400);
        config.setLongConcurrentThreshold(30000);
        config.setYoungGCFrequentIntervalThreshold(1000);
        config.setOldGCFrequentIntervalThreshold(15000);
        config.setFullGCFrequentIntervalThreshold(model.isGenerational() ? 60000 : 2000);
        config.setHighOldUsageThreshold(80);
        config.setHighHumongousUsageThreshold(50);
        config.setHighHeapUsageThreshold(60);
        config.setHighMetaspaceUsageThreshold(80);
        config.setSmallGenerationThreshold(10);
        config.setHighPromotionThreshold(3);
        config.setBadThroughputThreshold(90);
        config.setTooManyOldGCThreshold(20);
        config.setHighSysThreshold(50);
        config.setLowUsrThreshold(100);
        return config;
    }


    // time range is ignored here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        AnalysisConfig config = (AnalysisConfig) o;
        return Double.compare(config.longPauseThreshold, longPauseThreshold) == 0 && Double.compare(config.longConcurrentThreshold, longConcurrentThreshold) == 0 && Double.compare(config.youngGCFrequentIntervalThreshold, youngGCFrequentIntervalThreshold) == 0 && Double.compare(config.oldGCFrequentIntervalThreshold, oldGCFrequentIntervalThreshold) == 0 && Double.compare(config.fullGCFrequentIntervalThreshold, fullGCFrequentIntervalThreshold) == 0 && Double.compare(config.highOldUsageThreshold, highOldUsageThreshold) == 0 && Double.compare(config.highHumongousUsageThreshold, highHumongousUsageThreshold) == 0 && Double.compare(config.highHeapUsageThreshold, highHeapUsageThreshold) == 0 && Double.compare(config.highMetaspaceUsageThreshold, highMetaspaceUsageThreshold) == 0 && Double.compare(config.smallGenerationThreshold, smallGenerationThreshold) == 0 && Double.compare(config.highPromotionThreshold, highPromotionThreshold) == 0 && Double.compare(config.badThroughputThreshold, badThroughputThreshold) == 0 && Double.compare(config.tooManyOldGCThreshold, tooManyOldGCThreshold) == 0 && Double.compare(config.highSysThreshold, highSysThreshold) == 0 && Double.compare(config.lowUsrThreshold, lowUsrThreshold) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(longPauseThreshold, longConcurrentThreshold, youngGCFrequentIntervalThreshold, oldGCFrequentIntervalThreshold, fullGCFrequentIntervalThreshold, highOldUsageThreshold, highHumongousUsageThreshold, highHeapUsageThreshold, highMetaspaceUsageThreshold, smallGenerationThreshold, highPromotionThreshold, badThroughputThreshold, tooManyOldGCThreshold, highSysThreshold, lowUsrThreshold);
    }


    /**
     * @return the timeRange
     */
    public TimeRange getTimeRange() {
        return timeRange;
    }


    /**
     * @param timeRange the timeRange to set
     */
    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }


    /**
     * @return the longPauseThreshold
     */
    public double getLongPauseThreshold() {
        return longPauseThreshold;
    }


    /**
     * @param longPauseThreshold the longPauseThreshold to set
     */
    public void setLongPauseThreshold(double longPauseThreshold) {
        this.longPauseThreshold = longPauseThreshold;
    }


    /**
     * @return the longConcurrentThreshold
     */
    public double getLongConcurrentThreshold() {
        return longConcurrentThreshold;
    }


    /**
     * @param longConcurrentThreshold the longConcurrentThreshold to set
     */
    public void setLongConcurrentThreshold(double longConcurrentThreshold) {
        this.longConcurrentThreshold = longConcurrentThreshold;
    }


    /**
     * @return the youngGCFrequentIntervalThreshold
     */
    public double getYoungGCFrequentIntervalThreshold() {
        return youngGCFrequentIntervalThreshold;
    }


    /**
     * @param youngGCFrequentIntervalThreshold the youngGCFrequentIntervalThreshold to set
     */
    public void setYoungGCFrequentIntervalThreshold(double youngGCFrequentIntervalThreshold) {
        this.youngGCFrequentIntervalThreshold = youngGCFrequentIntervalThreshold;
    }


    /**
     * @return the oldGCFrequentIntervalThreshold
     */
    public double getOldGCFrequentIntervalThreshold() {
        return oldGCFrequentIntervalThreshold;
    }


    /**
     * @param oldGCFrequentIntervalThreshold the oldGCFrequentIntervalThreshold to set
     */
    public void setOldGCFrequentIntervalThreshold(double oldGCFrequentIntervalThreshold) {
        this.oldGCFrequentIntervalThreshold = oldGCFrequentIntervalThreshold;
    }


    /**
     * @return the fullGCFrequentIntervalThreshold
     */
    public double getFullGCFrequentIntervalThreshold() {
        return fullGCFrequentIntervalThreshold;
    }


    /**
     * @param fullGCFrequentIntervalThreshold the fullGCFrequentIntervalThreshold to set
     */
    public void setFullGCFrequentIntervalThreshold(double fullGCFrequentIntervalThreshold) {
        this.fullGCFrequentIntervalThreshold = fullGCFrequentIntervalThreshold;
    }


    /**
     * @return the highOldUsageThreshold
     */
    public double getHighOldUsageThreshold() {
        return highOldUsageThreshold;
    }


    /**
     * @param highOldUsageThreshold the highOldUsageThreshold to set
     */
    public void setHighOldUsageThreshold(double highOldUsageThreshold) {
        this.highOldUsageThreshold = highOldUsageThreshold;
    }


    /**
     * @return the highHumongousUsageThreshold
     */
    public double getHighHumongousUsageThreshold() {
        return highHumongousUsageThreshold;
    }


    /**
     * @param highHumongousUsageThreshold the highHumongousUsageThreshold to set
     */
    public void setHighHumongousUsageThreshold(double highHumongousUsageThreshold) {
        this.highHumongousUsageThreshold = highHumongousUsageThreshold;
    }


    /**
     * @return the highHeapUsageThreshold
     */
    public double getHighHeapUsageThreshold() {
        return highHeapUsageThreshold;
    }


    /**
     * @param highHeapUsageThreshold the highHeapUsageThreshold to set
     */
    public void setHighHeapUsageThreshold(double highHeapUsageThreshold) {
        this.highHeapUsageThreshold = highHeapUsageThreshold;
    }


    /**
     * @return the highMetaspaceUsageThreshold
     */
    public double getHighMetaspaceUsageThreshold() {
        return highMetaspaceUsageThreshold;
    }


    /**
     * @param highMetaspaceUsageThreshold the highMetaspaceUsageThreshold to set
     */
    public void setHighMetaspaceUsageThreshold(double highMetaspaceUsageThreshold) {
        this.highMetaspaceUsageThreshold = highMetaspaceUsageThreshold;
    }


    /**
     * @return the smallGenerationThreshold
     */
    public double getSmallGenerationThreshold() {
        return smallGenerationThreshold;
    }


    /**
     * @param smallGenerationThreshold the smallGenerationThreshold to set
     */
    public void setSmallGenerationThreshold(double smallGenerationThreshold) {
        this.smallGenerationThreshold = smallGenerationThreshold;
    }


    /**
     * @return the highPromotionThreshold
     */
    public double getHighPromotionThreshold() {
        return highPromotionThreshold;
    }


    /**
     * @param highPromotionThreshold the highPromotionThreshold to set
     */
    public void setHighPromotionThreshold(double highPromotionThreshold) {
        this.highPromotionThreshold = highPromotionThreshold;
    }


    /**
     * @return the badThroughputThreshold
     */
    public double getBadThroughputThreshold() {
        return badThroughputThreshold;
    }


    /**
     * @param badThroughputThreshold the badThroughputThreshold to set
     */
    public void setBadThroughputThreshold(double badThroughputThreshold) {
        this.badThroughputThreshold = badThroughputThreshold;
    }


    /**
     * @return the tooManyOldGCThreshold
     */
    public double getTooManyOldGCThreshold() {
        return tooManyOldGCThreshold;
    }


    /**
     * @param tooManyOldGCThreshold the tooManyOldGCThreshold to set
     */
    public void setTooManyOldGCThreshold(double tooManyOldGCThreshold) {
        this.tooManyOldGCThreshold = tooManyOldGCThreshold;
    }


    /**
     * @return the highSysThreshold
     */
    public double getHighSysThreshold() {
        return highSysThreshold;
    }


    /**
     * @param highSysThreshold the highSysThreshold to set
     */
    public void setHighSysThreshold(double highSysThreshold) {
        this.highSysThreshold = highSysThreshold;
    }


    /**
     * @return the lowUsrThreshold
     */
    public double getLowUsrThreshold() {
        return lowUsrThreshold;
    }


    /**
     * @param lowUsrThreshold the lowUsrThreshold to set
     */
    public void setLowUsrThreshold(double lowUsrThreshold) {
        this.lowUsrThreshold = lowUsrThreshold;
    }
}
