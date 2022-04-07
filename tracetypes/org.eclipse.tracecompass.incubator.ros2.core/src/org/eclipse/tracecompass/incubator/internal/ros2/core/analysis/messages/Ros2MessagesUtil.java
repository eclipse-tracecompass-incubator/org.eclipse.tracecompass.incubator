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
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

import com.google.common.collect.Streams;

/**
 * Utilities for generating a ROS 2 messages state system and for dealing with
 * one.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesUtil {

    /** Attribute name for nodes list */
    public static final @NonNull String LIST_NODES = "Nodes"; //$NON-NLS-1$
    /** Attribute name for publishers list */
    public static final @NonNull String LIST_PUBLISHERS = "Publishers"; //$NON-NLS-1$
    /** Attribute name for subscriptions list */
    public static final @NonNull String LIST_SUBSCRIPTIONS = "Subscriptions"; //$NON-NLS-1$
    /** Attribute name for timers list */
    public static final @NonNull String LIST_TIMERS = "Timers"; //$NON-NLS-1$
    /** Attribute name for transport instances list */
    public static final @NonNull String LIST_TRANSPORT = "Transport"; //$NON-NLS-1$
    /** Attribute name for callback-publication instances list */
    public static final @NonNull String LIST_CALLBACK_PUBLICATION = "Callback-publication"; //$NON-NLS-1$

    private static final @NonNull String SS_WILDCARD = "*"; //$NON-NLS-1$
    private static final @NonNull String SS_HANDLE_PAIR_SEP = "+"; //$NON-NLS-1$

    private Ros2MessagesUtil() {
        // Static utility class only
    }

    private static void assertStateSystem(ITmfStateSystem ss) {
        if (!ss.getSSID().equals(Ros2MessagesAnalysis.getFullAnalysisId())) {
            throw new IllegalArgumentException(String.format("wrong state system; need '%s', got '%s'", Ros2MessagesAnalysis.getFullAnalysisId(), ss.getSSID())); //$NON-NLS-1$
        }
    }

    /**
     * Get full attribute for given node object.
     *
     * @param nodeObject
     *            the node object
     * @return the full attribute
     */
    public static String[] getNodeAttribute(@NonNull Ros2NodeObject nodeObject) {
        // Include trace name to group nodes by trace/machine
        return new String[] { nodeObject.getTraceName(), LIST_NODES, nodeObject.getStringId() };
    }

    /**
     * Get publisher attribute relative to node attribute/quark.
     */
    private static String[] getPublisherRelativeAttribute(@NonNull Ros2ObjectHandle publisherHandle) {
        return new String[] { LIST_PUBLISHERS, publisherHandle.getStringId() };
    }

    /**
     * Get subscription attribute relative to node attribute/quark.
     */
    private static String[] getSubscriptionRelativeAttribute(@NonNull Ros2ObjectHandle subscriptionHandle) {
        return new String[] { LIST_SUBSCRIPTIONS, subscriptionHandle.getStringId() };
    }

    /**
     * Get timer attribute relative to node attribute/quark.
     */
    private static String[] getTimerRelativeAttribute(@NonNull Ros2ObjectHandle timerHandle) {
        return new String[] { LIST_TIMERS, timerHandle.getStringId() };
    }

    /**
     * Get transport instances attribute for publisher & subscription pair.
     */
    private static String[] getTransportInstanceAttribute(@NonNull Ros2ObjectHandle publisherHandle, @NonNull Ros2ObjectHandle subscriptionHandle) {
        return new String[] { LIST_TRANSPORT, publisherHandle.getStringId() + SS_HANDLE_PAIR_SEP + subscriptionHandle.getStringId() };
    }

    /**
     * Get callback-publication instances attribute for subscription/timer &
     * publisher pair.
     */
    private static String[] getCallbackPublicationInstanceAttribute(@NonNull Ros2ObjectHandle callbackOwnerHandle, @NonNull Ros2ObjectHandle publisherHandle) {
        return new String[] { LIST_CALLBACK_PUBLICATION, callbackOwnerHandle.getStringId() + SS_HANDLE_PAIR_SEP + publisherHandle.getStringId() };
    }

    /**
     * Get node quark.
     *
     * @param ss
     *            the messages state system
     * @param nodeObject
     *            the node object
     * @return the quark, or <code>null</code>
     */
    public static Integer getNodeQuark(ITmfStateSystem ss, @NonNull Ros2NodeObject nodeObject) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getNodeAttribute(nodeObject));
        } catch (AttributeNotFoundException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get node quark.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the node lifetime
     * @param nodeHandle
     *            the node handle
     * @return the node quark, or <code>null</code>
     */
    public static Integer getNodeQuark(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle nodeHandle) {
        Ros2NodeObject nodeObject = Ros2ObjectsUtil.getNodeObjectFromHandle(objectsSs, timestamp, nodeHandle);
        if (null == nodeObject) {
            Activator.getInstance().logError("could not find corresponding node object"); //$NON-NLS-1$
            return null;
        }
        return getNodeQuark(ss, nodeObject);
    }

    /**
     * Get node quark and add if needed.
     *
     * @param ss
     *            the messages state system builder
     * @param nodeObject
     *            the node object
     * @return the node quark
     */
    public static int getNodeQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2NodeObject nodeObject) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getNodeAttribute(nodeObject));
    }

    /**
     * Get publisher quark and add if needed.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the publisher & node lifetime
     * @param publisherHandle
     *            the publisher handle
     * @return the publisher quark, or <code>null</code>
     */
    public static @Nullable Integer getPublisherQuarkAndAdd(ITmfStateSystemBuilder ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle publisherHandle) {
        Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(objectsSs, timestamp, publisherHandle);
        if (null == pubObject) {
            Activator.getInstance().logError("could not find corresponding publisher object for publisherHandle=" + publisherHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, pubObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        return ss.getQuarkRelativeAndAdd(nodeQuark, getPublisherRelativeAttribute(publisherHandle));
    }

    /**
     * Get subscription quark and add if needed.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the subscription & node lifetime
     * @param subscriptionHandle
     *            the subscription handle
     * @return the subscription quark, or <code>null</code>
     */
    public static @Nullable Integer getSubscriptionQuarkAndAdd(ITmfStateSystemBuilder ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Ros2SubscriptionObject subscriptionObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(objectsSs, timestamp, subscriptionHandle);
        if (null == subscriptionObject) {
            Activator.getInstance().logError("could not find corresponding subscription object for subscriptionHandle=" + subscriptionHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, subscriptionObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        return ss.getQuarkRelativeAndAdd(nodeQuark, getSubscriptionRelativeAttribute(subscriptionHandle));
    }

    /**
     * Get timer quark and add if needed.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the timer & node lifetime
     * @param timerHandle
     *            the timer handle
     * @return the timer quark, or <code>null</code>
     */
    public static @Nullable Integer getTimerQuarkAndAdd(ITmfStateSystemBuilder ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle timerHandle) {
        Ros2TimerObject timerObject = Ros2ObjectsUtil.getTimerObjectFromHandle(objectsSs, timestamp, timerHandle);
        if (null == timerObject) {
            Activator.getInstance().logError("could not find corresponding timer object for timerHandle=" + timerHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, timerObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        return ss.getQuarkRelativeAndAdd(nodeQuark, getTimerRelativeAttribute(timerHandle));
    }

    /**
     * Get transport instances quark and add if needed.
     *
     * @param ss
     *            the messages state system
     * @param publisherHandle
     *            the publisher handle
     * @param subscriptionHandle
     *            the subscription handle
     * @return the new quark
     */
    public static int getTransportInstanceQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle publisherHandle, @NonNull Ros2ObjectHandle subscriptionHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getTransportInstanceAttribute(publisherHandle, subscriptionHandle));
    }

    /**
     * Get callback-publication instances quark and add if needed.
     *
     * @param ss
     *            the messages state system
     * @param callbackOwnerHandle
     *            the callback owner (subscription/timer) handle
     * @param publisherHandle
     *            the publisher handle
     * @return the new quark
     */
    public static int getCallbackPublicationInstanceQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle callbackOwnerHandle, @NonNull Ros2ObjectHandle publisherHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getCallbackPublicationInstanceAttribute(callbackOwnerHandle, publisherHandle));
    }

    /**
     * Get publisher quark.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the publisher & node lifetime
     * @param publisherHandle
     *            the publisher handle
     * @return the publisher quark, or <code>null</code>
     */
    public static @Nullable Integer getPublisherQuark(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle publisherHandle) {
        Ros2PublisherObject pubObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(objectsSs, timestamp, publisherHandle);
        if (null == pubObject) {
            Activator.getInstance().logError("could not find corresponding publisher object for publisherHandle=" + publisherHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, pubObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        try {
            return ss.getQuarkRelative(nodeQuark, getPublisherRelativeAttribute(publisherHandle));
        } catch (AttributeNotFoundException | IndexOutOfBoundsException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get subscription quark.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the subscription & node lifetime
     * @param subscriptionHandle
     *            the subscription handle
     * @return the subscription quark, or <code>null</code>
     */
    public static @Nullable Integer getSubscriptionQuark(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Ros2SubscriptionObject subscriptionObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(objectsSs, timestamp, subscriptionHandle);
        if (null == subscriptionObject) {
            Activator.getInstance().logError("could not find corresponding subscription object for subscriptionHandle=" + subscriptionHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, subscriptionObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        try {
            return ss.getQuarkRelative(nodeQuark, getSubscriptionRelativeAttribute(subscriptionHandle));
        } catch (AttributeNotFoundException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get timer quark.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the timer & node lifetime
     * @param timerHandle
     *            the timer handle
     * @return the timer quark, or <code>null</code>
     */
    public static @Nullable Integer getTimerQuark(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle timerHandle) {
        Ros2TimerObject timerObject = Ros2ObjectsUtil.getTimerObjectFromHandle(objectsSs, timestamp, timerHandle);
        if (null == timerObject) {
            Activator.getInstance().logError("could not find corresponding timer object for timerHandle=" + timerHandle.toString()); //$NON-NLS-1$
            return null;
        }
        Integer nodeQuark = getNodeQuark(ss, objectsSs, timestamp, timerObject.getNodeHandle());
        if (null == nodeQuark) {
            return null;
        }
        try {
            return ss.getQuarkRelative(nodeQuark, getTimerRelativeAttribute(timerHandle));
        } catch (AttributeNotFoundException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get transport instance quark.
     *
     * @param ss
     *            the messages state system
     * @param publisherHandle
     *            the publisher handle
     * @param subscriptionHandle
     *            the subscription handle
     * @return the transport instances quark, or <code>null</code>
     */
    public static @Nullable Integer getTransportInstanceQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle publisherHandle, @NonNull Ros2ObjectHandle subscriptionHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getTransportInstanceAttribute(publisherHandle, subscriptionHandle));
        } catch (AttributeNotFoundException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get transport instance quarks.
     *
     * @param ss
     *            the messages state system
     * @return the transport instance quarks
     */
    private static @NonNull Collection<@NonNull Integer> getTransportInstanceQuarks(ITmfStateSystem ss) {
        assertStateSystem(ss);
        try {
            int transportInstanceListQuark = ss.getQuarkAbsolute(LIST_TRANSPORT);
            /**
             * Two asterisks because we are using the first attribute (instance)
             * as a stack, so the actual values are at the second level.
             */
            return ss.getQuarks(transportInstanceListQuark, SS_WILDCARD, SS_WILDCARD);
        } catch (AttributeNotFoundException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get callback-publication instance quark.
     *
     * @param ss
     *            the messages state system
     * @param callbackOwnerHandle
     *            the callback owner (subscription/timer) handle
     * @param publisherHandle
     *            the publisher handle
     * @return the transport instances quark, or <code>null</code>
     */
    public static @Nullable Integer getCallbackPublicationInstanceQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle callbackOwnerHandle, @NonNull Ros2ObjectHandle publisherHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getCallbackPublicationInstanceAttribute(callbackOwnerHandle, publisherHandle));
        } catch (AttributeNotFoundException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get callback-publication instance quarks.
     *
     * @param ss
     *            the messages state system
     * @return the callback-publication instance quarks
     */
    private static @NonNull Collection<@NonNull Integer> getCallbackPublicationInstanceQuarks(ITmfStateSystem ss) {
        assertStateSystem(ss);
        try {
            int callbackPublicationInstanceListQuark = ss.getQuarkAbsolute(LIST_CALLBACK_PUBLICATION);
            return ss.getSubAttributes(callbackPublicationInstanceListQuark, false);
        } catch (AttributeNotFoundException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get state interval for a publication instance.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the instance interval, e.g., start
     *            timestamp
     * @param publisherHandle
     *            the publisher handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getPubInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle publisherHandle) {
        Integer pubQuark = getPublisherQuark(ss, objectsSs, timestamp, publisherHandle);
        if (null == pubQuark) {
            return null;
        }
        try {
            ITmfStateInterval pubInstanceInterval = ss.querySingleState(timestamp, pubQuark);
            if (null != pubInstanceInterval.getValue()) {
                return pubInstanceInterval;
            }

        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("pub instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get state interval for the next publication instance on after the given
     * timestamp.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp from which to find the next sub callback
     *            instance interval
     * @param publisherHandle
     *            the publisher handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getNextPubInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle publisherHandle) {
        Integer pubQuark = getPublisherQuark(ss, objectsSs, timestamp, publisherHandle);
        if (null == pubQuark) {
            return null;
        }
        try {
            /**
             * The state interval at the given timestamp will most probably be
             * null. In that case, use its start timestamp to find the next
             * state interval, which should not be null, unless we're at the end
             * of the state system.
             */
            ITmfStateInterval pubInstanceInterval = ss.querySingleState(timestamp, pubQuark);
            if (null != pubInstanceInterval.getValue()) {
                return pubInstanceInterval;
            }
            // Make sure the above null-value interval is not the last state
            if (pubInstanceInterval.getEndTime() < ss.getCurrentEndTime()) {
                pubInstanceInterval = ss.querySingleState(pubInstanceInterval.getEndTime() + 1, pubQuark);
                if (null != pubInstanceInterval.getValue()) {
                    return pubInstanceInterval;
                }
            }
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("next pub instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get state interval for the next publication instance on or after the
     * given start timestamp and before the given end timestamp.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param startTime
     *            the timestamp from which to find the next pub instance
     *            intervals
     * @param endTime
     *            the timestamp before which to find the next pub instance
     *            intervals
     * @param publisherHandle
     *            the publisher handle
     * @return the state intervals
     */
    public static Collection<@NonNull ITmfStateInterval> getPubInstanceIntervalsBetween(ITmfStateSystem ss, ITmfStateSystem objectsSs, long startTime, long endTime, @NonNull Ros2ObjectHandle publisherHandle) {
        Integer pubQuark = getPublisherQuark(ss, objectsSs, startTime, publisherHandle);
        if (null == pubQuark) {
            return Collections.emptyList();
        }
        Collection<@NonNull ITmfStateInterval> intervals = new ArrayList<>();
        try {
            /**
             * The state interval at the given timestamp will most probably be
             * null. In that case, use its end timestamp to find the next state
             * interval, which should not be null, unless we're at the end of
             * the state system.
             */
            for (ITmfStateInterval interval : ss.query2D(Collections.singleton(pubQuark), startTime, endTime)) {
                if (null != interval.getValue()) {
                    intervals.add(interval);
                }
            }

        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        if (intervals.isEmpty()) {
            Activator.getInstance().logError("no pub instances found between interval"); //$NON-NLS-1$
        }
        return intervals;
    }

    /**
     * Get state interval for a subscription callback instance.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the instance interval, e.g., start
     *            timestamp
     * @param subscriptionHandle
     *            the subscription handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getSubCallbackInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Integer subQuark = getSubscriptionQuark(ss, objectsSs, timestamp, subscriptionHandle);
        if (null == subQuark) {
            return null;
        }
        try {
            ITmfStateInterval subCallbackInstanceInterval = ss.querySingleState(timestamp, subQuark);
            if (null != subCallbackInstanceInterval.getValue()) {
                return subCallbackInstanceInterval;
            }
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("sub callback instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get state interval for the next subscription callback instance on after
     * the given timestamp.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp from which to find the next sub callback
     *            instance interval
     * @param subscriptionHandle
     *            the subscription handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getNextSubCallbackInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Integer subQuark = getSubscriptionQuark(ss, objectsSs, timestamp, subscriptionHandle);
        if (null == subQuark) {
            return null;
        }
        try {
            /**
             * The state interval at the given timestamp will most probably be
             * null. In that case, use its start timestamp to find the next
             * state interval, which should not be null, unless we're at the end
             * of the state system.
             */
            ITmfStateInterval subCallbackInstanceInterval = ss.querySingleState(timestamp, subQuark);
            if (null != subCallbackInstanceInterval.getValue()) {
                return subCallbackInstanceInterval;
            }
            // Make sure the above null-value interval is not the last state
            if (subCallbackInstanceInterval.getEndTime() < ss.getCurrentEndTime()) {
                subCallbackInstanceInterval = ss.querySingleState(subCallbackInstanceInterval.getEndTime() + 1, subQuark);
                if (null != subCallbackInstanceInterval.getValue()) {
                    return subCallbackInstanceInterval;
                }
            }
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("next sub callback instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get state interval for the previous subscription callback instance on or
     * before the given timestamp.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp from which to find the previous sub callback
     *            instance interval
     * @param subscriptionHandle
     *            the subscription handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getPreviousSubCallbackInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Integer subQuark = getSubscriptionQuark(ss, objectsSs, timestamp, subscriptionHandle);
        if (null == subQuark) {
            return null;
        }
        try {
            /**
             * The state interval at the given timestamp will most probably be
             * null. In that case, use its start timestamp to find the previous
             * state interval, which should not be null, unless we're at the
             * beginning of the state system.
             */
            ITmfStateInterval subCallbackInstanceInterval = ss.querySingleState(timestamp, subQuark);
            if (null != subCallbackInstanceInterval.getValue()) {
                return subCallbackInstanceInterval;
            }
            // Make sure the above null-value interval is not the first state
            if (subCallbackInstanceInterval.getStartTime() > ss.getStartTime()) {
                subCallbackInstanceInterval = ss.querySingleState(subCallbackInstanceInterval.getStartTime() - 1, subQuark);
                if (null != subCallbackInstanceInterval.getValue()) {
                    return subCallbackInstanceInterval;
                }
            }
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("previous sub callback instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get state interval for a subscription callback instance.
     *
     * @param ss
     *            the messages state system
     * @param objectsSs
     *            the objects state system
     * @param timestamp
     *            the timestamp within the instance interval, e.g., start
     *            timestamp
     * @param timerHandle
     *            the timer handle
     * @return the state interval, or <code>null</code>
     */
    public static @Nullable ITmfStateInterval getTimerCallbackInstanceInterval(ITmfStateSystem ss, ITmfStateSystem objectsSs, long timestamp, @NonNull Ros2ObjectHandle timerHandle) {
        Integer timerQuark = getTimerQuark(ss, objectsSs, timestamp, timerHandle);
        if (null == timerQuark) {
            return null;
        }
        try {
            ITmfStateInterval timerCallbackInstanceInterval = ss.querySingleState(timestamp, timerQuark);
            if (null != timerCallbackInstanceInterval.getValue()) {
                return timerCallbackInstanceInterval;
            }
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("timer callback instance not found"); //$NON-NLS-1$
        return null;
    }

    /**
     * Get message transport instances for the given quarks and interval.
     *
     * This includes all transport instances that intersect (or at least
     * partially "go through") the given time interval.
     *
     * The state interval value must be an {@link Ros2MessageTransportInstance}
     * object.
     *
     * @param ss
     *            the message transport state system
     * @param transportInstanceQuarks
     *            the transport instance quarks
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @return a stream of message transport instances
     */
    private static @NonNull Stream<@NonNull Ros2MessageTransportInstance> getTransportInstances(ITmfStateSystem ss, @NonNull Collection<@NonNull Integer> transportInstanceQuarks, long startTime, long endTime) {
        try {
            @SuppressWarnings("null")
            Iterable<@NonNull ITmfStateInterval> intervalsIterable = ss.query2D(transportInstanceQuarks, startTime, endTime);
            return Streams.stream(intervalsIterable)
                    .filter(interval -> null != interval.getValue())
                    .map(interval -> (@NonNull Ros2MessageTransportInstance) Objects.requireNonNull(interval.getValue()));
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("transport instances not found"); //$NON-NLS-1$
        return Objects.requireNonNull(Stream.empty());
    }

    /**
     * Get all message transport instances for a given interval.
     *
     * This includes all transport instances that intersect (or at least
     * partially "go through") the given time interval.
     *
     * The state interval value must be an {@link Ros2MessageTransportInstance}
     * object.
     *
     * @param ss
     *            the message transport state system
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @return a stream of message transport instances
     */
    public static @NonNull Stream<@NonNull Ros2MessageTransportInstance> getTransportInstances(ITmfStateSystem ss, long startTime, long endTime) {
        Collection<@NonNull Integer> transportInstanceQuarks = getTransportInstanceQuarks(ss);
        return getTransportInstances(ss, transportInstanceQuarks, startTime, endTime);
    }

    /**
     * Get outgoing transport links at given publication timestamp for given
     * source publisher.
     *
     * @param ss
     *            the message transport state system
     * @param sourcePublicationTimestamp
     *            the transport source publication timestamp
     * @param sourcePublisherHandle
     *            the publisher handle
     * @return a stream of corresponding links
     */
    public static @NonNull Stream<@NonNull Ros2MessageTransportInstance> getOutgoingTransportLinks(ITmfStateSystem ss, long sourcePublicationTimestamp, @NonNull Ros2ObjectHandle sourcePublisherHandle) {
        /**
         * Return entries with a matching source publication timestamp and
         * source publisher handle.
         */
        Collection<@NonNull Integer> transportInstanceQuarks = getTransportInstanceQuarks(ss);
        return getTransportInstances(ss, transportInstanceQuarks, sourcePublicationTimestamp, sourcePublicationTimestamp + 1)
                .filter(transportInstance -> transportInstance.getSourceTimestamp() == sourcePublicationTimestamp && transportInstance.getPublisherHandle().equals(sourcePublisherHandle));
    }

    /**
     * Get incoming transport links for given take timestamp and destination
     * subscription.
     *
     * @param ss
     *            the message transport state system
     * @param destinationTakeTimestamp
     *            the transport destination take timestamp
     * @param destinationSubscriptionHandle
     *            the subscription handle
     * @return a stream of corresponding links
     */
    public static @NonNull Stream<@NonNull Ros2MessageTransportInstance> getIncomingTransportLinks(ITmfStateSystem ss, long destinationTakeTimestamp, @NonNull Ros2ObjectHandle destinationSubscriptionHandle) {
        /**
         * Return entries with a matching destination take timestamp and
         * destination subscription handle.
         */
        Collection<@NonNull Integer> transportInstanceQuarks = getTransportInstanceQuarks(ss);
        return getTransportInstances(ss, transportInstanceQuarks, destinationTakeTimestamp - 1, destinationTakeTimestamp)
                .filter(transportInstance -> transportInstance.getDestinationTimestamp() == destinationTakeTimestamp && transportInstance.getSubscriptionHandle().equals(destinationSubscriptionHandle));
    }

    /**
     * Get all callback-publication instances for a given interval.
     *
     * The state interval value must be an
     * {@link Ros2CallbackPublicationInstance} object.
     *
     * @param ss
     *            the message transport state system
     * @param callbackPublicationInstanceQuarks
     *            the callback-publication instance quarks
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @return a stream of callback-publication instances
     */
    public static @NonNull Stream<@NonNull Ros2CallbackPublicationInstance> getCallbackPublicationInstances(ITmfStateSystem ss, @NonNull Collection<@NonNull Integer> callbackPublicationInstanceQuarks, long startTime, long endTime) {
        try {
            @SuppressWarnings("null")
            Iterable<@NonNull ITmfStateInterval> intervalsIterable = ss.query2D(callbackPublicationInstanceQuarks, startTime, endTime);
            return Streams.stream(intervalsIterable)
                    .filter(interval -> null != interval.getValue())
                    .map(interval -> (@NonNull Ros2CallbackPublicationInstance) Objects.requireNonNull(interval.getValue()));
        } catch (StateSystemDisposedException e) {
            // Do nothing
        }
        Activator.getInstance().logError("callback-publication instances not found"); //$NON-NLS-1$
        return Objects.requireNonNull(Stream.empty());
    }

    /**
     * Get all callback-publication instances for a given interval.
     *
     * The state interval value must be an
     * {@link Ros2CallbackPublicationInstance} object.
     *
     * @param ss
     *            the message transport state system
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @return a stream of callback-publication instances
     */
    public static @NonNull Stream<@NonNull Ros2CallbackPublicationInstance> getCallbackPublicationInstances(ITmfStateSystem ss, long startTime, long endTime) {
        Collection<@NonNull Integer> callbackPublicationInstanceQuarks = getCallbackPublicationInstanceQuarks(ss);
        return getCallbackPublicationInstances(ss, callbackPublicationInstanceQuarks, startTime, endTime);
    }

    /**
     * Get outgoing callback-publication links for given source callback handle
     * and interval.
     *
     * @param ss
     *            the message transport state system
     * @param callbackOwnerHandle
     *            the callback owner handle handle
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @return a stream of corresponding links
     */
    public static @NonNull Stream<@NonNull Ros2CallbackPublicationInstance> getOutgoingCallbackPublicationLinks(ITmfStateSystem ss, @NonNull Ros2ObjectHandle callbackOwnerHandle, long startTime, long endTime) {
        Collection<@NonNull Integer> callbackPublicationInstanceQuarks = getCallbackPublicationInstanceQuarks(ss);
        return getCallbackPublicationInstances(ss, callbackPublicationInstanceQuarks, startTime, endTime)
                .filter(callbackPublicationInstance -> callbackPublicationInstance.getCallbackOwnerHandle().equals(callbackOwnerHandle));
    }

    /**
     * Get incoming callback-publication links for given publisher handle and
     * publication timestamp.
     *
     * @param ss
     *            the message transport state system
     * @param publisherHandle
     *            the publisher handle
     * @param publicationTimestamp
     *            the publication time
     * @return a stream of corresponding links
     */
    public static @NonNull Stream<@NonNull Ros2CallbackPublicationInstance> getIncomingCallbackPublicationLinks(ITmfStateSystem ss, @NonNull Ros2ObjectHandle publisherHandle, long publicationTimestamp) {
        Collection<@NonNull Integer> callbackPublicationInstanceQuarks = getCallbackPublicationInstanceQuarks(ss);
        return getCallbackPublicationInstances(ss, callbackPublicationInstanceQuarks, publicationTimestamp, publicationTimestamp + 1)
                .filter(callbackPublicationInstance -> callbackPublicationInstance.getPublisherHandle().equals(publisherHandle) && callbackPublicationInstance.getPublicationTimestamp() == publicationTimestamp);
    }
}
