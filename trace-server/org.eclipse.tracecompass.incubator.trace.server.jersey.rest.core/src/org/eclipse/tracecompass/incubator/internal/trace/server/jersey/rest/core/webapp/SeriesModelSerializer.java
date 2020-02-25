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
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for XY series model {@link ISeriesModel}
 *
 * @author Simon Delisle
 */
public class SeriesModelSerializer extends StdSerializer<@NonNull ISeriesModel> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -4359431726167157401L;

    /**
     * Constructor.
     */
    protected SeriesModelSerializer() {
        super(ISeriesModel.class);
    }

    @Override
    public void serialize(ISeriesModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", value.getId()); //$NON-NLS-1$
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeObjectField("xValues", value.getXAxis()); //$NON-NLS-1$
        gen.writeObjectField("yValues", value.getData()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
