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

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_INT;

public class ObjectStatistics {
    private double objectCreationSpeed = UNKNOWN_DOUBLE; // B/ms
    private double objectPromotionSpeed = UNKNOWN_DOUBLE; // B/ms
    private long objectPromotionAvg = UNKNOWN_INT; // B
    private long objectPromotionMax = UNKNOWN_INT; // B

    public ObjectStatistics(double d, double e, long average, long max) {
        objectCreationSpeed = d;
        objectPromotionSpeed = e;
        objectPromotionAvg = average;
        objectPromotionMax = max;
    }

    /**
     * @return the objectCreationSpeed
     */
    public double getObjectCreationSpeed() {
        return objectCreationSpeed;
    }

    /**
     * @param objectCreationSpeed
     *            the objectCreationSpeed to set
     */
    public void setObjectCreationSpeed(double objectCreationSpeed) {
        this.objectCreationSpeed = objectCreationSpeed;
    }

    /**
     * @return the objectPromotionSpeed
     */
    public double getObjectPromotionSpeed() {
        return objectPromotionSpeed;
    }

    /**
     * @param objectPromotionSpeed
     *            the objectPromotionSpeed to set
     */
    public void setObjectPromotionSpeed(double objectPromotionSpeed) {
        this.objectPromotionSpeed = objectPromotionSpeed;
    }

    /**
     * @return the objectPromotionAvg
     */
    public long getObjectPromotionAvg() {
        return objectPromotionAvg;
    }

    /**
     * @param objectPromotionAvg
     *            the objectPromotionAvg to set
     */
    public void setObjectPromotionAvg(long objectPromotionAvg) {
        this.objectPromotionAvg = objectPromotionAvg;
    }

    /**
     * @return the objectPromotionMax
     */
    public long getObjectPromotionMax() {
        return objectPromotionMax;
    }

    /**
     * @param objectPromotionMax
     *            the objectPromotionMax to set
     */
    public void setObjectPromotionMax(long objectPromotionMax) {
        this.objectPromotionMax = objectPromotionMax;
    }
}
