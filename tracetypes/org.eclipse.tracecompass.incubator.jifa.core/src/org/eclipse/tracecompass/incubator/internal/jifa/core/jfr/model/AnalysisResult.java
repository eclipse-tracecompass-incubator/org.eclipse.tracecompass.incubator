/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

import java.util.List;

import org.openjdk.jmc.common.unit.QuantityConversionException.Problem;

public class AnalysisResult {
    private long processingTimeMillis;

    private DimensionResult<TaskCPUTime> cpuTime;

    private DimensionResult<TaskCount> cpuSample;

    private DimensionResult<TaskSum> wallClock;

    private DimensionResult<TaskAllocations> allocations;

    private DimensionResult<TaskAllocatedMemory> allocatedMemory;

    private DimensionResult<TaskCount> nativeExecutionSamples;

    private DimensionResult<TaskSum> fileIOTime;

    private DimensionResult<TaskSum> fileReadSize;

    private DimensionResult<TaskSum> fileWriteSize;

    private DimensionResult<TaskSum> socketReadSize;

    private DimensionResult<TaskSum> socketReadTime;

    private DimensionResult<TaskSum> socketWriteSize;

    private DimensionResult<TaskSum> socketWriteTime;

    private DimensionResult<TaskSum> synchronization;

    private DimensionResult<TaskSum> threadPark;

    private DimensionResult<TaskCount> classLoadCount;

    private DimensionResult<TaskSum> classLoadWallTime;

    private DimensionResult<TaskSum> threadSleepTime;

    private List<Problem> problems;

    /**
     * @return the processingTimeMillis
     */
    public long getProcessingTimeMillis() {
        return processingTimeMillis;
    }

    /**
     * @param processingTimeMillis the processingTimeMillis to set
     */
    public void setProcessingTimeMillis(long processingTimeMillis) {
        this.processingTimeMillis = processingTimeMillis;
    }

    /**
     * @return the cpuTime
     */
    public DimensionResult<TaskCPUTime> getCpuTime() {
        return cpuTime;
    }

    /**
     * @param cpuTime the cpuTime to set
     */
    public void setCpuTime(DimensionResult<TaskCPUTime> cpuTime) {
        this.cpuTime = cpuTime;
    }

    /**
     * @return the cpuSample
     */
    public DimensionResult<TaskCount> getCpuSample() {
        return cpuSample;
    }

    /**
     * @param cpuSample the cpuSample to set
     */
    public void setCpuSample(DimensionResult<TaskCount> cpuSample) {
        this.cpuSample = cpuSample;
    }

    /**
     * @return the wallClock
     */
    public DimensionResult<TaskSum> getWallClock() {
        return wallClock;
    }

    /**
     * @param wallClock the wallClock to set
     */
    public void setWallClock(DimensionResult<TaskSum> wallClock) {
        this.wallClock = wallClock;
    }

    /**
     * @return the allocations
     */
    public DimensionResult<TaskAllocations> getAllocations() {
        return allocations;
    }

    /**
     * @param allocations the allocations to set
     */
    public void setAllocations(DimensionResult<TaskAllocations> allocations) {
        this.allocations = allocations;
    }

    /**
     * @return the allocatedMemory
     */
    public DimensionResult<TaskAllocatedMemory> getAllocatedMemory() {
        return allocatedMemory;
    }

    /**
     * @param allocatedMemory the allocatedMemory to set
     */
    public void setAllocatedMemory(DimensionResult<TaskAllocatedMemory> allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }

    /**
     * @return the nativeExecutionSamples
     */
    public DimensionResult<TaskCount> getNativeExecutionSamples() {
        return nativeExecutionSamples;
    }

    /**
     * @param nativeExecutionSamples the nativeExecutionSamples to set
     */
    public void setNativeExecutionSamples(DimensionResult<TaskCount> nativeExecutionSamples) {
        this.nativeExecutionSamples = nativeExecutionSamples;
    }

    /**
     * @return the fileIOTime
     */
    public DimensionResult<TaskSum> getFileIOTime() {
        return fileIOTime;
    }

    /**
     * @param fileIOTime the fileIOTime to set
     */
    public void setFileIOTime(DimensionResult<TaskSum> fileIOTime) {
        this.fileIOTime = fileIOTime;
    }

