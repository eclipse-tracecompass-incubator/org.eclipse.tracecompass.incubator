/*******************************************************************************
 * Copyright (c) 2018, 2025 Ericsson
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
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link StdSerializer} for {@link IDataProviderDescriptor} to avoid building intermediate
 * representations.
 *
 * @author Bernd Hufmann
 */
public class DataProviderDescriptorSerializer extends StdSerializer<IDataProviderDescriptor> {

    /**
     * Generated serialVersionUID
     */
    private static final long serialVersionUID = 9170252203750031947L;

    /**
     * Public constructor
     */
    public DataProviderDescriptorSerializer() {
        super(IDataProviderDescriptor.class);
    }

    @Override
    public void serialize(IDataProviderDescriptor value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        String parentId = value.getParentId();
        if (parentId != null) {
            gen.writeStringField("parentId", value.getParentId()); //$NON-NLS-1$
        }
        gen.writeStringField("id", value.getId()); //$NON-NLS-1$
        gen.writeStringField("name", value.getName()); //$NON-NLS-1$
        gen.writeStringField("description", value.getDescription()); //$NON-NLS-1$
        gen.writeStringField("type", value.getType().name()); //$NON-NLS-1$
        ITmfConfiguration config = value.getConfiguration();
        if (config != null) {
            gen.writeObjectField("configuration", config); //$NON-NLS-1$
        }
        IDataProviderCapabilities cap = value.getCapabilities();
        if (cap != DataProviderCapabilities.NULL_INSTANCE) {
            gen.writeObjectFieldStart("capabilities"); //$NON-NLS-1$
            if (cap.canCreate()) {
                gen.writeBooleanField("canCreate", cap.canCreate()); //$NON-NLS-1$
            }
            if (cap.canDelete()) {
                gen.writeBooleanField("canDelete", cap.canDelete()); //$NON-NLS-1$
            }
            if (cap.selectionRange()) {
                gen.writeBooleanField("selectionRange", cap.selectionRange()); //$NON-NLS-1$
            }
            gen.writeEndObject();
        }
        gen.writeEndObject();
    }

}
