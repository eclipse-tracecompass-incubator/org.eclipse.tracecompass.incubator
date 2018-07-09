/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITmfTimeGraphDrawingHelper;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;

/**
 * Span life presentation provider
 *
 * @author Katherine Nadeau
 */
public class SpanLifePresentationProvider extends TimeGraphPresentationProvider {

    /**
     * Only states available
     */
    private static final StateItem[] STATE_TABLE = { new StateItem(new RGB(0, 0, 140), "Active") }; //$NON-NLS-1$

    /**
     * Constructor
     */
    public SpanLifePresentationProvider() {
        super("Span"); //$NON-NLS-1$
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> eventHoverToolTipInfo = super.getEventHoverToolTipInfo(event, hoverTime);
        if (eventHoverToolTipInfo == null) {
            eventHoverToolTipInfo = new LinkedHashMap<>();
        }
        ITimeGraphEntry entry = event.getEntry();
        if (entry instanceof TimeGraphEntry) {
            long id = ((TimeGraphEntry) entry).getModel().getId();
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = BaseDataProviderTimeGraphView.getProvider((TimeGraphEntry) entry);

            long windowStartTime = Long.MIN_VALUE;
            long windowEndTime = Long.MIN_VALUE;
            ITmfTimeGraphDrawingHelper drawingHelper = getDrawingHelper();
            if (drawingHelper instanceof TimeGraphControl) {
                TimeGraphControl timeGraphControl = (TimeGraphControl) drawingHelper;
                windowStartTime = timeGraphControl.getTimeDataProvider().getTime0();
                windowEndTime = timeGraphControl.getTimeDataProvider().getTime1();
            }

            List<@NonNull Long> times = new ArrayList<>();
            times.add(windowStartTime);
            times.add(hoverTime);
            times.add(windowEndTime);

            SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, Collections.singleton(id));
            TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> tooltipResponse = provider.fetchTooltip(filter, new NullProgressMonitor());
            Map<@NonNull String, @NonNull String> tooltipModel = tooltipResponse.getModel();
            if (tooltipModel != null) {
                eventHoverToolTipInfo.putAll(tooltipModel);
            }
        }
        return eventHoverToolTipInfo;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        return (event instanceof TimeEvent && ((TimeEvent) event).getValue() != Integer.MIN_VALUE) ? 0 : -1;
    }
}
