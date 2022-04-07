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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostThread;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTimestamp;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TimerCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Streams;

/**
 * State provider for the ROS 2 Messages analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesStateProvider extends AbstractRos2StateProvider {

    private static final int VERSION_NUMBER = 0;

    private final ITmfStateSystem fObjectsSs;
    private boolean fInitialSetupDone = false;

    // Publications
    private Collection<HostProcessPointer> fKnownDdsWriters = new ArrayList<>();
    private Map<HostProcessPointer, ITmfEvent> fRclcppPublishEvents = Maps.newHashMap();
    private Map<HostProcessPointer, ITmfEvent> fRclPublishEvents = Maps.newHashMap();
    private Map<HostProcessPointer, ITmfEvent> fDdsWritePreEvents = Maps.newHashMap();
    // Message takes
    private Map<HostProcessPointer, ITmfEvent> fRmwTakeEvents = Maps.newHashMap();
    private Map<Ros2ObjectHandle, Ros2TakeInstance> fTakeInstances = Maps.newHashMap();
    // Callback instances
    private Map<HostProcessPointer, ITmfEvent> fCallbackStartEvents = Maps.newHashMap();
    private Multimap<HostThread, Pair<@NonNull Ros2ObjectHandle, @NonNull Long>> fCallbackPublications = MultimapBuilder.hashKeys().arrayListValues().build();
    // Pub-sub links
    private Map<Ros2MessageTimestamp, Pair<@NonNull Ros2ObjectHandle, @NonNull Long>> fPublications = Maps.newHashMap();

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param objectsSs
     *            the objects state system
     */
    public Ros2MessagesStateProvider(ITmfTrace trace, ITmfStateSystem objectsSs) {
        super(trace, Ros2MessagesAnalysis.getFullAnalysisId());
        fObjectsSs = objectsSs;
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new Ros2MessagesStateProvider(getTrace(), fObjectsSs);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
        long timestamp = event.getTimestamp().toNanos();

        if (!fInitialSetupDone) {
            fInitialSetupDone = true;
            createObjects(ss);
        }

        eventHandleHelpers(event);
        eventHandlePublish(event, ss, timestamp);
        eventHandleTake(ss, event, timestamp);
        eventHandleCallback(event, ss, timestamp);
    }

    private void eventHandleHelpers(@NonNull ITmfEvent event) {
        // dds:create_writer
        if (isEvent(event, LAYOUT.eventDdsCreateWriter())) {
            /**
             * Many DDS writers are internal to:
             *
             * <pre>
             *   (1) the RMW implementation; and
             *   (2) the DDS implementation.
             * </pre>
             *
             * To simplify processing of DDS write-related events (i.e.,
             * dds:write), we keep a list of known/"valid" DDS writers and only
             * consider events concerning those DDS writers.
             *
             * For (1), we can filter out DDS writers using the topic name. Most
             * DDS writer topic names have prefixes. These prefixes are
             * implementation details and are not part of the actual ROS 2 topic
             * name:
             *
             * <pre>
             *   * "rt" for publication/subscription topics
             *   * "rq" and "rr" for service request and service reply topics, respectively
             * </pre>
             *
             * Some DDS writers with a topic name not starting with those
             * prefixes are internal to RMW:
             *
             * <pre>
             *   * "ros_discovery_info" for internal discovery in RMW (per RMW context)
             * </pre>
             *
             * For (2), we do not actually get dds:create_writer events for
             * those DDS writers (at least currently), so just going with an
             * allow-list allows us to filter those out.
             */
            String topicName = (String) getField(event, LAYOUT.fieldTopicName());
            if (!topicName.equals("ros_discovery_info")) { //$NON-NLS-1$
                HostProcessPointer writer = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldWriter()));
                fKnownDdsWriters.add(writer);
            }
        }
    }

    private void eventHandlePublish(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        eventHandlePublishRos2(event);

        eventHandlePublishDds(event, ss, timestamp);
    }

    private void eventHandlePublishRos2(@NonNull ITmfEvent event) {
        // rclcpp_publish
        if (isEvent(event, LAYOUT.eventRclcppPublish())) {
            HostProcessPointer message = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldMessage()));

            // Add to temporary map
            fRclcppPublishEvents.put(message, event);
        }
        // rcl_publish
        else if (isEvent(event, LAYOUT.eventRclPublish())) {
            HostProcessPointer message = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldMessage()));

            // Add to temporary map
            fRclPublishEvents.put(message, event);
        }
        // TODO rmw_publish, use rmw-level timestamp
    }

    private void eventHandlePublishDds(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // dds:write_pre
        if (isEvent(event, LAYOUT.eventDdsWritePre())) {
            HostProcessPointer writer = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldWriter()));

            // Add to temporary map
            fDdsWritePreEvents.put(writer, event);
        }
        // dds:write
        else if (isEvent(event, LAYOUT.eventDdsWrite())) {
            eventHandlePublishDdsWrite(event, ss, timestamp);
        }
    }

    private void eventHandlePublishDdsWrite(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // First check if we know the DDS writer
        HostProcessPointer writer = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldWriter()));
        if (!fKnownDdsWriters.contains(writer)) {
            return;
        }

        HostProcessPointer message = getDdsMessage(event, writer);
        if (null == message) {
            return;
        }

        /**
         * Get corresponding rcl_publish event, since it's the main r*_publish
         * event.
         */
        ITmfEvent rclPublish = fRclPublishEvents.remove(message);
        if (null == rclPublish) {
            Activator.getInstance().logError("could not find corresponding rcl_publish event for message=" + message.toString()); //$NON-NLS-1$
            return;
        }
        Ros2ObjectHandle publisherHandle = handleFrom(rclPublish, (long) getField(rclPublish, LAYOUT.fieldPublisherHandle()));
        Ros2PublisherObject publisherObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(fObjectsSs, timestamp, publisherHandle);
        if (null == publisherObject) {
            /**
             * FIXME this happens with publishers for /rosout for some reason.
             * Seems like they're not getting detected by the objects analysis.
             */
            Activator.getInstance().logError("could not find publisher object for publisher handle=0x" + Long.toHexString(publisherHandle.getHandle())); //$NON-NLS-1$
            return;
        }

        // Get corresponding rclcpp_publish event
        ITmfEvent rclcppPublish = fRclcppPublishEvents.remove(message);

        addPublicationInstance(event, ss, timestamp, publisherObject, message, rclcppPublish);
    }

    private HostProcessPointer getDdsMessage(@NonNull ITmfEvent event, HostProcessPointer writer) {
        /**
         * Currently, with the Fast DDS instrumentation, the dds:write event
         * does not contain the message/data pointer. We need to get it from a
         * separate previous event, dds:write_pre.
         */
        if (hasField(event, LAYOUT.fieldData())) {
            return hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldData()));
        }
        ITmfEvent ddsWritePre = fDdsWritePreEvents.remove(writer);
        if (null == ddsWritePre) {
            Activator.getInstance().logError("could not get corresponding dds:write_pre event for writer=0x" + Long.toHexString(writer.getPointer())); //$NON-NLS-1$
            return null;
        }
        return hostProcessPointerFrom(ddsWritePre, (long) getField(ddsWritePre, LAYOUT.fieldData()));
    }

    private void addPublicationInstance(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, Ros2PublisherObject publisherObject, @NonNull HostProcessPointer message, ITmfEvent rclcppPublish) {
        long sourceTimestamp = (long) getField(event, LAYOUT.fieldTimestamp());
        long tid = getTid(event);

        /**
         * Some rcl_publish events (e.g., for internal rcl-level publisher) do
         * not have a corresponding rclcpp_publish event.
         */
        if (null == rclcppPublish) {
            // TODO support this, using the rcl_publish timestamp instead
            return;
        }
        long pubTimestamp = rclcppPublish.getTimestamp().toNanos();

        Integer pubQuark = Ros2MessagesUtil.getPublisherQuarkAndAdd(ss, fObjectsSs, timestamp, publisherObject.getHandle());
        if (null == pubQuark) {
            return;
        }
        // Mark publication event using state from rclcpp_pub->dds:write
        Ros2PubInstance pubInstance = new Ros2PubInstance(publisherObject.getHandle(), tid, message, sourceTimestamp);
        ss.modifyAttribute(pubTimestamp, pubInstance, pubQuark);
        ss.modifyAttribute(timestamp, null, pubQuark);

        // Keep pub event for pub-sub links
        /**
         * TODO match using publisher GID when rmw_cyclonedds correctly supports
         * it.
         */
        Ros2MessageTimestamp messageSourceTimestamp = new Ros2MessageTimestamp(sourceTimestamp, publisherObject.getTopicName());
        fPublications.put(messageSourceTimestamp, new Pair<>(publisherObject.getHandle(), timestamp));

        // Add publication to multimap for in-callback links
        fCallbackPublications.put(hostThreadFrom(event), new Pair<>(publisherObject.getHandle(), pubTimestamp));
    }

    private void eventHandleTake(@NonNull ITmfStateSystemBuilder ss, @NonNull ITmfEvent event, long timestamp) {
        // rmw_take
        if (isEvent(event, LAYOUT.eventRmwTake())) {
            HostProcessPointer message = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldMessage()));

            // Add to temporary map
            fRmwTakeEvents.put(message, event);

            return;
        }

        // TODO rcl_take, use rcl-level timestamp

        // rclcpp_take
        if (isEvent(event, LAYOUT.eventRclcppTake())) {
            HostProcessPointer message = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldMessage()));
            long tid = getTid(event);

            // Get info from map
            ITmfEvent rmwTake = fRmwTakeEvents.remove(message);
            if (null == rmwTake) {
                Activator.getInstance().logError("could not find corresponding rmw_take event for message=" + message.toString()); //$NON-NLS-1$
                return;
            }
            /**
             * TODO sometimes (but rarely) the source timestamp from the
             * rmw_take tracepoint is 0. It seems to happen at the very end of a
             * trace, so it's probably internal cleanup and we could therefore
             * ignore this case.
             */
            long sourceTimestamp = (long) getField(rmwTake, LAYOUT.fieldSourceTimestamp());
            Ros2ObjectHandle rmwSubscriptionHandle = handleFrom(rmwTake, (long) getField(rmwTake, LAYOUT.fieldRmwSubscriptionHandle()));
            Long rmwTakeTimestamp = rmwTake.getTimestamp().toNanos();
            // TODO use/keep taken flag

            // Get corresponding subscription handle and topic name
            Ros2ObjectHandle subscriptionHandle = Ros2ObjectsUtil.getSubscriptionHandleFromRmwSubscriptionHandle(fObjectsSs, timestamp, rmwSubscriptionHandle);
            if (null == subscriptionHandle) {
                Activator.getInstance().logError("could not find subscription handle for rmw subscription handle"); //$NON-NLS-1$
                return;
            }
            Ros2SubscriptionObject subscriptionObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(fObjectsSs, timestamp, subscriptionHandle);
            if (null == subscriptionObject) {
                Activator.getInstance().logError("could not find subscription object for subscription handle"); //$NON-NLS-1$
                return;
            }

            // Create take instance object and add it to temporary map
            Ros2TakeInstance takeInstance = new Ros2TakeInstance(subscriptionHandle, tid, message, sourceTimestamp, rmwTakeTimestamp, timestamp);
            fTakeInstances.put(subscriptionHandle, takeInstance);

            /*
             * Get pub event without removing from map, since the same message
             * can be received by more than 1 subscription.
             *
             * TODO drop elements from map after some time, in case the map gets
             * too big?
             */
            Ros2MessageTimestamp messageSourceTimestamp = new Ros2MessageTimestamp(sourceTimestamp, subscriptionObject.getTopicName());
            Pair<@NonNull Ros2ObjectHandle, @NonNull Long> sourcePubInfo = fPublications.get(messageSourceTimestamp);
            if (null == sourcePubInfo) {
                Activator.getInstance().logError("could not find corresponding message publication for sourceTimestamp=[" + messageSourceTimestamp.toString() + "] for trace=" + getTrace().getName()); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            Ros2ObjectHandle sourcePubHandle = sourcePubInfo.getFirst();
            Long sourcePubTimestamp = sourcePubInfo.getSecond();

            Ros2MessageTransportInstance transportInstance = new Ros2MessageTransportInstance(sourcePubHandle, subscriptionHandle, sourcePubTimestamp, rmwTakeTimestamp);
            addTransportInstance(ss, transportInstance);
        }
    }

    private static void addTransportInstance(ITmfStateSystemBuilder ss, @NonNull Ros2MessageTransportInstance transportInstance) {
        /**
         * Transport instances are stored in sub-attributes of the
         * `publisher-subscriber_pair` sub-attribute. Different sub-attributes
         * might be used for transport instances of the same
         * publisher-subscriber pair. This is because the transport instance can
         * only be created at its completion time, when the message is
         * read/taken from the subscriber, and therefore has to be back-dated
         * during creation. This is only possible if the (sub-)attribute has not
         * already been modified during the lifetime of the transport instance.
         *
         * The sub-attributes are named with increasing integers and form a
         * list. New transport instances use the first free sub-attribute,
         * creating a new sub-attribute if necessary.
         */

        int transportInstanceQuark = Ros2MessagesUtil.getTransportInstanceQuarkAndAdd(ss, transportInstance.getPublisherHandle(), transportInstance.getSubscriptionHandle());
        long sourceTimestamp = transportInstance.getSourceTimestamp();
        long destinationTimestamp = transportInstance.getDestinationTimestamp();

        // Get list of all sub-quarks
        List<@NonNull Integer> subQuarks = ss.getSubAttributes(transportInstanceQuark, false);

        /**
         * Find first sub-attribute (starting from 1) which has room for the
         * whole pub->read/take time interval of the transport instance.
         */
        Integer freeQuark = null;
        for (Integer subQuark : subQuarks) {
            Iterable<@NonNull ITmfStateInterval> intervals;
            try {
                intervals = ss.query2D(Collections.singleton(subQuark), sourceTimestamp, destinationTimestamp);
            } catch (StateSystemDisposedException e) {
                continue;
            }
            /**
             * We need to make sure all these state intervals have null values.
             * This would then mean that the attribute corresponding to this
             * (sub-)quark has room for a new state interval covering the whole
             * time range of the transport instance: [pub timestamp, read/take
             * timestamp].
             *
             * The query2D() call can a single null state interval with
             * [start,end] strictly inside the [pub,read/take] interval (i.e.,
             * pub<start and end<read/take), so we need to filter those out.
             */
            Predicate<ITmfStateInterval> compatible = (interval) -> {
                return interval.getValue() == null
                        && !(sourceTimestamp < interval.getStartTime() && interval.getEndTime() < destinationTimestamp);
            };
            if (Streams.stream(intervals).allMatch(compatible)) {
                freeQuark = subQuark;
                break;
            }
        }

        // If no free attribute is available, create a new quark
        if (null == freeQuark) {
            freeQuark = ss.getQuarkRelativeAndAdd(transportInstanceQuark, Integer.toString(subQuarks.size() + 1));
        }
        ss.modifyAttribute(sourceTimestamp, transportInstance, freeQuark);
        ss.modifyAttribute(destinationTimestamp, null, freeQuark);
    }

    private void eventHandleCallback(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // callback_start
        if (isEvent(event, LAYOUT.eventCallbackStart())) {
            eventHandleCallbackStart(event);
        }
        // callback_end
        else if (isEvent(event, LAYOUT.eventCallbackEnd())) {
            eventHandleCallbackEnd(event, ss, timestamp);
        }
    }

    private void eventHandleCallbackStart(@NonNull ITmfEvent event) {
        long callback = (long) getField(event, LAYOUT.fieldCallback());

        // Add to temporary map
        fCallbackStartEvents.put(hostProcessPointerFrom(event, callback), event);

        /**
         * Reset map that collects message publications, since we don't want the
         * publication events that happened between callback_end and
         * callback_start, i.e., outside of the callback.
         */
        fCallbackPublications.removeAll(hostThreadFrom(event));
    }

    private void eventHandleCallbackEnd(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        HostProcessPointer callback = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldCallback()));

        // Get corresponding callback_start event
        ITmfEvent callbackStart = fCallbackStartEvents.remove(callback);
        if (null == callbackStart) {
            Activator.getInstance().logError("could not find corresponding callback_start for callback=" + callback.toString()); //$NON-NLS-1$
            return;
        }

        // Find owner of callback
        Pair<@NonNull Ros2ObjectHandle, Ros2CallbackType> callbackOwnerHandle = Ros2ObjectsUtil.getCallbackOwnerHandle(fObjectsSs, timestamp, callback);
        if (null != callbackOwnerHandle) {
            Long startTimestamp = callbackStart.getTimestamp().toNanos();
            Boolean isIntraProcess = 1 == (long) getField(callbackStart, LAYOUT.fieldIsIntraProcess());
            long tid = getTid(callbackStart);
            Ros2ObjectHandle ownerHandle = callbackOwnerHandle.getFirst();
            Ros2CallbackType callbackType = callbackOwnerHandle.getSecond();
            if (callbackType.equals(Ros2CallbackType.SUBSCRIPTION)) {
                addSubscriptionCallback(ss, timestamp, startTimestamp, isIntraProcess, tid, ownerHandle);
            } else if (callbackType.equals(Ros2CallbackType.TIMER)) {
                addTimerCallback(ss, timestamp, startTimestamp, isIntraProcess, tid, ownerHandle);
            }

            addCallbackPublications(ss, callbackStart, ownerHandle, callbackType);

            return;
        }

        Activator.getInstance().logError("could not find callback owner for callback=" + callback.toString()); //$NON-NLS-1$
    }

    private void addSubscriptionCallback(ITmfStateSystemBuilder ss, long timestamp, long startTimestamp, boolean isIntraProcess, long tid, @NonNull Ros2ObjectHandle subHandle) {
        // Get take instance from map
        Ros2TakeInstance takeInstance = fTakeInstances.remove(subHandle);
        if (null == takeInstance) {
            Activator.getInstance().logError("could not find corresponding take instance"); //$NON-NLS-1$
            return;
        }
        Ros2CallbackInstance callbackInstance = new Ros2CallbackInstance(subHandle, tid, isIntraProcess, startTimestamp, timestamp);
        Ros2SubCallbackInstance subCallbackInstance = new Ros2SubCallbackInstance(takeInstance, callbackInstance);

        Integer subQuark = Ros2MessagesUtil.getSubscriptionQuarkAndAdd(ss, fObjectsSs, timestamp, subHandle);
        if (null == subQuark) {
            return;
        }
        ss.modifyAttribute(takeInstance.getStartTime(), subCallbackInstance, subQuark);
        ss.modifyAttribute(callbackInstance.getEndTime(), null, subQuark);
    }

    private void addTimerCallback(ITmfStateSystemBuilder ss, long timestamp, long startTimestamp, boolean isIntraProcess, long tid, @NonNull Ros2ObjectHandle timerHandle) {
        Ros2CallbackInstance callbackInstance = new Ros2CallbackInstance(timerHandle, tid, isIntraProcess, startTimestamp, timestamp);
        Ros2TimerCallbackInstance timerCallbackInstance = new Ros2TimerCallbackInstance(timerHandle, callbackInstance);

        Integer timerQuark = Ros2MessagesUtil.getTimerQuarkAndAdd(ss, fObjectsSs, timestamp, timerHandle);
        if (null == timerQuark) {
            return;
        }
        ss.modifyAttribute(callbackInstance.getStartTime(), timerCallbackInstance, timerQuark);
        ss.modifyAttribute(callbackInstance.getEndTime(), null, timerQuark);
    }

    private void addCallbackPublications(ITmfStateSystemBuilder ss, @NonNull ITmfEvent callbackStart, @NonNull Ros2ObjectHandle ownerHandle, @NonNull Ros2CallbackType callbackType) {
        /**
         * Add publications that happened during this callback to the model and
         * then reset the map.
         */
        HostThread hostThread = hostThreadFrom(callbackStart);
        for (Pair<@NonNull Ros2ObjectHandle, @NonNull Long> pair : fCallbackPublications.get(hostThread)) {
            Ros2ObjectHandle publisherHandle = pair.getFirst();
            Long pubTimestamp = pair.getSecond();
            int callbackPublicationInstanceQuark = Ros2MessagesUtil.getCallbackPublicationInstanceQuarkAndAdd(ss, ownerHandle, publisherHandle);
            Ros2CallbackPublicationInstance callbackPublicationInstance = new Ros2CallbackPublicationInstance(ownerHandle, publisherHandle, pubTimestamp, callbackType);
            ss.modifyAttribute(pubTimestamp, callbackPublicationInstance, callbackPublicationInstanceQuark);
            ss.modifyAttribute(pubTimestamp + 1, null, callbackPublicationInstanceQuark);
        }
        fCallbackPublications.removeAll(hostThread);
    }

    private void createObjects(ITmfStateSystemBuilder ss) {
        /**
         * Get all node objects from the objects state system and create
         * corresponding quarks and states in this state system.
         *
         * TODO use addFutureEvent()?
         */
        for (@NonNull
        ITmfStateInterval nodeObjectInterval : Ros2ObjectsUtil.getNodeObjectIntervals(fObjectsSs)) {
            Ros2NodeObject nodeObject = (Ros2NodeObject) nodeObjectInterval.getValue();
            if (null == nodeObject) {
                continue;
            }
            int nodeQuark = Ros2MessagesUtil.getNodeQuarkAndAdd(ss, nodeObject);
            // Create lifetime
            ss.modifyAttribute(nodeObjectInterval.getStartTime(), nodeObject, nodeQuark);
            ss.modifyAttribute(nodeObjectInterval.getEndTime(), null, nodeQuark);
        }
        // TODO also do this for pubs/subs/timers?
    }
}
