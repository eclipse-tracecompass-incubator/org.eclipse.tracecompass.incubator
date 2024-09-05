/*******************************************************************************
 * Copyright (c) 2023, 2024 Ericsson
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

import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link StdSerializer} for {@link ITmfConfiguration} to avoid building intermediate
 * representations.
 *
 * @author Bernd Hufmann
 */
public class TmfConfigurationSerializer extends StdSerializer<ITmfConfiguration> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 9170252203750031947L;

    /**
     * Public constructor
     */
    public TmfConfigurationSerializer() {
        super(ITmfConfiguration.class);
    }

    @Override
    public void serialize(ITmfConfiguration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId()); //$NON-NLS-1$
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("description", value.getDescription()); //$NON-NLS-1$
        gen.writeStringField("sourceTypeId", value.getSourceTypeId()); //$NON-NLS-1$
//        if (!value.getParameters().isEmpty()) {
        gen.writeObjectField("parameters", value.getParameters()); //$NON-NLS-1$
//        }
        gen.writeEndObject();
    }
}
