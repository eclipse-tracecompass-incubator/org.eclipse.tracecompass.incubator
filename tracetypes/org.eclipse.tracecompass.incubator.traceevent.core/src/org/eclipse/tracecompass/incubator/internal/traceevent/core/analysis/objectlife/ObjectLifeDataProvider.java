/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.TreeMultimap;

/**
 * Data provider that will show the object lifespans.
 *
 * @author Matthew Khouzam
 *
 */
public class ObjectLifeDataProvider extends AbstractTimeGraphDataProvider<@NonNull ObjectLifeAnalysis, @NonNull TimeGraphEntryModel> {

    /**
     * Suffix for dataprovider ID
     */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public ObjectLifeDataProvider(ITmfTrace trace, ObjectLifeAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        Collection<@NonNull Integer> quarks = entries.values();
        long start = filter.getStart();
        if (ss == null || quarks.size() != 1 || !getAnalysisModule().isQueryable(start)) {
            /*
             * We need the ss to query, we should only be querying one attribute and the
             * query times should be valid.
             */
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        int quark = quarks.iterator().next();

        try {
            Map<String, String> retMap = null;
            ITmfStateInterval state = ss.querySingleState(start, quark);
            Object object = state.getValue();
            if (object instanceof String) {
                retMap = Collections.singletonMap("value", object.toString()); //$NON-NLS-1$
            } else {
                retMap = Collections.emptyMap();
            }
            return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }
    }

    @Override
    public @NonNull String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    protected @Nullable List<@NonNull ITimeGraphRowModel> getRowModel(@NonNull ITmfStateSystem ss, @NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        /* Do the actual query */
        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }
            intervals.put(interval.getAttribute(), interval);
        }
        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }

            List<ITimeGraphState> eventList = new ArrayList<>();
            for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                Object state = interval.getValue();
                TimeGraphState value = state == null ? new TimeGraphState(startTime, duration, Math.max(entry.getKey().intValue(), 0)) : new TimeGraphState(startTime, duration, Math.max(entry.getKey().intValue(), 0), String.valueOf(state));
                eventList.add(value);
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));

        }
        return rows;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @SuppressWarnings("restriction")
    @Override
    protected @NonNull List<@NonNull TimeGraphEntryModel> getTree(@NonNull ITmfStateSystem ss, @NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Builder<@NonNull TimeGraphEntryModel> builder = new Builder<>();
        long parentId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new TimeGraphEntryModel(parentId, -1, getTrace().getName(), ss.getStartTime(), ss.getCurrentEndTime()));
        addChildren(ss, builder, ITmfStateSystem.ROOT_ATTRIBUTE, parentId);
        return builder.build();
    }

    private void addChildren(ITmfStateSystem ss, Builder<@NonNull TimeGraphEntryModel> builder, int quark, long parentId) {
        for (Integer child : ss.getSubAttributes(quark, false)) {
            long childId = getId(child);
            builder.add(new TimeGraphEntryModel(childId, parentId, ss.getAttributeName(child), ss.getStartTime(), ss.getCurrentEndTime()));
            addChildren(ss, builder, child, childId);
        }
    }

}
