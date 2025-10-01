/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.webapp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.SamplingSerializer;
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Categories;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Range;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Ranges;
import org.eclipse.tracecompass.tmf.core.model.ISampling.Timestamps;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Test suite for {@link SamplingSerializer}.
 * <p>
 * Validates JSON round-trip behavior for different {@link ISampling}
 * subtypes: {@link Timestamps}, {@link Categories}, and {@link Ranges}.
 *
 * @author Siwei Zhang
 */
@SuppressWarnings("null")
public class SamplingSerializerTest {

    private ObjectMapper fMapper;

    /**
     * Set up the {@link ObjectMapper} and register the custom serializer and
     * deserializer for {@link ISampling}.
     */
    @Before
    public void setup() {
        fMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ISampling.class, new SamplingSerializer());
        module.addSerializer(TestSampling.class, new TestSamplingSerializer());
        fMapper.registerModule(module);
    }

    /**
     * Test round-trip serialization {@link ISampling.Timestamps}.
     * The format is a flat array of @NonNull Longs.
     *
     * @throws JsonProcessingException
     *             if JSON processing fails
     */
    @Test
    public void testTimestampsRoundTrip() throws JsonProcessingException {
        TestSampling original = new TestSampling(new Timestamps(new long[] { 1, 2, 3 }));
        String json = fMapper.writeValueAsString(original);
        assertEquals("{\"xValues\":[1,2,3]}", json);
    }

    /**
     * Test round-trip serialization {@link ISampling.Categories}.
     * The format is an array of strings.
     *
     * @throws JsonProcessingException
     *             if JSON processing fails
     */
    @Test
    public void testCategoriesRoundTrip() throws JsonProcessingException {
        TestSampling original = new TestSampling(new Categories(List.of("Read", "Write", "Idle")));
        String json = fMapper.writeValueAsString(original);
        assertEquals("{\"xCategories\":[\"Read\",\"Write\",\"Idle\"]}", json);
    }

    /**
     * Test round-trip serialization {@link ISampling.Ranges}.
     * The format is an array of range json objects.
     *
     * @throws JsonProcessingException
     *             if JSON processing fails
     */
    @Test
    public void testTimeRangesRoundTrip() throws JsonProcessingException {
        TestSampling original = new TestSampling(new Ranges(List.of(
                new Range<>(1L, 2L),
                new Range<>(2L, 3L),
                new Range<>(3L, 4L))
        ));
        String json = fMapper.writeValueAsString(original);
        assertEquals("{\"xRanges\":[{\"start\":1,\"end\":2},{\"start\":2,\"end\":3},{\"start\":3,\"end\":4}]}", json);
    }

    private class TestSampling implements Serializable {
        private static final long serialVersionUID = 4522987850097455165L;
        ISampling fSampling;

        public TestSampling(ISampling sampling) {
            fSampling = sampling;
        }

        public ISampling getSampling() {
            return fSampling;
        }
    }

    private class TestSamplingSerializer extends StdSerializer<TestSampling> {

        private static final long serialVersionUID = 4522987850097455165L;

        /**
         * Constructor
         */
        public TestSamplingSerializer() {
            super(TestSampling.class);
        }

        @Override
        public void serialize(TestSampling value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeObject(value.getSampling());
            gen.writeEndObject();
        }
    }
}
