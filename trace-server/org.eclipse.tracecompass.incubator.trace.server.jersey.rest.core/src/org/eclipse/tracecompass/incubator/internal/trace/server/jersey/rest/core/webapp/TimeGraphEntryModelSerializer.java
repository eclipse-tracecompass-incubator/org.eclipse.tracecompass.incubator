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
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for time graph entry model {@link ITimeGraphEntryModel}
 *
 * @author Geneviève Bastien
 */
public class TimeGraphEntryModelSerializer extends StdSerializer<@NonNull ITimeGraphEntryModel> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -4359431726167157401L;

    /**
     * Constructor.
     */
    protected TimeGraphEntryModelSerializer() {
        super(ITimeGraphEntryModel.class);
    }

    @Override
    public void serialize(ITimeGraphEntryModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", value.getId()); //$NON-NLS-1$
        gen.writeNumberField("parentId", value.getParentId()); //$NON-NLS-1$
        gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$
        gen.writeArrayFieldStart("labels"); //$NON-NLS-1$
        for (String label : value.getLabels()) {
            gen.writeString(label);
        }
        gen.writeEndArray();
        gen.writeNumberField("start", value.getStartTime()); //$NON-NLS-1$
        gen.writeNumberField("end", value.getEndTime()); //$NON-NLS-1$
        gen.writeBooleanField("hasData", value.hasRowModel()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
