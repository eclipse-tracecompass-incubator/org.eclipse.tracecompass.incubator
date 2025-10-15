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
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp.TmfXYAxisDescriptionSerializer;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Range;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.SeriesModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StyleValue;
import org.eclipse.tracecompass.tmf.core.model.ISampling;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.SeriesModel.SeriesModelBuilder;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel.DisplayType;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Test the {@link SeriesModelSerializer}
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class SeriesModelSerializerTest extends AbstractSerializerTest {

    private static final long ID = 0;
    private static final String TITLE = "valid-styles";
    private static final ISampling times = new ISampling.Timestamps(new long[] { 0, 1, 2, 3 });
    private static final List<Range> EXPECTED_RANGES = List.of(new Range().start(0L).end(1L),
            new Range().start(2L).end(3L),
            new Range().start(4L).end(5L),
            new Range().start(6L).end(7L));
    private static final List<String> EXPECTED_CATEGORIES= List.of("Q1", "Q2", "Q3", "Q4");
    private static final ISampling ranges = new ISampling.Ranges(List.of(new ISampling.Range<>(0L, 1L),
            new ISampling.Range<>(2L, 3L),
            new ISampling.Range<>(4L, 5L),
            new ISampling.Range<>(6L, 7L)));
    private static final ISampling categories = new ISampling.Categories(EXPECTED_CATEGORIES);
    private static final double[] fValues = { 0.1, 0.2, 0.3, 0.4 };

    /**
     * Verify that series models are serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @Test
    public void testValidStylesWithXValues() throws JsonProcessingException {

        SeriesModelBuilder builder = new SeriesModelBuilder(ID, TITLE, times, fValues);
        ISeriesModel lineModel = builder.build();
        builder.seriesDisplayType(DisplayType.SCATTER);
        ISeriesModel scatterModel = builder.build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        module.addSerializer(TmfXYAxisDescription.class, new TmfXYAxisDescriptionSerializer());
        fMapper.registerModule(module);

        String json = fMapper.writeValueAsString(lineModel);
        SeriesModel deserialized = fMapper.readValue(json, SeriesModel.class);
        assertNotNull(deserialized);
        assertEquals(TITLE, deserialized.getSeriesName());
        assertEquals(ID, deserialized.getSeriesId().longValue());
        List<Long> actual = deserialized.getxValues();
        assertTrue(deserialized.getxCategories().isEmpty());
        assertTrue(deserialized.getxRanges().isEmpty());
        assertEquals(List.of(0L, 1L, 2L, 3L), actual);
        List<Double> yValues = deserialized.getyValues();
        assertTrue(fValues.length == yValues.size());
        for (int i = 0; i < fValues.length; i++) {
            assertEquals(fValues[i], yValues.get(i), 0.000001);
        }
        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        StyleValue val = style.getValues().get(StyleProperties.SERIES_TYPE);
        assertNotNull(val);
        assertEquals(StyleProperties.SeriesType.LINE, val.getString());

        json = fMapper.writeValueAsString(scatterModel);
        deserialized = fMapper.readValue(json, SeriesModel.class);
        assertNotNull(deserialized);
        style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        val = style.getValues().get(StyleProperties.SERIES_TYPE);
        assertNotNull(val);
        assertEquals(StyleProperties.SeriesType.SCATTER, val.getString());
    }

    /**
     * Verify that series models are serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @Test
    public void testWithXRanges() throws JsonProcessingException {

        SeriesModelBuilder builder = new SeriesModelBuilder(ID, TITLE, ranges, fValues);
        builder.seriesDisplayType(DisplayType.BAR);
        ISeriesModel barModel = builder.build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        module.addSerializer(TmfXYAxisDescription.class, new TmfXYAxisDescriptionSerializer());
        fMapper.registerModule(module);

        String json = fMapper.writeValueAsString(barModel);
        SeriesModel deserialized = fMapper.readValue(json, SeriesModel.class);
        assertNotNull(deserialized);
        assertEquals(TITLE, deserialized.getSeriesName());
        assertEquals(ID, deserialized.getSeriesId().longValue());
        assertTrue(deserialized.getxValues().isEmpty());
        assertTrue(deserialized.getxCategories().isEmpty());
        List<Range> actual = deserialized.getxRanges();
        assertEquals(EXPECTED_RANGES, actual);
        List<Double> yValues = deserialized.getyValues();
        assertTrue(fValues.length == yValues.size());
        for (int i = 0; i < fValues.length; i++) {
            assertEquals(fValues[i], yValues.get(i), 0.000001);
        }
        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        StyleValue val = style.getValues().get(StyleProperties.SERIES_TYPE);
        assertNotNull(val);
        assertEquals(StyleProperties.SeriesType.BAR, val.getString());
    }

    /**
     * Verify that series models are serialized properly
     *
     * @throws JsonProcessingException
     *             if an error occurs
     */
    @Test
    public void testWithXCategories() throws JsonProcessingException {

        SeriesModelBuilder builder = new SeriesModelBuilder(ID, TITLE, categories, fValues);
        builder.seriesDisplayType(DisplayType.BAR);
        ISeriesModel barModel = builder.build();

        SimpleModule module = new SimpleModule();
        module.addSerializer(ISeriesModel.class, new SeriesModelSerializer());
        module.addSerializer(OutputElementStyle.class, new OutputElementStyleSerializer());
        module.addSerializer(TmfXYAxisDescription.class, new TmfXYAxisDescriptionSerializer());
        fMapper.registerModule(module);

        String json = fMapper.writeValueAsString(barModel);
        SeriesModel deserialized = fMapper.readValue(json, SeriesModel.class);
        assertNotNull(deserialized);
        assertEquals(TITLE, deserialized.getSeriesName());
        assertEquals(ID, deserialized.getSeriesId().longValue());
        assertTrue(deserialized.getxValues().isEmpty());
        assertTrue(deserialized.getxRanges().isEmpty());
        List<String> actual = deserialized.getxCategories();
        assertEquals(EXPECTED_CATEGORIES, actual);
        List<Double> yValues = deserialized.getyValues();
        assertTrue(fValues.length == yValues.size());
        for (int i = 0; i < fValues.length; i++) {
            assertEquals(fValues[i], yValues.get(i), 0.000001);
        }
        org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle style = deserialized.getStyle();
        assertNotNull(style);
        assertNull(style.getParentKey());
        StyleValue val = style.getValues().get(StyleProperties.SERIES_TYPE);
        assertNotNull(val);
        assertEquals(StyleProperties.SeriesType.BAR, val.getString());
    }
}
