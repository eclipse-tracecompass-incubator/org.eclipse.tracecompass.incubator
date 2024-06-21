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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.AnalysisConfig;


/**
 * This class provides some necessary information to the frontend.
 */

public class GCLogMetadata {


    /**
     * @return the collector
     */
    public String getCollector() {
        return collector;
    }
    /**
     * @param collector the collector to set
     */
    public void setCollector(String collector) {
        this.collector = collector;
    }
    /**
     * @return the logStyle
     */
    public String getLogStyle() {
        return logStyle;
    }
    /**
     * @param logStyle the logStyle to set
     */
    public void setLogStyle(String logStyle) {
        this.logStyle = logStyle;
    }
    /**
     * @return the startTime
     */
    public double getStartTime() {
        return startTime;
    }
    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    /**
     * @return the endTime
     */
    public double getEndTime() {
        return endTime;
    }
    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }
    /**
     * @return the timestamp
     */
    public double getTimestamp() {
        return timestamp;
    }
    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
    /**
     * @return the generational
     */
    public boolean isGenerational() {
        return generational;
    }
    /**
     * @param generational the generational to set
     */
    public void setGenerational(boolean generational) {
        this.generational = generational;
    }
    /**
     * @return the pauseless
     */
    public boolean isPauseless() {
        return pauseless;
    }
    /**
     * @param pauseless the pauseless to set
     */
    public void setPauseless(boolean pauseless) {
        this.pauseless = pauseless;
    }
    /**
     * @return the metaspaceCapacityReliable
     */
    public boolean isMetaspaceCapacityReliable() {
        return metaspaceCapacityReliable;
    }
    /**
     * @param metaspaceCapacityReliable the metaspaceCapacityReliable to set
     */
    public void setMetaspaceCapacityReliable(boolean metaspaceCapacityReliable) {
        this.metaspaceCapacityReliable = metaspaceCapacityReliable;
    }
    /**
     * @return the parallelGCThreads
     */
    public int getParallelGCThreads() {
        return parallelGCThreads;
    }
    /**
     * @param parallelGCThreads the parallelGCThreads to set
     */
    public void setParallelGCThreads(int parallelGCThreads) {
        this.parallelGCThreads = parallelGCThreads;
    }
    /**
     * @return the concurrentGCThreads
     */
    public int getConcurrentGCThreads() {
        return concurrentGCThreads;
    }
    /**
     * @param concurrentGCThreads the concurrentGCThreads to set
     */
    public void setConcurrentGCThreads(int concurrentGCThreads) {
        this.concurrentGCThreads = concurrentGCThreads;
    }
    /**
     * @return the parentEventTypes
     */
    public List<String> getParentEventTypes() {
        return parentEventTypes;
    }
    /**
     * @param parentEventTypes the parentEventTypes to set
     */
    public void setParentEventTypes(List<String> parentEventTypes) {
        this.parentEventTypes = parentEventTypes;
    }
    /**
     * @return the importantEventTypes
     */
    public List<String> getImportantEventTypes() {
        return importantEventTypes;
    }
    /**
     * @param importantEventTypes the importantEventTypes to set
     */
    public void setImportantEventTypes(List<String> importantEventTypes) {
        this.importantEventTypes = importantEventTypes;
    }
    /**
     * @return the pauseEventTypes
     */
    public List<String> getPauseEventTypes() {
        return pauseEventTypes;
    }
    /**
     * @param pauseEventTypes the pauseEventTypes to set
     */
    public void setPauseEventTypes(List<String> pauseEventTypes) {
        this.pauseEventTypes = pauseEventTypes;
    }
    /**
     * @return the mainPauseEventTypes
     */
    public List<String> getMainPauseEventTypes() {
        return mainPauseEventTypes;
    }
    /**
     * @param mainPauseEventTypes the mainPauseEventTypes to set
     */
    public void setMainPauseEventTypes(List<String> mainPauseEventTypes) {
        this.mainPauseEventTypes = mainPauseEventTypes;
    }
    /**
     * @return the allEventTypes
     */
    public List<String> getAllEventTypes() {
        return allEventTypes;
    }
    /**
     * @param allEventTypes the allEventTypes to set
     */
    public void setAllEventTypes(List<String> allEventTypes) {
        this.allEventTypes = allEventTypes;
    }
    /**
     * @return the causes
     */
    public List<String> getCauses() {
        return causes;
    }
    /**
     * @param causes the causes to set
     */
    public void setCauses(List<String> causes) {
        this.causes = causes;
    }
    /**
     * @return the analysisConfig
     */
    public AnalysisConfig getAnalysisConfig() {
        return analysisConfig;
    }
    /**
     * @param analysisConfig the analysisConfig to set
     */
    public void setAnalysisConfig(AnalysisConfig analysisConfig) {
        this.analysisConfig = analysisConfig;
    }
    private String collector;
    private String logStyle;
    private double startTime = UNKNOWN_DOUBLE;
    private double endTime = UNKNOWN_DOUBLE;
    private double timestamp = UNKNOWN_DOUBLE;
    private boolean generational = true;
    private boolean pauseless = false;
    private boolean metaspaceCapacityReliable = false;
    private int parallelGCThreads = UNKNOWN_INT;
    private int concurrentGCThreads = UNKNOWN_INT;
    private List<String> parentEventTypes;
    private List<String> importantEventTypes;
    private List<String> pauseEventTypes;
    private List<String> mainPauseEventTypes;
    private List<String> allEventTypes;
    private List<String> causes;
    private AnalysisConfig analysisConfig;
}
