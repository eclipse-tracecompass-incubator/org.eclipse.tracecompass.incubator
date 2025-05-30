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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.OutputElementStyleSerializer;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.SeriesModelSerializer;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.OutputElementStyleStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XySeriesStub;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel.SeriesModelBuilder;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Test the {@link SeriesModelSerializer}
 *
 * @author Bernd Hufmann
 */
public class SeriesModelSerializerTest extends AbstractSerializerTest {

    private static final long ID = 0;
    private static final String TITLE = "valid-styles";
    private static final long[] times = { 0, 1, 2, 3 };
    private static final double[] fValues = { 0.1, 0.2, 0.3, 0.4 };

    /**
     * Verify that series models are serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @SuppressWarnings("null")
    @Test
    public void testValidStyles() throws JsonProcessingException {

        SeriesModelBuilder builder = new SeriesModelBuilder(ID, TITLE, times, fValues);
        ISeriesModel lineModel = builder.build();
        builder.seriesDisplayType(DisplayType.SCATTER);
        ISeriesModel scatterModel = builder.build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        fMapper.registerModule(module);

        String json = fMapper.writeValueAsString(lineModel);
        XySeriesStub deserialized = fMapper.readValue(json, XySeriesStub.class);
        assertNotNull(deserialized);
        assertEquals(TITLE, deserialized.getName());
        assertEquals(ID, deserialized.getId());
        List<Long> xValues = deserialized.getXValues();
        assertTrue(times.length == xValues.size());
        for (int i = 0; i < times.length; i++) {
            assertEquals(i, times[i], xValues.get(i));
        }
        List<Double> yValues = deserialized.getYValues();
        assertTrue(fValues.length == yValues.size());
        for (int i = 0; i < fValues.length; i++) {
            assertEquals(fValues[i], yValues.get(i), 0.000001);
        }
        OutputElementStyleStub style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        assertEquals(StyleProperties.SeriesType.LINE, style.getStyleValues().get(StyleProperties.SERIES_TYPE));

        json = fMapper.writeValueAsString(scatterModel);
        deserialized = fMapper.readValue(json, XySeriesStub.class);
        assertNotNull(deserialized);
        style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        assertEquals(StyleProperties.SeriesType.SCATTER, style.getStyleValues().get(StyleProperties.SERIES_TYPE));
    }
}
