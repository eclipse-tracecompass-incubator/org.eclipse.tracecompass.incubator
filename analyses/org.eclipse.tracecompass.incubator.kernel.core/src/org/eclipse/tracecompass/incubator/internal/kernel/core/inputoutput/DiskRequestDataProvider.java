/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.Disk;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.DiskUtils;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.InputOutputAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.IoOperationType;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 *
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class DiskRequestDataProvider extends AbstractTimeGraphDataProvider<InputOutputAnalysisModule, TimeGraphEntryModel> implements IOutputStyleProvider {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.kernel.core.inputoutput.DiskRequestDataProvider"; //$NON-NLS-1$

    private static final String WAITING_QUEUE = Objects.requireNonNull(Messages.DiskRequestsDataProvider_WaitingQueue);
    private static final String DRIVER_QUEUE = Objects.requireNonNull(Messages.DiskRequestsDataProvider_DriverQueue);

    /**
     * The state index for the multiple state
     */
    private static final int MAX_SIZE = 500;
    private static final int NB_SIZE_STYLES = 5;
    private static final String SIZE_STYLE_PREFIX = "size"; //$NON-NLS-1$
    private static final String READ_STYLE = "read"; //$NON-NLS-1$
    private static final String WRITE_STYLE = "write"; //$NON-NLS-1$
    private static final String FLUSH_STYLE = "flush"; //$NON-NLS-1$
    private static final String OTHER_STYLE = "other"; //$NON-NLS-1$

    private static final Comparator<ITmfStateInterval> INTERVAL_COMPARATOR = Comparator.comparing(ITmfStateInterval::getStartTime);

    private static final Map<String, OutputElementStyle> STYLES;
    // Map of styles with the parent
    private static final Map<String, OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        String blackColor = X11ColorUtils.toHexColor("black"); //$NON-NLS-1$
        if (blackColor == null) {
            blackColor = X11ColorUtils.toHexColor(0, 0, 0);
        }
        String brownColor = X11ColorUtils.toHexColor("sienna"); //$NON-NLS-1$
        if (brownColor == null) {
            brownColor = X11ColorUtils.toHexColor(160, 82, 45);
        }
        String otherColor = X11ColorUtils.toHexColor("dark green"); //$NON-NLS-1$
        if (otherColor == null) {
            otherColor = X11ColorUtils.toHexColor(0, 100, 0);
        }
        // Put the request types
        builder.put(READ_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, READ_STYLE,
                StyleProperties.BACKGROUND_COLOR, Objects.requireNonNull(X11ColorUtils.toHexColor("blue")), //$NON-NLS-1$
                StyleProperties.COLOR, blackColor)));
        builder.put(WRITE_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, WRITE_STYLE,
                StyleProperties.BACKGROUND_COLOR, Objects.requireNonNull(X11ColorUtils.toHexColor("red")), //$NON-NLS-1$
                StyleProperties.COLOR, blackColor)));
        builder.put(FLUSH_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, FLUSH_STYLE,
                StyleProperties.BACKGROUND_COLOR, brownColor,
                StyleProperties.HEIGHT, 0.6f)));
        builder.put(OTHER_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.STYLE_NAME, OTHER_STYLE,
                StyleProperties.BACKGROUND_COLOR, otherColor,
                StyleProperties.HEIGHT, 0.6f)));
        // Put the styles for size of request
        for (int i = 0; i < NB_SIZE_STYLES; i++) {
            builder.put(SIZE_STYLE_PREFIX + i, new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.HEIGHT, (float) (i + 1) / NB_SIZE_STYLES)));
        }

        STYLES = builder.build();
    }

    /**
     * Inline class to encapsulate all the values required to build a series.
     * Allows for reuse of full query results to be faster than {@link Disk}.
     */
    private final class RequestBuilder {

        private final long fId;
        /** This series' sector quark. public because final */
        private final int fMainQuark;
        private final int fSizeQuark;

        /**
         * Constructor
         *
         * @param id
         *            The ID of the request entry
         * @param quark
         *            The main quark of the request
         * @param ss
         *            The state system
         */
        private RequestBuilder(Long id, Integer quark, ITmfStateSystem ss) {
            fId = id;
            fMainQuark = quark;
            fSizeQuark = ss.optQuarkRelative(quark, Attributes.REQUEST_SIZE);
        }

        private List<Integer> getQuarks() {
            List<Integer> quarks = new ArrayList<>();
            quarks.add(fMainQuark);
            if (fSizeQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                quarks.add(fSizeQuark);
            }
            return quarks;
        }

        private @Nullable ITmfStateInterval findInterval(@Nullable Set<ITmfStateInterval> intervals, long time) {
            if (intervals == null) {
                return null;
            }
            for (ITmfStateInterval interval : intervals) {
                // Intervals are sorted, return if interval has start time later than time, the interval is not there
                if (interval.getStartTime() > time) {
                    return null;
                }
                if (time >= interval.getStartTime() && time <= interval.getEndTime()) {
                    return interval;
                }
            }
            return null;
        }

        public ITimeGraphRowModel createStates(Map<Integer, Set<ITmfStateInterval>> intervals, Map<Integer, Predicate<Multimap<String, Object>>> predicates, @Nullable IProgressMonitor monitor) {
            Set<ITmfStateInterval> mainIntervals = intervals.get(fMainQuark);
            if (mainIntervals == null) {
                return new TimeGraphRowModel(fId, Collections.emptyList());
            }
            List<ITimeGraphState> states = new ArrayList<>();

            for (ITmfStateInterval mainInterval : mainIntervals) {
                long startTime = mainInterval.getStartTime();
                long duration = mainInterval.getEndTime() - startTime + 1;
                Object value = mainInterval.getValue();
                if (value == null) {
                    ITimeGraphState timeGraphState = new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
                    applyFilterAndAddState(states, timeGraphState, fId, predicates, monitor);
                } else {
                    // There should be one sector interval per request
                    if (fSizeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        ITimeGraphState timeGraphState = new TimeGraphState(startTime, duration, null, getStyleFor(IoOperationType.fromNumber((Integer) value), null));
                        applyFilterAndAddState(states, timeGraphState, fId, predicates, monitor);
                    } else {
                        long time = startTime;
                        while (time < mainInterval.getEndTime()) {
                            // Add a request for each size
                            ITmfStateInterval sizeInterval = findInterval(intervals.get(fSizeQuark), time);
                            ITimeGraphState timeGraphState = new TimeGraphState(startTime, duration, null, getStyleFor(IoOperationType.fromNumber((Integer) value), sizeInterval == null ? null : (Integer) sizeInterval.getValue()));
                            applyFilterAndAddState(states, timeGraphState, fId, predicates, monitor);

                            if (sizeInterval == null) {
                                break;
                            }
                            time = sizeInterval.getEndTime() + 1;
                        }
                    }
                }
            }
            return new TimeGraphRowModel(fId, states);
        }
    }

    private final Set<Integer> fRequestQuark = new TreeSet<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace this data provider is for
     * @param analysisModule
     *            The input output analysis module, source of the data
     */
    public DiskRequestDataProvider(ITmfTrace trace, InputOutputAnalysisModule analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        long start = ss.getStartTime();
        long end = ss.getCurrentEndTime();

        List<TimeGraphEntryModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TimeGraphEntryModel(rootId, -1, Objects.requireNonNull(getTrace().getName()), start, end));

        for (Integer diskQuark : ss.getQuarks(Attributes.DISKS, "*")) { //$NON-NLS-1$
            String diskName = DiskUtils.getDiskName(ss, diskQuark);
            long diskId = getId(diskQuark);

            List<TimeGraphEntryModel> driverQueue = getDiskQueue(ss, diskQuark, Attributes.DRIVER_QUEUE, DRIVER_QUEUE, diskId, start, end);
            List<TimeGraphEntryModel> waitingQueue = getDiskQueue(ss, diskQuark, Attributes.WAITING_QUEUE, WAITING_QUEUE, diskId, start, end);
            if (!driverQueue.isEmpty() && !waitingQueue.isEmpty()) {
                nodes.add(new TimeGraphEntryModel(diskId, rootId, diskName, start, end));
                nodes.addAll(driverQueue);
                nodes.addAll(waitingQueue);
            }
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    private List<TimeGraphEntryModel> getDiskQueue(ITmfStateSystem ss, Integer diskQuark, String queueAttribute, String queueName, long diskId, long start, long end) {
        // Does the queue exist for the disk
        int queueQuark = ss.optQuarkRelative(diskQuark, queueAttribute);
        if (queueQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            Collections.emptyList();
        }

        // Does the queue have requests
        List<Integer> subAttributes = ss.getSubAttributes(queueQuark, false);
        if (subAttributes.isEmpty()) {
            return Collections.emptyList();
        }

        // Add the queue and its sub-entries to the entry list
        List<TimeGraphEntryModel> entries = new ArrayList<>();
        long queueId = getId(queueQuark);
        entries.add(new TimeGraphEntryModel(queueId, diskId, queueName, start, end));
        for (Integer requestQuark : subAttributes) {
            fRequestQuark.add(requestQuark);
            entries.add(new TimeGraphEntryModel(getId(requestQuark), queueId, ss.getAttributeName(requestQuark), start, end));
        }
        return entries;
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        List<Long> ids = DataProviderParameterUtils.extractSelectedItems(parameters);
        if (ids == null) {
            return null;
        }
        Map<Long, Integer> selectedEntries = getSelectedEntries(ids);
        List<Long> times = DataProviderParameterUtils.extractTimeRequested(parameters);
        if (times == null) {
            // No time specified
            return null;
        }
        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        Multimap<Integer, String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<Integer> quarksToQuery = new ArrayList<>();
        List<RequestBuilder> builders = new ArrayList<>();
        for (Entry<Long, Integer> entry : selectedEntries.entrySet()) {
            if (fRequestQuark.contains(entry.getValue())) {
                RequestBuilder seriesBuilder = new RequestBuilder(entry.getKey(), entry.getValue(), ss);
                builders.add(seriesBuilder);
                quarksToQuery.addAll(seriesBuilder.getQuarks());
            }
        }

        // Put all intervals in a map, there shouldn't be too many, we'll handle them later
        Map<Integer, Set<ITmfStateInterval>> intervals = new HashMap<>();
        try {
            for (ITmfStateInterval interval : ss.query2D(quarksToQuery, times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return null;
                }
                intervals.computeIfAbsent(interval.getAttribute(), q -> new TreeSet<>(INTERVAL_COMPARATOR)).add(interval);
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return null;
        }

        List<ITimeGraphRowModel> models = new ArrayList<>();
        for (RequestBuilder builder : builders) {
            models.add(builder.createStates(intervals, predicates, monitor));
        }

        return new TimeGraphModel(models);
    }

    private static @Nullable OutputElementStyle getStyleFor(IoOperationType type, @Nullable Integer size) {
        String typeStyle = null;
        String sizeStyle = null;
        switch(type) {
        case FLUSH:
            typeStyle = FLUSH_STYLE;
            break;
        case OTHER:
            typeStyle = OTHER_STYLE;
            break;
        case READ:
            typeStyle = READ_STYLE;
            if (size != null) {
                sizeStyle = SIZE_STYLE_PREFIX + Math.min(NB_SIZE_STYLES - 1, (int) (((double) size / MAX_SIZE) * NB_SIZE_STYLES));
            }
            break;
        case WRITE:
            typeStyle = WRITE_STYLE;
            if (size != null) {
                sizeStyle = SIZE_STYLE_PREFIX + Math.min(NB_SIZE_STYLES - 1, (int) (((double) size / MAX_SIZE) * NB_SIZE_STYLES));
            }
            break;
        default:
            return null;
        }
        String styleKey = sizeStyle == null ? typeStyle : sizeStyle + ',' + typeStyle;
        return STYLE_MAP.computeIfAbsent(styleKey, style -> new OutputElementStyle(style));
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // Extract time and ids from parameters
        List<Long> ids = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (ids == null) {
            // No ids specified
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        Map<Long, Integer> selectedEntries = getSelectedEntries(ids);
        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null || times.isEmpty()) {
            // No time specified
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }


        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        long start = times.get(0);
        if (ss == null || selectedEntries.size() != 1 || !getAnalysisModule().isQueryable(start)) {
            /*
             * We need the ss to query, we should only be querying one attribute and the
             * query times should be valid.
             */
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        List<Integer> quarks = new ArrayList<>();
        Map<String, String> retMap = new LinkedHashMap<>(1);

        Integer quark = selectedEntries.values().iterator().next();
        retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_Sector), StringUtils.EMPTY);
        quarks.add(quark);
        int sectorQuark = ss.optQuarkRelative(quark, Attributes.CURRENT_REQUEST);
        if (sectorQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
            retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_Sector), StringUtils.EMPTY);
            quarks.add(sectorQuark);
        }
        int sizeQuark = ss.optQuarkRelative(quark, Attributes.REQUEST_SIZE);
        if (sizeQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
            retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_NbSectors), StringUtils.EMPTY);
            quarks.add(sizeQuark);
        }
        try {
            for (ITmfStateInterval interval : ss.query2D(quarks, start, start)) {
                int attribute = interval.getAttribute();
                if (attribute == sectorQuark) {
                    Object value = interval.getValue();
                    if (value instanceof Long) {
                        retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_Sector), "0x" + Long.toHexString(Objects.requireNonNull((Long) value))); //$NON-NLS-1$
                    }
                } else if (attribute == sizeQuark) {
                    Object value = interval.getValue();
                    if (value instanceof Integer) {
                        retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_NbSectors), String.valueOf(value));
                    }
                } else if (attribute == quark) {
                    Object value = interval.getValue();
                    if (!(value instanceof Integer)) {
                        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
                    }
                    retMap.put(Objects.requireNonNull(Messages.DiskRequestDataProvider_RequestType), String.valueOf(IoOperationType.fromNumber((Integer) value)));
                }                           }
            return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (StateSystemDisposedException e) {
        }

        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STYLES), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

}
