/**********************************************************************
 * Copyright (c) 2021 Ericsson
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
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.VirtualTableCell;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for tmf table cell model {@link VirtualTableCell}
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("restriction")
public class VirtualTableCellSerializer extends StdSerializer<@NonNull VirtualTableCell> {

    private static final long serialVersionUID = -692109996262904851L;

    /**
     * Constructor.
     */
    protected VirtualTableCellSerializer() {
        super(VirtualTableCell.class);
    }

    @Override
    public void serialize(VirtualTableCell value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("content", value.getContent()); //$NON-NLS-1$
        if (value.getActiveProperties() != 0) {
            gen.writeNumberField("tags", value.getActiveProperties()); //$NON-NLS-1$
        }
        gen.writeEndObject();
    }

}
