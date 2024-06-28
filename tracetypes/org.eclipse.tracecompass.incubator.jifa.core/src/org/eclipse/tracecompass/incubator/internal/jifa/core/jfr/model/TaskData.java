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

import java.util.Map;

public class TaskData {
    public TaskData(RecordedThread thread) {
        this.thread = thread;
    }

    private RecordedThread thread;

    private Map<RecordedStackTrace, Long> samples;

    /**
     * @return the thread
     */
    public RecordedThread getThread() {
        return thread;
    }

    /**
     * @param thread the thread to set
     */
    public void setThread(RecordedThread thread) {
        this.thread = thread;
    }

    /**
     * @return the samples
     */
    public Map<RecordedStackTrace, Long> getSamples() {
        return samples;
    }

    /**
     * @param samples the samples to set
     */
    public void setSamples(Map<RecordedStackTrace, Long> samples) {
        this.samples = samples;
    }
}