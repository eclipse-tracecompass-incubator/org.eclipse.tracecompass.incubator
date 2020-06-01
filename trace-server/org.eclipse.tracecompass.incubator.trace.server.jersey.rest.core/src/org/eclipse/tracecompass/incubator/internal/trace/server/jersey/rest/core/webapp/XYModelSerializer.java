/**********************************************************************
 * Copyright (c) 2019 Ericsson
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
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for XY model {@link ITmfXyModel}
 *
 * @author Simon Delisle
 */
public class XYModelSerializer extends StdSerializer<@NonNull ITmfXyModel> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -7271194941789981571L;

    /**
     * Constructor.
     */
    protected XYModelSerializer() {
        super(ITmfXyModel.class);
    }

    @Override
    public void serialize(@NonNull ITmfXyModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("title", value.getTitle()); //$NON-NLS-1$
        gen.writeObjectField("series", value.getSeriesData()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
