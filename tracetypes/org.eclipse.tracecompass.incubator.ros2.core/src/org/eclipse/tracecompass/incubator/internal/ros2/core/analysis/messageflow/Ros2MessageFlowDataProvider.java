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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModelType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2CallbackTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesDataProvider.ArrowType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2PubTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2TakeTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.IRos2MessageFlowVisitor;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2CallbackPubMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2MessageFlowModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2MessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2MessageFlowTraverser;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2PublicationMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2SubCallbackMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2TimerCallbackMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2TransportMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2WaitMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Data provider for the ROS 2 Messages view.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2MessageFlowDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> {

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    private static final AtomicLong ATOMIC_LONG = new AtomicLong();

    private final @NonNull ITmfStateSystem fObjectsSs;
    private final @NonNull Ros2MessageFlowAnalysis fAnalysis;
    private final long fStartTime;
    private final long fEndTime;

    /** Map for object handle <-> entry ID */
    private final BiMap<Ros2ObjectHandle, Long> fObjectHandleToEntryId = HashBiMap.create();
    /** Map for trace name <-> entry ID */
    private final BiMap<String, Long> fTraceNameToEntryId = HashBiMap.create();

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysis
     *            the message flow analysis
     */
    public Ros2MessageFlowDataProvider(@NonNull ITmfTrace trace, @NonNull Ros2MessageFlowAnalysis analysis) {
        super(trace);
        fAnalysis = analysis;
        fObjectsSs = Objects.requireNonNull(TmfStateSystemAnalysisModule.getStateSystem(trace, Ros2ObjectsAnalysis.getFullAnalysisId()));
        fStartTime = trace.getStartTime().toNanos();
        fEndTime = trace.getEndTime().toNanos();
    }

    @Override
    public @NonNull String getId() {
        return getFullDataProviderId();
    }

    private long getTraceEntryId(String traceName) {
        return fTraceNameToEntryId.computeIfAbsent(traceName, i -> ATOMIC_LONG.getAndIncrement());
    }

    private long getObjectEntryId(Ros2ObjectHandle handle) {
        return fObjectHandleToEntryId.computeIfAbsent(handle, i -> ATOMIC_LONG.getAndIncrement());
    }

    /**
     * Message flow visitor for object aggregation. Collects ROS 2 objects and
     * creates time graph entry models.
     */
    private class MessageFlowObjectAggregator implements IRos2MessageFlowVisitor {

        private final long fRootId;
        private final @NonNull List<@NonNull TimeGraphEntryModel> fEntries;
        private final HashMap<@NonNull String, @NonNull Ros2NodeObject> fTraceNodes = new HashMap<>();
        private final HashSet<@NonNull Ros2ObjectHandle> fNodeObjectHandles = new HashSet<>();
        private final HashSet<@NonNull Ros2NodeObject> fNodeObjects = new HashSet<>();
        private final HashSet<@NonNull Ros2PublisherObject> fPublisherObjects = new HashSet<>();
        private final HashSet<@NonNull Ros2SubscriptionObject> fSubscriptionObjects = new HashSet<>();
        private final HashSet<@NonNull Ros2TimerObject> fTimerObjects = new HashSet<>();

        public MessageFlowObjectAggregator(long rootId, @NonNull List<@NonNull TimeGraphEntryModel> entries) {
            fRootId = rootId;
            fEntries = entries;
        }

        @Override
        public void visit(@NonNull Ros2MessageFlowSegment segment) {
            // Collect node handles
            if (!segment.isLink()) {
                fNodeObjectHandles.add(Objects.requireNonNull(segment.getNodeHandle()));
            }
            // Collect objects
            if (segment instanceof Ros2SubCallbackMessageFlowSegment) {
                fSubscriptionObjects.add(((Ros2SubCallbackMessageFlowSegment) segment).getSubscription());
            } else if (segment instanceof Ros2TimerCallbackMessageFlowSegment) {
                fTimerObjects.add(((Ros2TimerCallbackMessageFlowSegment) segment).getTimer());
            } else if (segment instanceof Ros2PublicationMessageFlowSegment) {
                fPublisherObjects.add(((Ros2PublicationMessageFlowSegment) segment).getPublisher());
            } else if (segment instanceof Ros2TransportMessageFlowSegment) {
                Ros2TransportMessageFlowSegment transportSegment = (Ros2TransportMessageFlowSegment) segment;
                fPublisherObjects.add(transportSegment.getPublisher());
                fSubscriptionObjects.add(transportSegment.getSubscription());
            } else if (segment instanceof Ros2CallbackPubMessageFlowSegment) {
                fPublisherObjects.add(((Ros2CallbackPubMessageFlowSegment) segment).getPublisher());
            } else if (segment instanceof Ros2WaitMessageFlowSegment) {
                Ros2WaitMessageFlowSegment waitSegment = (Ros2WaitMessageFlowSegment) segment;
                fSubscriptionObjects.add(waitSegment.getSubscription());
                fPublisherObjects.add(waitSegment.getPublisher());
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public void postVisit() {
            // Transform node handles into objects
            for (@NonNull
            Ros2ObjectHandle nodeHandle : fNodeObjectHandles) {
                Ros2NodeObject nodeObject = Ros2ObjectsUtil.getNodeObjectFromHandle(fObjectsSs, nodeHandle);
                if (null != nodeObject) {
                    fNodeObjects.add(nodeObject);
                    fTraceNodes.put(nodeObject.getTraceName(), nodeObject);
                }
            }
            // Create entry model for each trace
            for (Entry<@NonNull String, @NonNull Ros2NodeObject> entry : fTraceNodes.entrySet()) {
                long entryId = getTraceEntryId(entry.getKey());
                fEntries.add(new Ros2ObjectTimeGraphEntryModel(entryId, fRootId, fStartTime, fEndTime, Ros2ObjectTimeGraphEntryModelType.TRACE, entry.getValue()));
            }
            // Create entry model for each node
            for (@NonNull
            Ros2NodeObject nodeObject : fNodeObjects) {
                long entryId = getObjectEntryId(nodeObject.getHandle());
                long parentEntryId = getTraceEntryId(nodeObject.getTraceName());
                fEntries.add(new Ros2ObjectTimeGraphEntryModel(entryId, parentEntryId, fStartTime, fEndTime, Ros2ObjectTimeGraphEntryModelType.NODE, nodeObject));
            }
            // Create entry model for each publisher, subscription, timer
            for (@NonNull
            Ros2PublisherObject pubObject : fPublisherObjects) {
                long entryId = getObjectEntryId(pubObject.getHandle());
                long parentEntryId = getObjectEntryId(pubObject.getNodeHandle());
                fEntries.add(new Ros2ObjectTimeGraphEntryModel(entryId, parentEntryId, fStartTime, fEndTime, Ros2ObjectTimeGraphEntryModelType.PUBLISHER, pubObject));
            }
            for (@NonNull
            Ros2SubscriptionObject subObject : fSubscriptionObjects) {
                long entryId = getObjectEntryId(subObject.getHandle());
                long parentEntryId = getObjectEntryId(subObject.getNodeHandle());
                fEntries.add(new Ros2ObjectTimeGraphEntryModel(entryId, parentEntryId, fStartTime, fEndTime, Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION, subObject));
            }
            for (@NonNull
            Ros2TimerObject timerObject : fTimerObjects) {
                long entryId = getObjectEntryId(timerObject.getHandle());
                long parentEntryId = getObjectEntryId(timerObject.getNodeHandle());
                fEntries.add(new Ros2ObjectTimeGraphEntryModel(entryId, parentEntryId, fStartTime, fEndTime, Ros2ObjectTimeGraphEntryModelType.TIMER, timerObject));
            }
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> fetchTree(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        Ros2MessageFlowModel model = fAnalysis.getModel();
        if (null == model || !model.isDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        List<@NonNull TimeGraphEntryModel> entries = new ArrayList<>();
        long rootId = ATOMIC_LONG.getAndIncrement();
        entries.add(new TimeGraphEntryModel(rootId, -1, String.valueOf(getTrace().getName()), fStartTime, fEndTime));

        MessageFlowObjectAggregator aggregator = new MessageFlowObjectAggregator(rootId, entries);
        Ros2MessageFlowTraverser traverser = new Ros2MessageFlowTraverser(aggregator);
        traverser.traverse(model.getInitialSegment());

        return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), entries), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Message flow visitor for instance aggregation. Collects non-link message
     * flow segments and creates the corresponding time graph states.
     */
    private class MessageFlowRowModelCollector implements IRos2MessageFlowVisitor {

        private Multimap<@NonNull Ros2ObjectHandle, @NonNull ITimeGraphState> fEvents = HashMultimap.create();
        private @NonNull List<@NonNull ITimeGraphRowModel> fRows = new ArrayList<>();

        public @NonNull List<@NonNull ITimeGraphRowModel> getRows() {
            return fRows;
        }

        @Override
        public void visit(@NonNull Ros2MessageFlowSegment segment) {
            if (segment.isLink()) {
                return;
            }

            // Create corresponding time graph states
            // TODO apply filter
            if (segment instanceof Ros2SubCallbackMessageFlowSegment) {
                Ros2SubCallbackMessageFlowSegment subCallbackSegment = (Ros2SubCallbackMessageFlowSegment) segment;
                Ros2SubCallbackInstance subCallbackInstance = subCallbackSegment.getCallbackInstance();

                Ros2TakeTimeGraphState takeState = new Ros2TakeTimeGraphState(subCallbackInstance.getTakeInstance());
                fEvents.put(subCallbackInstance.getCallbackInstance().getOwnerHandle(), takeState);

                Ros2CallbackTimeGraphState callbackState = new Ros2CallbackTimeGraphState(subCallbackInstance.getCallbackInstance());
                fEvents.put(subCallbackInstance.getCallbackInstance().getOwnerHandle(), callbackState);
            } else if (segment instanceof Ros2TimerCallbackMessageFlowSegment) {
                Ros2TimerCallbackMessageFlowSegment timerCallbackSegment = (Ros2TimerCallbackMessageFlowSegment) segment;
                Ros2CallbackInstance timerCallbackInstance = timerCallbackSegment.getCallbackInstance();

                Ros2CallbackTimeGraphState timerCallbackState = new Ros2CallbackTimeGraphState(timerCallbackInstance);
                fEvents.put(timerCallbackInstance.getOwnerHandle(), timerCallbackState);
            } else if (segment instanceof Ros2PublicationMessageFlowSegment) {
                Ros2PublicationMessageFlowSegment pubSegment = (Ros2PublicationMessageFlowSegment) segment;
                Ros2PubInstance pubInstance = pubSegment.getPubInstance();

                Ros2PubTimeGraphState pubState = new Ros2PubTimeGraphState(segment.getStartTime(), segment.getEndTime() - segment.getStartTime() + 1, pubInstance);
                fEvents.put(pubInstance.getPublisherHandle(), pubState);
            }
        }

        @Override
        public void postVisit() {
            // Create rows
            for (Entry<@NonNull Ros2ObjectHandle, Collection<@NonNull ITimeGraphState>> entry : fEvents.asMap().entrySet()) {
                long entryId = getObjectEntryId(entry.getKey());
                List<@NonNull ITimeGraphState> eventList = new ArrayList<>(entry.getValue());
                fRows.add(new TimeGraphRowModel(entryId, eventList));
            }
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        Ros2MessageFlowModel model = fAnalysis.getModel();
        if (null == model || !model.isDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        @NonNull
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        MessageFlowRowModelCollector collector = new MessageFlowRowModelCollector();
        Ros2MessageFlowTraverser traverser = new Ros2MessageFlowTraverser(collector);
        traverser.traverse(model.getInitialSegment());

        return new TmfModelResponse<>(new TimeGraphModel(collector.getRows()), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Message flow visitor for arrows aggregation. Collects link message flow
     * segments and creates the corresponding time graph arrows.
     */
    private class MessageFlowArrowsCollector implements IRos2MessageFlowVisitor {

        private final long fStart;
        private final long fEnd;
        private final List<@NonNull ITimeGraphArrow> fArrows = new ArrayList<>();

        public List<@NonNull ITimeGraphArrow> getArrows() {
            return fArrows;
        }

        public MessageFlowArrowsCollector(long startTime, long endTime) {
            fStart = startTime;
            fEnd = endTime;
        }

        @Override
        public void visit(@NonNull Ros2MessageFlowSegment segment) {
            if (!segment.isLink()) {
                return;
            }
            if (!(segment.getStartTime() <= fEnd && segment.getEndTime() >= fStart)) {
                return;
            }

            // Collect arrows
            long sourceId;
            long destinationId;
            ArrowType arrowType;
            if (segment instanceof Ros2TransportMessageFlowSegment) {
                Ros2TransportMessageFlowSegment transportSegment = (Ros2TransportMessageFlowSegment) segment;
                sourceId = getObjectEntryId(transportSegment.getPublisher().getHandle());
                destinationId = getObjectEntryId(transportSegment.getSubscription().getHandle());
                arrowType = ArrowType.TRANSPORT;
            } else if (segment instanceof Ros2CallbackPubMessageFlowSegment) {
                Ros2CallbackPubMessageFlowSegment callbackPubSegment = (Ros2CallbackPubMessageFlowSegment) segment;
                sourceId = getObjectEntryId(callbackPubSegment.getSource().getHandle());
                destinationId = getObjectEntryId(callbackPubSegment.getPublisher().getHandle());
                arrowType = ArrowType.CALLBACK_PUB;
            } else if (segment instanceof Ros2WaitMessageFlowSegment) {
                Ros2WaitMessageFlowSegment waitSegment = (Ros2WaitMessageFlowSegment) segment;
                sourceId = getObjectEntryId(waitSegment.getSubscription().getHandle());
                destinationId = getObjectEntryId(waitSegment.getPublisher().getHandle());
                arrowType = ArrowType.WAIT;
            } else {
                throw new IllegalStateException();
            }

            long time = segment.getStartTime();
            long duration = segment.getEndTime() - segment.getStartTime();
            fArrows.add(new TimeGraphArrow(sourceId, destinationId, time, duration, arrowType.getId()));
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }

        Ros2MessageFlowModel model = fAnalysis.getModel();
        if (null == model || !model.isDone()) {
            return new TmfModelResponse<>(null, Status.RUNNING, CommonStatusMessage.RUNNING);
        }

        MessageFlowArrowsCollector arrowsCollector = new MessageFlowArrowsCollector(filter.getStart(), filter.getEnd());
        Ros2MessageFlowTraverser traverser = new Ros2MessageFlowTraverser(arrowsCollector);
        traverser.traverse(model.getInitialSegment());

        return new TmfModelResponse<>(arrowsCollector.getArrows(), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * @return the full data provider ID
     */
    public static @NonNull String getFullDataProviderId() {
        return Ros2MessageFlowAnalysis.getFullAnalysisId() + Ros2MessageFlowDataProvider.SUFFIX;
    }
}
