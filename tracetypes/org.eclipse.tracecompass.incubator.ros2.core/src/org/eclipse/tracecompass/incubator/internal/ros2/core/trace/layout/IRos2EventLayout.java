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

package org.eclipse.tracecompass.incubator.internal.ros2.core.trace.layout;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This interface represents the events layout as defined in ros2_tracing.
 * <p>
 * This could eventually support different versions of ROS 2 in case they have
 * different event definitions.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("javadoc")
@NonNullByDefault
public interface IRos2EventLayout {

    public static String PROVIDER_NAME = "ros2:"; //$NON-NLS-1$
    public static String DDS_PROVIDER_NAME = "dds:"; //$NON-NLS-1$

    /**
     * The default layout
     *
     * @return the default layout
     */
    public static IRos2EventLayout getDefault() {
        return Ros2RollingEventLayout.getInstance();
    }

    /**
     * @return the provider name (ending with a colon)
     */
    public default String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Get all event names defined here
     *
     * @return the event names
     */
    public default Collection<String> getEventNames() {
        Collection<String> eventNames = Lists.newArrayList();
        Iterables.filter(Arrays.asList(IRos2EventLayout.class.getMethods()),
                method -> method.getName().startsWith("event")) //$NON-NLS-1$
                .forEach(eventMethod -> {
                    try {
                        eventNames.add(Objects.requireNonNull((String) eventMethod.invoke(this)));
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        // Do nothing
                    }
                });
        return ImmutableList.copyOf(Objects.requireNonNull(eventNames));
    }

    // ------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------

    /**
     * <code>rcl_init</code>
     */
    String eventRclInit();

    /**
     * <code>rcl_node_init</code>
     */
    String eventRclNodeInit();

    /**
     * <code>rmw_publisher_init</code>
     */
    String eventRmwPublisherInit();

    /**
     * <code>rcl_publisher_init</code>
     */
    String eventRclPublisherInit();

    /**
     * <code>rclcpp_publish</code>
     */
    String eventRclcppPublish();

    /**
     * <code>rcl_publish</code>
     */
    String eventRclPublish();

    /**
     * <code>rmw_publish</code>
     */
    String eventRmwPublish();

    /**
     * <code>rmw_subscription_init</code>
     */
    String eventRmwSubscriptionInit();

    /**
     * <code>rcl_subscription_init</code>
     */
    String eventRclSubscriptionInit();

    /**
     * <code>rclcpp_subscription_init</code>
     */
    String eventRclcppSubscriptionInit();

    /**
     * <code>rclcpp_subscription_callback_added</code>
     */
    String eventRclcppSubscriptionCallbackAdded();

    /**
     * <code>rmw_take</code>
     */
    String eventRmwTake();

    /**
     * <code>rcl_take</code>
     */
    String eventRclTake();

    /**
     * <code>rclcpp_take</code>
     */
    String eventRclcppTake();

    /**
     * <code>rcl_service_init</code>
     */
    String eventRclServiceInit();

    /**
     * <code>rclcpp_service_callback_added</code>
     */
    String eventRclcppServiceCallbackAdded();

    /**
     * <code>rcl_client_init</code>
     */
    String eventRclClientInit();

    /**
     * <code>rcl_timer_init</code>
     */
    String eventRclTimerInit();

    /**
     * <code>rclcpp_timer_callback_added</code>
     */
    String eventRclcppTimerCallbackAdded();

    /**
     * <code>rclcpp_timer_link_node</code>
     */
    String eventRclcppTimerLinkNode();

    /**
     * <code>rclcpp_callback_register</code>
     */
    String eventRclcppCallbackRegister();

    /**
     * <code>callback_start</code>
     */
    String eventCallbackStart();

    /**
     * <code>callback_end</code>
     */
    String eventCallbackEnd();

    /**
     * <code>rcl_lifecycle_state_machine_init</code>
     */
    String eventRclLifecycleStateMachineInit();

    /**
     * <code>rcl_lifecycle_transition</code>
     */
    String eventRclLifecycleTransition();

    /**
     * <code>rclcpp_executor_get_next_ready</code>
     */
    String eventRclcppExecutorGetNextReady();

    /**
     * <code>rclcpp_executor_wait_for_work</code>
     */
    String eventRclcppExecutorWaitForWork();

    /**
     * <code>rclcpp_executor_execute</code>
     */
    String eventRclcppExecutorExecute();

    // Message causal links

    /**
     * <code>message_link_periodic_async</code>
     */
    String eventMessageLinkPeriodicAsync();

    /**
     * <code>message_link_partial_sync</code>
     */
    String eventMessageLinkPartialSync();

    // DDS

    /**
     * <code>dds:create_writer</code>
     */
    String eventDdsCreateWriter();

    /**
     * <code>dds:write_pre</code>
     */
    String eventDdsWritePre();

    /**
     * <code>dds:write</code>
     */
    String eventDdsWrite();

    /**
     * <code>dds:create_reader</code>
     */
    String eventDdsCreateReader();

    /**
     * <code>dds:read</code>
     */
    String eventDdsRead();

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    String fieldContextHandle();
    String fieldVersion();
    String fieldNodeHandle();
    String fieldRmwHandle();
    String fieldNodeName();
    String fieldNamespace();
    String fieldRmwPublisherHandle();
    String fieldGid();
    String fieldPublisherHandle();
    String fieldTopicName();
    String fieldQueueDepth();
    String fieldMessage();
    String fieldRmwSubscriptionHandle();
    String fieldSubscriptionHandle();
    String fieldSubscription();
    String fieldCallback();
    String fieldSourceTimestamp();
    String fieldTaken();
    String fieldServiceHandle();
    String fieldRmwServiceHandle();
    String fieldServiceName();
    String fieldClientHandle();
    String fieldRmwClientHandle();
    String fieldTimerHandle();
    String fieldPeriod();
    String fieldSymbol();
    String fieldIsIntraProcess();
    String fieldStateMachine();
    String fieldStartLabel();
    String fieldGoalLabel();
    String fieldTimeout();
    String fieldHandle();

    // Message causal links
    String fieldSubs();
    String fieldPubs();

    // DDS
    String fieldGidPrefix();
    String fieldGidEntity();
    String fieldWriter();
    String fieldData();
    String fieldTimestamp();
    String fieldReader();
    String fieldBuffer();

    // Context fields
    String contextVpid();
    String contextVtid();
    String contextProcname();
    String contextPerfThreadTaskClock();
}
