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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ClientObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2Object;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ServiceObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Lists;

/**
 * Utilities for generating a ROS 2 objects state system and for dealing with
 * one.
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectsUtil {

    private static final @NonNull String OBJECT_NODE = "Nodes"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_PUBLISHER = "Publishers"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_SUBSCRIPTION = "Subscriptions"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_CLIENT = "Clients"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_SERVICE = "Services"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_TIMER = "Timers"; //$NON-NLS-1$
    private static final @NonNull String OBJECT_CALLBACK = "Callbacks"; //$NON-NLS-1$

    private Ros2ObjectsUtil() {
        // Static utility class only
    }

    private static void assertStateSystem(ITmfStateSystem ss) {
        if (!ss.getSSID().equals(Ros2ObjectsAnalysis.getFullAnalysisId())) {
            throw new IllegalArgumentException(String.format("wrong state system; need '%s', got '%s'", Ros2ObjectsAnalysis.getFullAnalysisId(), ss.getSSID())); //$NON-NLS-1$
        }
    }

    private static String[] getNodeAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_NODE, stringId };
    }

    private static String[] getPublisherAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_PUBLISHER, stringId };
    }

    private static String[] getSubscriptionAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_SUBSCRIPTION, stringId };
    }

    private static String[] getClientAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_CLIENT, stringId };
    }

    private static String[] getServiceAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_SERVICE, stringId };
    }

    private static String[] getTimerAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_TIMER, stringId };
    }

    private static String[] getCallbackAttribute(@NonNull String stringId) {
        return new String[] { OBJECT_CALLBACK, stringId };
    }

    private static String[] getNodeAttribute(@NonNull Ros2ObjectHandle nodeHandle) {
        return getNodeAttribute(nodeHandle.getStringId());
    }

    private static String[] getPublisherAttribute(@NonNull Ros2ObjectHandle publisherHandle) {
        return getPublisherAttribute(publisherHandle.getStringId());
    }

    private static String[] getSubscriptionAttribute(@NonNull Ros2ObjectHandle subscriptionHandle) {
        return getSubscriptionAttribute(subscriptionHandle.getStringId());
    }

    private static String[] getClientAttribute(@NonNull Ros2ObjectHandle clientHandle) {
        return getClientAttribute(clientHandle.getStringId());
    }

    private static String[] getServiceAttribute(@NonNull Ros2ObjectHandle serviceHandle) {
        return getServiceAttribute(serviceHandle.getStringId());
    }

    private static String[] getTimerAttribute(@NonNull Ros2ObjectHandle timerHandle) {
        return getTimerAttribute(timerHandle.getStringId());
    }

    private static String[] getCallbackAttribute(@NonNull HostProcessPointer callbackHandle) {
        return getCallbackAttribute(callbackHandle.getStringId());
    }

    /**
     * Get node quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param nodeHandle
     *            the node handle
     * @return the node quark
     */
    public static int getNodeQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle nodeHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getNodeAttribute(nodeHandle));
    }

    /**
     * Get publisher quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param publisherHandle
     *            the publisher handle
     * @return the publisher quark
     */
    public static int getPublisherQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle publisherHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getPublisherAttribute(publisherHandle));
    }

    /**
     * Get subscription quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param subscriptionHandle
     *            the subscription handle
     * @return the subscription quark
     */
    public static int getSubscriptionQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle subscriptionHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getSubscriptionAttribute(subscriptionHandle));
    }

    /**
     * Get client quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param clientHandle
     *            the client handle
     * @return the client quark
     */
    public static int getClientQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle clientHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getClientAttribute(clientHandle));
    }

    /**
     * Get client quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param serviceHandle
     *            the service handle
     * @return the service quark
     */
    public static int getServiceQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle serviceHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getServiceAttribute(serviceHandle));
    }

    /**
     * Get timer quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param timerHandle
     *            the timer handle
     * @return the timer quark
     */
    public static int getTimerQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull Ros2ObjectHandle timerHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getTimerAttribute(timerHandle));
    }

    /**
     * Get callback quark and add if needed.
     *
     * @param ss
     *            the objects state system
     * @param callbackHandle
     *            the callback handle
     * @return the callback quark
     */
    public static int getCallbackQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull HostProcessPointer callbackHandle) {
        assertStateSystem(ss);
        return ss.getQuarkAbsoluteAndAdd(getCallbackAttribute(callbackHandle));
    }

    private static Integer getNodeQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle nodeHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getNodeAttribute(nodeHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getPublisherQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle publisherHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getPublisherAttribute(publisherHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getSubscriptionQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle subscriptionHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getSubscriptionAttribute(subscriptionHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getClientQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle clientHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getClientAttribute(clientHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getServiceQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle serviceHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getServiceAttribute(serviceHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getTimerQuark(ITmfStateSystem ss, @NonNull Ros2ObjectHandle timerHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getTimerAttribute(timerHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    private static Integer getCallbackQuark(ITmfStateSystem ss, @NonNull HostProcessPointer callbackHandle) {
        assertStateSystem(ss);
        try {
            return ss.getQuarkAbsolute(getCallbackAttribute(callbackHandle));
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

    /**
     * Get node object interval from node handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param nodeHandle
     *            the node handle
     * @return the node object interval, or <code>null</code> if not found
     */
    public static @Nullable ITmfStateInterval getNodeObjectIntervalFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle nodeHandle) {
        Integer nodeQuark = getNodeQuark(ss, nodeHandle);
        if (null == nodeQuark) {
            return null;
        }

        try {
            return ss.querySingleState(timestamp, nodeQuark);
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get node object from node handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param nodeHandle
     *            the node handle
     * @return the node object, or <code>null</code> if not found
     */
    public static @Nullable Ros2NodeObject getNodeObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle nodeHandle) {
        @Nullable
        ITmfStateInterval nodeObjectInterval = getNodeObjectIntervalFromHandle(ss, timestamp, nodeHandle);
        if (null != nodeObjectInterval && null != nodeObjectInterval.getValue()) {
            return (Ros2NodeObject) nodeObjectInterval.getValue();
        }
        return null;
    }

    private static @NonNull String getMissingIntervalMessage(boolean multipleUnexpected, @NonNull Class<@NonNull ?> clazz, Integer quark) {
        return String.format(
                "%s %s state intervals for quark=%d", //$NON-NLS-1$
                (multipleUnexpected ? "more than 1" : "no"), clazz.getName(), quark); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Get object of a given type with the given handle.
     *
     * This does not take any timestamp and assumes that there is only one state
     * interval with a non-null value for the given attribute.
     */
    private static <@NonNull O extends Ros2Object<?>> @Nullable O getObjectFromHandle(ITmfStateSystem ss, @NonNull Class<@NonNull O> objectClass, @NonNull String objectListAttribute, @NonNull Ros2ObjectHandle objectHandle) {
        assertStateSystem(ss);
        try {
            int objectQuark = ss.getQuarkAbsolute(objectListAttribute, objectHandle.getStringId());
            // We assume that we should have at most one state interval per
            // object quark
            Iterable<@NonNull ITmfStateInterval> objectIntervalsIterable = ss.query2D(Collections.singleton(objectQuark), ss.getStartTime(), ss.getCurrentEndTime());
            List<@NonNull ITmfStateInterval> objectIntervals = Lists.newArrayList(objectIntervalsIterable).stream()
                    .filter(interval -> null != interval.getValue())
                    .collect(Collectors.toList());
            if (objectIntervals.isEmpty()) {
                Activator.getInstance().logError(getMissingIntervalMessage(false, objectClass, objectQuark));
            } else {
                if (objectIntervals.size() > 1) {
                    Activator.getInstance().logError(getMissingIntervalMessage(true, objectClass, objectQuark));
                }
                @SuppressWarnings("unchecked")
                O object = (O) objectIntervals.get(0).getValue();
                return object;
            }
        } catch (StateSystemDisposedException | AttributeNotFoundException e) {
            return null;
        }
        return null;
    }

    /**
     * Get subscription object from subscription handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param subscriptionHandle
     *            the subscription handle
     * @return the subscription object, or <code>null</code> if not found
     */
    public static @Nullable Ros2SubscriptionObject getSubscriptionObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle subscriptionHandle) {
        Integer subQuark = getSubscriptionQuark(ss, subscriptionHandle);
        if (null == subQuark) {
            return null;
        }

        try {
            return (Ros2SubscriptionObject) ss.querySingleState(timestamp, subQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get publisher object from publisher handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param publisherHandle
     *            the publisher handle
     * @return the publisher object, or <code>null</code> if not found
     */
    public static @Nullable Ros2PublisherObject getPublisherObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle publisherHandle) {
        Integer pubQuark = getPublisherQuark(ss, publisherHandle);
        if (null == pubQuark) {
            return null;
        }

        try {
            return (Ros2PublisherObject) ss.querySingleState(timestamp, pubQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get client object from client handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param clientHandle
     *            the client handle
     * @return the client object, or <code>null</code> if not found
     */
    public static @Nullable Ros2ClientObject getClientObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle clientHandle) {
        Integer clientQuark = getClientQuark(ss, clientHandle);
        if (null == clientQuark) {
            return null;
        }

        try {
            return (Ros2ClientObject) ss.querySingleState(timestamp, clientQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get service object from service handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param serviceHandle
     *            the service handle
     * @return the service object, or <code>null</code> if not found
     */
    public static @Nullable Ros2ServiceObject getServiceObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle serviceHandle) {
        Integer serviceQuark = getServiceQuark(ss, serviceHandle);
        if (null == serviceQuark) {
            return null;
        }

        try {
            return (Ros2ServiceObject) ss.querySingleState(timestamp, serviceQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get timer object from timer handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param timerHandle
     *            the timer handle
     * @return the timer object, or <code>null</code> if not found
     */
    public static @Nullable Ros2TimerObject getTimerObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle timerHandle) {
        Integer timerQuark = getTimerQuark(ss, timerHandle);
        if (null == timerQuark) {
            return null;
        }

        try {
            return (Ros2TimerObject) ss.querySingleState(timestamp, timerQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get publisher object from publisher handle.
     *
     * @param ss
     *            the objects state system
     * @param subscriptionHandle
     *            the subscription handle
     * @return the publisher object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2SubscriptionObject getSubscriptionObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle subscriptionHandle) {
        return getObjectFromHandle(ss, Ros2SubscriptionObject.class, OBJECT_SUBSCRIPTION, subscriptionHandle);
    }

    /**
     * Get publisher object from publisher handle.
     *
     * @param ss
     *            the objects state system
     * @param publisherHandle
     *            the publisher handle
     * @return the publisher object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2PublisherObject getPublisherObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle publisherHandle) {
        return getObjectFromHandle(ss, Ros2PublisherObject.class, OBJECT_PUBLISHER, publisherHandle);
    }

    /**
     * Get node object from node handle.
     *
     * @param ss
     *            the objects state system
     * @param nodeHandle
     *            the node handle
     * @return the node object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2NodeObject getNodeObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle nodeHandle) {
        return getObjectFromHandle(ss, Ros2NodeObject.class, OBJECT_NODE, nodeHandle);
    }

    /**
     * Get client object from client handle.
     *
     * @param ss
     *            the objects state system
     * @param clientHandle
     *            the client handle
     * @return the client object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2ClientObject getClientObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle clientHandle) {
        return getObjectFromHandle(ss, Ros2ClientObject.class, OBJECT_CLIENT, clientHandle);
    }

    /**
     * Get service object from service handle.
     *
     * @param ss
     *            the objects state system
     * @param serviceHandle
     *            the service handle
     * @return the service object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2ServiceObject getServiceObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle serviceHandle) {
        return getObjectFromHandle(ss, Ros2ServiceObject.class, OBJECT_SERVICE, serviceHandle);
    }

    /**
     * Get timer object from timer handle.
     *
     * @param ss
     *            the objects state system
     * @param timerHandle
     *            the timer handle
     * @return the timer object, or <code>null</code> if not found
     * @see Ros2ObjectsUtil#getObjectFromHandle
     */
    public static @Nullable Ros2TimerObject getTimerObjectFromHandle(ITmfStateSystem ss, @NonNull Ros2ObjectHandle timerHandle) {
        return getObjectFromHandle(ss, Ros2TimerObject.class, OBJECT_TIMER, timerHandle);
    }

    /**
     * Get callback object from callback handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param callback
     *            the callback
     * @return the callback object, or <code>null</code> if not found
     */
    public static @Nullable Ros2CallbackObject getCallbackObjectFromHandle(ITmfStateSystem ss, long timestamp, @NonNull HostProcessPointer callback) {
        Integer callbackQuark = getCallbackQuark(ss, callback);
        if (null == callbackQuark) {
            return null;
        }

        try {
            return (Ros2CallbackObject) ss.querySingleState(timestamp, callbackQuark).getValue();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    /**
     * Get subscription handle from rmw subscription handle.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param rmwSubscriptionHandle
     *            the rmw subscription handle
     * @return the subscription handle, or <code>null</code> if not found
     */
    public static @Nullable Ros2ObjectHandle getSubscriptionHandleFromRmwSubscriptionHandle(ITmfStateSystem ss, long timestamp, @NonNull Ros2ObjectHandle rmwSubscriptionHandle) {
        assertStateSystem(ss);
        try {
            int subscriptionsQuark = ss.getQuarkAbsolute(OBJECT_SUBSCRIPTION);
            List<@NonNull Integer> subsQuarks = ss.getSubAttributes(subscriptionsQuark, false);
            for (Iterator<@NonNull Integer> iterator = subsQuarks.iterator(); iterator.hasNext();) {
                Integer subQuark = iterator.next();
                Ros2SubscriptionObject subscription = (Ros2SubscriptionObject) ss.querySingleState(timestamp, subQuark).getValue();
                if (null != subscription && subscription.getRmwHandle().equals(rmwSubscriptionHandle)) {
                    return subscription.getHandle();
                }
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            return null;
        }
        return null;
    }

    /**
     * Get callback owner handle and type.
     *
     * @param ss
     *            the objects state system
     * @param timestamp
     *            the timestamp
     * @param callback
     *            the callback
     * @return the handle of the callback and its type, or <code>null/<code> if
     *         not found
     */
    public static @Nullable Pair<@NonNull Ros2ObjectHandle, Ros2CallbackType> getCallbackOwnerHandle(ITmfStateSystem ss, long timestamp, @NonNull HostProcessPointer callback) {
        @Nullable
        Ros2CallbackObject callbackObject = getCallbackObjectFromHandle(ss, timestamp, callback);
        if (null != callbackObject) {
            return new Pair<>(callbackObject.getOwnerHandle(), callbackObject.getCallbackType());
        }
        return null;
    }

    /**
     * Get all state interval for a given object type.
     */
    private static <@NonNull O extends Ros2Object<?>> Collection<@NonNull ITmfStateInterval> getObjectsIntervals(ITmfStateSystem ss, @NonNull Class<@NonNull O> objectClass, @NonNull String objectListAttribute) {
        assertStateSystem(ss);
        try {
            Collection<@NonNull ITmfStateInterval> intervals = new ArrayList<>();
            for (Integer quark : ss.getQuarks(objectListAttribute, "*")) { //$NON-NLS-1$
                /**
                 * We assume that we should have at most one state interval per
                 * object quark.
                 */
                Iterable<@NonNull ITmfStateInterval> objectIntervalsIterable = ss.query2D(Collections.singleton(quark), ss.getStartTime(), ss.getCurrentEndTime());
                List<@NonNull ITmfStateInterval> objectIntervals = Lists.newArrayList(objectIntervalsIterable).stream()
                        .filter(interval -> null != interval.getValue())
                        .collect(Collectors.toList());
                if (objectIntervals.isEmpty()) {
                    Activator.getInstance().logError(getMissingIntervalMessage(false, objectClass, quark));
                } else {
                    if (objectIntervals.size() > 1) {

                        Activator.getInstance().logError(getMissingIntervalMessage(true, objectClass, quark));
                    }
                    intervals.add(objectIntervals.get(0));
                }
            }
            return intervals;
        } catch (StateSystemDisposedException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get all node object state intervals.
     *
     * @param ss
     *            the objects state system
     * @return the node object state intervals
     */
    public static Collection<@NonNull ITmfStateInterval> getNodeObjectIntervals(ITmfStateSystem ss) {
        return getObjectsIntervals(ss, Ros2NodeObject.class, OBJECT_NODE);
    }

    /**
     * Get all objects of a given type.
     */
    @SuppressWarnings({ "unchecked", "null" })
    private static <@NonNull O extends Ros2Object<?>> Collection<@NonNull O> getObjects(ITmfStateSystem ss, @NonNull Class<@NonNull O> objectClass, @NonNull String objectListAttribute) {
        /**
         * Intervals should already have a non-null value thanks to
         * getObjectsIntervals.
         */
        return getObjectsIntervals(ss, objectClass, objectListAttribute).stream()
                .map(interval -> (O) interval.getValue())
                .collect(Collectors.toList());
    }

    /**
     * Get all nodes.
     *
     * @param ss
     *            the objects state system
     * @return the node objects
     */
    public static Collection<@NonNull Ros2NodeObject> getNodeObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2NodeObject.class, OBJECT_NODE);
    }

    /**
     * Get all publishers.
     *
     * @param ss
     *            the objects state system
     * @return the publisher objects
     */
    public static Collection<@NonNull Ros2PublisherObject> getPublisherObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2PublisherObject.class, OBJECT_PUBLISHER);
    }

    /**
     * Get all subscriptions.
     *
     * @param ss
     *            the objects state system
     * @return the subscription objects
     */
    public static Collection<@NonNull Ros2SubscriptionObject> getSubscriptionObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2SubscriptionObject.class, OBJECT_SUBSCRIPTION);
    }

    /**
     * Get all clients.
     *
     * @param ss
     *            the objects state system
     * @return the client objects
     */
    public static Collection<@NonNull Ros2ClientObject> getClientObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2ClientObject.class, OBJECT_CLIENT);
    }

    /**
     * Get all services.
     *
     * @param ss
     *            the objects state system
     * @return the service objects
     */
    public static Collection<@NonNull Ros2ServiceObject> getServiceObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2ServiceObject.class, OBJECT_SERVICE);
    }

    /**
     * Get all timers.
     *
     * @param ss
     *            the objects state system
     * @return the timer objects
     */
    public static Collection<@NonNull Ros2TimerObject> getTimerObjects(ITmfStateSystem ss) {
        return getObjects(ss, Ros2TimerObject.class, OBJECT_TIMER);
    }
}
