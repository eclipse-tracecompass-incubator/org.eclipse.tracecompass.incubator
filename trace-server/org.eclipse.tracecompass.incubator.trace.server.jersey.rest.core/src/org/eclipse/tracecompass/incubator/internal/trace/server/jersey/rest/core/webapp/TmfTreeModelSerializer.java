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
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for tmf tree entry model {@link TmfTreeDataModel}
 *
 * @author Geneviève Bastien
 */
public class TmfTreeModelSerializer extends StdSerializer<@NonNull TmfTreeDataModel> {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = 3393902091244330176L;

    /**
     * Constructor.
     */
    protected TmfTreeModelSerializer() {
        super(TmfTreeDataModel.class);
    }

    @Override
    public void serialize(TmfTreeDataModel value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", value.getId()); //$NON-NLS-1$
        gen.writeNumberField("parentId", value.getParentId()); //$NON-NLS-1$
        gen.writeObjectField("style", value.getStyle()); //$NON-NLS-1$
        gen.writeArrayFieldStart("labels"); //$NON-NLS-1$
        for (String label : value.getLabels()) {
            gen.writeString(label);
        }
        gen.writeEndArray();
        gen.writeBooleanField("hasData", value.hasRowModel()); //$NON-NLS-1$
        gen.writeEndObject();
    }

}
