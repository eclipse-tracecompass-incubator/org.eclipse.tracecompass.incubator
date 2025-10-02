/**********************************************************************
 * Copyright (c) 2025 Ericsson
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
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Categories;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Range;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Ranges;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Timestamps;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom serializer for all Sampling subtypes.
 * - Timestamps → flat array: [1, 2, 3]
 * - Categories → array of strings: ["Read", "Write"]
 * - Ranges → array of objects: [{"start": 1, "end": 2}, {"start": 2, "end": 3}]
 */
public class SamplingSerializer extends StdSerializer<ISampling> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public SamplingSerializer() {
        super(ISampling.class);
    }

    @Override
    public void serialize(ISampling value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value instanceof Timestamps timestamps) {
            gen.writeArray(timestamps.timestamps(), 0, timestamps.timestamps().length);

        } else if (value instanceof Categories categories) {
            gen.writeStartArray();
            for (String category : categories.categories()) {
                gen.writeString(category);
            }
            gen.writeEndArray();

        } else if (value instanceof Ranges timeRanges) {
            gen.writeStartArray();
            for (Range<@NonNull Long> range : timeRanges.ranges()) {
                gen.writeStartObject();
                gen.writeNumberField("start", range.start()); //$NON-NLS-1$
                gen.writeNumberField("end", range.end()); //$NON-NLS-1$
                gen.writeEndObject();
            }
            gen.writeEndArray();
        } else {
            throw new IllegalArgumentException("Unknown Sampling type: " + value.getClass().getName()); //$NON-NLS-1$
        }
    }
}
