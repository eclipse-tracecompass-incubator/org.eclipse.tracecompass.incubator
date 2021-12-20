/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event;

import java.util.Map;

/**
 * A trace event parsed from the binary FTrace file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceEvent {
    // Event info
    private final long fTimeSinceBoot;
    private final int fCpu;
    private final String fEventName;

    // Event payload
    private final Map<String, Object> fFields;

    /**
     * Constructor
     *
     * @param timeSinceBoot
     *            The time stamp of the event (since boot)
     * @param fields
     *            The properties of the event as key-value pairs
     * @param eventName
     *            The name of the event
     * @param cpu
     *            The CPU number of the event
     */
    public BinaryFTraceEvent(long timeSinceBoot, Map<String, Object> fields, String eventName, int cpu) {
        fTimeSinceBoot = timeSinceBoot;
        fFields = fields;
        fCpu = cpu;
        fEventName = eventName;
    }

    /**
     * Get the CPU of the event
     *
     * @return the CPU number of the event
     */
    public int getCpu() {
        return fCpu;
    }

    /**
     * Get the pay load of the event as a map, including the name of the event.
     * The key is the field name. The value is the field value as an Object
     * instance.
     *
     * @return the type length of the event
     */
    public Map<String, Object> getFields() {
        return fFields;
    }

    /**
     * Get the absolute time stamp of this event. This value is the time since
     * boot of the machine where the trace is recorded, in contrast to time
     * delta, which is the amount of time passed since last event.
     *
     * @return The absolute time stamp of this event.
     */
    public long getTimeSinceBoot() {
        return fTimeSinceBoot;
    }

    /**
     * Get the name of the event
     *
     * @return The name of the event
     */
    public String getEventName() {
        return fEventName;
    }
}
