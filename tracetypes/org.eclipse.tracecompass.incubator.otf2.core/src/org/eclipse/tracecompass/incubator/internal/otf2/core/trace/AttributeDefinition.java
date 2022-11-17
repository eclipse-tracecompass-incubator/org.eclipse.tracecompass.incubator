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
package org.eclipse.tracecompass.incubator.internal.otf2.core.trace;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.Otf2Type;

/**
 * Defines a type of attribute
 *
 * @author Arnaud Fiorini
 */
public class AttributeDefinition {

    private final int fNameId;
    private final int fDescriptionId;
    private final Otf2Type fType;

    /**
     * Constructor of the AttributeDefinition
     *
     * @param nameId
     *            The string id of the name
     * @param descriptionId
     *            The string id of the description
     * @param typeId
     *            The integer value of the {@link Otf2Type}
     */
    public AttributeDefinition(int nameId, int descriptionId, int typeId) {
        fNameId = nameId;
        fDescriptionId = descriptionId;
        fType = Otf2Type.fromInteger(typeId);
    }

    /**
     * Gets the name of the attribute from the nameId
     *
     * @param stringIdMap
     *            Mappings of id number to strings
     * @return The name of the attribute
     */
    public String getName(Map<Integer, String> stringIdMap) {
        return stringIdMap.getOrDefault(fNameId, IOtf2Constants.UNKNOWN_STRING);
    }

    /**
     * Gets the description of the attribute from the descriptionId
     *
     * @param stringIdMap
     *            Mappings of id number to strings
     * @return The description of the attribute
     */
    public String getDescription(Map<Integer, String> stringIdMap) {
        return stringIdMap.getOrDefault(fDescriptionId, IOtf2Constants.UNKNOWN_STRING);
    }

    /**
     * Gets the type of the attribute
     *
     * @return The enum value of the type
     */
    public Otf2Type getType() {
        return fType;
    }
}
