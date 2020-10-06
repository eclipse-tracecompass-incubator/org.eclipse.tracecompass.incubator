/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
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
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.table.IVirtualTableLine;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for tmf table line model {@link IVirtualTableLine}
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class VirtualTableLineSerializer extends StdSerializer<@NonNull IVirtualTableLine> {

    private static final long serialVersionUID = -8146058278334230086L;

    /**
     * Constructor.
     */
    protected VirtualTableLineSerializer() {
        super(IVirtualTableLine.class);
    }

    @Override
    public void serialize(IVirtualTableLine value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("index", value.getIndex()); //$NON-NLS-1$
        gen.writeObjectField("cells", value.getCells()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
