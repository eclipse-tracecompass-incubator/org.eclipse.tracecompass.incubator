/*******************************************************************************
 * Copyright (c) 2013, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.flamechart;

import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Presentation provider for the Call Stack view, based on the generic TMF
 * presentation provider.
 *
 * @author Patrick Tasse
 */
@SuppressWarnings("restriction")
public class FlameChartPresentationProvider extends TimeGraphPresentationProvider {

    /** Number of colors used for call stack events */
    public static final int NUM_COLORS = 360;

    /**
     * Minimum width of a displayed state below which we will not print any text
     * into it. It corresponds to the average width of 1 char, plus the width of the
     * ellipsis characters.
     */
    private Integer fMinimumBarWidth;

    private final ControlFlowPresentationProvider fCfProvider = new ControlFlowPresentationProvider();

    private final LoadingCache<FlameChartEvent, Optional<String>> fTimeEventNames = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<FlameChartEvent, Optional<String>>() {
                @Override
                public Optional<String> load(FlameChartEvent event) {
                    return Optional.ofNullable(event.getFunctionName());
                }
            });

    private enum State {
        MULTIPLE (new RGB(100, 100, 100)),
        EXEC     (new RGB(0, 200, 0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Constructor
     *
     * @since 1.2
     */
    public FlameChartPresentationProvider() {
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        return Messages.CallStackPresentationProvider_Thread;
    }

    @Override
    public StateItem[] getStateTable() {
        final float saturation = 0.6f;
        final float brightness = 0.6f;
        StateItem[] cfStateTable = fCfProvider.getStateTable();
        StateItem[] stateTable = new StateItem[NUM_COLORS + 1 + cfStateTable.length];
        stateTable[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        for (int i = 0; i < NUM_COLORS; i++) {
            RGB rgb = new RGB(i, saturation, brightness);
            stateTable[i + 1] = new StateItem(rgb, State.EXEC.toString());
        }
        for (int i = 0; i < cfStateTable.length; i++) {
            stateTable[NUM_COLORS + 1 + i] = cfStateTable[i];
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof FlameChartEvent) {
            FlameChartEvent callStackEvent = (FlameChartEvent) event;
            return callStackEvent.getValue() + 1;
        } else if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        } else if (event instanceof SpanLinkEvent) {
            return (NUM_COLORS + 1 + ((SpanLinkEvent) event).getId()) % NUM_COLORS;
        } else if (event instanceof TimeEvent) {
            int cfIndex = fCfProvider.getStateTableIndex(event);
            if (cfIndex >= 0) {
                return NUM_COLORS + 1 + cfIndex;
            }
        }
        return State.MULTIPLE.ordinal();
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof FlameChartEvent) {
            return fTimeEventNames.getUnchecked((FlameChartEvent) event).orElse(null);
        }
        return State.MULTIPLE.toString();
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (!(event instanceof FlameChartEvent)) {
            return;
        }

        if (fMinimumBarWidth == null) {
            fMinimumBarWidth = gc.getFontMetrics().getAverageCharWidth() + gc.stringExtent(Utils.ELLIPSIS).x;
        }
        if (bounds.width <= fMinimumBarWidth) {
            /*
             * Don't print anything if we cannot at least show one character and ellipses.
             */
            return;
        }

        String name = fTimeEventNames.getUnchecked((FlameChartEvent) event).orElse(""); //$NON-NLS-1$
        if (name.isEmpty()) {
            /* No text to print */
            return;
        }

        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        Utils.drawText(gc, name, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
    }

    /**
     * Indicate that the provider of function names has changed, so any cached
     * values must be reset.
     */
    void resetFunctionNames() {
        fTimeEventNames.invalidateAll();
    }

}
