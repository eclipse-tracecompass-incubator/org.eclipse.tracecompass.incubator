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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.OutputElementStyleSerializer;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StyleValue;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Test the {@link OutputElementStyleSerializer}
 *
 * @author Bernd Hufmann
 */
public class OutputElementStyleSerializerTest extends AbstractSerializerTest {

    private static final String VALID_STYLE_NAME = "valid-styles";
    private static final String MAPPED_STYLE_NAME = "mapped-styles";
    private static final String INVALID_STYLE_NAME = "invalid-styles";

    /**
     * Verify that valid style objects are serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @SuppressWarnings("null")
    @Test
    public void testValidStyles() throws JsonProcessingException {
        Map<String, Object> validStyles = new HashMap<>();
        validStyles.put(StyleProperties.BACKGROUND_COLOR, "ffffff");
        validStyles.put(StyleProperties.LINEAR_GRADIENT, "true");
        validStyles.put(StyleProperties.OPACITY, 0.4f);
        validStyles.put(StyleProperties.WIDTH, Integer.valueOf(4));

        SimpleModule module = new SimpleModule();
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        fMapper.registerModule(module);

        OutputElementStyle testStyle = new OutputElementStyle(VALID_STYLE_NAME, validStyles);
        String json = fMapper.writeValueAsString(testStyle);

        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle deserialized = fMapper.readValue(json, org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle.class);
        assertEquals(testStyle.getParentKey(), deserialized.getParentKey());

        verifyStyles(testStyle, deserialized);
    }

    /**
     * Verify that certain types of style objects are serialized properly, e.g. Long
     * are casted to Integer
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @SuppressWarnings("null")
    @Test
    public void testMappedStyles() throws JsonProcessingException {
        Map<String, Object> mappedStyles = new HashMap<>();
        mappedStyles.put(StyleProperties.LINEAR_GRADIENT, "true");
        mappedStyles.put(StyleProperties.OPACITY, 0.4d);
        mappedStyles.put("long-style", Long.valueOf(1234));
        mappedStyles.put("byte-style", Byte.valueOf((byte) 5));

        SimpleModule module = new SimpleModule();
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        fMapper.registerModule(module);

        OutputElementStyle testStyle = new OutputElementStyle(MAPPED_STYLE_NAME, mappedStyles);
        String json = fMapper.writeValueAsString(testStyle);

        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle deserialized = fMapper.readValue(json, org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle.class);
        assertEquals(testStyle.getParentKey(), deserialized.getParentKey());

        verifyStyles(testStyle, deserialized);
    }

    /**
     * Verify that types of style objects that are not specified in the TSP are not serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @SuppressWarnings("null")
    @Test
    public void testInvalidStyles() throws JsonProcessingException {
        Map<String, Object> invalidStyles = new HashMap<>();
        invalidStyles.put("ignore-invalid", invalidStyles);
        invalidStyles.put("ingore-long-style1", Long.valueOf(Long.MIN_VALUE));
        invalidStyles.put("ingore-long-style2", Long.valueOf(Long.MAX_VALUE));

        SimpleModule module = new SimpleModule();
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        fMapper.registerModule(module);

        OutputElementStyle testStyle = new OutputElementStyle(INVALID_STYLE_NAME, invalidStyles);
        String json = fMapper.writeValueAsString(testStyle);

        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle deserialized = fMapper.readValue(json, org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle.class);
        assertEquals(testStyle.getParentKey(), deserialized.getParentKey());
        assertTrue(deserialized.getValues().isEmpty());
    }

    private static void verifyStyles(OutputElementStyle testStyle, org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle deserialized) {
        for (Entry<String, Object> entry : testStyle.getStyleValues().entrySet()) {
            Map<String, StyleValue> styleValues = deserialized.getValues();
            assertNotNull(styleValues);
            String key = entry.getKey();
            Object entryValue = entry.getValue();
            assertTrue(key, styleValues.containsKey(key));
            StyleValue val = styleValues.get(key);
            assertNotNull(val);
            if (entryValue instanceof Float || entryValue instanceof Double) {
                assertEquals(key, Double.valueOf(entryValue.toString()), val.getDouble());
            } else if (entryValue instanceof Number longValue) {
                assertEquals(key, longValue.intValue(), (val.getInteger().intValue()));
            } else {
                assertEquals(key, entryValue, val.getString());
            }
        }
    }

}
