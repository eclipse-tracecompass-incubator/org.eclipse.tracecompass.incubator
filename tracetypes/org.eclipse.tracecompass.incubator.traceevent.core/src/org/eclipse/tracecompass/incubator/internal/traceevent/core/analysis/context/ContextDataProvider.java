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

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Context analysis datasource. Provides the locations of contexts.
 *
 * @author Matthew Khouzam
 */
public class ContextDataProvider extends AbstractTimeGraphDataProvider<@NonNull ContextAnalysis, @NonNull TimeGraphEntryModel> {

    /**
     * ID of this analysis
     */
    public static final @NonNull String ID = ContextAnalysis.ID + ".dataprovider"; //$NON-NLS-1$

    /**
     * Model of a marker, not necessarily bound to a timegraph, denotes an area
     * that's interesting.
     *
     * Note: Do we have anything that already does this?
     */
    public static class MarkerModel extends TimeGraphState {

        private final String fAnnotation;
        private final String fCategory;

        /**
         * Constructor, used for internal reasons, as the array is hard coded
         *
         * @param startTime
         *            the start time
         * @param duration
         *            the duration
         *
         * @param string
         *            {category, name, annotation}
         */
        private MarkerModel(long startTime, long duration, String[] sections) {
            super(startTime, duration, 0, sections[1]);
            fCategory = sections[0];
            fAnnotation = sections[2];
        }

        /**
         * Gets the category of the marker
         *
         * @return the category
         */
        public String getCategory() {
            return fCategory;
        }

        /**
         * Gets the annotation of the marker
         *
         * @return the annotation
         */
        public String getAnnotation() {
            return fAnnotation;
        }

    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public ContextDataProvider(ITmfTrace trace, ContextAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, "Not supported"); //$NON-NLS-1$
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, "Not supported"); //$NON-NLS-1$
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(@NonNull ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        /*
         * Order is not important here
         */
        Map<Integer, String[]> paths = new HashMap<>();
        List<ITimeGraphState> markerList = new ArrayList<>();

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        Set<Integer> quarks = new HashSet<>();
        Map<@NonNull Long, @NonNull Integer> selectedEntries = getSelectedEntries(filter);
        for (Integer quark : selectedEntries.values()) {
            quarks.addAll(ss.getSubAttributes(quark, true));
        }
        /* resolve the names */
        for (Integer quark : quarks) {
            paths.put(quark, ss.getFullAttributePathArray(quark));
        }
        /* Do the actual query */
        for (ITmfStateInterval interval : ss.query2D(quarks, times)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            if (interval.getValue() instanceof Integer) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;

                markerList.add(new MarkerModel(startTime, duration, paths.get(interval.getAttribute())));
            }
        }

        if (monitor != null && monitor.isCanceled()) {
            return new TimeGraphModel(Collections.emptyList());
        }
        return new TimeGraphModel(Collections.singletonList(new TimeGraphRowModel(getId(ITmfStateSystem.ROOT_ATTRIBUTE), markerList)));
    }

    @Override
    protected TmfTreeModel<@NonNull TimeGraphEntryModel> getTree(@NonNull ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        List<@NonNull Integer> attribs = ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false);
        List<@NonNull TimeGraphEntryModel> retVal = new ArrayList<>();
        for (Integer attrib : attribs) {
            String[] strings = ss.getFullAttributePathArray(attrib);
            retVal.add(new TimeGraphEntryModel(getId(attrib), rootId, Collections.singletonList(strings[0]), ss.getStartTime(), ss.getCurrentEndTime()));
        }
        return new TmfTreeModel<>(Collections.emptyList(), retVal);
    }

}
