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
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for time graph state {@link TimeGraphState}
 *
 * @author Geneviève Bastien
 */
public class TimeGraphStateSerializer extends StdSerializer<@NonNull TimeGraphState> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -4359431726167157401L;

    /**
     * Constructor.
     */
    protected TimeGraphStateSerializer() {
        super(TimeGraphState.class);
    }

    @Override
    public void serialize(TimeGraphState value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("start", value.getStartTime()); //$NON-NLS-1$
        gen.writeNumberField("end", value.getStartTime() + value.getDuration()); //$NON-NLS-1$
        gen.writeStringField("label", value.getLabel()); //$NON-NLS-1$
        OutputElementStyle style = value.getStyle();
        if (style != null) {
            gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$
        } else {
            // Transform the value to a style
            int stateValue = value.getValue();
            if (stateValue != Integer.MIN_VALUE) {
                gen.writeFieldName("style"); //$NON-NLS-1$
                gen.writeStartObject();
                gen.writeStringField("parentKey", String.valueOf(stateValue)); //$NON-NLS-1$
                gen.writeEndObject();
            }
        }
        if (value.getActiveProperties() != 0) {
            gen.writeNumberField("tags", value.getActiveProperties()); //$NON-NLS-1$
        }
        gen.writeEndObject();
    }

}
