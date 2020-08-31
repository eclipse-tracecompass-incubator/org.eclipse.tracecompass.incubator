/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.io;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.IODataPalette;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractStateSystemAnalysisDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Time graph data provider that shows the files read and written to by some selected
 * threads.
 *
 * TODO Support multiple TID selection as the tree allows it
 *
 * @author Matthew Khouzam
 */
@SuppressWarnings("restriction")
public class IoAccessDataProvider extends AbstractStateSystemAnalysisDataProvider implements ITimeGraphDataProvider<TimeGraphEntryModel>, IOutputStyleProvider {

    /**
     * Suffix for dataprovider ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.kernel.core.io.access.dataprovider"; //$NON-NLS-1$ ;
    /**
     * String for the selected TID parameter of the request. Expected type is
     * Integer or List of integers
     */
    public static final String SELECTED_TID_PARAM = "selected"; //$NON-NLS-1$
    /**
     * String for the checked TID parameter of the request. Expected type is
     * Integer or List of integers
     */
    public static final String CHECKED_TID_PARAM = "checked"; //$NON-NLS-1$

    private static final Logger LOGGER = TraceCompassLog.getLogger(IoAccessDataProvider.class);

    private static final String READ_STYLE = "Read"; //$NON-NLS-1$
    private static final String WRITE_STYLE = "Write"; //$NON-NLS-1$

    /* The map of basic styles */
    private static final Map<String, OutputElementStyle> STATE_MAP;
    /*
     * A map of styles names to a style that has the basic style as parent, to
     * avoid returning complete styles for each state
     */
    private static final Map<String, OutputElementStyle> STYLE_MAP = new HashMap<>();

