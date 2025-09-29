/*******************************************************************************
 * Copyright (c) 2025 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ISamplingStub.RangesStub.RangeStub;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for SamplingStub. Matches the protocol format used in real
 * {@link SamplingStubSerializer}
 *
 * @author Siwei Zhang
 */
public class SamplingStubSerializer extends StdSerializer<ISamplingStub> {

    private static final long serialVersionUID = 1L;

    public SamplingStubSerializer() {
        super(ISamplingStub.class);
    }

    @Override
    public void serialize(ISamplingStub value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value instanceof ISamplingStub.TimestampsStub timestamps) {
            gen.writeArray(timestamps.getTimestamps(), 0, timestamps.getTimestamps().length);

        } else if (value instanceof ISamplingStub.CategoriesStub categories) {
            gen.writeStartArray();
            for (String category : categories.getCategories()) {
                gen.writeString(category);
            }
            gen.writeEndArray();

        } else if (value instanceof ISamplingStub.RangesStub timeRanges) {
            gen.writeStartArray();
            for (RangeStub range : timeRanges.getRanges()) {
                gen.writeStartObject();
                gen.writeNumberField("start", range.getStart()); //$NON-NLS-1$
                gen.writeNumberField("end", range.getEnd()); //$NON-NLS-1$
                gen.writeEndObject();
            }
            gen.writeEndArray();
        } else {
            throw new IllegalArgumentException("Unknown SamplingStub type: " + value.getClass().getName());
        }
    }
}
