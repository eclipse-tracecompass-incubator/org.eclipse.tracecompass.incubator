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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFormatField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceValueSign;

/**
 * A helper class to parse trace events.
 *
 * @author Hoang Thuan Pham
 *
 */
public class BinaryFTraceIteratorHelper {
    private final BinaryFTraceHeaderInfo fHeader;

    private static final int TYPE_LENGTH_SHIFT_BIG = 27;
    private static final int TIME_DELTA_SHIFT_LITTLE = 5;
    private static final int DATALOC_SHIFT = 16;

    private static final int TYPE_LENGTH_MASK_LITTLE = 31;
    private static final int TYPE_LENGTH_MASK_BIG = 31 << 27;

    private static final int TIME_DELTA_MASK_LITTLE = ~0 << 5;
    private static final int TIME_DELTA_MASK_BIG = ~0 >> 5;

    private static final int DATA_LOC_OFFSET = 0x0000FFFF;

    private static final String hexPrefix = "0x"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param header
     *            A {@link BinaryFTraceHeaderInfo} that contains the
     *            information required for reading binary FTrace data.
     */
    public BinaryFTraceIteratorHelper(BinaryFTraceHeaderInfo header) {
        fHeader = header;
    }

    /**
     * Perform bit shift operations to obtain the type length (0-31) of each
     * event. The type length of the event is the first 5 bits of the header.
     *
     * @param value
     *            The 4 bytes unsigned integer that is the event header.
     * @return the type length of the event.
     */
    public int getEventTypeLength(int value) {
        if (fHeader.getEndianess() == ByteOrder.LITTLE_ENDIAN) {
            return value & TYPE_LENGTH_MASK_LITTLE;
        }

        // If the host machine is in Big endian
        return (value & TYPE_LENGTH_MASK_BIG) >> TYPE_LENGTH_SHIFT_BIG;
    }

    /**
     * Perform bit shift operations to obtain the time delta of each event. The
     * time delta of the event is the last 27 bits of the header.
     *
     * @param value
     *            The 4 bytes unsigned integer that is the event header.
     * @return the type length of the event.
     */
    public int getDeltaTime(int value) {
        if (fHeader.getEndianess() == ByteOrder.LITTLE_ENDIAN) {
            return (value & TIME_DELTA_MASK_LITTLE) >> TIME_DELTA_SHIFT_LITTLE;
        }

        // If the host machine is in Big endian
        return value & TIME_DELTA_MASK_BIG;
    }

    /**
     * Create a new page iterator from a {@link BinaryFTraceCPUDataPage}
     *
     * @param page
     *            The trace page to iterate over
     * @param headerInfo
     *            The trace header
     * @return The iterator to get events from this page
     * @throws IOException
     *             if an error occurs while creating the page iterator
     */
    public static @Nullable BinaryFTraceCPUPageIterator getPageIterator(BinaryFTraceCPUDataPage page, BinaryFTraceHeaderInfo headerInfo) throws IOException {
        if (headerInfo != null && page != null) {
            return new BinaryFTraceCPUPageIterator(page, headerInfo);
        }

        return null;
    }

    /**
     * Extract all field values from the binary payload of a binary FTrace
     * event.
     *
     * @param eventFormat
     *            The {@link BinaryFTraceEventFormat} that contains information
     *            required to extract the event data.
     * @param data
     *            The binary payload of a binary FTrace event.
     * @return A Map containing all the the parsed field values.
     */
    public Map<String, Object> parseEventData(BinaryFTraceEventFormat eventFormat, byte[] data) {
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put("name", eventFormat.getEventName()); // Store //$NON-NLS-1$
                                                                 // the
                                                                 // event name

        for (Entry<String, BinaryFTraceFormatField> fieldEntry : eventFormat.getCommonFields().entrySet()) {
            Object value = getFieldValue(fieldEntry.getValue(), data);
            eventProperties.put(fieldEntry.getKey(), value);
        }

        for (Entry<String, BinaryFTraceFormatField> fieldEntry : eventFormat.getCustomFields().entrySet()) {
            Object value = getFieldValue(fieldEntry.getValue(), data);
            eventProperties.put(fieldEntry.getKey(), value);
        }

        return eventProperties;
    }

    /**
     * Extract the field value from a binary payload of a binary FTrace event
     * using information provided by a {@link BinaryFTraceFormatField}.
     *
     * @param formatField
     *            The {@link BinaryFTraceFormatField} that contains information
     *            to extract the field value from the binary payload of a binary
     *            FTrace event.
     * @param data
     *            The binary payload of a binary FTrace event.
     * @return An {@link Object} that is the extracted value.
     */
    public Object getFieldValue(BinaryFTraceFormatField formatField, byte[] data) {
        if (formatField.isPointer()) {
            byte[] fieldData = extractValue(formatField, data);
            return readStringPointerField(fieldData);
        }

        if (formatField.isString()) {
            byte[] fieldData = new byte[0];
            if (formatField.isDataLoc()) {
                fieldData = extractDataLocValue(formatField, data);
            } else {
                fieldData = extractValue(formatField, data);
            }

            return readStringField(fieldData);
        }

        if (formatField.getArrayLength() == 0) {
            // For now for non string type we just print out the pointer value
            byte[] fieldData = extractValue(formatField, data);
            return readStringPointerField(fieldData);
        }

        byte[] fieldData = extractValue(formatField, data);
        return getPrimitiveDataValue(formatField, fieldData);
    }

