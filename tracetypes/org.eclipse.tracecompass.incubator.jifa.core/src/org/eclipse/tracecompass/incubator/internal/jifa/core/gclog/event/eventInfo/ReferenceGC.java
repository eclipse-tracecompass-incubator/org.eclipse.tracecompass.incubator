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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

public class ReferenceGC {

    private double softReferenceStartTime = UNKNOWN_DOUBLE;
    private int softReferenceCount = UNKNOWN_INT;
    private double softReferencePauseTime = UNKNOWN_DOUBLE;

    private double weakReferenceStartTime = UNKNOWN_DOUBLE;
    private int weakReferenceCount = UNKNOWN_INT;
    private double weakReferencePauseTime = UNKNOWN_DOUBLE;

    private double finalReferenceStartTime = UNKNOWN_DOUBLE;
    private int finalReferenceCount = UNKNOWN_INT;
    private double finalReferencePauseTime = UNKNOWN_DOUBLE;

    private double phantomReferenceStartTime = UNKNOWN_DOUBLE;
    private int phantomReferenceCount = UNKNOWN_INT;
    private int phantomReferenceFreedCount;
    private double phantomReferencePauseTime = UNKNOWN_DOUBLE;

    private double jniWeakReferenceStartTime = UNKNOWN_DOUBLE;
    private double jniWeakReferencePauseTime = UNKNOWN_DOUBLE;
    /**
     * @return the softReferenceStartTime
     */
    public double getSoftReferenceStartTime() {
        return softReferenceStartTime;
    }
    /**
     * @param softReferenceStartTime the softReferenceStartTime to set
     */
    public void setSoftReferenceStartTime(double softReferenceStartTime) {
        this.softReferenceStartTime = softReferenceStartTime;
    }
    /**
     * @return the softReferenceCount
     */
    public int getSoftReferenceCount() {
        return softReferenceCount;
    }
    /**
     * @param softReferenceCount the softReferenceCount to set
     */
    public void setSoftReferenceCount(int softReferenceCount) {
        this.softReferenceCount = softReferenceCount;
    }
    /**
     * @return the softReferencePauseTime
     */
    public double getSoftReferencePauseTime() {
        return softReferencePauseTime;
    }
    /**
     * @param softReferencePauseTime the softReferencePauseTime to set
     */
    public void setSoftReferencePauseTime(double softReferencePauseTime) {
        this.softReferencePauseTime = softReferencePauseTime;
    }
    /**
     * @return the weakReferenceStartTime
     */
    public double getWeakReferenceStartTime() {
        return weakReferenceStartTime;
    }
    /**
     * @param weakReferenceStartTime the weakReferenceStartTime to set
     */
    public void setWeakReferenceStartTime(double weakReferenceStartTime) {
        this.weakReferenceStartTime = weakReferenceStartTime;
    }
    /**
     * @return the weakReferenceCount
     */
    public int getWeakReferenceCount() {
        return weakReferenceCount;
    }
    /**
     * @param weakReferenceCount the weakReferenceCount to set
     */
    public void setWeakReferenceCount(int weakReferenceCount) {
        this.weakReferenceCount = weakReferenceCount;
    }
    /**
     * @return the weakReferencePauseTime
     */
    public double getWeakReferencePauseTime() {
        return weakReferencePauseTime;
    }
    /**
     * @param weakReferencePauseTime the weakReferencePauseTime to set
     */
    public void setWeakReferencePauseTime(double weakReferencePauseTime) {
        this.weakReferencePauseTime = weakReferencePauseTime;
    }
    /**
     * @return the finalReferenceStartTime
     */
    public double getFinalReferenceStartTime() {
        return finalReferenceStartTime;
    }
    /**
     * @param finalReferenceStartTime the finalReferenceStartTime to set
     */
    public void setFinalReferenceStartTime(double finalReferenceStartTime) {
        this.finalReferenceStartTime = finalReferenceStartTime;
    }
    /**
     * @return the finalReferenceCount
     */
    public int getFinalReferenceCount() {
        return finalReferenceCount;
    }
    /**
     * @param finalReferenceCount the finalReferenceCount to set
     */
    public void setFinalReferenceCount(int finalReferenceCount) {
        this.finalReferenceCount = finalReferenceCount;
    }
    /**
     * @return the finalReferencePauseTime
     */
    public double getFinalReferencePauseTime() {
        return finalReferencePauseTime;
    }
    /**
     * @param finalReferencePauseTime the finalReferencePauseTime to set
     */
    public void setFinalReferencePauseTime(double finalReferencePauseTime) {
        this.finalReferencePauseTime = finalReferencePauseTime;
    }
    /**
     * @return the phantomReferenceStartTime
     */
    public double getPhantomReferenceStartTime() {
        return phantomReferenceStartTime;
    }
    /**
     * @param phantomReferenceStartTime the phantomReferenceStartTime to set
     */
    public void setPhantomReferenceStartTime(double phantomReferenceStartTime) {
        this.phantomReferenceStartTime = phantomReferenceStartTime;
    }
    /**
     * @return the phantomReferenceCount
     */
    public int getPhantomReferenceCount() {
        return phantomReferenceCount;
    }
    /**
     * @param phantomReferenceCount the phantomReferenceCount to set
     */
    public void setPhantomReferenceCount(int phantomReferenceCount) {
        this.phantomReferenceCount = phantomReferenceCount;
    }
    /**
     * @return the phantomReferenceFreedCount
     */
    public int getPhantomReferenceFreedCount() {
        return phantomReferenceFreedCount;
    }
    /**
     * @param phantomReferenceFreedCount the phantomReferenceFreedCount to set
     */
    public void setPhantomReferenceFreedCount(int phantomReferenceFreedCount) {
        this.phantomReferenceFreedCount = phantomReferenceFreedCount;
    }
    /**
     * @return the phantomReferencePauseTime
     */
    public double getPhantomReferencePauseTime() {
        return phantomReferencePauseTime;
    }
    /**
     * @param phantomReferencePauseTime the phantomReferencePauseTime to set
     */
    public void setPhantomReferencePauseTime(double phantomReferencePauseTime) {
        this.phantomReferencePauseTime = phantomReferencePauseTime;
    }
    /**
     * @return the jniWeakReferenceStartTime
     */
    public double getJniWeakReferenceStartTime() {
        return jniWeakReferenceStartTime;
    }
    /**
     * @param jniWeakReferenceStartTime the jniWeakReferenceStartTime to set
     */
    public void setJniWeakReferenceStartTime(double jniWeakReferenceStartTime) {
        this.jniWeakReferenceStartTime = jniWeakReferenceStartTime;
    }
    /**
     * @return the jniWeakReferencePauseTime
     */
    public double getJniWeakReferencePauseTime() {
        return jniWeakReferencePauseTime;
    }
    /**
     * @param jniWeakReferencePauseTime the jniWeakReferencePauseTime to set
     */
    public void setJniWeakReferencePauseTime(double jniWeakReferencePauseTime) {
        this.jniWeakReferencePauseTime = jniWeakReferencePauseTime;
    }
}
