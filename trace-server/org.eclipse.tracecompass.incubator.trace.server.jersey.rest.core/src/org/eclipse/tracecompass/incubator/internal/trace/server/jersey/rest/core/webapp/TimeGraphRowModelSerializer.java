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
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.traceeventlogger.LogUtils.ScopeLog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for time graph row models {@link TimeGraphRowModel}
 *
 * @author Geneviève Bastien
 */
public class TimeGraphRowModelSerializer extends StdSerializer<@NonNull TimeGraphRowModel> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = -4359431726167157401L;
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(TimeGraphRowModelSerializer.class);

    /**
     * Constructor.
     */
    protected TimeGraphRowModelSerializer() {
        super(TimeGraphRowModel.class);
    }

    @Override
    public void serialize(TimeGraphRowModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "RowModelSerialize")) { //$NON-NLS-1$
            gen.writeStartObject();
            gen.writeNumberField("entryId", value.getEntryID()); //$NON-NLS-1$
            gen.writeObjectField("states", value.getStates()); //$NON-NLS-1$
            gen.writeEndObject();
        }
    }

}
