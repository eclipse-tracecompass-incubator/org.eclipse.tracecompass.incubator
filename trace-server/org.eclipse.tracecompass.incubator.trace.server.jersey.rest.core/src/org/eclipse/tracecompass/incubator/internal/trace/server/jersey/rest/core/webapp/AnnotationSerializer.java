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
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for annotation {@link Annotation}
 *
 * @author PatrickTasse
 */
public class AnnotationSerializer extends StdSerializer<@NonNull Annotation> {

    /**
     *
     */
    private static final long serialVersionUID = -2494393848498351276L;

    /**
     * Constructor.
     */
    protected AnnotationSerializer() {
        super(Annotation.class);
    }

    @Override
    public void serialize(@NonNull Annotation value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeNumberField("time", value.getTime()); //$NON-NLS-1$
        gen.writeNumberField("duration", value.getDuration()); //$NON-NLS-1$
        gen.writeNumberField("entryId", value.getEntryId()); //$NON-NLS-1$
        gen.writeObjectField("type", value.getType()); //$NON-NLS-1$
        if (value.getLabel() != null) {
            gen.writeStringField("label", value.getLabel()); //$NON-NLS-1$
        }
        if (value.getStyle() != null) {
            gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$
        }

        gen.writeEndObject();
    }

}
