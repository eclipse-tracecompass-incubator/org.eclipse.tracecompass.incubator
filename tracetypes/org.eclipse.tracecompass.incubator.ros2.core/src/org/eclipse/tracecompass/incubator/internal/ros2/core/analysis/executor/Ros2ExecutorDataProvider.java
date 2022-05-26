/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Data provider for the ROS 2 Executor view.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2ExecutorDataProvider extends AbstractTimeGraphDataProvider<@NonNull Ros2ExecutorAnalysis, @NonNull TimeGraphEntryModel> {

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Ros2ExecutorDataProvider(@NonNull ITmfTrace trace, @NonNull Ros2ExecutorAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(@NonNull ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        Collection<@NonNull Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        @SuppressWarnings("null")
        Collection<Integer> valuesNull = entries.values();
        @SuppressWarnings("null")
        Collection<Long> timesNull = times;

        // Query
        for (ITmfStateInterval interval : ss.query2D(valuesNull, timesNull)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            intervals.put(interval.getAttribute(), interval);
        }

        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<@NonNull Long, @NonNull Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }

            List<ITimeGraphState> eventList = new ArrayList<>();
            for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                Object valObject = interval.getValue();
                if (valObject instanceof Ros2ExecutorStateInstance) {
                    Ros2ExecutorStateInstance executorStateInstance = (Ros2ExecutorStateInstance) valObject;
                    Ros2ExecutorTimeGraphState state = new Ros2ExecutorTimeGraphState(startTime, duration, executorStateInstance);
                    applyFilterAndAddState(eventList, state, entry.getKey(), predicates, monitor);
                }
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));

        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected @NonNull TmfTreeModel<@NonNull TimeGraphEntryModel> getTree(@NonNull ITmfStateSystem ss, @NonNull Map<@NonNull String, @NonNull Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Builder<@NonNull TimeGraphEntryModel> builder = new Builder<>();
        long parentId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new TimeGraphEntryModel(parentId, -1, String.valueOf(getTrace().getName()), ss.getStartTime(), ss.getCurrentEndTime()));
        addChildren(ss, builder, ITmfStateSystem.ROOT_ATTRIBUTE, parentId);
        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    private void addChildren(ITmfStateSystem ss, Builder<@NonNull TimeGraphEntryModel> builder, int quark, long parentId) {
        for (Integer child : ss.getSubAttributes(quark, false)) {
            long childId = getId(child);
            String name = ss.getAttributeName(child);

            // For trace attributes, add hostname+host ID as labels
            if (ITmfStateSystem.ROOT_ATTRIBUTE == quark) {
                HostInfo hostInfo = getHostInfo(ss, child);
                if (null != hostInfo) {
                    List<@NonNull String> labels = Lists.newArrayList(name, hostInfo.getHostname(), hostInfo.getId());
                    builder.add(new TimeGraphEntryModel(childId, parentId, Objects.requireNonNull(labels), ss.getStartTime(), ss.getCurrentEndTime(), true));
                    addChildren(ss, builder, child, childId);
                }
            } else {
                boolean isRowModel = child != ITmfStateSystem.ROOT_ATTRIBUTE;
                /**
                 * For processes with only one executor instance (or only one
                 * executor thread), we want to simply show the TID row directly
                 * instead of showing it below the PID row.
                 *
                 * For example, in this case, process 1234 only has one thread,
                 * so we simply display one row.
                 *
                 * <pre>
                 * * 1234 (PID)
                 * * 5678 (PID)
                 *    * 5678 (TID)
                 *    * 5679 (TID)
                 * </pre>
                 *
                 * Therefore, check if this (child) quark has only one (grand)
                 * child quark with the same attribute name (i.e., PID == TID).
                 */
                List<@NonNull Integer> grandChildrenQuarks = ss.getSubAttributes(child, false);
                String grandChildName = !grandChildrenQuarks.isEmpty() ? ss.getAttributeName(grandChildrenQuarks.get(0)) : null;
                if (grandChildrenQuarks.size() == 1 && null != grandChildName && grandChildName.equals(name)) {
                    Integer grandchild = grandChildrenQuarks.get(0);
                    childId = getId(grandchild);
                    builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), isRowModel));
                } else {
                    builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), isRowModel));
                    addChildren(ss, builder, child, childId);
                }
            }
        }
    }

    private static @Nullable HostInfo getHostInfo(ITmfStateSystem ss, int quark) {
        try {
            // The state should span the whole trace
            ITmfStateInterval stateInterval = ss.querySingleState(ss.getStartTime(), quark);
            if (!(stateInterval.getValue() instanceof HostInfo)) {
                return null;
            }
            return (HostInfo) stateInterval.getValue();
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * @return the full dataprovider ID
     */
    public static String getFullDataProviderId() {
        return Ros2ExecutorAnalysis.getFullAnalysisId() + Ros2ExecutorDataProvider.SUFFIX;
    }
}
