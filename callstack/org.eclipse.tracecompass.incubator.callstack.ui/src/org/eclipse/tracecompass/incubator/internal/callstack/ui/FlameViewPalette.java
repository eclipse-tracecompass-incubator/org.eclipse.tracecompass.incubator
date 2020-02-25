/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowPresentationProvider;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;
import org.eclipse.tracecompass.tmf.ui.colors.RGBAUtil;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;

/**
 * Class to manage the colors of the flame chart and flame graph views
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public final class FlameViewPalette {

    /**
     * The state index for the multiple state
     */
    public static final int MULTIPLE_STATE_INDEX = State.MULTIPLE.ordinal();
    private static final int NUM_COLORS = 360;

    private enum State {
        MULTIPLE(new RGB(100, 100, 100)), EXEC(new RGB(0, 200, 0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    private final StateItem[] fStateTable;

    private final ControlFlowPresentationProvider fCfProvider = new ControlFlowPresentationProvider();

    private static @Nullable FlameViewPalette fInstance = null;

    private FlameViewPalette() {
        IPaletteProvider palette = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();
        StateItem[] cfStateTable = fCfProvider.getStateTable();
        fStateTable = new StateItem[NUM_COLORS + 1 + cfStateTable.length];
        fStateTable[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        int i = 1;
        for (RGBAColor color : palette.get()) {
            fStateTable[i] = new StateItem(RGBAUtil.fromRGBAColor(color).rgb, color.toString());
            i++;
        }
        for (i = 0; i < cfStateTable.length; i++) {
            fStateTable[NUM_COLORS + 1 + i] = cfStateTable[i];
        }
    }

    /**
     * Get the instance of this palette
     *
     * @return The instance of the palette
     */
    public static FlameViewPalette getInstance() {
        FlameViewPalette instance = fInstance;
        if (instance == null) {
            instance = new FlameViewPalette();
            fInstance = instance;
        }
        return instance;
    }

    /**
     * Get the state table from this palette
     *
     * @return The state table
     */
    public StateItem[] getStateTable() {
        return fStateTable;
    }

    /**
     * Get the index in the flame views state table of an event that corresponds
     * to a control flow state
     *
     * @param event
     *            The time event that corresponds to a control flow event
     * @return The index in the flame view state table of this event, or
     *         <code>-1</code> if none found
     */
    public int getControlFlowIndex(ITimeEvent event) {
        int cfIndex = fCfProvider.getStateTableIndex(event);
        if (cfIndex >= 0) {
            return NUM_COLORS + 1 + cfIndex;
        }
        return -1;
    }

    /**
     * Get the index in the flame views state table for the integer value in
     * parameter
     *
     * @param value
     *            The value of the state event
     * @return The index in the flame view for this value
     */
    public static int getIndexForValue(int value) {
        return Math.floorMod(value, NUM_COLORS) + 1;
    }

}
