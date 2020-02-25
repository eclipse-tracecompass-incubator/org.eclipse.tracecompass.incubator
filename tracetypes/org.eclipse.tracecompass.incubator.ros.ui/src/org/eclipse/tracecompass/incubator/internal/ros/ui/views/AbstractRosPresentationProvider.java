/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Base presentation provider for ROS views
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRosPresentationProvider extends TimeGraphPresentationProvider {

    /** Tab */
    protected static final String TAB = "\t"; //$NON-NLS-1$

    private static final int NUM_COLORS = 360;
    private static final StateItem[] STATE_TABLE;
    private static final IPaletteProvider PALETTE = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();

    static {
        STATE_TABLE = new StateItem[NUM_COLORS + 1];
        STATE_TABLE[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        int i = 1;
        for (RGBAColor color : PALETTE.get()) {
            STATE_TABLE[i] = new StateItem(RGBAUtil.fromRGBAColor(color).rgb, color.toString());
            i++;
        }
    }

    private enum State {
        MULTIPLE(new RGB(100, 100, 100)), EXEC(new RGB(0, 200, 0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /** The event cache */
    protected final LoadingCache<NamedTimeEvent, Optional<String>> fTimeEventNames = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<NamedTimeEvent, Optional<String>>() {
                @Override
                public Optional<String> load(NamedTimeEvent event) {
                    return Optional.ofNullable(event.getLabel());
                }
            });

    /**
     * Constructor
     */
    public AbstractRosPresentationProvider() {
        super(StringUtils.EMPTY);
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof NamedTimeEvent) {
            NamedTimeEvent namedEvent = (NamedTimeEvent) event;
            return Math.floorMod(namedEvent.getLabel().hashCode(), PALETTE.get().size()) + 1;
        }
        return INVISIBLE;
    }

    @Override
    public @Nullable String getEventName(ITimeEvent event) {
        return null;
    }

    @Override
    public @Nullable String getStateTypeName(ITimeGraphEntry entry) {
        return null;
    }

    @Override
    public @Nullable Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        return null;
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (!(event instanceof NamedTimeEvent)) {
            return;
        }

        String name = fTimeEventNames.getUnchecked((NamedTimeEvent) event).orElse(StringUtils.EMPTY);
        if (name.isEmpty()) {
            // No text to print
            return;
        }

        if (bounds.width > bounds.height) {
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
            Utils.drawText(gc, name, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
        }
    }
}
