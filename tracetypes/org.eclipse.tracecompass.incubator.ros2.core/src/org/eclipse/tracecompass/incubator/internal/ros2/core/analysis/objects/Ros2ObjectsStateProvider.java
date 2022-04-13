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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Gid;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Maps;

/**
 * State provider for the ROS 2 Objects analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectsStateProvider extends AbstractRos2StateProvider {

    private static final int VERSION_NUMBER = 0;
    private static final @NonNull String ROS_2_NAMESPACE_SEP = "/"; //$NON-NLS-1$

    // Publishers
    private Map<@NonNull Gid, ITmfEvent> fDdsCreateWriterEvents = Maps.newHashMap();
    private Map<HostProcessPointer, ITmfEvent> fRmwPublisherInitEvents = Maps.newHashMap();
    // Callbacks
    private Collection<HostProcessPointer> fIgnoredCallbacks = new ArrayList<>();
    private Map<HostProcessPointer, Pair<Ros2CallbackType, @NonNull Ros2ObjectHandle>> fCallbackOwners = Maps.newHashMap();
    // Subscriptions
    private Map<@NonNull Gid, ITmfEvent> fDdsCreateReaderEvents = Maps.newHashMap();
    private Map<@NonNull Ros2ObjectHandle, ITmfEvent> fRmwSubscriptionInitEvents = Maps.newHashMap();
    private Map<@NonNull Ros2ObjectHandle, ITmfEvent> fRclSubscriptionInitEvents = Maps.newHashMap();
    private Map<HostProcessPointer, ITmfEvent> fRclcppSubscriptionInitEvents = Maps.newHashMap();
    // Timers
    private Map<@NonNull Ros2ObjectHandle, ITmfEvent> fRclTimerInit = Maps.newHashMap();
    private Map<@NonNull Ros2ObjectHandle, ITmfEvent> fRclcppTimerCallbackAdded = Maps.newHashMap();

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public Ros2ObjectsStateProvider(ITmfTrace trace) {
        super(trace, Ros2ObjectsAnalysis.getFullAnalysisId());
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new Ros2ObjectsStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
        long timestamp = event.getTimestamp().toNanos();

        // TODO(christophebedard) handle rcl_init?
        eventHandleNode(event, ss, timestamp);
        eventHandlePublisher(event, ss);
        eventHandleSubscription(event, ss);
        eventHandleTimer(event, ss, timestamp);
        eventHandleService(event);
        eventHandleCallback(event, ss, timestamp);
    }

    private static void eventHandleNode(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // rcl_node_init
        if (isEvent(event, LAYOUT.eventRclNodeInit())) {
            String nodeName = (String) getField(event, LAYOUT.fieldNodeName());
            String nodeNamespace = (String) getField(event, LAYOUT.fieldNamespace());
            Ros2ObjectHandle nodeHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldNodeHandle()));

            String fullNodeName = getFullNodeName(nodeNamespace, nodeName);
            Ros2NodeObject node = new Ros2NodeObject(nodeHandle, fullNodeName, Objects.requireNonNull(event.getTrace().getName()));

            int nodeQuark = Ros2ObjectsUtil.getNodeQuarkAndAdd(ss, node.getHandle());
            ss.modifyAttribute(timestamp, node, nodeQuark);
        }
    }

    private void eventHandlePublisher(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss) {
        eventHandlePublisherDdsCreate(event);
        eventHandlePublisherInit(event, ss);
    }


    private void eventHandlePublisherDdsCreate(@NonNull ITmfEvent event) {
        // dds:create_writer
        if (isEvent(event, LAYOUT.eventDdsCreateWriter())) {
            long[] gid;
            if (hasField(event, LAYOUT.fieldGid())) {
                // Cyclone DDS
                gid = (long[]) getField(event, LAYOUT.fieldGid());
            } else {
                // Fast DDS
                long[] gidPrefix = (long[]) getField(event, LAYOUT.fieldGidPrefix());
                long[] gidEntityId = (long[]) getField(event, LAYOUT.fieldGidEntity());
                gid = combineFastDdsGid(gidPrefix, gidEntityId);
            }

            // Add to temporary map
            fDdsCreateWriterEvents.put(new Gid(gid), event);
        }
    }


    private void eventHandlePublisherInit(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss) {
        // rmw_publisher_init
        if (isEvent(event, LAYOUT.eventRmwPublisherInit())) {
            Ros2ObjectHandle rmwPublisherHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldRmwPublisherHandle()));

            // Add to temporary map
            fRmwPublisherInitEvents.put(rmwPublisherHandle, event);

            return;
        }

        // rcl_publisher_init
        if (isEvent(event, LAYOUT.eventRclPublisherInit())) {
            Ros2ObjectHandle publisherHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldPublisherHandle()));
            Ros2ObjectHandle rmwPublisherHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldRmwPublisherHandle()));
            Ros2ObjectHandle nodeHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldNodeHandle()));
            String topicName = Objects.requireNonNull((String) getField(event, LAYOUT.fieldTopicName()));

            /**
             * FIXME there is no rmw_publisher_init event for a
             * rcl_publisher_init event for the internal /rosout publisher for
             * some reason. For now, just don't create publisher objects for
             * those.
             */
            if (topicName.equals("/rosout")) { //$NON-NLS-1$
                return;
            }

            // Get corresponding rmw_publisher_init event
            ITmfEvent rmwPublisherInit = fRmwPublisherInitEvents.remove(rmwPublisherHandle);
            if (null == rmwPublisherInit) {
                Activator.getInstance().logError("could not find corresponding rmw_publisher_init event for rmwPublisherHandle=" + rmwPublisherHandle.toString()); //$NON-NLS-1$
                return;
            }
            long[] rmwGid = (long[]) getField(rmwPublisherInit, LAYOUT.fieldGid());
            Gid gid = getDdsGidFromRmwGidArray(rmwGid);

            // Get corresponding dds:create_writer event
            ITmfEvent ddsCreateWriter = fDdsCreateWriterEvents.remove(gid);
            if (null == ddsCreateWriter) {
                Activator.getInstance().logError("could find corresponding dds:dds_create writer for gid=" + gid.toString()); //$NON-NLS-1$
                return;
            }
            HostProcessPointer ddsWriter = hostProcessPointerFrom(ddsCreateWriter, (long) getField(ddsCreateWriter, LAYOUT.fieldWriter()));

            // Add to pubs list
            Ros2PublisherObject pubObject = new Ros2PublisherObject(publisherHandle, rmwPublisherHandle, topicName, nodeHandle, gid, ddsWriter);
            int pubQuark = Ros2ObjectsUtil.getPublisherQuarkAndAdd(ss, pubObject.getHandle());
            // Use timestamp from dds:create_writer event
            long ddsTimestamp = ddsCreateWriter.getTimestamp().toNanos();
            ss.modifyAttribute(ddsTimestamp, pubObject, pubQuark);
        }
    }

    private void eventHandleSubscription(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss) {
        eventHandleSubscriptionDdsCreate(event);
        eventHandleSubscriptionInit(event);
        eventHandleSubscriptionCallbackAdded(event, ss);
    }


    private void eventHandleSubscriptionDdsCreate(@NonNull ITmfEvent event) {
        // dds:create_reader
        if (isEvent(event, LAYOUT.eventDdsCreateReader())) {
            long[] gid;
            if (hasField(event, LAYOUT.fieldGid())) {
                // Cyclone DDS
                gid = (long[]) getField(event, LAYOUT.fieldGid());
            } else {
                // Fast DDS
                long[] gidPrefix = (long[]) getField(event, LAYOUT.fieldGidPrefix());
                long[] gidEntityId = (long[]) getField(event, LAYOUT.fieldGidEntity());
                gid = ArrayUtils.addAll(gidPrefix, gidEntityId);
            }

            // Add to temporary map
            fDdsCreateReaderEvents.put(new Gid(gid), event);
        }
    }

    private void eventHandleSubscriptionInit(@NonNull ITmfEvent event) {
        // rmw_subscription_init
        if (isEvent(event, LAYOUT.eventRmwSubscriptionInit())) {
            Ros2ObjectHandle rmwSubscriptionHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldRmwSubscriptionHandle()));

            // Add to temporary map
            fRmwSubscriptionInitEvents.put(rmwSubscriptionHandle, event);

            return;
        }

        // rcl_subscription_init
        if (isEvent(event, LAYOUT.eventRclSubscriptionInit())) {
            Ros2ObjectHandle subscriptionHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldSubscriptionHandle()));

            // Add to temporary map
            fRclSubscriptionInitEvents.put(subscriptionHandle, event);

            return;
        }

        // rclcpp_subscription_init
        if (isEvent(event, LAYOUT.eventRclcppSubscriptionInit())) {
            HostProcessPointer subscription = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldSubscription()));

            // Add to temporary map
            fRclcppSubscriptionInitEvents.put(subscription, event);
        }
    }

    private void eventHandleSubscriptionCallbackAdded(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss) {
        // rclcpp_subscription_callback_added
        if (isEvent(event, LAYOUT.eventRclcppSubscriptionCallbackAdded())) {
            HostProcessPointer subscription = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldSubscription()));
            HostProcessPointer callback = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldCallback()));

            // Get corresponding rclcpp_subscription_init event
            ITmfEvent rclcppSubscriptionInit = fRclcppSubscriptionInitEvents.remove(subscription);
            if (null == rclcppSubscriptionInit) {
                Activator.getInstance().logError("could not find corresponding rclcpp_subscription_init event for subscription=" + subscription.toString()); //$NON-NLS-1$
                return;
            }
            Ros2ObjectHandle subscriptionHandle = handleFrom(rclcppSubscriptionInit, (long) getField(rclcppSubscriptionInit, LAYOUT.fieldSubscriptionHandle()));

            // Get corresponding rcl_subscription_init event
            ITmfEvent rclSubscriptionInit = fRclSubscriptionInitEvents.remove(subscriptionHandle);
            if (null == rclSubscriptionInit) {
                Activator.getInstance().logError("could not find corresponding rcl_subscription_init event for subscriptionHandle=" + subscriptionHandle.toString()); //$NON-NLS-1$
                return;
            }
            Ros2ObjectHandle rmwSubscriptionHandle = handleFrom(rclSubscriptionInit, (long) getField(rclSubscriptionInit, LAYOUT.fieldRmwSubscriptionHandle()));
            Ros2ObjectHandle nodeHandle = handleFrom(rclSubscriptionInit, (long) getField(rclSubscriptionInit, LAYOUT.fieldNodeHandle()));
            String topicName = Objects.requireNonNull((String) getField(rclSubscriptionInit, LAYOUT.fieldTopicName()));

            // Get corresponding rmw_susbcription_init event
            ITmfEvent rmwSubscriptionInit = fRmwSubscriptionInitEvents.remove(rmwSubscriptionHandle);
            if (null == rmwSubscriptionInit) {
                Activator.getInstance().logError("could not find corresponding rmw_susbcription_init event for rmwSubscriptionHandle=" + rmwSubscriptionHandle.toString()); //$NON-NLS-1$
                return;
            }
            long[] gidRmw = (long[]) getField(rmwSubscriptionInit, LAYOUT.fieldGid());
            Gid gid = getDdsGidFromRmwGidArray(gidRmw);

            // Get corresponding dds:create_reader event
            ITmfEvent ddsCreateReader = fDdsCreateReaderEvents.remove(gid);
            if (null == ddsCreateReader) {
                Activator.getInstance().logError("could not find corresponding dds:create_reader event for gid=" + gid.toString()); //$NON-NLS-1$
                return;
            }
            HostProcessPointer ddsReader = hostProcessPointerFrom(ddsCreateReader, (long) getField(ddsCreateReader, LAYOUT.fieldReader()));

            // Add callback owner info to map
            fCallbackOwners.put(callback, new Pair<>(Ros2CallbackType.SUBSCRIPTION, subscriptionHandle));

            // Add to subs list
            Ros2SubscriptionObject subscriptionObject = new Ros2SubscriptionObject(
                    subscriptionHandle, rmwSubscriptionHandle, topicName, nodeHandle, gid, ddsReader, subscription, callback);
            int subQuark = Ros2ObjectsUtil.getSubscriptionQuarkAndAdd(ss, subscriptionObject.getHandle());
            // Use timestamp from dds:create_reader event
            long ddsTimestamp = ddsCreateReader.getTimestamp().toNanos();
            ss.modifyAttribute(ddsTimestamp, subscriptionObject, subQuark);
        }
    }

    private void eventHandleTimer(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        eventHandleTimerInit(event);
        eventHandleTimerCallbackAdded(event);
        eventHandleTimerLinkNode(event, ss, timestamp);
    }

    private void eventHandleTimerInit(@NonNull ITmfEvent event) {
        // rcl_timer_init
        if (isEvent(event, LAYOUT.eventRclTimerInit())) {
            Ros2ObjectHandle timerHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldTimerHandle()));

            // Add to temporary map
            fRclTimerInit.put(timerHandle, event);
        }
    }

    private void eventHandleTimerCallbackAdded(@NonNull ITmfEvent event) {
        // rclcpp_timer_callback_added
        if (isEvent(event, LAYOUT.eventRclcppTimerCallbackAdded())) {
            Ros2ObjectHandle timerHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldTimerHandle()));

            // Add to temporary map
            fRclcppTimerCallbackAdded.put(timerHandle, event);

            // Add callback owner info to map
            HostProcessPointer callback = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldCallback()));
            fCallbackOwners.put(callback, new Pair<>(Ros2CallbackType.TIMER, timerHandle));
        }
    }

    private void eventHandleTimerLinkNode(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // rclcpp_timer_link_node
        if (isEvent(event, LAYOUT.eventRclcppTimerLinkNode())) {
            Ros2ObjectHandle timerHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldTimerHandle()));
            Ros2ObjectHandle nodeHandle = handleFrom(event, (long) getField(event, LAYOUT.fieldNodeHandle()));

            // Get corresponding rclcpp_timer_callback_added event
            ITmfEvent rclcppTimerCallbackAdded = fRclcppTimerCallbackAdded.remove(timerHandle);
            if (null == rclcppTimerCallbackAdded) {
                Activator.getInstance().logError("could not find corresponding rclcpp_timer_callback_added event for timerHandle=" + timerHandle.toString()); //$NON-NLS-1$
                return;
            }
            HostProcessPointer callback = hostProcessPointerFrom(rclcppTimerCallbackAdded, (long) getField(rclcppTimerCallbackAdded, LAYOUT.fieldCallback()));

            // Get corresponding rcl_timer_init event
            ITmfEvent rclTimerInit = fRclTimerInit.remove(timerHandle);
            if (null == rclTimerInit) {
                Activator.getInstance().logError("could not find corresponding rcl_timer_init event for timerHandle=" + timerHandle.toString()); //$NON-NLS-1$
                return;
            }
            long period = (long) getField(rclTimerInit, LAYOUT.fieldPeriod());

            // Add to timers list
            Ros2TimerObject timerObject = new Ros2TimerObject(timerHandle, period, callback, nodeHandle);
            int timerQuark = Ros2ObjectsUtil.getTimerQuarkAndAdd(ss, timerObject.getHandle());
            ss.modifyAttribute(timestamp, timerObject, timerQuark);
        }
    }

    private void eventHandleService(@NonNull ITmfEvent event) {
        // rclcpp_service_callback_added
        if (isEvent(event, LAYOUT.eventRclcppServiceCallbackAdded())) {
            HostProcessPointer callback = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldCallback()));

            // Add to list of ignored callbacks, since we don't currently
            // process services
            fIgnoredCallbacks.add(callback);
        }
    }

    private void eventHandleCallback(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp) {
        // rclcpp_callback_register
        if (isEvent(event, LAYOUT.eventRclcppCallbackRegister())) {
            HostProcessPointer callback = hostProcessPointerFrom(event, (long) getField(event, LAYOUT.fieldCallback()));
            String symbol = (String) getField(event, LAYOUT.fieldSymbol());

            // Check if we should ignore this callback registration
            if (fIgnoredCallbacks.contains(callback)) {
                return;
            }

            // Get owner info from map
            Pair<Ros2CallbackType, @NonNull Ros2ObjectHandle> ownerInfo = fCallbackOwners.remove(callback);
            if (null == ownerInfo) {
                Activator.getInstance().logError("could not find callback owner info for callback=" + callback.toString()); //$NON-NLS-1$
                return;
            }
            Ros2CallbackType type = ownerInfo.getFirst();
            @NonNull
            Ros2ObjectHandle ownerHandle = ownerInfo.getSecond();

            // Add to callbacks list
            Ros2CallbackObject callbackObject = new Ros2CallbackObject(callback, ownerHandle, Objects.requireNonNull(symbol), type);
            int callbackQuark = Ros2ObjectsUtil.getCallbackQuarkAndAdd(ss, callbackObject.getCallback());
            ss.modifyAttribute(timestamp, callbackObject, callbackQuark);
        }
    }

    private static @NonNull String getFullNodeName(String namespace, String nodeName) {
        /**
         * Add separator between namespace and node name if the namespace is not
         * just the root.
         */
        String separator = StringUtils.EMPTY;
        if (!namespace.endsWith(ROS_2_NAMESPACE_SEP)) {
            separator = ROS_2_NAMESPACE_SEP;
        }
        return namespace + separator + nodeName;
    }

    private static long[] combineFastDdsGid(long[] gidPrefix, long[] gidEntityId) {
        /**
         * GIDs in Fast DDS are simply split into a prefix and an entity ID. To
         * get the the full GID, simply concatenate prefix + entity ID.
         */
        return ArrayUtils.addAll(gidPrefix, gidEntityId);
    }

    private static @NonNull Gid getDdsGidFromRmwGidArray(long[] rmwGid) {
        /**
         * The GID at the rmw level is 8 elements longer than the one at the DDS
         * level; the difference is just all zeros. See RMW_GID_STORAGE_SIZE in
         * rmw.
         */
        long[] ddsGid = Arrays.copyOfRange(rmwGid, 0, rmwGid.length - 8);
        return new Gid(ddsGid);
    }
}
