/**********************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.provisional.scripting.core.analysis.ScriptedAnalysis;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.TimeGraphStateQueryFilter;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

import com.google.common.collect.Multimap;

/**
 * This data provider will return time graph models (wrapped in a response)
 * based on a query filter. The models can be used afterwards by any viewer to
 * draw time graphs. Model returned is for XML analysis.
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class ScriptedTimeGraphDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<ITimeGraphEntryModel> {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.tracecompass.tmf.analysis.xml.core.output.DataDrivenTimeGraphDataProvider"; //$NON-NLS-1$

    private final ITmfStateSystem fSs;
    private final Map<Long, Integer> fIDToDisplayQuark = new HashMap<>();

    private final String fId;
    private final Function<TimeQueryFilter, @Nullable List<ITimeGraphEntryModel>> fEntryMethod;
    private final @Nullable Function<TimeQueryFilter, @Nullable List<ITimeGraphArrow>> fArrowMethod;
    private final @Nullable Function<TimeQueryFilter, @Nullable List<ITimeGraphRowModel>> fRowModelMethod;

    /**
     * Constructor
     *
     * @param analysis
     *            The scripted analysis for this data provider
     * @param entryMethod
     *            The method to get the entries for this provider
     * @param rowModelMethod
     *            The method to get the row model
     * @param arrowMethod
     *            The method to get the arrows
     */
    public ScriptedTimeGraphDataProvider(ScriptedAnalysis analysis,
            Function<TimeQueryFilter, @Nullable List<ITimeGraphEntryModel>> entryMethod,
            @Nullable Function<TimeQueryFilter, @Nullable List<ITimeGraphRowModel>> rowModelMethod,
            @Nullable Function<TimeQueryFilter, @Nullable List<ITimeGraphArrow>> arrowMethod) {
        super(analysis.getTrace());
        fSs = Objects.requireNonNull(analysis.getStateSystem(true));
        fEntryMethod = entryMethod;
        fRowModelMethod = rowModelMethod;
        fArrowMethod = arrowMethod;
        fId = ScriptingDataProviderManager.PROVIDER_ID + ':' + analysis.getName();
    }

    @Override
    public TmfModelResponse<@NonNull List<ITimeGraphEntryModel>> fetchTree(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        boolean isComplete = fSs.waitUntilBuilt(0);

        List<ITimeGraphEntryModel> entryList = fEntryMethod.apply(filter);
        if (entryList == null) {
            entryList = Collections.emptyList();
        }
        for (ITimeGraphEntryModel entry : entryList) {
            if (entry instanceof ScriptedEntryDataModel) {
                fIDToDisplayQuark.put(entry.getId(), ((ScriptedEntryDataModel) entry).getQuark());
            }
        }
        Status status = isComplete ? Status.COMPLETED : Status.RUNNING;
        String msg = isComplete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;
        return new TmfModelResponse<>(entryList, status, msg);
    }

    @Override
    public @NonNull String getId() {
        return fId;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> fetchRowModel(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Function<TimeQueryFilter, @Nullable List<ITimeGraphRowModel>> rowModelMethod = fRowModelMethod;
        try {
            List<@NonNull ITimeGraphRowModel> rowModels = (rowModelMethod != null) ? rowModelMethod.apply(filter) : getDefaultRowModels(filter, monitor);
            if (rowModels == null) {
                rowModels = Collections.emptyList();
            }
            return new TmfModelResponse<>(rowModels, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
    }

    private @Nullable List<ITimeGraphRowModel> getDefaultRowModels(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        Map<Integer, ITimeGraphRowModel> quarkToRow = new HashMap<>();
        // Get the quarks to display
        for (Long id : filter.getSelectedItems()) {
            Integer quark = fIDToDisplayQuark.get(id);
            if (quark != null) {
                quarkToRow.put(quark, new TimeGraphRowModel(id, new ArrayList<>()));
            }
        }
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull String>>> predicates = new HashMap<>();
        if (filter instanceof TimeGraphStateQueryFilter) {
            TimeGraphStateQueryFilter timeEventFilter = (TimeGraphStateQueryFilter) filter;
            predicates.putAll(computeRegexPredicate(timeEventFilter));
        }

        long currentEndTime = fSs.getCurrentEndTime();
        for (ITmfStateInterval interval : fSs.query2D(quarkToRow.keySet(), getTimes(fSs, filter.getTimesRequested()))) {
            if (monitor != null && monitor.isCanceled()) {
                return Collections.emptyList();
            }
            ITimeGraphRowModel row = quarkToRow.get(interval.getAttribute());
            if (row != null) {
                List<@NonNull ITimeGraphState> states = row.getStates();
                ITimeGraphState timeGraphState = getStateFromInterval(interval, currentEndTime);
                applyFilterAndAddState(states, timeGraphState, row.getEntryID(), predicates, monitor);
            }
        }
        for (ITimeGraphRowModel model : quarkToRow.values()) {
            model.getStates().sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
        }

        return new ArrayList<>(quarkToRow.values());
    }

    private static TimeGraphState getStateFromInterval(ITmfStateInterval statusInterval, long currentEndTime) {
        long time = statusInterval.getStartTime();
        long duration = Math.min(currentEndTime, statusInterval.getEndTime() + 1) - time;
        Object o = statusInterval.getValue();
        if (o instanceof Integer) {
            return new TimeGraphState(time, duration, ((Integer) o).intValue(), String.valueOf(o));
        } else if (o instanceof Long) {
            long l = (long) o;
            return new TimeGraphState(time, duration, (int) l, "0x" + Long.toHexString(l)); //$NON-NLS-1$
        } else if (o instanceof String) {
            return new TimeGraphState(time, duration, Integer.MIN_VALUE, (String) o);
        } else if (o instanceof Double) {
            return new TimeGraphState(time, duration, ((Double) o).intValue());
        }
        return new TimeGraphState(time, duration, Integer.MIN_VALUE);
    }

    private static Set<Long> getTimes(ITmfStateSystem key, long[] timesRequested) {
        Set<@NonNull Long> times = new HashSet<>();
        for (long t : timesRequested) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        return times;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Function<TimeQueryFilter, @Nullable List<ITimeGraphArrow>> arrowMethod = fArrowMethod;
        if (arrowMethod == null) {
            return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        List<ITimeGraphArrow> arrows = arrowMethod.apply(filter);
        boolean completed = fSs.waitUntilBuilt(0);
        Status status = completed ? Status.COMPLETED : Status.RUNNING;
        String msg = completed ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;
        return new TmfModelResponse<>(arrows, status, msg);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

}
