/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.FlameViewPalette;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Presentation provider for the flame graph view, based on the generic TMF
 * presentation provider.
 *
 * @author Sonia Farrah
 */
public class FlameGraphPresentationProvider extends TimeGraphPresentationProvider {
    /** Number of colors used for flameGraph events */
    public static final int NUM_COLORS = 360;

    private @Nullable FlameGraphView fView;

    private FlameViewPalette fFlameViewPalette;

    /**
     * Constructor
     */
    public FlameGraphPresentationProvider() {
        fFlameViewPalette = FlameViewPalette.getInstance();
    }

    @Override
    public StateItem[] getStateTable() {
        return fFlameViewPalette.getStateTable();
    }

    @Override
    public boolean displayTimesInTooltip() {
        return false;
    }

    @Override
    public String getStateTypeName() {
        return Objects.requireNonNull(Messages.FlameGraph_Depth);
    }

    @NonNullByDefault({})
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
        ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = FlameGraphView.getProvider(entry);
        TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = dataProvider.fetchTooltip(
        FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(hoverTime, hoverTime, 1, Collections.singletonList(entry.getEntryModel().getId()))), null);
        Map<@NonNull String, @NonNull String> tooltipModel = response.getModel();
        if (tooltipModel != null) {
            retMap.putAll(tooltipModel);
        }

        return retMap;
    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event == null || event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        ITimeGraphEntry entry = event.getEntry();
        if (event instanceof TimeEvent && entry instanceof TimeGraphEntry) {
            TimeGraphEntry tgEntry = (TimeGraphEntry) entry;
            ITmfTreeDataModel entryModel = tgEntry.getEntryModel();
            if (entryModel instanceof FlameChartEntryModel) {
                switch(((FlameChartEntryModel) entryModel).getEntryType()) {
                case FUNCTION:
                    return FlameViewPalette.getIndexForValue(((TimeEvent) event).getValue());
                case KERNEL:
                    int cfIndex = fFlameViewPalette.getControlFlowIndex(event);
                    if (cfIndex >= 0) {
                        return cfIndex;
                    }
                    break;
                case LEVEL: // Fallthrough
                case TRACE: // Fallthrough
                default:
                    break;
                }
            }
        }


        return FlameViewPalette.MULTIPLE_STATE_INDEX;
    }

    /**
     * The flame graph view
     *
     * @return The flame graph view
     */
    public @Nullable FlameGraphView getView() {
        return fView;
    }

    /**
     * The flame graph view
     *
     * @param view
     *            The flame graph view
     */
    public void setView(FlameGraphView view) {
        fView = view;
    }

}
