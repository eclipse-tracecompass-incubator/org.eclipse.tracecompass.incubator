/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.criticalpath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.CriticalPathPalette;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.QualitativePaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;

import com.google.common.collect.ImmutableMap;

/**
 * The palette for the critical path aggregated. The critical path states will
 * use the {@link CriticalPathPalette} and threads and processes will use a qualitative palette.
 *
 * @author gbastien
 */
@SuppressWarnings("restriction")
public class CriticalPathAggregatedPalette implements IDataPalette {

    private static final int NUM_COLORS = 8;
    private static final String DEFAULT_PREFIX = "process"; //$NON-NLS-1$
    // Map of base styles
    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.putAll(CriticalPathPalette.getStyles());
        IPaletteProvider palette = new QualitativePaletteProvider.Builder().setNbColors(NUM_COLORS).build();
        int i = 0;
        for (RGBAColor color : palette.get()) {
            builder.put(DEFAULT_PREFIX + String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, DEFAULT_PREFIX + String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255)));
            i++;
        }
        STYLES = builder.build();
    }

    private static @Nullable CriticalPathAggregatedPalette fInstance = null;

    private CriticalPathAggregatedPalette() {
        // Do nothing
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static CriticalPathAggregatedPalette getInstance() {
        CriticalPathAggregatedPalette instance = fInstance;
        if (instance == null) {
            instance = new CriticalPathAggregatedPalette();
            fInstance = instance;
        }
        return instance;
    }

    @Override
    public OutputElementStyle getStyleFor(Object object) {
        if (object instanceof WeightedTree) {
            WeightedTree<?> tree = (WeightedTree<?>) object;
            Object treeObject = tree.getObject();
            if (treeObject instanceof EdgeType) {
                return STYLE_MAP.computeIfAbsent(((EdgeType) treeObject).name(), style -> new OutputElementStyle(style));
            }
            return STYLE_MAP.computeIfAbsent(DEFAULT_PREFIX + String.valueOf(Math.floorMod(treeObject.hashCode(), NUM_COLORS)), style -> new OutputElementStyle(style));
        }
        return STYLE_MAP.computeIfAbsent(DEFAULT_PREFIX + String.valueOf(Math.floorMod(object.hashCode(), NUM_COLORS)), style -> new OutputElementStyle(style));
    }

    @Override
    public Map<String, OutputElementStyle> getStyles() {
        return STYLES;
    }

}
