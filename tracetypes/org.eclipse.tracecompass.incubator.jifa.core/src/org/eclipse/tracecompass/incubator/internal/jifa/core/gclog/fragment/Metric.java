/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment;

import java.util.HashMap;
import java.util.Map;

public class Metric {
    private long timestamp;
    private Map<String, String> label;
    private String fName;
    private double value;

    public Metric(long ts, Map<String, String> lbl, String name, double val) {
        timestamp = ts;
        label = new HashMap<>();
        label.putAll(lbl);
        fName = name;
        value = val;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp
     *            the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the label
     */
    public Map<String, String> getLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    /**
     * @return the fName
     */
    public String getName() {
        return fName;
    }

    /**
     * @param fName
     *            the fName to set
     */
    public void setName(String name) {
        this.fName = name;
    }

    /**
     * @return the value
     */
    double getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    void setValue(double value) {
        this.value = value;
    }
}
