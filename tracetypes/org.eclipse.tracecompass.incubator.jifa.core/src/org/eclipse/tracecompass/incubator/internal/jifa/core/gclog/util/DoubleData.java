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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util;
import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoubleData {

    private int n = 0;
    private double sum = 0;
    private double min = Double.MAX_VALUE;
    // min value of double is not Double.MIN_VALUE
    private double max = -Double.MAX_VALUE;
    private List<Double> originalData;
    private boolean dataSorted;

    public DoubleData(boolean recordOriginalData) {
        // recording all data is expensive, only do it if necessary
        if (recordOriginalData) {
            originalData = new ArrayList<>();
        }
    }

    public DoubleData() {
        this(false);
    }

    public double getMedian() {
        return getPercentile(0.5);
    }

    public double getPercentile(double percentile) {
        // should not call this method if originalData is null
        if (originalData.size() == 0) {
            return UNKNOWN_DOUBLE;
        }
        if (!dataSorted) {
            Collections.sort(originalData);
            dataSorted = true;
        }

        double p = (n - 1) * percentile;
        int i = (int) Math.floor(p);
        double weight = p - i;
        if (weight == 0) {
            return originalData.get(i);
        }
        return weight * originalData.get(i + 1) + (1 - weight) * originalData.get(i);
    }

    public void add(double x) {
        if (x == UNKNOWN_DOUBLE) {
            return;
        }
        if (originalData != null) {
            originalData.add(x);
            dataSorted = false;
        }
        sum += x;
        n++;
        min = Math.min(min, x);
        max = Math.max(max, x);
    }

    public int getN() {
        return n;
    }

    public double getSum() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return sum;
    }

    public double getMin() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return min;
    }

    public double getMax() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return max;
    }

    public double average() {
        if (n == 0) {
            return UNKNOWN_DOUBLE;
        }
        return sum / n;
    }
}
