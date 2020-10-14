/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.Trace;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link StdSerializer} for {@link Trace}.
 *
 * @author Loic Prieur-Drevon
 */
public class TraceSerializer extends StdSerializer<@NonNull Trace> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 9170252203750031947L;

    /**
     * Public constructor
     */
    public TraceSerializer() {
        super(Trace.class);
    }

    @Override
    public void serialize(Trace value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("path", value.getPath()); //$NON-NLS-1$
        gen.writeStringField("UUID", value.getUUID().toString()); //$NON-NLS-1$
        gen.writeNumberField("nbEvents", value.getNbEvents()); //$NON-NLS-1$
        gen.writeNumberField("start", value.getStart()); //$NON-NLS-1$
        gen.writeNumberField("end", value.getEnd()); //$NON-NLS-1$
        gen.writeStringField("indexingStatus", value.getIndexingStatus()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
