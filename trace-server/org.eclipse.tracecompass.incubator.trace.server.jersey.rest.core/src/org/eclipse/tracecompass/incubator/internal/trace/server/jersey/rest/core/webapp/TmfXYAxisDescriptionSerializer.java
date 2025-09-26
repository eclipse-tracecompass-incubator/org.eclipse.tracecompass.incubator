/**********************************************************************
 * Copyright (c) 2025 Ericsson
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
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for XY axis description series model {@link TmfXYAxisDescription}
 *
 * @author Bernd Hufmann
 */
public class TmfXYAxisDescriptionSerializer extends StdSerializer<@NonNull TmfXYAxisDescription> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 250364996978808548L;

    /**
     * Constructor.
     */
    public TmfXYAxisDescriptionSerializer() {
        super(TmfXYAxisDescription.class);
    }

    @Override
    public void serialize(TmfXYAxisDescription value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("label", value.getLabel()); //$NON-NLS-1$
        gen.writeStringField("unit", value.getUnit()); //$NON-NLS-1$
        gen.writeObjectField("dataType", value.getDataType()); //$NON-NLS-1$
        if (value.getAxisDomain() != null) {
            gen.writeObjectField("axisDomain", value.getAxisDomain()); //$NON-NLS-1$
        }
        gen.writeEndObject();
    }
}
