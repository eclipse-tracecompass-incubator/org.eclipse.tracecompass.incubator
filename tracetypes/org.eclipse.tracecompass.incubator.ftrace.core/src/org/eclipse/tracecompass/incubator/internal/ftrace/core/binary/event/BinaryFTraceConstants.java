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

import java.util.regex.Pattern;

/**
 * Constants values used for parsing Binary FTrace file.
 *
 * @author Hoang Thuan Pham
 */
public final class BinaryFTraceConstants {

    private BinaryFTraceConstants() {
        // Do Nothing
    }


    // Constants for parsing event formats
    /**
     * Label of the event format name in an event format in the binary FTrace
     * file header.
     */
    public static final String EVENT_FORMAT_NAME_LABEL = "Name:"; //$NON-NLS-1$
    /**
     * Label of the event format ID in an event format in the binary FTrace file
     * header.
     */
    public static final String EVENT_FORMAT_ID_LABEL = "ID"; //$NON-NLS-1$
    /**
     * Label of the actual format specification in an event format in the binary
     * FTrace file header.
     */
    public static final String EVENT_FORMAT_FORMAT_LABEL = "format:"; //$NON-NLS-1$
    /**
     * Label of the print format of an event in an event format in the binary
     * FTrace file header.
     */
    public static final String EVENT_FORMAT_PRINT_FORMAT_LABEL = "print fmt:"; //$NON-NLS-1$

    // In each event format there is a format section, divided into different
    // fields. Each field contains different properties.
    /**
     * Label of the field property in an event format in the binary FTrace file
     * header. The field property contains the field name, field type and other
     * modifiers in C.
     */
    public static final String EVENT_FORMAT_FIELD_NAME_LABEL = "field"; //$NON-NLS-1$
    /**
     * Label of the offset property in an event format in the binary FTrace file
     * header. The offset property is used to parse the field value in the
     * binary byte stream from the trace file in relative to the binary stream.
     * The offset is in bytes.
     */
    public static final String EVENT_FORMAT_FIELD_OFFSET_LABEL = "offset"; //$NON-NLS-1$
    /**
     * Label of the size property in an event format in the binary FTrace file
     * header. The size property is used to parse the field value in the binary
     * byte stream from the trace file. The size is in bytes.
     */
    public static final String EVENT_FORMAT_FIELD_SIZE_LABEL = "size"; //$NON-NLS-1$
    /**
     * Label of the signed property in an event format in the binary FTrace file
     * header. The signed property is used to parse the field value in the
     * binary byte stream from the trace file. Its possible value is 0(unsigned)
     * or 1(signed).
     */
    public static final String EVENT_FORMAT_FIELD_SIGN_LABEL = "signed"; //$NON-NLS-1$
    /**
     * A separator used to separate each field in an event format. Has to be
     * string type to be used with String.split()
     */
    public static final String EVENT_FORMAT_FIELD_SEPARATOR = "\n"; //$NON-NLS-1$
    /**
     * A separator to separate different properties of a field.
     */
    public static final String EVENT_FORMAT_FIELD_PROPERTIES_SEPARATOR = ";"; //$NON-NLS-1$
    /**
     * A separator to separate the property label with the property value.
     */
    public static final String EVENT_FORMAT_VALUE_SEPARATOR = ":"; //$NON-NLS-1$
    /**
     * A separator to separate different modifiers of a field property that
     * contains the field type, field name and other C modifiers.
     */
    public static final String EVENT_FORMAT_FIELD_MODIFIER_SEPARATOR = " "; //$NON-NLS-1$
    /**
     * A type modifier that indicates the field is a constant.
     */
    public static final String EVENT_FORMAT_FIELD_CONSTANT_MODIFIER = "const"; //$NON-NLS-1$
    /**
     * A type modifier that indicates the field is a signed value.
     */
    public static final String EVENT_FORMAT_FIELD_SIGNED_MODIFIER = "signed"; //$NON-NLS-1$
    /**
     * A type modifier that indicates the field is a unsigned value.
     */
    public static final String EVENT_FORMAT_FIELD_UNSIGNED_MODIFIER = "unsigned"; //$NON-NLS-1$
    /**
     * A type modifier that indicates the data type of the field is used
     * internally.
     */
    public static final String EVENT_FORMAT_FIELD_INTERNAL_PREFIX = "__"; //$NON-NLS-1$
    /**
     * A prefix indicating that the field is a common field.
     */
    public static final String EVENT_FORMAT_COMMON_FIELD_PREFIX = "common_"; //$NON-NLS-1$
    /**
     * A modifier indicating that the field is of type pid_t (int).
     */
    public static final String FIELD_MODIFIER_PID_T = "pid_t"; //$NON-NLS-1$
    /**
     * A modifier indicating that the field is a string (is a char array).
     */
    public static final String FIELD_MODIFIER_CHAR_ARRAY = "char[]"; //$NON-NLS-1$
    /**
     * Pattern to validate the signed property of a field in an event format.
     */
    public static final Pattern VALID_SIGN_VALUES = Pattern.compile("[01]"); //$NON-NLS-1$