    static {
        /* Build three different styles to use as examples */
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        List<Pair<String, String>> COLOR_LIST = IODataPalette.getColors();
        Pair<String, String> colorPair = COLOR_LIST.get(0);
        builder.put(READ_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, READ_STYLE,
                StyleProperties.BACKGROUND_COLOR, colorPair.getFirst())));
        builder.put(WRITE_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, WRITE_STYLE,
                StyleProperties.BACKGROUND_COLOR, colorPair.getSecond())));
        STATE_MAP = builder.build();
        STYLE_MAP.put(READ_STYLE, new OutputElementStyle(READ_STYLE));
        STYLE_MAP.put(WRITE_STYLE, new OutputElementStyle(WRITE_STYLE));
    }

    private IoAnalysis fAnalysisModule;
    private final BiMap<Long, Pair<Integer, String>> fIdToFile = HashBiMap.create();
    private final AtomicLong fIdGenerator = new AtomicLong();
    private final BiMap<Long, Integer> fIdToTid = HashBiMap.create();

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public IoAccessDataProvider(ITmfTrace trace, IoAnalysis analysisModule) {
        super(trace);
        fAnalysisModule = analysisModule;
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<TimeGraphModel> fetchRowModel(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fAnalysisModule.waitForInitialization();
        ITmfStateSystem ss = fAnalysisModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        long currentEnd = ss.getCurrentEndTime();
        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        List<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);

        Object end = Iterables.getLast(times);
        if (!(end instanceof Number)) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        boolean complete = ss.waitUntilBuilt(0) || ((Number) end).longValue() <= currentEnd;

        if (monitor != null && monitor.isCanceled()) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
        }
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTimeGraphDataProvider#fetchRowModel") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {

            TimeGraphModel models = getRowModel(ss, fetchParameters, monitor, selectedItems);
            if (models == null) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                // There was some other failure that returned null
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, "Request failed"); //$NON-NLS-1$
            }
            return new TmfModelResponse<>(models, complete ? Status.COMPLETED : Status.RUNNING,
                    complete ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING);
        } catch (StateSystemDisposedException | TimeRangeException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return new TmfModelResponse<>(null, Status.FAILED, String.valueOf(e.getMessage()));
        }
    }

    private @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor, List<Long> selectedItems) throws StateSystemDisposedException {

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(parameters);
        if (times == null || times.isEmpty()) {
            // No time specified
            return new TimeGraphModel(Collections.emptyList());
        }

        // Prepare the set of file names being requested
        Map<Integer, Long> quarkToId = new HashMap<>();
        for (Long selectedItem : selectedItems) {

            Pair<Integer, String> filename = fIdToFile.get(selectedItem);
            if (filename == null) {
                continue;
            }
            // Look in the "Resources" section to find the quark for the thread
            // for each requested file
            int quark = ss.optQuarkAbsolute(IoStateProvider.ATTRIBUTE_RESOURCES, filename.getSecond(), String.valueOf(filename.getFirst()), IoStateProvider.ATTRIBUTE_OPERATION);
            if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                // No data for this file, ignore
                continue;
            }
            quarkToId.put(quark, selectedItem);
        }

        if (quarkToId.isEmpty()) {
            return new TimeGraphModel(Collections.emptyList());
        }

        // Query the operations intervals for the files
        TreeMultimap<Long, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        for (ITmfStateInterval interval : ss.query2D(quarkToId.keySet(), times)) {
            intervals.put(Objects.requireNonNull(quarkToId.get(interval.getAttribute())), interval);
        }

        if (monitor != null && monitor.isCanceled()) {
            return new TimeGraphModel(Collections.emptyList());
        }

        // Create the states for each requested file
        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (Entry<Long, Collection<ITmfStateInterval>> entryIntervals : intervals.asMap().entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            List<ITimeGraphState> states = new ArrayList<>();
            for (ITmfStateInterval interval : entryIntervals.getValue()) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                states.add(new TimeGraphState(startTime, duration, null, getStyleFor(interval)));
            }
            rows.add(new TimeGraphRowModel(entryIntervals.getKey(), states));
        }
        return new TimeGraphModel(rows);
    }

    private static @Nullable OutputElementStyle getStyleFor(ITmfStateInterval interval) {
        Object value = interval.getValue();
        if (!(value instanceof String)) {
            return null;
        }
        String operation = (String) value;
        if (operation.equals(IoStateProvider.ATTRIBUTE_READ)) {
            return STYLE_MAP.get(READ_STYLE);
        }
        return STYLE_MAP.get(WRITE_STYLE);
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(Collections.emptyMap(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<TmfTreeModel<TimeGraphEntryModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fAnalysisModule.waitForInitialization();
        ITmfStateSystem ss = fAnalysisModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean complete = ss.waitUntilBuilt(0);
        try (FlowScopeLog scope = new FlowScopeLogBuilder(LOGGER, Level.FINE, "AbstractTreeDataProvider#fetchTree") //$NON-NLS-1$
                .setCategory(getClass().getSimpleName()).build()) {
            TmfTreeModel<TimeGraphEntryModel> tree = getTree(ss, fetchParameters, monitor);
            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            return new TmfModelResponse<>(tree,
                    complete ? ITmfResponse.Status.COMPLETED : ITmfResponse.Status.RUNNING,
                    complete ? CommonStatusMessage.RUNNING : CommonStatusMessage.COMPLETED);

        } catch (StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }
    }

    private TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {

        // Map the TID to the current operation intervals
        Multimap<Integer, ITmfStateInterval> currentOperations = HashMultimap.create();
        // Map a TID to the tid files
        Map<Integer, TidFiles> fds = new HashMap<>();
        boolean gotData = fillQueryIntervals(ss, parameters, monitor, currentOperations, fds);
        if (!gotData || (monitor != null && monitor.isCanceled())) {
            return new TmfTreeModel<>(Collections.emptyList(), Collections.emptyList());
        }

        Multimap<Integer, String> files = HashMultimap.create();
        // For each rw operation, find the file descriptor interval that matches
        // the file name
        for (Entry<Integer, ITmfStateInterval> entry : currentOperations.entries()) {
            ITmfStateInterval rwInterval = entry.getValue();
            Integer tid = entry.getKey();
            Object fdObj = rwInterval.getValue();
            TidFiles tidFiles = fds.get(tid);
            if (!(fdObj instanceof Long) || tidFiles == null) {
                // Not a proper file descriptor
                continue;
            }
            String filename = tidFiles.getFilename(rwInterval, String.valueOf(fdObj));
            if (filename != null) {
                files.put(entry.getKey(), filename);
            }
        }

        Builder<@NonNull TimeGraphEntryModel> builder = new Builder<>();
        for (Entry<Integer, Collection<String>> fileEntry : files.asMap().entrySet()) {
            Integer tid = fileEntry.getKey();
            long tidId = getTidId(tid);
            builder.add(new TimeGraphEntryModel(tidId, -1, String.valueOf(tid), ss.getStartTime(), ss.getCurrentEndTime()));
            for (String file : fileEntry.getValue()) {
                long id = getId(new Pair<>(tid, file));
                builder.add(new TimeGraphEntryModel(id, tidId, file, ss.getStartTime(), ss.getCurrentEndTime()));
            }
        }

        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    private static Collection<Integer> extractIntegerList(Map<String, Object> parameters, String selectedTidParam) {
        Object collectionObject = parameters.get(selectedTidParam);
        if (collectionObject instanceof Collection<?>) {
            Collection<?> collection = (Collection <?>) collectionObject;
            if (collection.stream().allMatch(e -> e instanceof Integer)) {
                    return (Collection<Integer>) collection;
            }
        }
        return Collections.emptyList();
    }

    private static class TidFiles {

        Multimap<String, ITmfStateInterval> fIntervals = HashMultimap.create();

        public TidFiles() {

        }

        public @Nullable String getFilename(ITmfStateInterval rwInterval, String fdStr) {
            Collection<ITmfStateInterval> intervals = fIntervals.get(fdStr);
            for (ITmfStateInterval interval : intervals) {
                if (!(interval.getStartTime() > rwInterval.getEndTime() || interval.getEndTime() < rwInterval.getStartTime())) {
                    Object value = interval.getValue();
                    return (value instanceof String) ? (String) value : null;
                }
            }
            return null;
        }

        public void addInterval(ITmfStateSystem ss, ITmfStateInterval interval) {
            fIntervals.put(ss.getAttributeName(interval.getAttribute()), interval);
        }

    }

    /**
     * Fill 2 arrays with intervals: the currentOperations that contains the
     * current non-null reads and writes operations for the thread and the fds
     * map which has the intervals containing the filename for each file
     * descriptor
     *
     * @param discrete
     *            Whether to get only the intervals for the requested times, or
     *            all intervals in the range (for instance to build the tree)
     */
    private static boolean fillQueryIntervals(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor, Multimap<Integer, ITmfStateInterval> currentOperations, Map<Integer, TidFiles> fds)
            throws IndexOutOfBoundsException, TimeRangeException, StateSystemDisposedException {
        Collection<Integer> selectedTid = extractIntegerList(parameters, SELECTED_TID_PARAM);
        if (selectedTid.isEmpty()) {
            return false;
        }

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(parameters);
        if (times == null || times.isEmpty()) {
            // No time specified
            return false;
        }
        long start = Long.MAX_VALUE;
        long end = 0;
        for (Long time : times) {
            start = Math.min(start, time);
            end = Math.max(start, time);
        }
        start = Math.max(start, ss.getStartTime());
        end = Math.min(end, ss.getCurrentEndTime());

        // Get the quarks to query for all threads
        Set<Integer> toQuery = new HashSet<>();
        // Map a quark to the TID
        Map<Integer, Integer> operationQuarks = new HashMap<>();
        // Map the file tbl quarks to the tidFile
        Map<Integer, TidFiles> tblQuarks = new HashMap<>();
        for (Integer tid : selectedTid) {

            // First, get the file descriptor table quark for this thread, all
            // the rest can be 2d queried
            int fdTblQuark = ss.optQuarkAbsolute(IoStateProvider.ATTRIBUTE_TID, String.valueOf(tid), IoStateProvider.ATTRIBUTE_FDTBL);
            if (fdTblQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                continue;
            }

            ITmfStateInterval fdLink = StateSystemUtils.queryUntilNonNullValue(ss, fdTblQuark, start, end);
            if (fdLink == null) {
                // No file descriptor table for this time range, return empty
                continue;
            }

            fdTblQuark = ss.optQuarkAbsolute(IoStateProvider.ATTRIBUTE_FDTBL, String.valueOf(fdLink.getValue()));
            if (fdTblQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                // For some reason, the fd table is not available, just return empty list
                continue;
            }

            /*
             * Get the attributes to query: they will be
             *
             * 1- The file descriptors under the thread's FD table, they contain
             * the resource name
             *
             * 2- The CURRENT and FD attributes under the current thread to get
             * the current read/write operations
             */

            List<Integer> fdQuarks = ss.getSubAttributes(fdTblQuark, false);
            toQuery.addAll(ss.getSubAttributes(fdTblQuark, false));
            int readFd = ss.optQuarkAbsolute(IoStateProvider.ATTRIBUTE_TID, String.valueOf(tid), IoStateProvider.ATTRIBUTE_READ, IoStateProvider.ATTRIBUTE_FD);
            addIfExist(toQuery, operationQuarks, readFd, tid);
            int writeFd = ss.optQuarkAbsolute(IoStateProvider.ATTRIBUTE_TID, String.valueOf(tid), IoStateProvider.ATTRIBUTE_WRITE, IoStateProvider.ATTRIBUTE_FD);
            addIfExist(toQuery, operationQuarks, writeFd, tid);
            TidFiles tidFiles = tblQuarks.get(fdTblQuark);
            if (tidFiles == null) {
                // Use the same object for all quarks under the file table descriptor
                tidFiles = new TidFiles();
                tblQuarks.put(fdTblQuark, tidFiles);
                for (Integer quark : fdQuarks) {
                    tblQuarks.put(quark, tidFiles);
                }
            }
            fds.put(tid, tidFiles);
        }
        if (toQuery.isEmpty()) {
            return false;
        }

        for (ITmfStateInterval interval : ss.query2D(toQuery, start, end)) {
            if (monitor != null && monitor.isCanceled()) {
                return false;
            }
            if (interval.getValue() == null) {
                continue;
            }

            if (operationQuarks.containsKey(interval.getAttribute())) {
                // Save the current operations in a list
                currentOperations.put(Objects.requireNonNull(operationQuarks.get(interval.getAttribute())), interval);
            } else {
                // Add this interval to the tidFiles object
                TidFiles tidFiles = Objects.requireNonNull(tblQuarks.get(interval.getAttribute()));
                tidFiles.addInterval(ss, interval);
            }
        }
        return true;

    }

    private static void addIfExist(Set<Integer> toQuery, Map<Integer, Integer> operationQuarks, int quark, Integer tid) {
        if (quark != ITmfStateSystem.INVALID_ATTRIBUTE) {
            toQuery.add(quark);
            operationQuarks.put(quark, tid);
        }
    }

    /**
     * Get (and generate if necessary) a unique id for this quark. Should be
     * called inside {@link #getTree(ITmfStateSystem, Map, IProgressMonitor)},
     * where the write lock is held.
     *
     * @param file
     *            quark to map to
     * @return the unique id for this quark
     */
    private long getId(Pair<Integer, String> file) {
        return fIdToFile.inverse().computeIfAbsent(file, q -> getEntryId());
    }

    /**
     * Get (and generate if necessary) a unique id for this quark. Should be
     * called inside {@link #getTree(ITmfStateSystem, Map, IProgressMonitor)},
     * where the write lock is held.
     *
     * @param file
     *            quark to map to
     * @return the unique id for this quark
     */
    private long getTidId(Integer tid) {
        return fIdToTid .inverse().computeIfAbsent(tid, q -> getEntryId());
    }

    /**
     * Get a new unique id, unbound to any quark.
     *
     * @return the unique id
     */
    protected long getEntryId() {
        return fIdGenerator.getAndIncrement();
    }

    // public @Nullable Long getBytesRead(long start, long end, long
    // attributeId) {
    // ITmfStateSystem ss = getAnalysisModule().getStateSystem();
    // if (ss == null) {
    // return null;
    // }
    //
    // Map<Long, Integer> selectedEntries = getSelectedEntries(new
    // SelectionTimeQueryFilter(Arrays.asList(start, end),
    // Collections.singleton(attributeId)));
    // Integer startingNodeQuark = selectedEntries.get(attributeId);
    // if (startingNodeQuark == null || startingNodeQuark >= OFFSET) {
    // return null;
    // }
    // int readQuark = ss.optQuarkRelative(startingNodeQuark,
    // IoStateProvider.ATTRIBUTE_READ);
    // return getdelta(start, end, ss, readQuark);
    // }
    //
    // public @Nullable Long getBytesWrite(long start, long end, long
    // attributeId) {
    // ITmfStateSystem ss = getAnalysisModule().getStateSystem();
    // if (ss == null) {
    // return null;
    // }
    //
    // Map<Long, Integer> selectedEntries = getSelectedEntries(new
    // SelectionTimeQueryFilter(Arrays.asList(start, end),
    // Collections.singleton(attributeId)));
    // Integer startingNodeQuark = selectedEntries.get(attributeId);
    // if (startingNodeQuark == null || startingNodeQuark >= OFFSET) {
    // return null;
    // }
    // int readQuark = ss.optQuarkRelative(startingNodeQuark,
    // IoStateProvider.ATTRIBUTE_WRITE);
    // return getdelta(start, end, ss, readQuark);
    // }

    // private static @Nullable Long getdelta(long start, long end,
    // ITmfStateSystem ss, int readQuark) {
    // if (readQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
    // return null;
    // }
    // try {
    // ITmfStateInterval startInterval = ss.querySingleState(start, readQuark);
    // ITmfStateInterval endInterval = ss.querySingleState(end, readQuark);
    // return endInterval.getValueLong() - startInterval.getValueLong();
    // } catch (StateSystemDisposedException e) {
    // return null;
    // }
    // }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public String getId() {
        return ID;
    }

}
