/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.flamegraph;

import java.text.Format;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.common.core.format.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraphGroupBy;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AggregatedThreadStatus;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * A data provider for flame graphs, using a {@link IWeightedTreeProvider} as
 * input for the data
 *
 * TODO: Publish the presentation provider
 *
 * TODO: Find a way to advertise extra parameters (group_by, selection range)
 *
 * TODO: Use weighted tree instead of callgraph provider
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class FlameGraphDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<FlameChartEntryModel> {

    /**
     * Provider ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.callstack.core.flamegraph.provider"; //$NON-NLS-1$
    /**
     * The key used to specify how to group the entries of the flame graph
     */
    public static final String GROUP_BY_KEY = "group_by"; //$NON-NLS-1$
    /**
     * The key used to specify a time selection to get the callgraph for. It
     * should be a list of 2 longs
     */
    public static final String SELECTION_RANGE_KEY = "selection_range"; //$NON-NLS-1$
    private static final AtomicLong ENTRY_ID = new AtomicLong();
    private static final Comparator<AggregatedCallSite> CCT_COMPARATOR = Comparator.comparingLong(AggregatedCallSite::getWeight).thenComparing(s -> String.valueOf(s.getObject()));
    /**
     * Logger for Abstract Tree Data Providers.
     */
    private static final Logger LOGGER = TraceCompassLog.getLogger(FlameGraphDataProvider.class);
    private static final Format FORMATTER = SubSecondTimeWithUnitFormat.getInstance();

    private final ICallGraphProvider fCgProvider;
    private final String fAnalysisId;
    private final long fTraceId = ENTRY_ID.getAndIncrement();

    private final ReentrantReadWriteLock fLock = new ReentrantReadWriteLock(false);
    private @Nullable Pair<Map<String, Object>, TmfModelResponse<TmfTreeModel<FlameChartEntryModel>>> fCached;
    private final Map<Long, FlameChartEntryModel> fEntries = new HashMap<>();
    private final Map<Long, CallGraphEntry> fCgEntries = new HashMap<>();
    private final Collection<ISymbolProvider> fSymbolProviders;
    private final Map<Long, Long> fEndTimes = new HashMap<>();

    /** An internal class to describe the data for an entry */
    private static class CallGraphEntry {
        private final ICallStackElement fElement;
        private final CallGraph fCallgraph;
        private final int fDepth;

        private CallGraphEntry(ICallStackElement element, CallGraph callgraph, int depth) {
            fElement = element;
            fCallgraph = callgraph;
            fDepth = depth;
        }
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     * @param module
     *            The weighted tree provider encapsulated by this provider
     * @param secondaryId
     *            The ID of the weighted tree provider
     */
    public FlameGraphDataProvider(ITmfTrace trace, ICallGraphProvider module, String secondaryId) {
        super(trace);
        fCgProvider = module;
        fAnalysisId = secondaryId;
        Collection<ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(trace);
        symbolProviders.forEach(provider -> provider.loadConfiguration(new NullProgressMonitor()));
        fSymbolProviders = symbolProviders;
    }

    @Override
    public String getId() {
        return ID + ':' + fAnalysisId;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> fetchTree(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // Did we cache this tree with those parameters
        Pair<Map<String, Object>, TmfModelResponse<TmfTreeModel<FlameChartEntryModel>>> cached = fCached;
        if (cached != null && cached.getFirst().equals(fetchParameters)) {
            return cached.getSecond();
        }
        fLock.writeLock().lock();
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {

            fEntries.clear();
            fCgEntries.clear();
            SubMonitor subMonitor = Objects.requireNonNull(SubMonitor.convert(monitor, "FlameGraphDataProvider#fetchRowModel", 2)); //$NON-NLS-1$

            CallGraph callGraph = getCallGraph(fetchParameters, subMonitor);
            if (subMonitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            if (callGraph == null) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.TASK_CANCELLED);
            }

            long start = 0;

            // Initialize the first element of the tree
            List<FlameChartEntryModel.Builder> builder = new ArrayList<>();
            FlameChartEntryModel.Builder traceEntry = new FlameChartEntryModel.Builder(fTraceId, -1, getTrace().getName(), start, FlameChartEntryModel.EntryType.TRACE, -1);

            buildCallGraphEntries(callGraph, builder, traceEntry);

            ImmutableList.Builder<FlameChartEntryModel> treeBuilder = ImmutableList.builder();
            long end = traceEntry.getEndTime();
            for (FlameChartEntryModel.Builder builderEntry : builder) {
                treeBuilder.add(builderEntry.build());
                end = Math.max(end, builderEntry.getEndTime());
            }
            traceEntry.setEndTime(end);
            treeBuilder.add(traceEntry.build());
            List<FlameChartEntryModel> tree = treeBuilder.build();

            tree.forEach(entry -> {
                fEntries.put(entry.getId(), entry);
                fEndTimes.put(entry.getId(), entry.getEndTime());
            });

            TmfModelResponse<TmfTreeModel<FlameChartEntryModel>> response = new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), tree),
                    ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            fCached = new Pair<>(fetchParameters, response);
            return response;

        } finally {
            fLock.writeLock().unlock();
        }
    }

    private @Nullable CallGraph getCallGraph(Map<String, Object> fetchParameters, SubMonitor subMonitor) {
        // Get the provider and wait for the analysis completion
        ICallGraphProvider fcProvider = fCgProvider;
        if (fcProvider instanceof IAnalysisModule) {
            ((IAnalysisModule) fcProvider).waitForCompletion(subMonitor);
        }
        if (subMonitor.isCanceled()) {
            return null;
        }

        // Get the full or selection callgraph
        List<Long> selectionRange = DataProviderParameterUtils.extractLongList(fetchParameters, SELECTION_RANGE_KEY);
        CallGraph callGraph;
        if (selectionRange == null || selectionRange.size() != 2) {
            callGraph = fcProvider.getCallGraph();
        } else {
            long time0 = selectionRange.get(0);
            long time1 = selectionRange.get(1);
            callGraph = fcProvider.getCallGraph(TmfTimestamp.fromNanos(Math.min(time0, time1)), TmfTimestamp.fromNanos(Math.max(time0, time1)));
        }

        // Look if we need to group the callgraph
        ICallStackGroupDescriptor groupDescriptor = extractGroupDescriptor(fetchParameters, fcProvider);
        if (groupDescriptor != null) {
            callGraph = CallGraphGroupBy.groupCallGraphBy(groupDescriptor, callGraph);
        }

        return callGraph;
    }

    private static @Nullable ICallStackGroupDescriptor extractGroupDescriptor(Map<String, Object> fetchParameters, ICallGraphProvider fcProvider) {
        Object groupBy = fetchParameters.get(GROUP_BY_KEY);
        if (groupBy == null) {
            return null;
        }
        String groupName = String.valueOf(groupBy);
        // Is it the all group descriptor
        if (groupName.equals(AllGroupDescriptor.getInstance().getName())) {
            return AllGroupDescriptor.getInstance();
        }
        // Try to find the right group descriptor
        for (ICallStackGroupDescriptor groupDescriptor : fcProvider.getGroupDescriptors()) {
            if (groupDescriptor.getName().equals(groupName)) {
                return groupDescriptor;
            }
        }
        return null;
    }

    private void buildCallGraphEntries(CallGraph callgraph, List<FlameChartEntryModel.Builder> builder, FlameChartEntryModel.Builder traceEntry) {
        Collection<ICallStackElement> elements = callgraph.getElements();
        for (ICallStackElement element : elements) {
            buildChildrenEntries(element, callgraph, builder, traceEntry);
        }

    }

    /**
     * Build the entry list for one thread
     *
     * @param element
     *            The node of the aggregation tree
     */
    private void buildChildrenEntries(ICallStackElement element, CallGraph callgraph, List<FlameChartEntryModel.Builder> builder, FlameChartEntryModel.Builder parent) {
        // Add the entry
        FlameChartEntryModel.Builder entry = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), parent.getId(), element.getName(), 0, FlameChartEntryModel.EntryType.LEVEL, -1);
        builder.add(entry);

        // Create the children entries
        for (ICallStackElement child : element.getChildrenElements()) {
            buildChildrenEntries(child, callgraph, builder, entry);
        }

        // Update endtime with the children and add them to builder
        long endTime = entry.getEndTime();
        for (FlameChartEntryModel.Builder childEntry : builder) {
            if (childEntry.getParentId() == entry.getId()) {
                endTime = Math.max(childEntry.getEndTime(), endTime);
            }
        }
        entry.setEndTime(endTime);

        // Create the function callsite entries
        if (!(element.isLeaf())) {
            return;
        }

        List<FlameChartEntryModel.Builder> childrenEntries = new ArrayList<>();
        List<FlameChartEntryModel.Builder> extraEntries = new ArrayList<>();
        Deque<Long> timestampStack = new ArrayDeque<>();
        timestampStack.push(0L);

        // Sort children by duration
        List<AggregatedCallSite> rootFunctions = new ArrayList<>(callgraph.getCallingContextTree(element));
        rootFunctions.sort(CCT_COMPARATOR);
        for (AggregatedCallSite rootFunction : rootFunctions) {
            createLevelChildren(element, rootFunction, childrenEntries, timestampStack, entry.getId());
            createExtraChildren(rootFunction, extraEntries, timestampStack, entry.getId());
            long currentThreadDuration = timestampStack.pop() + rootFunction.getWeight();
            timestampStack.push(currentThreadDuration);
        }
        for (FlameChartEntryModel.Builder child : childrenEntries) {
            builder.add(child);
            fCgEntries.put(child.getId(), new CallGraphEntry(element, callgraph, child.getDepth()));
        }
        for (FlameChartEntryModel.Builder child : extraEntries) {
            builder.add(child);
            fCgEntries.put(child.getId(), new CallGraphEntry(element, callgraph, child.getDepth()));
        }
        entry.setEndTime(timestampStack.pop());
        return;
    }

    /**
     * Parse the aggregated tree created by the callGraphAnalysis and creates
     * the event list (functions) for each entry (depth)
     *
     * @param element
     *
     * @param firstNode
     *            The first node of the aggregation tree
     * @param childrenEntries
     *            The list of entries for one thread
     * @param timestampStack
     *            A stack used to save the functions timeStamps
     */
    private static void createLevelChildren(ICallStackElement element, AggregatedCallSite firstNode, List<FlameChartEntryModel.Builder> childrenEntries, Deque<Long> timestampStack, long parentId) {
        Long lastEnd = timestampStack.peek();
        if (lastEnd == null) {
            return;
        }
        // Prepare all the level entries for this callsite
        for (int i = 0; i <= firstNode.getMaxDepth() - 1; i++) {
            if (i >= childrenEntries.size()) {
                FlameChartEntryModel.Builder entry = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), parentId, String.valueOf(i), 0, EntryType.FUNCTION, i);
                childrenEntries.add(entry);
            }
            childrenEntries.get(i).setEndTime(lastEnd + firstNode.getWeight());
        }
    }

    private static void createExtraChildren(AggregatedCallSite firstNode, List<FlameChartEntryModel.Builder> extraEntries, Deque<Long> timestampStack, long parentId) {
        Long lastEnd = timestampStack.peek();
        if (lastEnd == null) {
            return;
        }
        Iterator<AggregatedCallSite> extraChildrenSites = firstNode.getExtraChildrenSites().iterator();

        if (!extraChildrenSites.hasNext()) {
            return;
        }
        // Get or add the entry
        if (extraEntries.isEmpty()) {
            FlameChartEntryModel.Builder entry = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), parentId, Objects.requireNonNull(Messages.FlameGraph_KernelStatusTitle), 0, EntryType.KERNEL, -1);
            extraEntries.add(entry);
        }
        FlameChartEntryModel.Builder entry = extraEntries.get(0);

        while (extraChildrenSites.hasNext()) {
            AggregatedCallSite next = extraChildrenSites.next();
            lastEnd += next.getWeight();
            entry.setEndTime(lastEnd);
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        SubMonitor subMonitor = Objects.requireNonNull(SubMonitor.convert(monitor, "FlameGraphDataProvider#fetchRowModel", 2)); //$NON-NLS-1$

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        List<ITimeGraphRowModel> rowModels = new ArrayList<>();

        // Get the selected entries
        Collection<Long> selected = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selected == null) {
            // No entry selected, assume all
            selected = fEntries.keySet();
        }
        List<CallGraphEntry> selectedEntries = new ArrayList<>();
        Multimap<Pair<CallGraph, ICallStackElement>, Pair<Integer, Long>> requested = HashMultimap.create();
        for (Long id : selected) {
            CallGraphEntry entry = fCgEntries.get(id);
            if (entry != null) {
                selectedEntries.add(entry);
                requested.put(new Pair<>(entry.fCallgraph, entry.fElement), new Pair<>(entry.fDepth, id));
            }
        }

        // Prepare the regexes
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        if (subMonitor.isCanceled()) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }

        // For each element and callgraph, get the states
        for (Pair<CallGraph, ICallStackElement> element : requested.keySet()) {
            if (subMonitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            Collection<Pair<Integer, Long>> depths = requested.get(element);
            rowModels.addAll(getStatesForElement(times, predicates, subMonitor, element.getFirst(), element.getSecond(), depths));
        }

        return new TmfModelResponse<>(new TimeGraphModel(rowModels), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private List<ITimeGraphRowModel> getStatesForElement(List<Long> times, Map<Integer, Predicate<Multimap<String, Object>>> predicates, IProgressMonitor monitor,
            CallGraph callgraph, ICallStackElement csElement,
            Collection<Pair<Integer, Long>> depths) {
        // Get the cct for this element (first level callsites) and sort them
        Collection<AggregatedCallSite> cct = callgraph.getCallingContextTree(csElement);
        List<AggregatedCallSite> sortedCct = new ArrayList<>(cct);
        sortedCct.sort(CCT_COMPARATOR);

        // Maps a depth with a pair of entry ID and list of states
        Map<Integer, Pair<Long, List<ITimeGraphState>>> depthIds = new HashMap<>();
        int maxDepth = 0;
        long maxEndTime = 0;
        for (Pair<Integer, Long> depth : depths) {
            maxDepth = Math.max(depth.getFirst(), maxDepth);
            Long endTime = fEndTimes.get(depth.getSecond());
            maxEndTime = endTime != null ? Math.max(endTime, maxEndTime) : maxEndTime;
            depthIds.put(depth.getFirst(), new Pair<>(depth.getSecond(), new ArrayList<>()));
        }

        long currentWeightTime = 0;
        Pair<Long, List<ITimeGraphState>> kernelData = depthIds.get(-1);
        // Start parsing the callgraph
        for (AggregatedCallSite callsite : sortedCct) {
            if (timeOverlap(currentWeightTime, callsite.getWeight(), times)) {
                recurseAddCallsite(callsite, currentWeightTime, predicates, times, depthIds, 0, maxDepth, monitor);

                // Get the kernel data if necessary
                if (kernelData != null) {
                    List<AggregatedCallSite> extraChildrenSites = new ArrayList<>(callsite.getExtraChildrenSites());
                    extraChildrenSites.sort(CCT_COMPARATOR);
                    // Add the required children
                    long weightTime = currentWeightTime;
                    for (AggregatedCallSite child : extraChildrenSites) {
                        if (timeOverlap(weightTime, child.getWeight(), times)) {
                            ITimeGraphState timeGraphState = new TimeGraphState(weightTime, child.getWeight(), ((AggregatedThreadStatus) child).getProcessStatus().getStateValue().unboxInt());
                            applyFilterAndAddState(kernelData.getSecond(), timeGraphState, kernelData.getFirst(), predicates, monitor);

                        }
                        weightTime += child.getWeight();
                    }
                }
            }
            currentWeightTime += callsite.getWeight();
        }

        // We may need to fill with null after the last callsite
        if (maxEndTime > currentWeightTime && timeOverlap(currentWeightTime, maxEndTime - currentWeightTime, times)) {
            fillDeeperWithNull(0, maxDepth, depthIds, currentWeightTime, maxEndTime - currentWeightTime);
        }

        List<ITimeGraphRowModel> rowModels = new ArrayList<>();
        for (Pair<Long, List<ITimeGraphState>> states : depthIds.values()) {
            rowModels.add(new TimeGraphRowModel(states.getFirst(), states.getSecond()));
        }
        return rowModels;
    }

    // Recursively adds this callsite states and its children
    private void recurseAddCallsite(AggregatedCallSite callsite, long stateStartTime,
            Map<Integer, Predicate<Multimap<String, Object>>> predicates,
            List<Long> times, Map<Integer, Pair<Long, List<ITimeGraphState>>> depthIds,
            int depth, int maxDepth, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }
        // Add the state if current depth is requested
        Pair<Long, List<ITimeGraphState>> stateList = depthIds.get(depth);
        if (stateList != null) {
            ITimeGraphState timeGraphState = createTimeGraphState(callsite, stateStartTime);
            applyFilterAndAddState(stateList.getSecond(), timeGraphState, stateList.getFirst(), predicates, monitor);
        }
        // Stop recursing if there's no more depth requested or if depth is -1
        if (depth >= maxDepth) {
            return;
        }

        /* We can fill with null states all depth deeper than the current site's max depth. Max depth includes the current element, so we -1 */
        int thisMaxDepth = depth + callsite.getMaxDepth() - 1;
        fillDeeperWithNull(thisMaxDepth, maxDepth, depthIds, stateStartTime, callsite.getWeight());

        // Get and sort the children
        List<AggregatedCallSite> children = new ArrayList<>(callsite.getCallees());
        if (children.isEmpty()) {
            return;
        }
        children.sort(CCT_COMPARATOR);

        // Add the required children
        long weightTime = stateStartTime;
        for (AggregatedCallSite child : children) {
            if (timeOverlap(weightTime, child.getWeight(), times)) {
                recurseAddCallsite(child, weightTime, predicates, times, depthIds, depth + 1, thisMaxDepth, monitor);
            }
            weightTime += child.getWeight();
        }
        // We may need to fill the remaining data with null states
        if (callsite.getWeight() > weightTime - stateStartTime && timeOverlap(weightTime, callsite.getWeight() - (weightTime - stateStartTime), times)) {
            fillDeeperWithNull(depth, thisMaxDepth, depthIds, weightTime, callsite.getWeight() - (weightTime - stateStartTime));
        }

    }

    private static void fillDeeperWithNull(int depth, int depthLimit, Map<Integer, Pair<Long, List<ITimeGraphState>>> depthIds, long time, long duration) {
        if (depthLimit <= depth) {
            return;
        }
        /* Fill with null time graph states all entries deeper than depth */
        for (Entry<Integer, Pair<Long, List<ITimeGraphState>>> depthEntry : depthIds.entrySet()) {
            if (depthEntry.getKey() > depth && depthEntry.getKey() <= depthLimit) {
                depthEntry.getValue().getSecond().add(new TimeGraphState(time, duration, Integer.MIN_VALUE));
            }
        }
    }

    private ITimeGraphState createTimeGraphState(AggregatedCallSite callsite, long currentWeightTime) {
        ICallStackSymbol value = callsite.getObject();
        String resolved = value.resolve(fSymbolProviders);
        return new TimeGraphState(currentWeightTime, callsite.getWeight(), value.hashCode(), resolved);
    }

    /** Verify if one of the requested time overlaps this callsite */
    private static boolean timeOverlap(long start, long duration, List<Long> times) {
        long end = start + duration;
        for (Long time : times) {
            if (time >= start && time <= end) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(Collections.emptyList(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null || times.size() != 1) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.FAILED, "Invalid time requested for tooltip"); //$NON-NLS-1$
        }
        List<Long> items = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (items == null || items.size() != 1) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.FAILED, "Invalid selection requested for tooltip"); //$NON-NLS-1$
        }
        Long time = times.get(0);
        Long item = items.get(0);
        CallGraphEntry callGraphEntry = fCgEntries.get(item);
        if (callGraphEntry == null) {
            return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        AggregatedCallSite callSite = findCallSite(callGraphEntry.fCallgraph.getCallingContextTree(callGraphEntry.fElement), time, callGraphEntry.fDepth, 0, 0);
        if (callSite != null) {
            return new TmfModelResponse<>(getTooltip(callSite), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private static Map<String, String> getTooltip(AggregatedCallSite callSite) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        for (Entry<String, IStatistics<?>> entry : callSite.getStatistics().entrySet()) {
            String statType = String.valueOf(entry.getKey());
            IStatistics<?> stats = entry.getValue();
            if (stats.getMax() != IHostModel.TIME_UNKNOWN) {
                builder.put(statType, ""); //$NON-NLS-1$
                String lowerType = statType.toLowerCase();
                builder.put("\t" + Messages.FlameGraph_Total + ' ' + lowerType, FORMATTER.format(stats.getTotal())); //$NON-NLS-1$
                builder.put("\t" + Messages.FlameGraph_Average + ' ' + lowerType, FORMATTER.format(stats.getMean())); //$NON-NLS-1$
                builder.put("\t" + Messages.FlameGraph_Max + ' ' + lowerType, FORMATTER.format(stats.getMax())); //$NON-NLS-1$
                builder.put("\t" + Messages.FlameGraph_Min + ' ' + lowerType, FORMATTER.format(stats.getMin())); //$NON-NLS-1$
                builder.put("\t" + Messages.FlameGraph_Deviation + ' ' + lowerType, FORMATTER.format(stats.getStdDev())); //$NON-NLS-1$

            }
        }
        return builder.build();
    }

    /** Find the callsite at the time and depth requested */
    private static @Nullable AggregatedCallSite findCallSite(Collection<AggregatedCallSite> collection, Long time, int depth, long currentTime, int currentDepth) {
        List<AggregatedCallSite> cct = new ArrayList<>(collection);
        cct.sort(CCT_COMPARATOR);
        long weight = currentTime;
        for (AggregatedCallSite callsite : cct) {
            if (weight + callsite.getWeight() < time) {
                weight += callsite.getWeight();
                continue;
            }
            // This is the right callsite, let's check the depth
            if (currentDepth == depth) {
                return callsite;
            }
            return findCallSite(callsite.getCallees(), time, depth, weight, currentDepth + 1);
        }
        return null;
    }

    @Deprecated
    @Override
    public TmfModelResponse<List<FlameChartEntryModel>> fetchTree(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull FlameChartEntryModel>> response = fetchTree(parameters, monitor);
        TmfTreeModel<@NonNull FlameChartEntryModel> model = response.getModel();
        List<FlameChartEntryModel> treeModel = null;
        if (model != null) {
            treeModel = model.getEntries();
        }
        return new TmfModelResponse<>(treeModel, response.getStatus(),
                response.getStatusMessage());
    }

    @Deprecated
    @Override
    public TmfModelResponse<List<ITimeGraphRowModel>> fetchRowModel(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        TmfModelResponse<TimeGraphModel> response = fetchRowModel(parameters, monitor);
        TimeGraphModel model = response.getModel();
        List<ITimeGraphRowModel> rows = null;
        if (model != null) {
            rows = model.getRows();
        }
        return new TmfModelResponse<>(rows, response.getStatus(), response.getStatusMessage());
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        return fetchArrows(parameters, monitor);
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        return fetchTooltip(parameters, monitor);
    }

}
