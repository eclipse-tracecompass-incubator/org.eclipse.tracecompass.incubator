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

    @Override
    public void serialize(@NonNull OutputElementStyle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("parentKey", value.getParentKey()); //$NON-NLS-1$
        gen.writeObjectField("values", value.getStyleValues()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
