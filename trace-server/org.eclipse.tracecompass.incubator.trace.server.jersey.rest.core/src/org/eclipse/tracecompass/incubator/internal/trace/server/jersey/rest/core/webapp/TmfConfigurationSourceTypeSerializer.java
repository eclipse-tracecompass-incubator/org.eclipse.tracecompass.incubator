/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link StdSerializer} for {@link ITmfConfigurationSourceType} to avoid building intermediate
 * representations.
 *
 * @author Bernd Hufmann
 */
public class TmfConfigurationSourceTypeSerializer extends StdSerializer<ITmfConfigurationSourceType> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 9170252203750031947L;

    /**
     * Public constructor
     */
    public TmfConfigurationSourceTypeSerializer() {
        super(ITmfConfigurationSourceType.class);
    }

    @Override
    public void serialize(ITmfConfigurationSourceType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("id", value.getId()); //$NON-NLS-1$
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("description", value.getDescription()); //$NON-NLS-1$
        if (!value.getConfigParamDescriptors().isEmpty()) {
            gen.writeObjectField("parameterDescriptors", value.getConfigParamDescriptors()); //$NON-NLS-1$
        }
        File schemaFile = value.getSchemaFile();
        if (schemaFile != null) {
            try (InputStream inputStream = new FileInputStream(schemaFile)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode schema = mapper.readTree(inputStream);
                gen.writeFieldName("schema"); //$NON-NLS-1$
                gen.writeTree(schema);
            }
        }
        gen.writeEndObject();
    }
}
