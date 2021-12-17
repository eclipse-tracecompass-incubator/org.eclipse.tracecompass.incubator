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
import java.util.Map.Entry;

/**
 * An event format to parse different FTrace in the CPU data section
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceEventFormat {
    private static final String COMMON_FIELD_PREFIX = "common_"; //$NON-NLS-1$

    private final int fEventFormatID;
    private final String fEventName;
    private final Map<String, BinaryFTraceFormatField> fCustomFields;
    private final Map<String, BinaryFTraceFormatField> fCommonFields;
    private final String fFormatString;

    /**
     * Constructor
     *
     * @param eventFormatID
     *            The ID of the event format. This ID is used to match an event
     *            to its event format using the field common_type of the event.
     * @param eventName
     *            The name of the trace event.
     * @param commonFields
     *            The list of common fields. Common fields start with with the
     *            prefix common_ and all events should have the same common
     *            fields.
     * @param customFields
     *            The list of fields that are specific to this event format
     *            (event type).
     * @param formatString
     *            The string used for printing the event (used my trace-cmd).
     */
    public BinaryFTraceEventFormat(int eventFormatID,
            String eventName,
            Map<String, BinaryFTraceFormatField> commonFields,
            Map<String, BinaryFTraceFormatField> customFields,
            String formatString) {
        fEventFormatID = eventFormatID;
        fEventName = eventName;
        fCommonFields = commonFields;
        fCustomFields = customFields;
        fFormatString = formatString;
    }

    /**
     * Get the format ID of this event format
     *
     * @return the ID of this event format
     */
    public int getEventFormatID() {
        return fEventFormatID;
    }

    /**
     * Get the custom fields (that do not start with common_) of this event
     * format
     *
     * @return A hash map of custom fields
     */
    public Map<String, BinaryFTraceFormatField> getCustomFields() {
        return fCustomFields;
    }

    /**
     * Get the custom fields (that start with common_) of this event format
     *
     * @return A hash map of common fields
     */
    public Map<String, BinaryFTraceFormatField> getCommonFields() {
        return fCommonFields;
    }

    /**
     * Get the name of the event that follows this format
     *
     * @return the name of the event
     */
    public String getEventName() {
        return fEventName;
    }

    /**
     * Get the format string used by print fmt function
     *
     * @return the format string
     */
    public String getFormatString() {
        return fFormatString;
    }

    /**
     * Get the total size of the event type by summing the size of all fields
     *
     * @return The total size of the event
     */
    public int getEventFormatSize() {
        int eventSize = 0;
        for (Entry<String, BinaryFTraceFormatField> entry : fCommonFields.entrySet()) {
            eventSize += entry.getValue().getSize();
        }

        for (Entry<String, BinaryFTraceFormatField> entry : fCustomFields.entrySet()) {
            eventSize += entry.getValue().getSize();
        }

        return eventSize;
    }

    /**
     * Get a field format by its name.
     *
     * @param name
     *            Name of the field
     * @return The field format as a BinaryFTraceFormatField instance
     */
    public BinaryFTraceFormatField getFieldByName(String name) {
        if (name.startsWith(COMMON_FIELD_PREFIX) && fCommonFields.containsKey(name)) {
            return fCommonFields.get(name);
        } else if (fCustomFields.containsKey(name)) {
            return fCustomFields.get(name);
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("Event name: ").append(fEventName).append('\n'); //$NON-NLS-1$
        strBuilder.append("Event ID: ").append(fEventFormatID).append('\n'); //$NON-NLS-1$
        strBuilder.append("Fields: ").append('\n'); //$NON-NLS-1$

        for (Entry<String, BinaryFTraceFormatField> entry : fCommonFields.entrySet()) {
            strBuilder.append('\t').append(entry.getValue()).append('\n');
        }

        for (Entry<String, BinaryFTraceFormatField> entry : fCustomFields.entrySet()) {
            strBuilder.append('\t').append(entry.getValue()).append('\n');
        }

        strBuilder.append("Format string: ").append(fFormatString); //$NON-NLS-1$
        return strBuilder.toString();
    }
}