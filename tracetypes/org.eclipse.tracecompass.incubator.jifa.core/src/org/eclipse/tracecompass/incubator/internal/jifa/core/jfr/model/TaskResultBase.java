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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TaskResultBase {
    private Task task;
    private Map<StackTrace, Long> samples;

    public TaskResultBase(Task task) {
        this.task = task;
        samples = new HashMap<>();
    }

    public TaskResultBase() {
    }

    public void merge(StackTrace st, long value) {
        if (samples == null) {
            samples = new HashMap<>();
        }
        if (st == null || value <= 0) {
            return;
        }
        Long retVal = samples.get(st);
        samples.put(st, retVal != null ? retVal + value : value);
    }

    /**
     * @return the task
     */
    public Task getTask() {
        return task;
    }

    /**
     * @return the samples
     */
    public Map<StackTrace, Long> getSamples() {
        return samples;
    }

    /**
     * @param task
     *            the task to set
     */
    public void setTask(Task task) {
        this.task = task;
    }

    /**
     * @param map
     *            the samples to set
     */
    public void setSamples(Map<StackTrace, Long> gcSamples) {
        this.samples.clear();
        this.samples.putAll(gcSamples);
    }
    /**
     * @param map
     *            the samples to set
     */
    public void setUnknownSamples(Map<Object, Long> map) {
        this.samples.clear();
        for(Entry<Object, Long> entry : map.entrySet()) {
            if (entry.getKey() instanceof StackTrace) {
            this.samples.put((StackTrace) entry.getKey(), entry.getValue());
            }

        }
    }
}
