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
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for time graph arrow model {@link ITimeGraphArrow}
 *
 * @author Arnaud Fiorini
 */
public class TimeGraphArrowSerializer extends StdSerializer<@NonNull ITimeGraphArrow> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -8967973449240455869L;

    /**
     * Constructor.
     */
    protected TimeGraphArrowSerializer() {
        super(ITimeGraphArrow.class);
    }

    @Override
    public void serialize(@NonNull ITimeGraphArrow value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeNumberField("start", value.getStartTime()); //$NON-NLS-1$
        gen.writeNumberField("end", value.getStartTime() + value.getDuration()); //$NON-NLS-1$
        gen.writeNumberField("sourceId", value.getSourceId()); //$NON-NLS-1$
        gen.writeNumberField("targetId", value.getDestinationId()); //$NON-NLS-1$
        gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$

        gen.writeEndObject();
    }

}
