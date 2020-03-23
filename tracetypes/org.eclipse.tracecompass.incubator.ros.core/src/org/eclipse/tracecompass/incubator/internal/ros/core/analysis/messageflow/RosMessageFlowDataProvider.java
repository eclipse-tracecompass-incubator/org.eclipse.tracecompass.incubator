/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.IRosMessageFlowModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment.SegmentType;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Data provider for the ROS Message Flow view
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class RosMessageFlowDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> {

    /** Data provider suffix ID */
    public static final @NonNull String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    /** Separator for info in entry model name */
    public static final @NonNull String SEGMENT_NAME_SEP = ";"; //$NON-NLS-1$

    private static final AtomicLong ATOMIC_LONG = new AtomicLong();

    private @NonNull IRosMessageFlowModel fModel;

    /**
     * Map for message flow segment <-> id
     * TODO eventually unflatten this tree
     */
    private final BiMap<RosMessageFlowSegment, Long> fSegmentToId = HashBiMap.create();

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param model
     *            the message flow model
     */
    public RosMessageFlowDataProvider(@NonNull ITmfTrace trace, @NonNull IRosMessageFlowModel model) {
        super(trace);
        fModel = model;
    }

    private long getSegmentId(RosMessageFlowSegment segment) {
        return fSegmentToId.computeIfAbsent(segment, i -> ATOMIC_LONG.getAndIncrement());
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> fetchTree(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        if (!fModel.isModelDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        List<@NonNull TimeGraphEntryModel> entries = new ArrayList<>();
        long rootId = ATOMIC_LONG.getAndIncrement();
        entries.add(new TimeGraphEntryModel(rootId, -1, String.valueOf(getTrace().getName()), filter.getStart(), filter.getEnd()));
        RosMessageFlowSegment firstSegment = fModel.getFirstSegment();
        addTreeChildren(entries, firstSegment, rootId);
        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entries), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private void addTreeChildren(List<@NonNull TimeGraphEntryModel> entries, RosMessageFlowSegment segment, long parentId) {
        // TODO eventually find a better way to represent this (not flat/similar
        // to queues view)
        long entryId = getSegmentId(segment);
        entries.add(new RosMessageFlowSegmentEntryModel(entryId, parentId, segment.getStartTime(), segment.getEndTime(), segment));

        Collection<RosMessageFlowSegment> next = segment.getNext();
        for (RosMessageFlowSegment n : next) {
            addTreeChildren(entries, n, parentId);
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        if (!fModel.isModelDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        @NonNull Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        RosMessageFlowSegment firstSegment = fModel.getFirstSegment();
        addRowModels(rows, firstSegment, predicates, monitor);
        return new TmfModelResponse<>(new TimeGraphModel(rows), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private void addRowModels(
            List<@NonNull ITimeGraphRowModel> rows,
            RosMessageFlowSegment segment,
            @NonNull Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates,
            @Nullable IProgressMonitor monitor) {
        @NonNull List<@NonNull ITimeGraphState> eventList = new ArrayList<>();

        // TODO eventually find a better way
        long entryId = getSegmentId(segment);
        long startTime = segment.getStartTime();
        long duration = segment.getEndTime() - startTime + 1;
        TimeGraphState state = new TimeGraphState(startTime, duration, getMatchingSegmentState(segment.getType()));
        applyFilterAndAddState(eventList, state, entryId, predicates, monitor);
        rows.add(new TimeGraphRowModel(entryId, eventList));

        Collection<RosMessageFlowSegment> next = segment.getNext();
        for (RosMessageFlowSegment n : next) {
            addRowModels(rows, n, predicates, monitor);
        }
    }

    private static int getMatchingSegmentState(SegmentType type) {
        // See RosMessageFlowPresentationProvider.State
        switch (type) {
        case PUB_QUEUE:
            return 0;
        case SUB_QUEUE:
            return 1;
        case SUB_CALLBACK:
            return 2;
        case INVALID:
            break;
        default:
            break;
        }
        return 4;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        if (!fModel.isModelDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        return new TmfModelResponse<>(getArrows(filter.getStart(), filter.getEnd()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private @Nullable List<@NonNull ITimeGraphArrow> getArrows(long startTime, long endTime) {
        List<@NonNull ITimeGraphArrow> arrows = Lists.newArrayList();
        RosMessageFlowSegment firstSegment = fModel.getFirstSegment();
        addArrows(firstSegment, arrows, startTime, endTime);
        return arrows;
    }

    private void addArrows(RosMessageFlowSegment parent, List<@NonNull ITimeGraphArrow> arrows, long startTime, long endTime) {
        Collection<RosMessageFlowSegment> next = parent.getNext();
        for (RosMessageFlowSegment n : next) {
            if (parent.getEndTime() <= endTime && n.getStartTime() >= startTime) {
                addArrow(parent, n, arrows);
            }
            addArrows(n, arrows, startTime, endTime);
        }
    }

    private void addArrow(RosMessageFlowSegment parent, RosMessageFlowSegment child, List<@NonNull ITimeGraphArrow> arrows) {
        long sourceId = getSegmentId(parent);
        long destinationId = getSegmentId(child);
        long time = parent.getEndTime();
        long duration = child.getStartTime() - parent.getEndTime();
        arrows.add(new TimeGraphArrow(sourceId, destinationId, time, duration, getMatchingArrowState(parent.getType(), child.getType())));
    }

    private static int getMatchingArrowState(SegmentType source, SegmentType destination) {
        // See RosMessageFlowPresentationProvider.State
        if (source.equals(SegmentType.PUB_QUEUE) && destination.equals(SegmentType.SUB_QUEUE)) {
            return 4;
        } else if (source.equals(SegmentType.SUB_QUEUE) && destination.equals(SegmentType.SUB_CALLBACK)) {
            return 5;
        } else if (destination.equals(SegmentType.PUB_QUEUE)) {
            return 6;
        }
        return 7;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // TODO
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull String getId() {
        return getFullDataProviderId();
    }

    /**
     * @return the full dataprovider ID
     */
    public static @NonNull String getFullDataProviderId() {
        return RosMessageFlowAnalysis.ID + SUFFIX;
    }
}