    /**
     * Get the event format based on the common_type field in a binary payload
     * of a binary FTrace event.
     *
     * @param data
     *            The binary payload of a binary FTrace event
     * @return A {@link BinaryFTraceEventFormat} containing the format to parse
     *         the binary FTrace event.
     */
    public BinaryFTraceEventFormat getEventFormat(byte[] data) {
        BinaryFTraceFormatField commonTypeField = fHeader.getEventCommonFields().get("common_type"); //$NON-NLS-1$
        Long eventId = (Long) getFieldValue(commonTypeField, data);

        if (eventId == null) {
            return null;
        }

        return fHeader.getEventFormatByID(eventId.intValue());
    }

    private Object getPrimitiveDataValue(BinaryFTraceFormatField formatField, byte[] data) {
        Object value = null;
        switch (formatField.getFieldType()) {
        case CHAR:
            value = readCharField(formatField, data);
            break;
        case SHORT:
            value = readShortField(formatField, data);
            break;
        case INT:
            value = readIntField(formatField, data);
            break;
        case LONG:
            value = readLongField(formatField, data);
            break;
        case UNKNOWN:
            break;
        default:
            break;
        }

        return value;
    }

    private static String readStringField(byte[] fieldData) {
        for (int i = 0; i < fieldData.length; i++) {
            if (fieldData[i] == 0) {
                return new String(fieldData, 0, i);
            }
        }
        return new String(fieldData);
    }

    private static Long readCharField(BinaryFTraceFormatField formatField, byte[] fieldData) {
        /** A char for ftrace is a 8bit = 1byte value */
        if (fieldData.length == 1) {
            Byte byteValue = fieldData[0];

            if (formatField.getSigned() == BinaryFTraceValueSign.SIGNED || canBeSignedByte(byteValue)) {
                return Long.valueOf(byteValue);
            }

            return Byte.toUnsignedLong(byteValue);
        }

        return null;
    }

    private static boolean canBeSignedByte(Byte byteValue) {
        return (Byte.toUnsignedInt(byteValue) - Byte.toUnsignedInt(Byte.MAX_VALUE)) <= 0;
    }

    private Object readShortField(BinaryFTraceFormatField formatField, byte[] fieldData) {
        Short shortValue = ByteBuffer.wrap(fieldData).order(fHeader.getEndianess()).getShort();

        if (formatField.getSigned() == BinaryFTraceValueSign.SIGNED || canBeSignedShort(shortValue)) {
            return Long.valueOf(shortValue);
        }

        return Short.toUnsignedLong(shortValue);
    }

    private static boolean canBeSignedShort(Short shortValue) {
        return (Short.toUnsignedInt(shortValue) - Short.toUnsignedInt(Short.MAX_VALUE)) <= 0;
    }

    private Object readIntField(BinaryFTraceFormatField formatField, byte[] fieldData) {
        Integer intValue = ByteBuffer.wrap(fieldData).order(fHeader.getEndianess()).getInt();

        if (formatField.getSigned() == BinaryFTraceValueSign.SIGNED || canBeSignedInt(intValue)) {
            return Long.valueOf(intValue);
        }

        return Integer.toUnsignedLong(intValue);
    }

    private static boolean canBeSignedInt(Integer byteValue) {
        return Integer.compareUnsigned(byteValue, Integer.MAX_VALUE) <= 0;
    }

    private Object readLongField(BinaryFTraceFormatField formatField, byte[] fieldData) {
        Long longValue = ByteBuffer.wrap(fieldData).order(fHeader.getEndianess()).getLong();

        // If the value is unsigned, but can not be fit into a signed value, we
        // return a string of the unsigned value
        if (formatField.getSigned() == BinaryFTraceValueSign.UNSIGNED && !canBeSignedLong(longValue)) {
            return Long.toUnsignedString(longValue);
        }

        return longValue;
    }

    private static boolean canBeSignedLong(Long longValue) {
        return Long.compareUnsigned(longValue, Long.MAX_VALUE) <= 0;
    }

    private static byte[] extractValue(BinaryFTraceFormatField field, byte[] data) {
        int size = field.getSize();
        int offset = field.getOffset();
        byte[] valueInBytes = Arrays.copyOfRange(data, offset, size + offset);

        return valueInBytes;
    }

    private byte[] extractDataLocValue(BinaryFTraceFormatField field, byte[] data) {
        // The dynamic field should be 4 bytes
        if (field.getSize() != 4) {
            return null;
        }

        byte[] dataLocationInBytes = Arrays.copyOfRange(data, field.getOffset(), field.getOffset() + field.getSize()); // The
        int dataLocation = ByteBuffer.wrap(dataLocationInBytes).order(fHeader.getEndianess()).getInt();

        int length = BinaryFTraceIteratorHelper.getDataLocLength(dataLocation);
        int offset = BinaryFTraceIteratorHelper.getDataLocOffset(dataLocation);

        if (offset + length <= data.length) {
            byte[] valueInBytes = Arrays.copyOfRange(data, offset, offset + length);
            return valueInBytes;
        }

        return new byte[0];
    }

    private String readStringPointerField(byte[] fieldData) {
        long pointerAddress = ByteBuffer.wrap(fieldData).order(fHeader.getEndianess()).getLong();

        // If the we have a mapping of address to string in the file header, get
        // the string
        String hexAddress = hexPrefix + Long.toHexString(pointerAddress);
        if (fHeader.getPrintKPointerStringMapping().containsKey(hexAddress)) {
            return fHeader.getPrintKPointerStringMapping().get(hexAddress);
        }

        // Else we return the hex string representation like trace-cmd
        return hexAddress;
    }

    private static int getDataLocLength(int dataLoc) {
        return dataLoc >> DATALOC_SHIFT;
    }

    private static int getDataLocOffset(int value) {
        return value & DATA_LOC_OFFSET;
    }
}