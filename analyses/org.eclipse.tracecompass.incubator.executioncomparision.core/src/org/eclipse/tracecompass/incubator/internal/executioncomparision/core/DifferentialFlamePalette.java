/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;

import com.google.common.collect.ImmutableMap;

/**
 * Class to manage the colors of the differential flame graph views
 *
 * @author Fateme Faraji Daneshgar
 */
public final class DifferentialFlamePalette implements IDataPalette {

    private static final String NAN = Objects.requireNonNull(Objects.toString(Double.NaN));
    private static final String NO_DIFFERENCE = "NO-Difference"; //$NON-NLS-1$
    private static String generateRed(int i) {
        return "RED" + i; //$NON-NLS-1$
    }

    private static String generateBlue(int i) {
        return "BLUE" + i; //$NON-NLS-1$
    }

    /**
     * The state index for the multiple state
     */
    private static double fMinThreshold = 1;
    private static final int MIN_HUE = 0;
    private static final int MAX_HUE = 255;
    private static final int NUM_COLORS = 360;
    private static final String RED_COLOR = Objects.requireNonNull(X11ColorUtils.toHexColor(255, 0, 0));
    private static final String WHITE_COLOR = Objects.requireNonNull(X11ColorUtils.toHexColor(255, 255, 255));


    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        IPaletteProvider palette = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();
        int i = 0;
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        for (RGBAColor color : palette.get()) {
            builder.put(String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255,
                    StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID)));
            i++;
        }
        int j=0;
        // Add dark red color for Nan
        builder.put(NAN, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, NAN,
                StyleProperties.BACKGROUND_COLOR, RED_COLOR,
                StyleProperties.OPACITY, 1,
                StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID)));
        // Add White White for NoDifference
        String noDiff = NO_DIFFERENCE;
        builder.put(noDiff, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, noDiff,
                StyleProperties.BACKGROUND_COLOR, WHITE_COLOR,
                StyleProperties.OPACITY, 1,
                StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID)));
        // Add Blue color palette for Shorter duration
        for (i = MIN_HUE; i <= MAX_HUE; i++) {
            j = (i-50)>0 ? i-50:0;
            String blueKey = generateBlue(i);
            builder.put(blueKey, new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, blueKey,
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(j, i, 255),
                    StyleProperties.OPACITY, 1,
                    StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID)));
        }
        // Add Red color palette for Longer duration
        for (i = MIN_HUE; i <= MAX_HUE; i++) {
            String redKey = generateRed(i);
            builder.put(redKey, new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, redKey,
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(255, i, i),
                    StyleProperties.OPACITY, 1,
                    StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID)));
        }
        STYLES = builder.build();
        STYLE_MAP.putAll(STYLES);
    }


    private static @Nullable DifferentialFlamePalette fInstance = null;

    private DifferentialFlamePalette() {
        // Do nothing
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static DifferentialFlamePalette getInstance() {
        DifferentialFlamePalette instance = fInstance;
        if (instance == null) {
            instance = new DifferentialFlamePalette();
            fInstance = instance;
        }
        return instance;
    }

    /**
     * Get the map of styles for this palette
     *
     * @return The styles
     */
    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }

    /**
     * Get the style element for a given value
     *
     * @param object
     *            The value to get an element for
     * @return The output style
     */

    @Override
    public OutputElementStyle getStyleFor(Object object) {
        if (object instanceof DifferentialWeightedTree) {
            DifferentialWeightedTree<?> tree = (DifferentialWeightedTree<?>) object;
            double difference = tree.getDifference();
            double step = MAX_HUE - MIN_HUE;
            if (Double.isNaN(difference)) {
                return STYLE_MAP.computeIfAbsent(NAN, OutputElementStyle::new);
            }
            if ((difference <= 0.05) &&(difference >= -0.05)) {
                return STYLE_MAP.computeIfAbsent(NO_DIFFERENCE, OutputElementStyle::new);
            }
            if (difference < 0) {
                return STYLE_MAP.computeIfAbsent(generateBlue((int) (MAX_HUE + Math.floor(difference * step))), OutputElementStyle::new);
            }

            if (Math.abs(difference) > fMinThreshold) {
                difference = fMinThreshold;
            }

            return STYLE_MAP.computeIfAbsent(generateRed(MAX_HUE - (int) (Math.floor((difference / fMinThreshold) * step))), OutputElementStyle::new);

        }
        throw new IllegalStateException("Cannot find the value of " + object); //$NON-NLS-1$
    }

    /**
     * @param min the minimum threshold for coloring.
     */
    public static void setMinThreshold(double min) {
        fMinThreshold = Math.round(min);
    }

}