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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializer for SamplingStub.
 *
 * @author Siwei Zhang
 */
public class SamplingStubDeserializer extends JsonDeserializer<ISamplingStub> {

    @Override
    public ISamplingStub deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            ctxt.reportInputMismatch(ISamplingStub.class, "Expected array for SamplingStub");
        }

        token = p.nextToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            List<Long> timestamps = new ArrayList<>();
            do {
                timestamps.add(p.getLongValue());
            } while (p.nextToken() != JsonToken.END_ARRAY);
            long[] ts = timestamps.stream().mapToLong(Long::longValue).toArray();
            return new ISamplingStub.TimestampsStub(ts);

        } else if (token == JsonToken.VALUE_STRING) {
            List<String> categories = new ArrayList<>();
            do {
                categories.add(p.getText());
            } while (p.nextToken() != JsonToken.END_ARRAY);
            return new ISamplingStub.CategoriesStub(categories);

        } else if (token == JsonToken.START_OBJECT) {
            List<ISamplingStub.RangesStub.RangeStub> ranges = new ArrayList<>();
            // Loop through the array of range objects
            do {
                long start = -1;
                long end = -1;
                boolean startFound = false;
                boolean endFound = false;

                while (p.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = p.currentName();
                    p.nextToken(); // Move to the value token
                    if ("start".equals(fieldName)) {
                        start = p.getLongValue();
                        startFound = true;
                    } else if ("end".equals(fieldName)) {
                        end = p.getLongValue();
                        endFound = true;
                    } else {
                        p.skipChildren();
                    }
                }

                if (startFound && endFound) {
                    ranges.add(new ISamplingStub.RangesStub.RangeStub(start, end));
                } else {
                    ctxt.reportInputMismatch(ISamplingStub.RangesStub.RangeStub.class, "RangeStub object requires both 'start' and 'end' fields");
                    return null;
                }
            } while (p.nextToken() != JsonToken.END_ARRAY);
            return new ISamplingStub.RangesStub(ranges);
        }

        ctxt.reportInputMismatch(ISamplingStub.class, "Unrecognized structure for SamplingStub");
        return null;
    }
}
