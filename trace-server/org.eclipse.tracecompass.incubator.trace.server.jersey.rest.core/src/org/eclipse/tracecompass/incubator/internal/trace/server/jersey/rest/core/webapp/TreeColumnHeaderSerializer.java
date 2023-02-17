/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.TreeModelWrapper.TreeColumnHeader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for tmf tree entry model {@link TreeColumnHeader}
 *
 * @author Bernd Hufmann
 */
public class TreeColumnHeaderSerializer extends StdSerializer<@NonNull TreeColumnHeader> {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = 3393902091244330176L;

    /**
     * Constructor.
     */
    protected TreeColumnHeaderSerializer() {
        super(TreeColumnHeader.class);
    }

    @Override
    public void serialize(TreeColumnHeader value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("tooltip", value.getTooltip()); //$NON-NLS-1$
        if (value.getDataType() != null) {
            gen.writeStringField("dataType", value.getDataType()); //$NON-NLS-1$
        }
        gen.writeEndObject();
    }
}
