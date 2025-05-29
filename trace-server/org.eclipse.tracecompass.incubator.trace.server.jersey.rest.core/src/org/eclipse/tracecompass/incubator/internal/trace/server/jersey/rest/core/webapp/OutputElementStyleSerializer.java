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
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author Geneviève Bastien
 *
 */
public class OutputElementStyleSerializer extends StdSerializer<@NonNull OutputElementStyle> {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = 8663734251280456250L;

    /**
     * Constructor
     */
    public OutputElementStyleSerializer() {
        super(OutputElementStyle.class);
    }

    /**
     * Serialize {@link OutputElementStyle} according to TSP, see here for details:
     * {@link org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.OutputElementStyle}
     */
    @SuppressWarnings({ "nls", "null" })
    @Override
    public void serialize(@NonNull OutputElementStyle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("parentKey", value.getParentKey());

        // Verify the type of style values. Make sure only supported types are returned.
        gen.writeObjectFieldStart("values");
        for (Entry<String, Object> entry : value.getStyleValues().entrySet()) {
            Object entryValue = entry.getValue();
            if (entryValue instanceof Long longValue) {
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    gen.writeFieldName(entry.getKey());
                    gen.writeNumber(longValue.intValue());
                }
            } else if ((entryValue instanceof String) || (entryValue instanceof Number)) {
                gen.writeFieldName(entry.getKey());
                gen.writeObject(entryValue);
            }
        }
        gen.writeEndObject();
        gen.writeEndObject();
    }

}
