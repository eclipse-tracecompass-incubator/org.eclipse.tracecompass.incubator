/**********************************************************************
 * Copyright (c) 2020, 2025 École Polytechnique de Montréal
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.ScopeLog;

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
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(TimeGraphArrowSerializer.class);

    /**
     * Constructor.
     */
    protected TimeGraphArrowSerializer() {
        super(ITimeGraphArrow.class);
    }

    @Override
    public void serialize(@NonNull ITimeGraphArrow value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "TimeGraphArrowSerialize")) { //$NON-NLS-1$
            gen.writeStartObject();

            gen.writeNumberField("start", value.getStartTime()); //$NON-NLS-1$
            gen.writeNumberField("end", value.getStartTime() + value.getDuration()); //$NON-NLS-1$
            gen.writeNumberField("sourceId", value.getSourceId()); //$NON-NLS-1$
            gen.writeNumberField("targetId", value.getDestinationId()); //$NON-NLS-1$
            if (value.getStyle() != null) {
                gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$
            }

            gen.writeEndObject();
        }
    }

}
