/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Enum type for OTF2_Type in OTF2 standard
 *
 * @author Arnaud Fiorini
 */
public enum Otf2Type {
    /** Undefined type */
    OTF2_TYPE_NONE,
    /** Unsigned 8-bit integer */
    OTF2_TYPE_UINT8,
    /** Unsigned 8-bit integer */
    OTF2_TYPE_UINT16,
    /** Unsigned 8-bit integer */
    OTF2_TYPE_UINT32,
    /** Unsigned 8-bit integer */
    OTF2_TYPE_UINT64,
    /** Signed 8-bit integer */
    OTF2_TYPE_INT8,
    /** Signed 8-bit integer */
    OTF2_TYPE_INT16,
    /** Signed 8-bit integer */
    OTF2_TYPE_INT32,
    /** Signed 8-bit integer */
    OTF2_TYPE_INT64,
    /** 32-bit floating point value */
    OTF2_TYPE_FLOAT,
    /** 64-bit floating point value */
    OTF2_TYPE_DOUBLE,
    /** Mapping of String identifiers */
    OTF2_TYPE_STRING,
    /** Mapping of Attribute identifiers */
    OTF2_TYPE_ATTRIBUTE,
    /** Mapping of Location identifiers */
    OTF2_TYPE_LOCATION,
    /** Mapping of Region identifiers */
    OTF2_TYPE_REGION,
    /** Mapping of Group identifiers */
    OTF2_TYPE_GROUP,
    /** Mapping of Metric identifiers */
    OTF2_TYPE_METRIC,
    /** Mapping of Communicator identifiers */
    OTF2_TYPE_COMM,
    /** Mapping of Parameter identifiers */
    OTF2_TYPE_PARAMETER,
    /** Mapping of Remote memory access operation identifiers */
    OTF2_TYPE_RMA_WIN,
    /** Mapping of Source code location identifiers */
    OTF2_TYPE_SOURCE_CODE_LOCATION,
    /** Mapping of Calling context identifiers */
    OTF2_TYPE_CALLING_CONTEXT,
    /** Mapping of Interrupt generator identifiers */
    OTF2_TYPE_INTERRUPT_GENERATOR,
    /** Mapping of Io file identifiers */
    OTF2_TYPE_IO_FILE,
    /** Mapping of Io handle identifiers */
    OTF2_TYPE_IO_HANDLE,
    /** Mapping of Location group identifiers */
    OTF2_TYPE_LOCATION_GROUP;

    private static @NonNull Otf2Type[] fOtf2TypeValues = Otf2Type.values();

    /**
     * Convert from integer to enum type
     *
     * @param typeId
     *            The id of the type
     * @return The enum value
     */
    public static Otf2Type fromInteger(int typeId) {
        if (typeId > 0 && typeId < fOtf2TypeValues.length) {
            return fOtf2TypeValues[typeId];
        }
        return Otf2Type.OTF2_TYPE_NONE;
    }
}
