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

/**
 * Represent information to parse a specific field (from a trace event binary
 * payload) inside a {@link BinaryFTraceEventFormat}. Each
 * BinaryFTraceFormatField contains the starting offset (in bytes) of the value
 * of the field (relative to the trace event binary payload). It also contains
 * the size of the value of the field (in bytes), the name and data type of the
 * field, and the sign of the field (signed or unsigned).
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFormatField {
    private final String fFieldName;
    private final BinaryFTraceDataType fFieldType;
    private final String fStrFieldType;
    private final int fOffset;
    private final int fSize;
    private final BinaryFTraceValueSign fSigned;

    /**
     * Constructor
     */
    private BinaryFTraceFormatField(BinaryFTraceFormatFieldBuilder builder) {
        fFieldName = builder.fBuilderFieldName;
        fFieldType = builder.fBuilderFieldType;
        fStrFieldType = builder.fBuilderStrFieldType; // Stores the string of the data type
                            // in case the
                            // value is unknown
        fOffset = builder.fBuilderOffset;
        fSize = builder.fBuilderSize;
        fSigned = builder.fBuilderSigned;
    }

    /**
     * Get the original string that represent the type of a field parsed from
     * the binary file
     *
     * @return The original type of the field
     */
    public String getStrFieldType() {
        return fStrFieldType;
    }

    /**
     * Get the parsed field type of the field
     *
     * @return The parsed field of type of the field
     */
    public BinaryFTraceDataType getFieldType() {
        return fFieldType;
    }

    /**
     * Get the name of the field
     *
     * @return The name of the field
     */
    public String getFieldName() {
        return fFieldName;
    }

    /**
     * Get the offset of the field compared to the event format
     *
     * @return The offset of the field
     */
    public int getOffset() {
        return fOffset;
    }

    /**
     * Get the size of the field
     *
     * @return The size of the field
     */
    public int getSize() {
        return fSize;
    }

    /**
     * Get the sign of the field
     *
     * @return The sign of the field
     */
    public BinaryFTraceValueSign getSigned() {
        return fSigned;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("Field name: ").append(fFieldName) //$NON-NLS-1$
                .append("; Field type: ").append(fFieldType) //$NON-NLS-1$
                .append("; Offset: ").append(fOffset) //$NON-NLS-1$
                .append("; Size: ").append(fSize) //$NON-NLS-1$
                .append("; Signed: ").append(fSigned); //$NON-NLS-1$

        return strBuilder.toString();
    }

    /**
     * A builder to help create immutable {@link BinaryFTraceFormatField}
     *
     * @author Hoang Thuan Pham
     */
    public static class BinaryFTraceFormatFieldBuilder {
        private String fBuilderFieldName;
        private BinaryFTraceDataType fBuilderFieldType;
        private String fBuilderStrFieldType;
        private int fBuilderOffset;
        private int fBuilderSize;
        private BinaryFTraceValueSign fBuilderSigned;

        /**
         * Constructor
         */
        public BinaryFTraceFormatFieldBuilder() {
            fBuilderFieldName = ""; //$NON-NLS-1$
            fBuilderFieldType = BinaryFTraceDataType.UNKNOWN;
            fBuilderStrFieldType = ""; //$NON-NLS-1$
            fBuilderOffset = -1;
            fBuilderSize = 0;
            fBuilderSigned = BinaryFTraceValueSign.UNSIGNED;
        }

        /**
         * Set the field name of the FTrace format field.
         *
         * @param fieldName The name of the field
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder fieldName(String fieldName) {
            fBuilderFieldName = fieldName;
            return this;
        }

        /**
         * Set the field type (data type) of the FTrace format field
         *
         * @param fieldType The {@link BinaryFTraceDataType} value that represents the data type of the field
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder fieldType(BinaryFTraceDataType fieldType) {
            fBuilderFieldType = fieldType;
            return this;
        }

        /**
         * Set the original field type-name string of the field. It contains the field and the C modifiers.
         * It is helpful to store this information to debug when trace compass cannot parse the field.
         *
         * @param strFieldType The original field type-name string of the field.
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder strFieldType(String strFieldType) {
            fBuilderStrFieldType = strFieldType;
            return this;
        }

        /**
         * Set the offset (in bytes) where the field value starts relative to the trace event binary payload.
         *
         * @param offset The offset (in bytes) where the field value starts
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder offset(int offset) {
            fBuilderOffset = offset;
            return this;
        }

        /**
         * Set the size of the field (in bytes).
         *
         * @param size The size of the field (in bytes)
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder size(int size) {
            fBuilderSize = size;
            return this;
        }

        /**
         * Set the sign of the field. A field value can be signed or unsigned.
         *
         * @param signed The {@link BinaryFTraceValueSign} that represents the sign of the value
         * @return The current builder
         */
        public BinaryFTraceFormatFieldBuilder signed(BinaryFTraceValueSign signed) {
            fBuilderSigned = signed;
            return this;
        }

        /**
         * Create an immutable {@link BinaryFTraceFormatField} based on current state of the builder.
         *
         * @return A {@link BinaryFTraceFormatField} object
         */
        public BinaryFTraceFormatField build() {
            return new BinaryFTraceFormatField(this);
        }
    }
}
