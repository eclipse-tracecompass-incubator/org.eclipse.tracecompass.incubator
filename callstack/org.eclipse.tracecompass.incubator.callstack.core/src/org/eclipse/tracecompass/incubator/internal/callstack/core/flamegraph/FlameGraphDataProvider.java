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
import java.util.NavigableSet;
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
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraphGroupBy;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.AggregatedThreadStatus;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider.FlameChartEntryModel.EntryType;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
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
import com.google.common.collect.TreeMultimap;

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
    private final Comparator<WeightedTree<ICallStackSymbol>> CCT_COMPARATOR2 = Comparator.comparing(WeightedTree<ICallStackSymbol>::getWeight).thenComparing(s -> String.valueOf(s.getObject()));
    /**
     * Logger for Abstract Tree Data Providers.
     */
    private static final Logger LOGGER = TraceCompassLog.getLogger(FlameGraphDataProvider.class);
    private static final Format FORMATTER = SubSecondTimeWithUnitFormat.getInstance();

    /* State System attributes for the root levels */
    private static final String FUNCTION_LEVEL = "Function"; //$NON-NLS-1$
    private static final String EXTRA_LEVEL = "Extra"; //$NON-NLS-1$

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
        private ITmfStateSystem fSs;
        private Integer fQuark;

        public CallGraphEntry(ICallStackElement element, ITmfStateSystem ss, Integer quark) {
            fElement = element;
            fSs = ss;
            fQuark = quark;
        }
    }

    private static class CallSiteCustomValue extends CustomStateValue {
        private WeightedTree<ICallStackSymbol> fCallSite;

        public CallSiteCustomValue(WeightedTree<ICallStackSymbol> rootFunction) {
            fCallSite = rootFunction;
        }

        @Override
        public int compareTo(ITmfStateValue o) {
            if (!(o instanceof CallSiteCustomValue)) {
                return -1;
            }
            return fCallSite.compareTo(((CallSiteCustomValue) o).fCallSite);
        }

        @Override
        protected Byte getCustomTypeId() {
            return 103;
        }

        @Override
        protected void serializeValue(ISafeByteBufferWriter buffer) {
            throw new UnsupportedOperationException("This state value is not meant to be written to disk"); //$NON-NLS-1$
        }

        @Override
        protected int getSerializedValueSize() {
            throw new UnsupportedOperationException("This state value is not meant to be written to disk"); //$NON-NLS-1$
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

    private ITmfStateSystem elementToStateSystem(CallGraph callgraph, ICallStackElement element) {
        // Create an in-memory state system for this element
        IStateHistoryBackend backend = StateHistoryBackendFactory.createInMemoryBackend("org.eclipse.tracecompass.incubator.callgraph.ss", 0L); //$NON-NLS-1$
        ITmfStateSystemBuilder ssb = StateSystemFactory.newStateSystem(backend);

        // Add the functions
        List<AggregatedCallSite> rootFunctions = new ArrayList<>(callgraph.getCallingContextTree(element));
        rootFunctions.sort(CCT_COMPARATOR);
        int quarkFct = ssb.getQuarkAbsoluteAndAdd(FUNCTION_LEVEL);
        Deque<Long> timestampStack = new ArrayDeque<>();
        timestampStack.push(0L);
        for (AggregatedCallSite rootFunction : rootFunctions) {
            recursivelyAddChildren(ssb, quarkFct, rootFunction, timestampStack);
        }
        Long endTime = timestampStack.pop();
        ssb.closeHistory(endTime);

        return ssb;
    }

    private void recursivelyAddChildren(ITmfStateSystemBuilder ssb, int quarkFct, WeightedTree<ICallStackSymbol> callSite, Deque<Long> timestampStack) {
        Long lastEnd = timestampStack.peek();
        if (lastEnd == null) {
            return;
        }
        ssb.pushAttribute(lastEnd, (Object) new CallSiteCustomValue(callSite), quarkFct);

        // Push the children to the state system
        timestampStack.push(lastEnd);
        List<WeightedTree<ICallStackSymbol>> children = new ArrayList<>(callSite.getChildren());
        children.sort(CCT_COMPARATOR2);
        for (WeightedTree<ICallStackSymbol> callsite : children) {
            recursivelyAddChildren(ssb, quarkFct, callsite, timestampStack);
        }
        timestampStack.pop();

        // Add the extra sites
        // TODO Support extra sites from weighted tree
        if (callSite instanceof AggregatedCallSite) {
            Iterator<AggregatedCallSite> extraChildrenSites = ((AggregatedCallSite) callSite).getExtraChildrenSites().iterator();
            if (extraChildrenSites.hasNext()) {
                int quarkExtra = ssb.getQuarkAbsoluteAndAdd(EXTRA_LEVEL);
                long extraStartTime = lastEnd;
                while (extraChildrenSites.hasNext()) {
                    AggregatedCallSite next = extraChildrenSites.next();
                    ssb.modifyAttribute(extraStartTime, (Object) new CallSiteCustomValue(next), quarkExtra);
                    extraStartTime += next.getWeight();
                }
            }
        }

        long currentEnd = timestampStack.pop() + callSite.getWeight();
        timestampStack.push(currentEnd);
        ssb.popAttribute(currentEnd, quarkFct);
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

        Deque<Long> timestampStack = new ArrayDeque<>();
        timestampStack.push(0L);

        // Get the state system to represent this callgraph
        ITmfStateSystem ss = elementToStateSystem(callgraph, element);
        entry.setEndTime(ss.getCurrentEndTime());

        // Add items for the function entries
        int quark = ss.optQuarkAbsolute(FUNCTION_LEVEL);
        if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }
        int i = 0;
        for (Integer subQuark : ss.getSubAttributes(quark, false)) {
            FlameChartEntryModel.Builder child = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), entry.getId(), String.valueOf(i), 0, EntryType.FUNCTION, i);
            child.setEndTime(ss.getCurrentEndTime());
            builder.add(child);
            i++;
            fCgEntries.put(child.getId(), new CallGraphEntry(element, ss, subQuark));
        }

        // Add items for the extra entries
        quark = ss.optQuarkAbsolute(EXTRA_LEVEL);
        if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }
        FlameChartEntryModel.Builder child = new FlameChartEntryModel.Builder(ENTRY_ID.getAndIncrement(), entry.getId(), Objects.requireNonNull(Messages.FlameGraph_KernelStatusTitle), 0, EntryType.KERNEL, -1);
        child.setEndTime(ss.getCurrentEndTime());
        builder.add(child);
        fCgEntries.put(child.getId(), new CallGraphEntry(element, ss, quark));

        return;
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
        Multimap<Pair<ITmfStateSystem, ICallStackElement>, Pair<Integer, Long>> requested = HashMultimap.create();
        for (Long id : selected) {
            CallGraphEntry entry = fCgEntries.get(id);
            if (entry != null) {
                selectedEntries.add(entry);
                requested.put(new Pair<>(entry.fSs, entry.fElement), new Pair<>(entry.fQuark, id));
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
        try {
            for (Pair<ITmfStateSystem, ICallStackElement> element : requested.keySet()) {
                if (subMonitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                Collection<Pair<Integer, Long>> depths = requested.get(element);
                rowModels.addAll(getStatesForElement(times, predicates, subMonitor, element.getFirst(), depths));
            }
        } catch (StateSystemDisposedException e) {
            // Nothing to do
        }

        return new TmfModelResponse<>(new TimeGraphModel(rowModels), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private List<ITimeGraphRowModel> getStatesForElement(List<Long> times, Map<Integer, Predicate<Multimap<String, Object>>> predicates, IProgressMonitor monitor,
            ITmfStateSystem ss, Collection<Pair<Integer, Long>> depths) throws StateSystemDisposedException {
        List<Integer> quarks = new ArrayList<>();
        for (Pair<Integer, Long> pair : depths) {
            quarks.add(pair.getFirst());
        }
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        long ssEndTime = ss.getCurrentEndTime();
        for (ITmfStateInterval interval : ss.query2D(quarks, times)) {
            if (monitor.isCanceled()) {
                return Collections.emptyList();
            }
            // Ignore the null intervals of value 1 at the end of the state
            // system
            if (interval.getStartTime() == ssEndTime &&
                    interval.getStartTime() == interval.getEndTime() &&
                    interval.getValue() == null) {
                continue;
            }
            intervals.put(interval.getAttribute(), interval);
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (Pair<Integer, Long> pair : depths) {
            int quark = pair.getFirst();
            NavigableSet<ITmfStateInterval> states = intervals.get(quark);

            if (monitor.isCanceled()) {
                return Collections.emptyList();
            }
            List<ITimeGraphState> eventList = new ArrayList<>();
            Long key = Objects.requireNonNull(pair.getSecond());
            states.forEach(i -> {
                ITimeGraphState timegraphState = createTimeGraphState(i, ssEndTime);
                applyFilterAndAddState(eventList, timegraphState, key, predicates, monitor);
            });
            rows.add(new TimeGraphRowModel(key, eventList));
        }
        return rows;

    }

    private ITimeGraphState createTimeGraphState(ITmfStateInterval interval, long ssEndTime) {
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + (ssEndTime == interval.getEndTime() ? 0 : 1);
        Object valueObject = interval.getValue();
        if (valueObject instanceof CallSiteCustomValue) {

            WeightedTree<ICallStackSymbol> callsite = ((CallSiteCustomValue) valueObject).fCallSite;
            ICallStackSymbol value = callsite.getObject();
            String resolved = value.resolve(fSymbolProviders);
            // FIXME there shouldn't be any direct reference to AggregatedThreadStatus. Passing from CallGraph to WeightedTree should fix this
            if (callsite instanceof AggregatedThreadStatus) {
                return new TimeGraphState(startTime, duration, ((AggregatedThreadStatus) callsite).getProcessStatus().getStateValue().unboxInt(), resolved);
            }
            return new TimeGraphState(startTime, duration, value.hashCode(), resolved);
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
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
        WeightedTree<ICallStackSymbol> callSite = findCallSite(callGraphEntry, time);
        if (callSite != null) {
            return new TmfModelResponse<>(getTooltip(callSite), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private Map<String, String> getTooltip(WeightedTree<ICallStackSymbol> tree) {
        if (!(tree instanceof AggregatedCallSite)) {
            return Collections.emptyMap();
        }
        AggregatedCallSite callSite = (AggregatedCallSite) tree;
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        builder.put(Messages.FlameGraph_Symbol, callSite.getObject().resolve(fSymbolProviders));
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

    /** Find the callsite at the time requested */
    private static @Nullable WeightedTree<ICallStackSymbol> findCallSite(CallGraphEntry cgEntry, Long time) {
        try {
            ITmfStateInterval interval = cgEntry.fSs.querySingleState(time, cgEntry.fQuark);

            Object valueObject = interval.getValue();
            if (valueObject instanceof CallSiteCustomValue) {
                return ((CallSiteCustomValue) valueObject).fCallSite;
            }
        } catch (StateSystemDisposedException e) {
            // Nothing to do
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