    /**
     * @return the fileReadSize
     */
    public DimensionResult<TaskSum> getFileReadSize() {
        return fileReadSize;
    }

    /**
     * @param fileReadSize the fileReadSize to set
     */
    public void setFileReadSize(DimensionResult<TaskSum> fileReadSize) {
        this.fileReadSize = fileReadSize;
    }

    /**
     * @return the fileWriteSize
     */
    public DimensionResult<TaskSum> getFileWriteSize() {
        return fileWriteSize;
    }

    /**
     * @param fileWriteSize the fileWriteSize to set
     */
    public void setFileWriteSize(DimensionResult<TaskSum> fileWriteSize) {
        this.fileWriteSize = fileWriteSize;
    }

    /**
     * @return the socketReadSize
     */
    public DimensionResult<TaskSum> getSocketReadSize() {
        return socketReadSize;
    }

    /**
     * @param socketReadSize the socketReadSize to set
     */
    public void setSocketReadSize(DimensionResult<TaskSum> socketReadSize) {
        this.socketReadSize = socketReadSize;
    }

    /**
     * @return the socketReadTime
     */
    public DimensionResult<TaskSum> getSocketReadTime() {
        return socketReadTime;
    }

    /**
     * @param socketReadTime the socketReadTime to set
     */
    public void setSocketReadTime(DimensionResult<TaskSum> socketReadTime) {
        this.socketReadTime = socketReadTime;
    }

    /**
     * @return the socketWriteSize
     */
    public DimensionResult<TaskSum> getSocketWriteSize() {
        return socketWriteSize;
    }

    /**
     * @param socketWriteSize the socketWriteSize to set
     */
    public void setSocketWriteSize(DimensionResult<TaskSum> socketWriteSize) {
        this.socketWriteSize = socketWriteSize;
    }

    /**
     * @return the socketWriteTime
     */
    public DimensionResult<TaskSum> getSocketWriteTime() {
        return socketWriteTime;
    }

    /**
     * @param socketWriteTime the socketWriteTime to set
     */
    public void setSocketWriteTime(DimensionResult<TaskSum> socketWriteTime) {
        this.socketWriteTime = socketWriteTime;
    }

    /**
     * @return the synchronization
     */
    public DimensionResult<TaskSum> getSynchronization() {
        return synchronization;
    }

    /**
     * @param synchronization the synchronization to set
     */
    public void setSynchronization(DimensionResult<TaskSum> synchronization) {
        this.synchronization = synchronization;
    }

    /**
     * @return the threadPark
     */
    public DimensionResult<TaskSum> getThreadPark() {
        return threadPark;
    }

    /**
     * @param threadPark the threadPark to set
     */
    public void setThreadPark(DimensionResult<TaskSum> threadPark) {
        this.threadPark = threadPark;
    }

    /**
     * @return the classLoadCount
     */
    public DimensionResult<TaskCount> getClassLoadCount() {
        return classLoadCount;
    }

    /**
     * @param classLoadCount the classLoadCount to set
     */
    public void setClassLoadCount(DimensionResult<TaskCount> classLoadCount) {
        this.classLoadCount = classLoadCount;
    }

    /**
     * @return the classLoadWallTime
     */
    public DimensionResult<TaskSum> getClassLoadWallTime() {
        return classLoadWallTime;
    }

    /**
     * @param classLoadWallTime the classLoadWallTime to set
     */
    public void setClassLoadWallTime(DimensionResult<TaskSum> classLoadWallTime) {
        this.classLoadWallTime = classLoadWallTime;
    }

    /**
     * @return the threadSleepTime
     */
    public DimensionResult<TaskSum> getThreadSleepTime() {
        return threadSleepTime;
    }

    /**
     * @param threadSleepTime the threadSleepTime to set
     */
    public void setThreadSleepTime(DimensionResult<TaskSum> threadSleepTime) {
        this.threadSleepTime = threadSleepTime;
    }

    /**
     * @return the problems
     */
    public List<Problem> getProblems() {
        return problems;
    }

    /**
     * @param problems the problems to set
     */
    public void setProblems(List<Problem> problems) {
        this.problems = problems;
    }
}
