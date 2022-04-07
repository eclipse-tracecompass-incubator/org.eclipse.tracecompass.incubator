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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModelType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TimerCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Data provider for the ROS 2 Messages view.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2MessagesDataProvider extends AbstractTimeGraphDataProvider<@NonNull Ros2MessagesAnalysis, @NonNull TimeGraphEntryModel> {

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    // Maps object handle to time graph row model entry/row ID
    private Map<@NonNull Ros2ObjectHandle, Long> fHandleToIdMap = Maps.newHashMap();
    private final ITmfStateSystem fObjectsSs;

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Ros2MessagesDataProvider(@NonNull ITmfTrace trace, @NonNull Ros2MessagesAnalysis analysisModule) {
        super(trace, analysisModule);
        fObjectsSs = TmfStateSystemAnalysisModule.getStateSystem(trace, Ros2ObjectsAnalysis.getFullAnalysisId());
    }

    /**
     * Arrow type for links.
     *
     * Int IDs start at 1 to avoid being equal to the default int value of
     * TimeGraphState/TimeGraphArrow (0).
     *
     * @author Christophe Bedard
     */
    public enum ArrowType {
        /** Transport link (pub->sub over network) */
        TRANSPORT(1),
        /** Callback-publication link */
        CALLBACK_PUB(2),
        /** Wait link */
        WAIT(3);

        private final int id;

        private ArrowType(int id) {
            this.id = id;
        }

        /**
         * @return the corresponding int ID
         */
        public int getId() {
            return id;
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        return new TmfModelResponse<>(getArrows(filter.getStart(), filter.getEnd()), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private @Nullable List<@NonNull ITimeGraphArrow> getArrows(long startTime, long endTime) {
        ITmfStateSystem messagesSs = getAnalysisModule().getStateSystem();
        if (null == messagesSs) {
            return null;
        }
        List<@NonNull ITimeGraphArrow> arrows = Lists.newArrayList();

        // Transport links
        Iterator<@NonNull Ros2MessageTransportInstance> filteredTransportLinks = Ros2MessagesUtil.getTransportInstances(messagesSs, startTime, endTime).iterator();
        while (filteredTransportLinks.hasNext()) {
            Ros2MessageTransportInstance transportInstance = filteredTransportLinks.next();
            Long sourceId = fHandleToIdMap.get(transportInstance.getPublisherHandle());
            Long destinationId = fHandleToIdMap.get(transportInstance.getSubscriptionHandle());
            if (null != sourceId && null != destinationId) {
                long time = transportInstance.getSourceTimestamp();
                long duration = transportInstance.getDestinationTimestamp() - transportInstance.getSourceTimestamp();
                arrows.add(new TimeGraphArrow(sourceId, destinationId, time, duration, ArrowType.TRANSPORT.id));
            }
        }

        // Pub links
        Iterator<@NonNull Ros2CallbackPublicationInstance> filteredCallbackPubLinks = Ros2MessagesUtil.getCallbackPublicationInstances(messagesSs, startTime, endTime).iterator();
        while (filteredCallbackPubLinks.hasNext()) {
            Ros2CallbackPublicationInstance callbackPublicationInstance = filteredCallbackPubLinks.next();
            Long sourceId = fHandleToIdMap.get(callbackPublicationInstance.getCallbackOwnerHandle());
            Long destinationId = fHandleToIdMap.get(callbackPublicationInstance.getPublisherHandle());
            if (null != sourceId && null != destinationId) {
                long time = callbackPublicationInstance.getPublicationTimestamp();
                long duration = 0;
                arrows.add(new TimeGraphArrow(sourceId, destinationId, time, duration, ArrowType.CALLBACK_PUB.id));
            }
        }
        return arrows;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull String getId() {
        return getFullDataProviderId();
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

        // Query
        queryIntervals(ss, intervals, entries, filter, monitor);
        if (monitor != null && monitor.isCanceled()) {
            return new TimeGraphModel(Collections.emptyList());
        }

        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = getPredicates(parameters);
        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<@NonNull Long, @NonNull Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            addRows(rows, entry, intervals, predicates, monitor);
        }
        return new TimeGraphModel(rows);
    }

    private static void queryIntervals(@NonNull ITmfStateSystem ss, TreeMultimap<Integer, ITmfStateInterval> intervals, Map<@NonNull Long, @NonNull Integer> entries, @NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor)
            throws StateSystemDisposedException {
        Collection<@NonNull Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        @SuppressWarnings("null")
        Collection<Integer> valuesNull = entries.values();
        @SuppressWarnings("null")
        Collection<Long> timesNull = times;

        for (ITmfStateInterval interval : ss.query2D(valuesNull, timesNull)) {
            if (monitor != null && monitor.isCanceled()) {
                return;
            }
            intervals.put(interval.getAttribute(), interval);
        }
    }

    private @NonNull Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> getPredicates(@NonNull Map<@NonNull String, @NonNull Object> parameters) {
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }
        return predicates;
    }

    private void addRows(List<@NonNull ITimeGraphRowModel> rows, Map.Entry<@NonNull Long, @NonNull Integer> entry, TreeMultimap<Integer, ITmfStateInterval> intervals,
            @NonNull Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates, @Nullable IProgressMonitor monitor) {
        List<@NonNull ITimeGraphState> eventList = new ArrayList<>();
        for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
            addRow(entry, predicates, monitor, eventList, interval);
        }
        rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
    }

    private void addRow(Map.Entry<@NonNull Long, @NonNull Integer> entry, @NonNull Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates, @Nullable IProgressMonitor monitor,
            @NonNull List<@NonNull ITimeGraphState> eventList, ITmfStateInterval interval) {
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + 1;
        Object valObject = interval.getValue();
        if (valObject instanceof Ros2NodeObject) {
            // Node
            Ros2NodeObject nodeObject = (Ros2NodeObject) valObject;

            Ros2NodeTimeGraphState nodeState = new Ros2NodeTimeGraphState(startTime, duration, nodeObject);
            applyFilterAndAddState(eventList, nodeState, entry.getKey(), predicates, monitor);
        } else if (valObject instanceof Ros2PubInstance) {
            // Publication
            Ros2PubInstance pubInstance = (Ros2PubInstance) valObject;
            Ros2PubTimeGraphState pubState = new Ros2PubTimeGraphState(startTime, duration, pubInstance);
            applyFilterAndAddState(eventList, pubState, entry.getKey(), predicates, monitor);

            fHandleToIdMap.put(pubInstance.getPublisherHandle(), entry.getKey());
        } else if (valObject instanceof Ros2SubCallbackInstance) {
            // Subscription callback
            Ros2SubCallbackInstance subCallbackInstance = (Ros2SubCallbackInstance) valObject;

            Ros2TakeInstance takeInstance = subCallbackInstance.getTakeInstance();
            Ros2TakeTimeGraphState takeState = new Ros2TakeTimeGraphState(takeInstance);
            applyFilterAndAddState(eventList, takeState, entry.getKey(), predicates, monitor);

            Ros2CallbackInstance callbackInstance = subCallbackInstance.getCallbackInstance();
            Ros2CallbackTimeGraphState callbackState = new Ros2CallbackTimeGraphState(callbackInstance);
            applyFilterAndAddState(eventList, callbackState, entry.getKey(), predicates, monitor);

            fHandleToIdMap.put(takeInstance.getSubscriptionHandle(), entry.getKey());
        } else if (valObject instanceof Ros2TimerCallbackInstance) {
            // Timer callback
            Ros2TimerCallbackInstance timerCallbackInstance = (Ros2TimerCallbackInstance) valObject;
            Ros2CallbackInstance callbackInstance = timerCallbackInstance.getCallbackInstance();
            Ros2CallbackTimeGraphState callbackState = new Ros2CallbackTimeGraphState(callbackInstance);
            applyFilterAndAddState(eventList, callbackState, entry.getKey(), predicates, monitor);

            fHandleToIdMap.put(timerCallbackInstance.getTimerHandle(), entry.getKey());
        }
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
            addChildrenEntryModel(ss, builder, quark, parentId, child);
        }
    }

    private void addChildrenEntryModel(ITmfStateSystem ss, Builder<@NonNull TimeGraphEntryModel> builder, int quark, long parentId, Integer child) {
        long childId = getId(child);
        String name = ss.getAttributeName(child);
        String parentName = quark != ITmfStateSystem.ROOT_ATTRIBUTE ? ss.getAttributeName(quark) : StringUtils.EMPTY;
        if (ITmfStateSystem.ROOT_ATTRIBUTE == quark) {
            if (addEntryModel(ss, builder, childId, parentId, child, Ros2ObjectTimeGraphEntryModelType.TRACE)) {
                addChildren(ss, builder, child, childId);
            }
        } else if (parentName.equals(Ros2MessagesUtil.LIST_NODES)) {
            if (addEntryModel(ss, builder, childId, parentId, child, Ros2ObjectTimeGraphEntryModelType.NODE)) {
                addChildren(ss, builder, child, childId);
            }
        } else if (parentName.equals(Ros2MessagesUtil.LIST_PUBLISHERS)) {
            addEntryModel(ss, builder, childId, parentId, child, Ros2ObjectTimeGraphEntryModelType.PUBLISHER);
        } else if (parentName.equals(Ros2MessagesUtil.LIST_SUBSCRIPTIONS)) {
            addEntryModel(ss, builder, childId, parentId, child, Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION);
        } else if (parentName.equals(Ros2MessagesUtil.LIST_TIMERS)) {
            addEntryModel(ss, builder, childId, parentId, child, Ros2ObjectTimeGraphEntryModelType.TIMER);
        } else if (name.equals(Ros2MessagesUtil.LIST_NODES) || name.equals(Ros2MessagesUtil.LIST_PUBLISHERS) || name.equals(Ros2MessagesUtil.LIST_SUBSCRIPTIONS) || name.equals(Ros2MessagesUtil.LIST_TIMERS)) {
            /**
             * Skip this attribute: don't add an entry model, but do proceed
             * with children, effectively skipping a layer in the state system
             * attribute tree.
             */
            addChildren(ss, builder, child, parentId);
        } else {
            builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), true));
            addChildren(ss, builder, child, childId);
        }
    }

    private boolean addEntryModel(ITmfStateSystem ss, Builder<@NonNull TimeGraphEntryModel> builder, long id, long parentId, int quark, Ros2ObjectTimeGraphEntryModelType type) {
        switch (type) {
        case TRACE:
            @Nullable
            Ros2NodeObject traceNodeObject = getNodeFromTrace(ss, quark);
            if (null != traceNodeObject) {
                builder.add(new Ros2ObjectTimeGraphEntryModel(id, parentId, ss.getStartTime(), ss.getCurrentEndTime(), Ros2ObjectTimeGraphEntryModelType.TRACE, traceNodeObject));
                return true;
            }
            break;
        case NODE:
            @Nullable
            Ros2NodeObject nodeObject = getNodeObject(ss, quark);
            if (null != nodeObject) {
                builder.add(new Ros2ObjectTimeGraphEntryModel(id, parentId, ss.getStartTime(), ss.getCurrentEndTime(), Ros2ObjectTimeGraphEntryModelType.NODE, nodeObject));
                return true;
            }
            break;
        case PUBLISHER:
            @Nullable
            Ros2PublisherObject publisherObject = getPublisherObject(ss, quark);
            if (null != publisherObject) {
                builder.add(new Ros2ObjectTimeGraphEntryModel(id, parentId, ss.getStartTime(), ss.getCurrentEndTime(), Ros2ObjectTimeGraphEntryModelType.PUBLISHER, publisherObject));
                return true;
            }
            break;
        case SUBSCRIPTION:
            @Nullable
            Ros2SubscriptionObject subscriptionObject = getSubscriptionObject(ss, quark);
            if (null != subscriptionObject) {
                builder.add(new Ros2ObjectTimeGraphEntryModel(id, parentId, ss.getStartTime(), ss.getCurrentEndTime(), Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION, subscriptionObject));
                return true;
            }
            break;
        case TIMER:
            @Nullable
            Ros2TimerObject timerObject = getTimerObject(ss, quark);
            if (null != timerObject) {
                builder.add(new Ros2ObjectTimeGraphEntryModel(id, parentId, ss.getStartTime(), ss.getCurrentEndTime(), Ros2ObjectTimeGraphEntryModelType.TIMER, timerObject));
                return true;
            }
            break;
        default:
            break;
        }
        return false;
    }

    private static @Nullable Ros2NodeObject getNodeFromTrace(ITmfStateSystem ss, int traceQuark) {
        /**
         * Just get the first node quark under this trace, since they should all
         * provide the same information.
         */
        List<@NonNull Integer> nodeQuarks = ss.getQuarks(traceQuark, Ros2MessagesUtil.LIST_NODES, "*"); //$NON-NLS-1$
        if (nodeQuarks.isEmpty()) {
            Activator.getInstance().logError("there should be at least one node under a trace quark"); //$NON-NLS-1$
            return null;
        }
        return getNodeObject(ss, nodeQuarks.get(0));
    }

    private static @Nullable Ros2NodeObject getNodeObject(ITmfStateSystem ss, int quark) {
        try {
            // Get node handle from a time graph state
            Iterable<@NonNull ITmfStateInterval> query2d = ss.query2D(Collections.singleton(quark), ss.getStartTime(), ss.getCurrentEndTime());
            for (ITmfStateInterval iTmfStateInterval : query2d) {
                if (iTmfStateInterval.getValue() instanceof String) {
                    return null;
                }
                @Nullable
                Ros2NodeObject nodeObject = (Ros2NodeObject) iTmfStateInterval.getValue();
                if (null != nodeObject) {
                    return nodeObject;
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("could not get node object for entry model"); //$NON-NLS-1$
        return null;
    }

    private @Nullable Ros2PublisherObject getPublisherObject(ITmfStateSystem ss, int quark) {
        try {
            // Get publisher handle from a time graph state
            Iterable<@NonNull ITmfStateInterval> query2d = ss.query2D(Collections.singleton(quark), ss.getStartTime(), ss.getCurrentEndTime());
            for (ITmfStateInterval iTmfStateInterval : query2d) {
                @Nullable
                Ros2PubInstance pubInstance = (Ros2PubInstance) iTmfStateInterval.getValue();
                if (null != pubInstance) {
                    return Ros2ObjectsUtil.getPublisherObjectFromHandle(fObjectsSs, ss.getCurrentEndTime(), pubInstance.getPublisherHandle());
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("could not get publisher object for entry model"); //$NON-NLS-1$
        return null;
    }

    private @Nullable Ros2SubscriptionObject getSubscriptionObject(ITmfStateSystem ss, int quark) {
        try {
            // Get subscription handle from a time graph state
            Iterable<@NonNull ITmfStateInterval> query2d = ss.query2D(Collections.singleton(quark), ss.getStartTime(), ss.getCurrentEndTime());
            for (ITmfStateInterval iTmfStateInterval : query2d) {
                if (iTmfStateInterval.getValue() instanceof String) {
                    return null;
                }
                @Nullable
                Ros2SubCallbackInstance subCallbackInstance = (Ros2SubCallbackInstance) iTmfStateInterval.getValue();
                if (null != subCallbackInstance) {
                    return Ros2ObjectsUtil.getSubscriptionObjectFromHandle(fObjectsSs, ss.getCurrentEndTime(), subCallbackInstance.getTakeInstance().getSubscriptionHandle());
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("could not get subscription object for entry model"); //$NON-NLS-1$
        return null;
    }

    private @Nullable Ros2TimerObject getTimerObject(ITmfStateSystem ss, int quark) {
        try {
            // Get timer handle from a time graph state
            Iterable<@NonNull ITmfStateInterval> query2d = ss.query2D(Collections.singleton(quark), ss.getStartTime(), ss.getCurrentEndTime());
            for (ITmfStateInterval iTmfStateInterval : query2d) {
                @Nullable
                Ros2TimerCallbackInstance timerCallbackInstance = (Ros2TimerCallbackInstance) iTmfStateInterval.getValue();
                if (null != timerCallbackInstance) {
                    return Ros2ObjectsUtil.getTimerObjectFromHandle(fObjectsSs, ss.getCurrentEndTime(), timerCallbackInstance.getTimerHandle());
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("could not get subscription object for entry model"); //$NON-NLS-1$
        return null;
    }

    /**
     * @return the full dataprovider ID
     */
    public static @NonNull String getFullDataProviderId() {
        return Ros2MessagesAnalysis.getFullAnalysisId() + Ros2MessagesDataProvider.SUFFIX;
    }
}
