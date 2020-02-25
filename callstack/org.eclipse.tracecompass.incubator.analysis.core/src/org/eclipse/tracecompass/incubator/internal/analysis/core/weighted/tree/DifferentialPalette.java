/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.analysis.core.weighted.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.internal.analysis.core.Activator;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.presentation.DefaultColorPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.SequentialPaletteProvider;

import com.google.common.collect.ImmutableMap;

/**
 * A palette for differential values, which shows different tones of color
 * depending on the differential values. Positive values are green and negative
 * are red, paler shows a smaller difference, while darker shows bigger
 * difference.
 *
 * @author Geneviève Bastien
 */
public class DifferentialPalette implements IDataPalette {

    private static @Nullable DifferentialPalette fInstance = null;
    private static final int NB_COLORS = 5;
    private static final String NO_DIFF_STYLE = "equal"; //$NON-NLS-1$
    private static final String LESS_STYLES = "less"; //$NON-NLS-1$
    private static final String MORE_STYLES = "more"; //$NON-NLS-1$
    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        // Almost white style for when there is no diff
        builder.put(NO_DIFF_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, "No diff", //$NON-NLS-1$
                StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(200, 200, 200),
                StyleProperties.OPACITY, 1.0f)));

        // Create the green palette (for less)
        IPaletteProvider palette = SequentialPaletteProvider.create(DefaultColorPaletteProvider.GREEN, NB_COLORS + 1);
        int i = 0;
        for (RGBAColor color : palette.get()) {
            if (i == 0) {
                // Skip first color (white)
                i++;
                continue;
            }
            builder.put(LESS_STYLES + String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255)));
            i++;
        }

        // Create the red palette (for more)
        palette = SequentialPaletteProvider.create(DefaultColorPaletteProvider.RED, NB_COLORS + 1);
        i = 0;
        for (RGBAColor color : palette.get()) {
            if (i == 0) {
                // Skip first color (white)
                i++;
                continue;
            }
            builder.put(MORE_STYLES + String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255)));
            i++;
        }
        STYLES = builder.build();
    }

    private final int fMinThreshold;
    private final int fMaxThreshold;
    private final double fHeatStep;

    /**
     * Creates a palette with thresholds from/to which to define the heat of the
     * difference.
     *
     * @param minThreshold
     *            Minimal threshold (in %, typically between 0 and 100) of
     *            significance for the heat (absolute value). Any percentage
     *            below this value (whether positive or negative) will be
     *            considered as equal.
     * @param maxThreshold
     *            Maximal threshold (in %, typically between 0 and 100) of
     *            significance for the heat (absolute value). Any percentage
     *            above this value (whether positive or negative) will be
     *            considered at maximum heat
     * @return A differential palette with the given threshold
     */
    public static DifferentialPalette create(int minThreshold, int maxThreshold) {
        if (minThreshold == maxThreshold) {
            Activator.getInstance().logWarning("Creating differential palette with wrong arguments: min threshold should be different from max threshold " + minThreshold); //$NON-NLS-1$
            return new DifferentialPalette(minThreshold, minThreshold + 1);
        }
        return new DifferentialPalette(minThreshold, maxThreshold);
    }

    /**
     * Get the default instance of this palette
     *
     * @return The default instance of the palette
     */
    public static DifferentialPalette getInstance() {
        DifferentialPalette instance = fInstance;
        if (instance == null) {
            instance = new DifferentialPalette();
            fInstance = instance;
        }
        return instance;
    }

    private DifferentialPalette() {
        this(0, NB_COLORS - 1);
    }

    private DifferentialPalette(int minThreshold, int maxThreshold) {
        fMinThreshold = Math.min(Math.abs(minThreshold), Math.abs(maxThreshold));
        fMaxThreshold = Math.max(Math.abs(minThreshold), Math.abs(maxThreshold));
        fHeatStep = (double) (fMaxThreshold - fMinThreshold) / (NB_COLORS - 1);
    }

    @Override
    public OutputElementStyle getStyleFor(Object object) {
        if (object instanceof DifferentialWeightedTree) {
            DifferentialWeightedTree<?> tree = (DifferentialWeightedTree<?>) object;
            double difference = tree.getDifference();
            if (difference == Double.NaN) {
                return STYLE_MAP.computeIfAbsent(MORE_STYLES + NB_COLORS, styleStr -> new OutputElementStyle(styleStr));
            }
            double percent = (Math.abs(difference) * 100);
            // Not a significant difference
            if (percent <= fMinThreshold) {
                return STYLE_MAP.computeIfAbsent(NO_DIFF_STYLE, styleStr -> new OutputElementStyle(styleStr));
            }

            // Find the heat for this value
            int diffHeat = NB_COLORS;
            if (percent < fMaxThreshold) {
                // The heat must be at least 1
                diffHeat = Math.min(NB_COLORS, Math.max(1, (int) (((percent - fMinThreshold) / fHeatStep)) + 1));
            }

            // Find the right style, more or less, depending on the difference sign
            return STYLE_MAP.computeIfAbsent((difference < 0) ? LESS_STYLES + diffHeat : MORE_STYLES + diffHeat, styleStr -> new OutputElementStyle(styleStr));

        }
        return STYLE_MAP.computeIfAbsent(NO_DIFF_STYLE, styleStr -> new OutputElementStyle(styleStr));
    }

    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }

}