    /**
     * A regex pattern that matches the array data type in the field property of
     * a event format field.
     */
    public static final Pattern PATTERN_DATA_TYPE_ARRAY = Pattern.compile(".*\\[[0-9]*\\]$"); //$NON-NLS-1$
    /**
     * A regex pattern that matches the square brackets that indicate the size
     * of an array.
     */
    public static final Pattern PATTERN_DATA_TYPE_ARRAY_SIZE = Pattern.compile("\\[([0-9]*)\\]"); //$NON-NLS-1$
    /**
     * A regex pattern that matches the pointer type modifier.
     */
    public static final Pattern PATTERN_POINTER_TYPE = Pattern.compile(".*\\*.*"); //$NON-NLS-1$

    // Constants for parsing binary FTRace in general
    /**
     * A newline character. Has to be string type to be used by String.split()
     */
    public static final String NEW_LINE = "\n"; //$NON-NLS-1$

    // Constants to parse the header event section
    /**
     * A separator to separate label and value in the header event section.
     */
    public static final String HEADER_EVENT_TYPE_LENGTH_VALUE_SEPARATOR = "=="; //$NON-NLS-1$
    /**
     * Label for the type length value of padding event.
     */
    public static final String HEADER_EVENT_INFO_PADDING_LABEL = "padding"; //$NON-NLS-1$
    /**
     * Label for the type length value of absolute time stamp event.
     */
    public static final String HEADER_EVENT_INFO_TIMESTAMP_LABEL = "time_stamp"; //$NON-NLS-1$
    /**
     * Label for the type length value of extended time stamp event.
     */
    public static final String HEADER_EVENT_INFO_EXTENDED_TIMESTAMP_LABEL = "time_extend"; //$NON-NLS-1$
    /**
     * Label for the maximum type length value for trace events with payload.
     */
    public static final String HEADER_EVENT_INFO_DATA_MAX_TYPE_LENGTH_LABEL = "data max type_len"; //$NON-NLS-1$
    /**
     * Label for the the number of bits used for the type length value in each
     * event header.
     */
    public static final String HEADER_EVENT_INFO_TYPE_LENGTH_SIZE_LABEL = "type_len"; //$NON-NLS-1$
    /**
     * Label for the the number of bits used for the time delta value in each
     * event header.
     */
    public static final String HEADER_EVENT_INFO_TIMESTAMP_SIZE_LABEL = "time_delta"; //$NON-NLS-1$
    /**
     * A separator for the label and value in the header event section.
     */
    public static final String HEADER_EVENT_LABEL_VALUE_SEPARATOR = ":"; //$NON-NLS-1$
    /**
     * A separator between the bit number and the bit unit (e.g 12 bits)
     */
    public static final String HEADER_EVENT_BIT_VALUE_UNIT_SEPARATOR = " "; //$NON-NLS-1$

    // Constants to parse the function address to name section.
    /**
     * A separator for the function address to name mappings.
     */
    public static final String FUNCTION_ADDRESS_NAME_SEPARATOR = " "; //$NON-NLS-1$

    // Constants to parse the trace
    /**
     * Label for each trace file option.
     */
    public static final String OPTIONS_SECTION_NAME = "options"; //$NON-NLS-1$
    /**
     * Value indicating that the file is an ASCII file.
     */
    public static final String LATENCY_SECTION_NAME = "latency"; //$NON-NLS-1$
    /**
     * Value indicating that the file is a fly record file.
     */
    public static final String FLYRECORD_SECTION_NAME = "flyrecord"; //$NON-NLS-1$
}
