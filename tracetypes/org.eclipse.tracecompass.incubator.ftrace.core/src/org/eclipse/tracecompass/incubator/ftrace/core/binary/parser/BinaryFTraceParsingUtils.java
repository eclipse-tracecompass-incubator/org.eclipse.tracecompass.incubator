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

package org.eclipse.tracecompass.incubator.ftrace.core.binary.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!strField.isEmpty()) { //$NON-NLS-1$
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
     * @throws Exception
     *             Cannot parse the field string
     */
    private static BinaryFTraceFormatField parseEventFormatField(String strField)  {
        BinaryFTraceFormatFieldBuilder eventFormatBuilder = new BinaryFTraceFormatFieldBuilder();
        String[] fieldProperties = strField.split(BinaryFTraceConstants.EVENT_FORMAT_FIELD_PROPERTIES_SEPARATOR);
        for (String property : fieldProperties) {
            String[] keyValuePair = property.split(BinaryFTraceConstants.EVENT_FORMAT_VALUE_SEPARATOR);
            String key = keyValuePair[0].trim();
            String value = keyValuePair[1].trim();

            switch (key) {
            case BinaryFTraceConstants.EVENT_FORMAT_FIELD_NAME_LABEL: {
                String[] words = value.split(BinaryFTraceConstants.EVENT_FORMAT_FIELD_MODIFIER_SEPARATOR);
                String fieldName = words[words.length - 1];
                Matcher matcher = Pattern.compile(".*\\[[0-9]*\\]$").matcher(fieldName); //$NON-NLS-1$

                if (matcher.matches()) {
                    Matcher arraySizeMatcher = BinaryFTraceConstants.PATTERN_DATA_TYPE_ARRAY_SIZE.matcher(fieldName);
                    if (arraySizeMatcher.find()) {
                        fieldName = fieldName.replace(arraySizeMatcher.group(), ""); //$NON-NLS-1$
                    }
                }
                eventFormatBuilder.fieldName(fieldName);
                eventFormatBuilder.fieldType(BinaryFTraceParsingUtils.getFtraceDataTypeFromString(value));
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

    /**
     * Parse the data type from the field name property of a event format and
     * map the C data type to a value of the {@link BinaryFTraceDataType} enum
     * type.
     *
     * @param typeModifier
     *            A string containing the modifiers of a field
     * @return A {@link BinaryFTraceDataType} value.
     */
    private static BinaryFTraceDataType getFtraceDataTypeFromString(String typeModifier) {
        for (BinaryFTraceDataType type : BinaryFTraceDataType.values()) {
            boolean result = type.getPattern().matcher(typeModifier).matches();
            if(result) {
                return type;
            }
        }
        return BinaryFTraceDataType.UNKNOWN;
    }
}
