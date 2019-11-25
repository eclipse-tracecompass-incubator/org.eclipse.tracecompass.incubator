/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.analysis.core.weighted.tree;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
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
    private static final OutputElementStyle WHITE_STYLE;
    private static final String LESS_STYLES = "less"; //$NON-NLS-1$
    private static final String MORE_STYLES = "more"; //$NON-NLS-1$
    private static final Map<String, OutputElementStyle> STYLES;

    static {
        // Almost white style for when there is no diff
        WHITE_STYLE = new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, "No diff", //$NON-NLS-1$
                StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(200, 200, 200),
                StyleProperties.OPACITY, 1.0f));

        // Create the green palette (for less)
        IPaletteProvider palette = SequentialPaletteProvider.create(DefaultColorPaletteProvider.GREEN, NB_COLORS + 1);
        int i = 0;
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.put(NO_DIFF_STYLE, WHITE_STYLE);
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

    private DifferentialPalette() {
        // Private constructor
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static DifferentialPalette getInstance() {
        DifferentialPalette instance = fInstance;
        if (instance == null) {
            instance = new DifferentialPalette();
            fInstance = instance;
        }
        return instance;
    }

    @Override
    public OutputElementStyle getStyleFor(Object object) {
        if (object instanceof DifferentialWeightedTree) {
            DifferentialWeightedTree<?> tree = (DifferentialWeightedTree<?>) object;
            double difference = tree.getDifference();
            if (difference == Double.NaN) {
                return STYLES.getOrDefault(MORE_STYLES + NB_COLORS, WHITE_STYLE);
            }
            if (difference == 0) {
                return WHITE_STYLE;
            }
            if (difference < 0) {
                // The heat will be between 1 and NB_COLORS
                int diffHeat = Math.max(1, Math.min(NB_COLORS, (int) (Math.abs(difference) * 100)));
                return STYLES.getOrDefault(LESS_STYLES + diffHeat, WHITE_STYLE);
            }
            int diffHeat = Math.max(1, Math.min(NB_COLORS, (int) difference * 100));
            return STYLES.getOrDefault(MORE_STYLES + diffHeat, WHITE_STYLE);
        }
        return WHITE_STYLE;
    }

    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }

}