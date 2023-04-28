/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.ui.markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context.ContextDataProvider;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context.ContextDataProvider.MarkerModel;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

import com.google.common.collect.Lists;

/**
 * Context Marker event source, the source of the markers
 *
 * @author Matthew Khouzam
 *
 */
public class ContextMarkers implements IMarkerEventSource {

    /**
     * Randomly picked
     */
    private static final RGBA ONE_TRUE_COLOUR = new RGBA(44, 33, 88, 93);

    /**
     * Constructor
     */
    public ContextMarkers() {
        // Do nothing
    }

    @Override
    public @NonNull List<@NonNull String> getMarkerCategories() {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace == null) {
            return Collections.emptyList();
        }

        ContextDataProvider dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(activeTrace, ContextDataProvider.ID, ContextDataProvider.class);
        if (dataProvider == null) {
            return Collections.emptyList();
        }
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> tree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(Collections.emptyList())), new NullProgressMonitor());
        TmfTreeModel<@NonNull TimeGraphEntryModel> model = tree.getModel();

        if (model == null) {
            return Collections.emptyList();
        }
        Set<String> data = new HashSet<>();
        for (TmfTreeDataModel elem : model.getEntries()) {
            data.add(elem.getName());
        }
        List<String> dataList = Lists.newArrayList(data);
        dataList.sort(null);
        return dataList;
    }

    @Override
    public @NonNull List<@NonNull IMarkerEvent> getMarkerList(@NonNull String category, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace == null) {
            return Collections.emptyList();
        }
        ContextDataProvider dataProvider = DataProviderManager.getInstance().getOrCreateDataProvider(activeTrace, ContextDataProvider.ID, ContextDataProvider.class);
        if (dataProvider == null) {
            return Collections.emptyList();
        }
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> tree = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(Collections.emptyList())), monitor);
        TmfTreeModel<@NonNull TimeGraphEntryModel> treeModels = tree.getModel();

        if (treeModels == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (TmfTreeDataModel treeModel : treeModels.getEntries()) {
            if (treeModel.getName().startsWith(category)) {
                ids.add(treeModel.getId());
            }
        }
        TmfModelResponse<@NonNull TimeGraphModel> res = dataProvider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(startTime, endTime, (int) Math.max(2, (((double) endTime - startTime) / resolution)), ids)), monitor);
        TimeGraphModel rowModels = res.getModel();
        if (rowModels == null || rowModels.getRows().isEmpty()) {
            return Collections.emptyList();
        }
        List<IMarkerEvent> events = new ArrayList<>();
        for (ITimeGraphRowModel rowModel : rowModels.getRows()) {
            for (ITimeGraphState model : rowModel.getStates()) {
                if (model instanceof MarkerModel) {
                    events.add(new MarkerEvent(null, model.getStartTime(), model.getDuration(), ((MarkerModel) model).getCategory(), ONE_TRUE_COLOUR, model.getLabel(), true));
                }
            }
        }
        return events;
    }

}
