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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceDataType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField.BinaryFTraceFormatFieldBuilder;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceValueSign;

/**
 * A class contains static methods to help parse the binary FTrace files. It
 * contains utility functions that does not require a buffer, mainly to process
 * the data retrieve from the file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceParsingUtils {
    private BinaryFTraceParsingUtils() {
        // Do nothing
    }

    /**
     * Parse an event format string
     *
     * @param strEventFormat
     *            A string representation of the event format
     * @return A BinaryFTraceEventFormat that contains all the format
     *         information
     */
    protected static BinaryFTraceEventFormat parseEventFormat(String strEventFormat) {
        int nameLabelIndex = strEventFormat.indexOf(BinaryFTraceConstants.EVENT_FORMAT_NAME_LABEL);
        int idLabelIndex = strEventFormat.indexOf(BinaryFTraceConstants.EVENT_FORMAT_ID_LABEL);
        int formatLabelIndex = strEventFormat.indexOf(BinaryFTraceConstants.EVENT_FORMAT_FORMAT_LABEL);
        int formatStringLabelIndex = strEventFormat.indexOf(BinaryFTraceConstants.EVENT_FORMAT_PRINT_FORMAT_LABEL);

        String eventFormatName = strEventFormat.substring(nameLabelIndex + BinaryFTraceConstants.EVENT_FORMAT_NAME_LABEL.length() + 1, idLabelIndex).trim();
        int eventFormatID = Integer
                .parseInt(strEventFormat.substring(idLabelIndex + BinaryFTraceConstants.EVENT_FORMAT_ID_LABEL.length() + 1, formatLabelIndex).trim());
        String strEventFields = strEventFormat
                .substring(formatLabelIndex + BinaryFTraceConstants.EVENT_FORMAT_FORMAT_LABEL.length() + 1, formatStringLabelIndex).trim();
        String formatString = strEventFormat.substring(formatStringLabelIndex);

        /*
         * Parse the fields of the format
         */
        String[] fields = strEventFields.split(BinaryFTraceConstants.EVENT_FORMAT_FIELD_SEPARATOR);
        Map<String, BinaryFTraceFormatField> lstFieldsCommon = new HashMap<>();
        Map<String, BinaryFTraceFormatField> lstFieldsCustom = new HashMap<>();

        for (int i = 0; i < fields.length; i++) {
            String strField = fields[i].trim();
            if (!strField.equals("")) { //$NON-NLS-1$
                BinaryFTraceFormatField eventFormatField = BinaryFTraceParsingUtils.parseEventFormatField(strField);

                if (eventFormatField.getFieldName().contains(BinaryFTraceConstants.EVENT_FORMAT_COMMON_FIELD_PREFIX)) {
                    lstFieldsCommon.put(eventFormatField.getFieldName(), eventFormatField);
                } else {
                    lstFieldsCustom.put(eventFormatField.getFieldName(), eventFormatField);
                }
            }
        }

        return new BinaryFTraceEventFormat(eventFormatID,
                eventFormatName,
                lstFieldsCommon,
                lstFieldsCustom,
                formatString);
    }

    /**
     * Parse fields (used when fields are not associated with an event format).
     *
     * @param fields
     *            A String array of field strings.
     * @return An array list of fields
     */
    protected static List<BinaryFTraceFormatField> parseEventFormatFields(String[] fields) {
        List<BinaryFTraceFormatField> lstFields = new ArrayList<>();

        for (int i = 0; i < fields.length; i++) {
            String strField = fields[i].trim();
            if (!strField.isEmpty()) { // $NON-NLS-1$
                BinaryFTraceFormatField eventFormatField = BinaryFTraceParsingUtils.parseEventFormatField(strField);
                lstFields.add(eventFormatField);
            }
        }

        return lstFields;
    }

    /**
     * Parse a string representation of a field and map the values to a
     * BinaryFTraceFormatField object.
     *
     * @param strField
     *            A string representation of a field
     * @return A BinaryFTraceFormatField that contains all information of a
     *         field
     */
    private static BinaryFTraceFormatField parseEventFormatField(String strField) {
        BinaryFTraceFormatFieldBuilder eventFormatBuilder = new BinaryFTraceFormatFieldBuilder();
        String[] fieldProperties = strField.split(BinaryFTraceConstants.EVENT_FORMAT_FIELD_PROPERTIES_SEPARATOR);
        for (String property : fieldProperties) {
            String[] keyValuePair = property.split(BinaryFTraceConstants.EVENT_FORMAT_VALUE_SEPARATOR);
            String key = keyValuePair[0].trim();
            String value = keyValuePair[1].trim();

            switch (key) {
            case BinaryFTraceConstants.EVENT_FORMAT_FIELD_NAME_LABEL: {

                // Match the field a regex to extract different fields
                Matcher fieldNameMatcher = BinaryFTraceConstants.FIELD_NAME_PATTERN.matcher(value);

                if (fieldNameMatcher.matches()) {
                    // Parse the field name
                    String fieldName = fieldNameMatcher.group(BinaryFTraceConstants.NAME_GROUP);
                    eventFormatBuilder.fieldName(fieldName);

                    // Check if the field is an array and try to parse its length
                    int arrayLength = BinaryFTraceFormatFieldBuilder.DEFAULT_ARRAY_LENGTH;
                    String strArrayLength = fieldNameMatcher.group(BinaryFTraceConstants.ARRAY_LENGTH_GROUP);
                    if (strArrayLength != null) {
                        if (strArrayLength.trim().isEmpty()) {
                            arrayLength = BinaryFTraceFormatFieldBuilder.UNKNOWN_ARRAY_LENGTH;
                        } else {
                            arrayLength = Integer.parseInt(strArrayLength);
                        }
                    } else if (fieldNameMatcher.group(BinaryFTraceConstants.ARRAY_GROUP) != null) {
                        arrayLength = BinaryFTraceFormatFieldBuilder.UNKNOWN_ARRAY_LENGTH;
                    }
                    eventFormatBuilder.array(arrayLength);

                    // Parse the data type of the field
                    BinaryFTraceDataType type = BinaryFTraceDataType.getDataType(fieldNameMatcher.group(BinaryFTraceConstants.DATA_TYPE_GROUP));
                    eventFormatBuilder.fieldType(type);

                    // Check if the field is a data_loc (data location)
                    boolean isDataLoc = fieldNameMatcher.group((BinaryFTraceConstants.DATALOC_GROUP)) != null;
                    eventFormatBuilder.dataLoc(isDataLoc);

                    // Check if the field is a pointer
                    boolean isPointer = fieldNameMatcher.group((BinaryFTraceConstants.POINTER_GROUP)) != null;
                    eventFormatBuilder.pointer(isPointer);

                    // Save the original field string in case we need it
                    eventFormatBuilder.strFieldType(value);
                }

                break;
            }
            case BinaryFTraceConstants.EVENT_FORMAT_FIELD_OFFSET_LABEL: {
                eventFormatBuilder.offset(Integer.parseInt(value));
                break;
            }
            case BinaryFTraceConstants.EVENT_FORMAT_FIELD_SIZE_LABEL: {
                eventFormatBuilder.size(Integer.parseInt(value));
                break;
            }
            case BinaryFTraceConstants.EVENT_FORMAT_FIELD_SIGN_LABEL: {
                BinaryFTraceValueSign sign = BinaryFTraceValueSign.getEnumFromString(value);
                if (sign != null) {
                    eventFormatBuilder.signed(sign);
                }
                break;
            }
            default:
                break;
            }
        }

        return eventFormatBuilder.build();
    }
}
