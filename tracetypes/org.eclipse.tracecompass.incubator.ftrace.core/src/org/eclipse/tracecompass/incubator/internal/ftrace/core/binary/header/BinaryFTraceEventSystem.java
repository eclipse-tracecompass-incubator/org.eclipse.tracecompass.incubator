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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

import java.util.Map;

/**
 * A binary FTrace event system, which contains different event formats
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceEventSystem {
    private final String fSystemName;
    private final Map<Integer, BinaryFTraceEventFormat> fEventFormats;

    /**
     * Constructor
     *
     * @param systemName The name of the event system
     * @param eventFormats The event formats under this system
     */
    public BinaryFTraceEventSystem(String systemName, Map<Integer, BinaryFTraceEventFormat> eventFormats) {
        fSystemName = systemName;
        fEventFormats = eventFormats;
    }

    /**
     * Get the name of the event system
     *
     * @return The name of the event system
     */
    public String getSystemName() {
        return fSystemName;
    }

    /**
     * Add an event format to the list of event formats of the system
     *
     * @param eventFormat An event format to add to the system
     */
    public void addEventFormat(BinaryFTraceEventFormat eventFormat) {
        fEventFormats.put(eventFormat.getEventFormatID(), eventFormat);
    }

    /**
     * Get a hash map of event formats of this event system
     *
     * @return A hash map of event formats of this event system
     */
    public Map<Integer, BinaryFTraceEventFormat> getMapEventFormat() {
        return fEventFormats;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("===Event System===").append('\n'); //$NON-NLS-1$

        strBuilder.append("Event system name: ").append(fSystemName).append('\n'); //$NON-NLS-1$

        strBuilder.append("List of events: ").append('\n'); //$NON-NLS-1$
        for (BinaryFTraceEventFormat value : fEventFormats.values()) {
            strBuilder.append(value).append('\n');
        }

        return strBuilder.toString();
    }
}
