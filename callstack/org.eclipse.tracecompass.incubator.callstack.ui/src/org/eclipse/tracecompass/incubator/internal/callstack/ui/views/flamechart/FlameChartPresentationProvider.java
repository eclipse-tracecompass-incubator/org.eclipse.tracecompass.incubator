/*******************************************************************************
 * Copyright (c) 2013, 2017 Ericsson
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowPresentationProvider;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

import com.google.common.collect.ImmutableMap;

/**
 * Presentation provider for the Call Stack view, based on the generic TMF
 * presentation provider.
 *
 * @author Patrick Tasse
 */
public class FlameChartPresentationProvider extends TimeGraphPresentationProvider {

    /** Number of colors used for call stack events */
    public static final int NUM_COLORS = 360;

    private static final ControlFlowPresentationProvider CF_PROVIDER = new ControlFlowPresentationProvider();

    private static final StateItem[] STATE_TABLE;
    static {
        final float saturation = 0.6f;
        final float brightness = 0.6f;
        StateItem[] cfStateTable = CF_PROVIDER.getStateTable();
        STATE_TABLE = new StateItem[NUM_COLORS + 1 + cfStateTable.length];
        STATE_TABLE[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        for (int i = 0; i < NUM_COLORS; i++) {
            RGB rgb = new RGB(i, saturation, brightness);
            STATE_TABLE[i + 1] = new StateItem(rgb, State.EXEC.toString());
        }
        for (int i = 0; i < cfStateTable.length; i++) {
            STATE_TABLE[NUM_COLORS + 1 + i] = cfStateTable[i];
        }
    }

    /**
     * Minimum width of a displayed state below which we will not print any text
     * into it. It corresponds to the average width of 1 char, plus the width of
     * the ellipsis characters.
     */
    private Integer fMinimumBarWidth;

    private enum State {
        MULTIPLE (new RGB(100, 100, 100)),
        EXEC     (new RGB(0, 200, 0));

        private final RGB rgb;

        private State (RGB rgb) {
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

    /**
     * Sets the call stack view
     *
     * @param view
     *            The call stack view that will contain the time events
     * @since 1.2
     * @deprecated {@link FlameChartPresentationProvider} no longer needs the
     *             reference to the {@link FlameChartView}
     */
    @Deprecated
    public void setCallStackView(FlameChartView view) {
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        if (entry instanceof TimeGraphEntry) {
            ITimeGraphEntryModel model = ((TimeGraphEntry) entry).getModel();
            if (model instanceof FlameChartEntryModel) {
                return ((FlameChartEntryModel) model).getEntryType().name();
            }
        }
        return null;
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        // See if it is a thread status state
        TimeGraphEntry entry = (TimeGraphEntry) event.getEntry();
        FlameChartEntryModel model = (FlameChartEntryModel) entry.getModel();
        if (model.getEntryType().equals(EntryType.KERNEL)) {
            int cfIndex = CF_PROVIDER.getStateTableIndex(event);
            return (cfIndex >= 0) ? NUM_COLORS + 1 + cfIndex : cfIndex;
        }
        if (event instanceof NamedTimeEvent) {
            NamedTimeEvent callStackEvent = (NamedTimeEvent) event;
            return callStackEvent.getValue() + 1;
        } else if (event instanceof TimeLinkEvent) {
            return (NUM_COLORS + 1 + ((TimeLinkEvent) event).getValue()) % NUM_COLORS;
        } else if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return State.MULTIPLE.ordinal();
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof NamedTimeEvent) {
            return ((NamedTimeEvent) event).getLabel();
        }
        return State.MULTIPLE.toString();
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (!(event instanceof NamedTimeEvent)) {
            return;
        }

        if (fMinimumBarWidth == null) {
            fMinimumBarWidth = gc.getFontMetrics().getAverageCharWidth() + gc.stringExtent(Utils.ELLIPSIS).x;
        }
        if (bounds.width <= fMinimumBarWidth) {
            /*
             * Don't print anything if we cannot at least show one character and
             * ellipses.
             */
            return;
        }

        String label = ((NamedTimeEvent) event).getLabel();
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        Utils.drawText(gc, label, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> retMap = super.getEventHoverToolTipInfo(event, hoverTime);
        if (retMap == null) {
            retMap = new LinkedHashMap<>(1);
        }

        if (!(event instanceof TimeEvent) || !((TimeEvent) event).hasValue() ||
                !(event.getEntry() instanceof TimeGraphEntry)) {
            return retMap;
        }

        TimeGraphEntry entry = (TimeGraphEntry) event.getEntry();
        ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = BaseDataProviderTimeGraphView.getProvider(entry);
        TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = dataProvider.fetchTooltip(
                new SelectionTimeQueryFilter(hoverTime, hoverTime, 1, Collections.singletonList(entry.getModel().getId())), null);
        Map<@NonNull String, @NonNull String> map = response.getModel();
        if (map != null) {
            retMap.putAll(map);
        }

        return retMap;
    }

    @Override
    public Map<String, Object> getSpecificEventStyle(ITimeEvent event) {
        if (event instanceof TimeLinkEvent) {
            // styles are reused for links and states, make sure link height is 0.1f
            return ImmutableMap.of(ITimeEventStyleStrings.heightFactor(), 0.1f);
        }
        return Collections.emptyMap();
    }

}
