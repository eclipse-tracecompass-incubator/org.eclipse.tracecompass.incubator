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
import org.eclipse.tracecompass.internal.tmf.core.markers.MarkerSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for marker set model {@link MarkerSet}
 *
 * @author Patrick Tasse
 */
public class MarkerSetSerializer extends StdSerializer<@NonNull MarkerSet> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -5592479268641762715L;

    /**
     * Constructor.
     */
    protected MarkerSetSerializer() {
        super(MarkerSet.class);
    }

    @Override
    public void serialize(@NonNull MarkerSet value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeObjectField("id", value.getId()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
