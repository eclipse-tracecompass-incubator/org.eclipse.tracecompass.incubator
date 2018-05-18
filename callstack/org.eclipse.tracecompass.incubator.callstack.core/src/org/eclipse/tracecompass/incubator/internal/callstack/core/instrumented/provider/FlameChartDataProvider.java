/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.CallStackDepth;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * This class provides the data from an instrumented callstack analysis, in the
 * form of a flamechart, ie the groups are returned hierarchically and leaf
 * groups return their callstacks.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class FlameChartDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<FlameChartEntryModel> {

    /**
     * Provider ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.flamechart"; //$NON-NLS-1$
    private static final AtomicLong ENTRY_ID = new AtomicLong();
    /**
     * Logger for Abstract Tree Data Providers.
     */
    private static final Logger LOGGER = TraceCompassLog.getLogger(FlameChartDataProvider.class);

    private final Map<Long, FlameChartEntryModel> fEntries = new HashMap<>();
    // Key is the row ID that requires linked data (for instance a kernel row) and
    // value is the row being linked to (the one from the callstack)
    private final BiMap<Long, Long> fLinkedEntries = HashBiMap.create();
    private final Collection<ISymbolProvider> fProviders = new ArrayList<>();
    private final BiMap<Long, CallStackDepth> fIdToCallstack = HashBiMap.create();
    private final BiMap<Long, ICallStackElement> fIdToElement = HashBiMap.create();
    private final long fTraceId = ENTRY_ID.getAndIncrement();

    private static class TidInformation {
        private final HostThread fTid;
        private final long fStart;
        private final long fEnd;
        private final Long fLinked;

        public TidInformation(HostThread hostThread, long start, long end, Long linked) {
            fTid = hostThread;
            fStart = start;
            fEnd = end;
            fLinked = linked;
        }

        public boolean intersects(ITimeGraphState state) {
            return !(state.getStartTime() > fEnd || (state.getStartTime() + state.getDuration()) < fStart);
        }

        public boolean precedes(ITimeGraphState state) {
            return (state.getStartTime() + state.getDuration() < fEnd);
        }

        public ITimeGraphState sanitize(ITimeGraphState state) {
            if (state.getStartTime() < fStart || state.getStartTime() + state.getDuration() > fEnd) {
                long start = Math.max(state.getStartTime(), fStart);
                long end = Math.min(state.getStartTime() + state.getDuration(), fEnd);
                String label = state.getLabel();
                if (label != null) {
                    return new TimeGraphState(start, end - start, state.getValue(), label);
                }
                return new TimeGraphState(start, end - start, state.getValue());
            }
            return state;
        }
    }

    private static class ThreadData {

        private final ThreadStatusDataProvider fThreadDataProvider;
        private final List<ThreadEntryModel> fThreadTree;
        private final Status fStatus;

        public ThreadData(ThreadStatusDataProvider dataProvider, List<ThreadEntryModel> threadTree, Status status) {
            fThreadDataProvider = dataProvider;
            fThreadTree = threadTree;
            fStatus = status;
        }

        public @Nullable Map<String, String> fetchTooltip(int threadId, long time, @Nullable IProgressMonitor monitor) {
            for (ThreadEntryModel entry : fThreadTree) {
                if (entry.getThreadId() == threadId && entry.getStartTime() <= time && entry.getEndTime() >= time) {
                    TmfModelResponse<Map<String, String>> tooltip = fThreadDataProvider.fetchTooltip(new SelectionTimeQueryFilter(Collections.singletonList(time), Collections.singleton(entry.getId())), monitor);
                    return tooltip.getModel();
                }
            }
            return null;
        }

    }

    private final LoadingCache<Pair<Integer, ICalledFunction>, @Nullable String> fTimeEventNames = Objects.requireNonNull(CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<Pair<Integer, ICalledFunction>, @Nullable String>() {
                @Override
                public @Nullable String load(Pair<Integer, ICalledFunction> pidInterval) {
                    Integer pid = pidInterval.getFirst();
                    ICalledFunction interval = pidInterval.getSecond();

                    Object nameValue = interval.getSymbol();
                    Long address = null;
                    String name = null;
                    if (nameValue instanceof String) {
                        name = (String) nameValue;
                        try {
                            address = Long.parseLong(name, 16);
                        } catch (NumberFormatException e) {
                            // leave name as null
                        }
                    } else if (nameValue instanceof Integer) {
                        Integer intValue = (Integer) nameValue;
                        name = "0x" + Integer.toUnsignedString(intValue, 16); //$NON-NLS-1$
                        address = intValue.longValue();
                    } else if (nameValue instanceof Long) {
                        address = (long) nameValue;
                        name = "0x" + Long.toUnsignedString(address, 16); //$NON-NLS-1$
                    }
                    if (address != null) {
                        name = SymbolProviderUtils.getSymbolText(fProviders, pid, interval.getStart(), address);
                    }
                    return name;
                }
            }));

    private final IFlameChartProvider fFcProvider;

    private final String fAnalysisId;
    private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock(false);
    private final FlameChartArrowProvider fArrowProvider;
    private @Nullable TmfModelResponse<List<FlameChartEntryModel>> fCached;
    private @Nullable ThreadData fThreadData = null;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     * @param module
     *            The flame chart provider encapsulated by this provider
     * @param secondaryId
     *            The ID of the flame chart provider
     */
    public FlameChartDataProvider(ITmfTrace trace, IFlameChartProvider module, String secondaryId) {
        super(trace);
        fFcProvider = module;
        fAnalysisId = secondaryId;
        fArrowProvider = new FlameChartArrowProvider(trace);
        resetFunctionNames(new NullProgressMonitor());
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        List<ITmfStateInterval> arrows = fArrowProvider.fetchArrows(filter, monitor);
        List<ITimeGraphArrow> tgArrows = new ArrayList<>();
        // First, get the distinct callstacks
        Set<CallStack> callstacks = fIdToCallstack.values().stream()
                .map(CallStackDepth::getCallStack)
                .distinct()
                .collect(Collectors.toSet());
        // Find the source and destination entry for each arrow
        for (ITmfStateInterval interval : arrows) {
            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            EdgeStateValue edge = (EdgeStateValue) interval.getValue();
            if (edge == null) {
                /*
                 * by contract all the intervals should have EdgeStateValues but need to check
                 * to avoid NPE
                 */
                continue;
            }
            Long src = findEntry(callstacks, edge.getSource(), interval.getStartTime());
            Long dst = findEntry(callstacks, edge.getDestination(), interval.getEndTime() + 1);
            if (src != null && dst != null) {
                long duration = interval.getEndTime() - interval.getStartTime() + 1;
                tgArrows.add(new TimeGraphArrow(src, dst, interval.getStartTime(), duration, edge.getId()));
            }
        }
        return new TmfModelResponse<>(tgArrows, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private @Nullable Long findEntry(Set<CallStack> callstacks, @NonNull HostThread hostThread, long ts) {
        // Get the
        for (CallStack callstack : callstacks) {
            HostThread csHt = callstack.getHostThread(ts);
            if (csHt == null || !csHt.equals(hostThread)) {
                continue;
            }
            // We found the callstack, find the right depth and its entry id
            int currentDepth = callstack.getCurrentDepth(ts);
            for (Entry<Long, CallStackDepth> csdEntry : fIdToCallstack.entrySet()) {
                CallStackDepth csd = csdEntry.getValue();
                if (csd.getDepth() == currentDepth && csd.getCallStack().equals(callstack)) {
                    return csdEntry.getKey();
                }
            }
        }
        return null;
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchTooltip") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            Map<Long, FlameChartEntryModel> entries = getSelectedEntries(filter);
            if (entries.size() != 1) {
                // Not the expected size of tooltip, just return empty
                return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }
            Entry<@NonNull Long, @NonNull FlameChartEntryModel> entry = entries.entrySet().iterator().next();
            Map<String, String> tooltip = getTooltip(entry, filter, monitor);

            return new TmfModelResponse<>(tooltip, Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
    }

    private @Nullable Map<String, String> getTooltip(Entry<Long, FlameChartEntryModel> entry, SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        FlameChartEntryModel value = Objects.requireNonNull(entry.getValue());
        switch (value.getEntryType()) {
        case FUNCTION:
        {
            CallStackDepth selectedDepth = fIdToCallstack.get(entry.getKey());
            if (selectedDepth == null) {
                return null;
            }
            Multimap<CallStackDepth, ISegment> csFunctions = fFcProvider.queryCallStacks(Collections.singleton(selectedDepth), Collections.singleton(filter.getStart()));
            Collection<ISegment> functions = csFunctions.get(selectedDepth);
            if (functions.isEmpty()) {
                return null;
            }
            ISegment next = functions.iterator().next();
            if (!(next instanceof ICalledFunction)) {
                return null;
            }
            ICalledFunction currentFct = (ICalledFunction) next;
            Map<String, String> tooltips = new HashMap<>();
            int threadId = currentFct.getThreadId();
            if (threadId > 0) {
                tooltips.put(String.valueOf(Messages.FlameChartDataProvider_ThreadId), String.valueOf(threadId));
            }
            // TODO: Add symbol origin (library, language, etc) when better supported
            return tooltips;
        }
        case KERNEL:
            // Get the tooltip from the the ThreadStatusDataProvider
            // First get the linked function to know which TID to retrieve
            Long csId = fLinkedEntries.get(entry.getKey());
            if (csId == null) {
                return null;
            }
            CallStackDepth selectedDepth = fIdToCallstack.get(csId);
            if (selectedDepth == null) {
                return null;
            }
            int threadId = selectedDepth.getCallStack().getThreadId(filter.getStart());
            ThreadData threadData = fThreadData;
            if (threadData == null) {
                return null;
            }
            return threadData.fetchTooltip(threadId, filter.getStart(), monitor);
        case LEVEL:
            // Fall-through
        case TRACE:
            // Fall-through
        default:
            return null;
        }

    }

    @Override
    public String getId() {
        return ID + ':' + fAnalysisId;
    }

    // Get an entry for a quark
    private long getEntryId(CallStackDepth stack) {
        return fIdToCallstack.inverse().computeIfAbsent(stack, q -> ENTRY_ID.getAndIncrement());
    }

    private long getEntryId(ICallStackElement instrumentedCallStackElement) {
        return fIdToElement.inverse().computeIfAbsent(instrumentedCallStackElement, q -> ENTRY_ID.getAndIncrement());
    }

    // Get a new entry for a kernel entry ID
    private long getKernelEntryId(long baseId) {
        return fLinkedEntries.inverse().computeIfAbsent(baseId, id -> ENTRY_ID.getAndIncrement());
    }

    @Override
    public TmfModelResponse<List<FlameChartEntryModel>> fetchTree(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        if (fCached != null) {
            return fCached;
        }

        fLock.writeLock().lock();
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            IFlameChartProvider fcProvider = fFcProvider;
            boolean complete = fcProvider.isComplete();
            CallStackSeries callstack = fcProvider.getCallStackSeries();
            if (callstack == null) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
            }
            long start = getTrace().getStartTime().getValue();
            long end = getTrace().readEnd().getValue();

            // Initialize the first element of the tree
            ImmutableList.Builder<FlameChartEntryModel> builder = ImmutableList.builder();
            FlameChartEntryModel traceEntry = new FlameChartEntryModel(fTraceId, -1, getTrace().getName(), start, end, FlameChartEntryModel.EntryType.TRACE);
            builder.add(traceEntry);

            FlameChartEntryModel callStackRoot = traceEntry;
            // If there is more than one callstack objects in the analysis, create a root
            // per series
            boolean needsKernel = false;
            for (ICallStackElement element : callstack.getRootElements()) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                needsKernel |= processCallStackElement(element, builder, callStackRoot);
            }
            // Initialize the thread status data provider
            if (needsKernel) {
                prepareKernelData(monitor, start);
            }
            List<FlameChartEntryModel> tree = builder.build();
            tree.forEach(entry -> fEntries.put(entry.getId(), entry));
            if (complete) {
                TmfModelResponse<List<FlameChartEntryModel>> response = new TmfModelResponse<>(tree,
                        ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                fCached = response;
                return response;
            }
            return new TmfModelResponse<>(tree, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        } finally {
            fLock.writeLock().unlock();
        }
    }

    private void prepareKernelData(@Nullable IProgressMonitor monitor, long start) {
        ThreadData data = fThreadData;
        if (data != null && data.fStatus.equals(Status.COMPLETED)) {
            return;
        }
        // FIXME: Wouldn't work correctly if trace is an experiment as it would cover many hosts
        Set<ITmfTrace> tracesForHost = TmfTraceManager.getInstance().getTracesForHost(getTrace().getHostId());
        for (ITmfTrace trace : tracesForHost) {
            ThreadStatusDataProvider dataProvider = DataProviderManager.getInstance().getDataProvider(trace, ThreadStatusDataProvider.ID, ThreadStatusDataProvider.class);
            if (dataProvider != null) {
                // Get the tree for the trace's current range
                TmfModelResponse<List<ThreadEntryModel>> threadTreeResp = dataProvider.fetchTree(new TimeQueryFilter(start, Long.MAX_VALUE, 2), monitor);
                List<ThreadEntryModel> threadTree = threadTreeResp.getModel();
                if (threadTree != null) {
                    fThreadData = new ThreadData(dataProvider, threadTree, threadTreeResp.getStatus());
                    break;
                }
            }
        }
    }

    private boolean processCallStackElement(ICallStackElement element, Builder<FlameChartEntryModel> builder, FlameChartEntryModel parentEntry) {

        long elementId = getEntryId(element);
        FlameChartEntryModel entry = new FlameChartEntryModel(elementId, parentEntry.getId(), element.getName(), parentEntry.getStartTime(), parentEntry.getEndTime(), FlameChartEntryModel.EntryType.LEVEL);
        builder.add(entry);

        boolean needsKernel = false;

        // Is this an intermediate or leaf element
        if ((element instanceof InstrumentedCallStackElement) && element.isLeaf()) {
            // For the leaf element, add the callstack entries
            InstrumentedCallStackElement finalElement = (InstrumentedCallStackElement) element;
            CallStack callStack = finalElement.getCallStack();
            for (int depth = 0; depth < callStack.getMaxDepth(); depth++) {
                FlameChartEntryModel flameChartEntry = new FlameChartEntryModel(getEntryId(new CallStackDepth(callStack, depth + 1)), entry.getId(), element.getName(), parentEntry.getStartTime(), parentEntry.getEndTime(),
                        FlameChartEntryModel.EntryType.FUNCTION, depth + 1);
                builder.add(flameChartEntry);
                if (depth == 0 && callStack.hasKernelStatuses()) {
                    needsKernel = true;
                    builder.add(new FlameChartEntryModel(getKernelEntryId(flameChartEntry.getId()), entry.getId(), String.valueOf(Messages.FlameChartDataProvider_KernelStatusTitle), parentEntry.getStartTime(), parentEntry.getEndTime(), FlameChartEntryModel.EntryType.KERNEL));
                }
            }
            return needsKernel;
        }
        // Intermediate element, process children
        for (ICallStackElement child : element.getChildren()) {
            needsKernel |= processCallStackElement(child, builder, entry);
        }
        return needsKernel;
    }

    // Get the selected entries with the quark
    private BiMap<Long, FlameChartEntryModel> getSelectedEntries(SelectionTimeQueryFilter filter) {
        fLock.readLock().lock();
        try {
            BiMap<Long, FlameChartEntryModel> selectedEntries = HashBiMap.create();

            for (Long selectedItem : filter.getSelectedItems()) {
                FlameChartEntryModel entryModel = fEntries.get(selectedItem);
                if (entryModel != null) {
                    selectedEntries.put(selectedItem, entryModel);
                }
            }
            return selectedEntries;
        } finally {
            fLock.readLock().unlock();
        }
    }

    @Override
    public TmfModelResponse<List<ITimeGraphRowModel>> fetchRowModel(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameChartDataProvider#fetchRowModel") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {

            Map<Long, FlameChartEntryModel> entries = getSelectedEntries(filter);
            if (entries.size() == 1 && filter.getTimesRequested().length == 2) {
                // this is a request for a follow event.
                Entry<@NonNull Long, @NonNull FlameChartEntryModel> entry = entries.entrySet().iterator().next();
                if (filter.getStart() == Long.MIN_VALUE) {
                    return new TmfModelResponse<>(getFollowEvent(entry, filter.getEnd(), false), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                } else if (filter.getEnd() == Long.MAX_VALUE) {
                    return new TmfModelResponse<>(getFollowEvent(entry, filter.getStart(), true), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                }
            }
            // For each kernel status entry, add the first row of the callstack
            addRequiredCallstacks(entries);

            SubMonitor subMonitor = SubMonitor.convert(monitor, "FlameChartDataProvider#fetchRowModel", 2); //$NON-NLS-1$
            IFlameChartProvider fcProvider = fFcProvider;
            boolean complete = fcProvider.isComplete();

            Map<Long, List<ITimeGraphState>> csRows = getCallStackRows(filter, entries, subMonitor);
            if (csRows == null) {
                // getRowModel returns null if the query was cancelled.
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            List<ITimeGraphRowModel> collect = csRows.entrySet().stream().map(entry -> new TimeGraphRowModel(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            return new TmfModelResponse<>(collect, complete ? Status.COMPLETED : Status.RUNNING,
                    complete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING);
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, Status.FAILED, String.valueOf(e.getMessage()));
        }
    }

    private void addRequiredCallstacks(Map<Long, FlameChartEntryModel> entries) {
        fLock.readLock().lock();
        try {
            Map<Long, FlameChartEntryModel> toAdd = new HashMap<>();
            for (Long id : entries.keySet()) {
                Long csId = fLinkedEntries.get(id);
                if (csId != null) {
                    FlameChartEntryModel entry = fEntries.get(csId);
                    if (entry != null) {
                        toAdd.put(csId, entry);
                    }
                }
            }
            entries.putAll(toAdd);
        } finally {
            fLock.readLock().unlock();
        }
    }

    private @Nullable Map<Long, List<ITimeGraphState>> getCallStackRows(SelectionTimeQueryFilter filter, Map<Long, FlameChartEntryModel> entries, SubMonitor subMonitor) throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {

        // Get the data for the model entries that are of type function
        Map<Long, List<ITimeGraphState>> rows = new HashMap<>();
        List<TidInformation> tids = new ArrayList<>();
        Map<Long, CallStackDepth> csEntries = new HashMap<>();
        for (Entry<Long, @NonNull FlameChartEntryModel> entry : entries.entrySet()) {
            CallStackDepth selectedDepth = fIdToCallstack.get(entry.getKey());
            if (selectedDepth != null && entry.getValue().getEntryType().equals(EntryType.FUNCTION)) {
                csEntries.put(entry.getKey(), selectedDepth);
            }
        }

        long[] timesRequested = filter.getTimesRequested();
        // Prepare the list of times
        List<Long> times = new ArrayList<>();
        for (long time : timesRequested) {
            times.add(time);
        }
        Collections.sort(times);
        Multimap<CallStackDepth, ISegment> csFunctions = fFcProvider.queryCallStacks(csEntries.values(), times);

        for (Map.Entry<Long, CallStackDepth> entry : csEntries.entrySet()) {
            if (subMonitor.isCanceled()) {
                return null;
            }
            Collection<ISegment> states = csFunctions.get(entry.getValue());

            // Create the time graph states for this row
            List<ITimeGraphState> eventList = new ArrayList<>(states.size());
            states.forEach(state -> eventList.add(createTimeGraphState(state)));
            eventList.sort(Comparator.comparingLong(ITimeGraphState::getStartTime));
            rows.put(entry.getKey(), eventList);

            // See if any more row needs to be filled with these function's data
            // TODO: Kernel might not be the only type of linked entries (for instance,
            // locations of sampling data)
            Long linked = fLinkedEntries.inverse().get(entry.getKey());
            if (linked == null || !entries.containsKey(linked)) {
                continue;
            }
            tids.addAll(getKernelTids(entry.getValue(), states, linked));

        }
        // Add an empty state to rows that do not have data
        for (Long key : entries.keySet()) {
            if (!rows.containsKey(key)) {
                rows.put(key, Collections.emptyList());
            }
        }
        if (!tids.isEmpty()) {
            rows.putAll(getKernelStates(tids, times, subMonitor));
        }
        subMonitor.worked(1);
        return rows;
    }

    private Map<Long, List<ITimeGraphState>> getKernelStates(List<TidInformation> tids, List<Long> times, SubMonitor monitor) {
        // Get the thread statuses from the thread status provider
        ThreadData threadData = fThreadData;
        if (threadData == null) {
            return Collections.emptyMap();

        }
        List<ThreadEntryModel> tree = threadData.fThreadTree;

        // FIXME: A callstack analysis may be for an experiment that span many hosts,
        // the thread data provider will be a composite and the models may be for
        // different host IDs. But for now, suppose the callstack is a composite also
        // and the trace filtered the right host.
        BiMap<Long, Integer> threadModelIds = filterThreads(tree, tids);
        SelectionTimeQueryFilter tidFilter = new SelectionTimeQueryFilter(times, threadModelIds.keySet());
        TmfModelResponse<List<ITimeGraphRowModel>> rowModel = threadData.fThreadDataProvider.fetchRowModel(tidFilter, monitor);
        List<ITimeGraphRowModel> rowModels = rowModel.getModel();
        if (rowModel.getStatus().equals(Status.CANCELLED) || rowModel.getStatus().equals(Status.FAILED) || rowModels == null) {
            return Collections.emptyMap();
        }
        return mapThreadStates(rowModels, threadModelIds, tids);
    }

    private static Map<Long, List<ITimeGraphState>> mapThreadStates(List<ITimeGraphRowModel> rowModels, BiMap<Long, Integer> threadModelIds, List<TidInformation> tids) {
        ImmutableMap<Long, ITimeGraphRowModel> statusRows = Maps.uniqueIndex(rowModels, m -> m.getEntryID());
        // Match the states of thread status to the requested tid lines
        Long prevId = -1L;
        List<ITimeGraphState> states = null;
        Map<Long, List<ITimeGraphState>> kernelStatuses = new HashMap<>();
        // The tid information data are ordered by id and times
        for (TidInformation tidInfo : tids) {
            Long tidEntryId = threadModelIds.inverse().get(tidInfo.fTid.getTid());
            if (tidEntryId == null) {
                continue;
            }
            ITimeGraphRowModel rowModel = statusRows.get(tidEntryId);
            if (tidInfo.fLinked != prevId || states == null) {
                if (states != null) {
                    kernelStatuses.put(prevId, states);
                }
                states = new ArrayList<>();
            }
            rowModel.getStates();
            for (ITimeGraphState state : rowModel.getStates()) {
                if (tidInfo.intersects(state)) {
                    states.add(tidInfo.sanitize(state));
                }
                if (!tidInfo.precedes(state)) {
                    break;
                }
            }
            prevId = tidInfo.fLinked;
        }
        if (states != null) {
            kernelStatuses.put(prevId, states);
        }
        return kernelStatuses;
    }

    private static BiMap<Long, Integer> filterThreads(List<ThreadEntryModel> model, List<TidInformation> tids) {
        // Get the entry model IDs that match requested tids
        BiMap<Long, Integer> tidEntries = HashBiMap.create();
        Set<Integer> selectedTids = new HashSet<>();
        for (TidInformation tidInfo : tids) {
            selectedTids.add(tidInfo.fTid.getTid());
        }
        for (ThreadEntryModel entryModel : model) {
            if (selectedTids.contains(entryModel.getThreadId())) {
                try {
                    tidEntries.put(entryModel.getId(), entryModel.getThreadId());
                } catch (IllegalArgumentException e) {
                    // FIXME: There may be many entries for one tid, don't rely on exception for
                    // real workflow. Works for now.
                }
            }
        }
        return tidEntries;
    }

    private static Collection<TidInformation> getKernelTids(CallStackDepth callStackDepth, Collection<ISegment> states, Long linked) {

        List<TidInformation> tids = new ArrayList<>();
        CallStack callStack = callStackDepth.getCallStack();
        if (!callStack.isTidVariable()) {
            // Find the time of the first function to know which timestamp to query
            HostThread hostThread = callStack.getHostThread();
            if (hostThread != null) {
                tids.add(new TidInformation(hostThread, Long.MIN_VALUE, Long.MAX_VALUE, linked));
            }
            return tids;
        }
        // Get the thread IDs for all functions
        for (ISegment state : states) {
            if (!(state instanceof ICalledFunction)) {
                continue;
            }
            ICalledFunction function = (ICalledFunction) state;
            HostThread hostThread = callStack.getHostThread(function.getStart());
            if (hostThread != null) {
                tids.add(new TidInformation(hostThread, function.getStart(), function.getEnd(), linked));
            }
        }

        return tids;
    }

    private ITimeGraphState createTimeGraphState(ISegment state) {
        if (!(state instanceof ICalledFunction)) {
            return new TimeGraphState(state.getStart(), state.getLength(), Integer.MIN_VALUE);
        }
        ICalledFunction function = (ICalledFunction) state;
        Object value = function.getSymbol();
        Integer pid = function.getProcessId();
        String name = String.valueOf(fTimeEventNames.getUnchecked(new Pair<>(pid, function)));
        return new TimeGraphState(function.getStart(), function.getLength(), value.hashCode(), name);
    }

    /**
     * Invalidate the function names cache and load the symbol providers. This
     * function should be used at the beginning of the provider, or whenever new
     * symbol providers are added
     *
     * @param monitor
     *            A progress monitor to follow this operation
     */
    public void resetFunctionNames(IProgressMonitor monitor) {
        fTimeEventNames.invalidateAll();
        synchronized (fProviders) {
            Collection<@NonNull ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(getTrace());
            SubMonitor sub = SubMonitor.convert(monitor, "CallStackDataProvider#resetFunctionNames", symbolProviders.size()); //$NON-NLS-1$
            fProviders.clear();
            for (ISymbolProvider symbolProvider : symbolProviders) {
                fProviders.add(symbolProvider);
                symbolProvider.loadConfiguration(sub);
                sub.worked(1);
            }
        }
    }

    /**
     * Get the next or previous interval for a call stack entry ID, time and
     * direction
     *
     * @param entry
     *            whose key is the ID and value is the quark for the entry whose
     *            next / previous state we are searching for
     * @param time
     *            selection start time
     * @param forward
     *            if going to next or previous
     * @return the next / previous state encapsulated in a row if it exists, else
     *         null
     */
    private @Nullable List<ITimeGraphRowModel> getFollowEvent(Entry<Long, FlameChartEntryModel> entry, long time, boolean forward) {
        FlameChartEntryModel value = Objects.requireNonNull(entry.getValue());
        switch (value.getEntryType()) {
        case FUNCTION:
            CallStackDepth selectedDepth = fIdToCallstack.get(entry.getKey());
            if (selectedDepth == null) {
                return null;
            }
            // Ask the callstack the depth at the current time
            ITmfStateInterval nextDepth = selectedDepth.getCallStack().getNextDepth(time, forward);
            if (nextDepth == null) {
                return null;
            }
            Object depthVal = nextDepth.getValue();
            int depth = (depthVal instanceof Number) ? ((Number) depthVal).intValue() : 0;
            TimeGraphState state = new TimeGraphState(nextDepth.getStartTime(), nextDepth.getEndTime() - nextDepth.getStartTime(), depth);
            TimeGraphRowModel row = new TimeGraphRowModel(entry.getKey(), Collections.singletonList(state));
            return Collections.singletonList(row);

        case KERNEL:
            break;
        case LEVEL:
            // Fall-through
        case TRACE:
            // Fall-through
        default:
            return null;
        }
        return null;
    }

}
