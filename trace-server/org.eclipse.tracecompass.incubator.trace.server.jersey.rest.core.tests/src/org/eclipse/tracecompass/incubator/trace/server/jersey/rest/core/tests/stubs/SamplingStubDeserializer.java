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

        } else if (token == JsonToken.START_ARRAY) {
            List<ISamplingStub.RangesStub.RangeStub> ranges = new ArrayList<>();
            while (token != JsonToken.END_ARRAY) {
                p.nextToken(); // start
                long start = p.getLongValue();
                p.nextToken(); // end
                long end = p.getLongValue();
                p.nextToken(); // end of inner array
                ranges.add(new ISamplingStub.RangesStub.RangeStub(start, end));
                token = p.nextToken(); // next outer token
            }
            return new ISamplingStub.RangesStub(ranges);
        }

        ctxt.reportInputMismatch(ISamplingStub.class, "Unrecognized structure for SamplingStub");
        return null;
    }
}
