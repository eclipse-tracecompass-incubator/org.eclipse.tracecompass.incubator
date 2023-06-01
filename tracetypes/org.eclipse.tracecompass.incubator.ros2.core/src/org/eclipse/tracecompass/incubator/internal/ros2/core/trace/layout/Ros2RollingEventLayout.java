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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Definitions used in ros2_tracing
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("nls")
@NonNullByDefault
public class Ros2RollingEventLayout implements IRos2EventLayout {

    /** Event names */
    private static final String RCL_INIT = "rcl_init";
    private static final String RCL_NODE_INIT = "rcl_node_init";
    private static final String RMW_PUBLISHER_INIT = "rmw_publisher_init";
    private static final String RCL_PUBLISHER_INIT = "rcl_publisher_init";
    private static final String RCLCPP_PUBLISH = "rclcpp_publish";
    private static final String RCL_PUBLISH = "rcl_publish";
    private static final String RMW_PUBLISH = "rmw_publish";
    private static final String RMW_SUBSCRIPTION_INIT = "rmw_subscription_init";
    private static final String RCL_SUBSCRIPTION_INIT = "rcl_subscription_init";
    private static final String RCLCPP_SUBSCRIPTION_INIT = "rclcpp_subscription_init";
    private static final String RCLCPP_SUBSCRIPTION_CALLBACK_ADDED = "rclcpp_subscription_callback_added";
    private static final String RMW_TAKE = "rmw_take";
    private static final String RCL_TAKE = "rcl_take";
    private static final String RCLCPP_TAKE = "rclcpp_take";
    private static final String RCL_SERVICE_INIT = "rcl_service_init";
    private static final String RCLCPP_SERVICE_CALLBACK_ADDED = "rclcpp_service_callback_added";
    private static final String RCL_CLIENT_INIT = "rcl_client_init";
    private static final String RCL_TIMER_INIT = "rcl_timer_init";
    private static final String RCLCPP_TIMER_CALLBACK_ADDED = "rclcpp_timer_callback_added";
    private static final String RCLCPP_TIMER_LINK_NODE = "rclcpp_timer_link_node";
    private static final String RCLCPP_CALLBACK_REGISTER = "rclcpp_callback_register";
    private static final String CALLBACK_START = "callback_start";
    private static final String CALLBACK_END = "callback_end";
    private static final String RCL_LIFECYCLE_STATE_MACHINE_INIT = "rcl_lifecycle_state_machine_init";
    private static final String RCL_LIFECYCLE_TRANSITION = "rcl_lifecycle_transition";
    private static final String RCLCPP_EXECUTOR_GET_NEXT_READY = "rclcpp_executor_get_next_ready";
    private static final String RCLCPP_EXECUTOR_WAIT_FOR_WORK = "rclcpp_executor_wait_for_work";
    private static final String RCLCPP_EXECUTOR_EXECUTE = "rclcpp_executor_execute";

    private static final String MESSAGE_LINK_PERIODIC_ASYNC = "message_link_periodic_async";
    private static final String MESSAGE_LINK_PARTIAL_SYNC = "message_link_partial_sync";

    private static final String DDS_CREATE_WRITER = "dds:create_writer";
    private static final String DDS_WRITE_PRE = "dds:write_pre";
    private static final String DDS_WRITE = "dds:write";
    private static final String DDS_CREATE_READER = "dds:create_reader";
    private static final String DDS_READ = "dds:read";

    /** Field names */
    private static final String CONTEXT_HANDLE = "context_handle";
    private static final String VERSION = "version";
    private static final String NODE_HANDLE = "node_handle";
    private static final String RMW_HANDLE = "rmw_handle";
    private static final String NODE_NAME = "node_name";
    private static final String NAMESPACE = "namespace";
    private static final String RMW_PUBLISHER_HANDLE = "rmw_publisher_handle";
    private static final String GID = "gid";
    private static final String PUBLISHER_HANDLE = "publisher_handle";
    private static final String TOPIC_NAME = "topic_name";
    private static final String QUEUE_DEPTH = "queue_depth";
    private static final String MESSAGE = "message";
    private static final String RMW_SUBSCRIPTION_HANDLE = "rmw_subscription_handle";
    private static final String SUBSCRIPTION_HANDLE = "subscription_handle";
    private static final String SUBSCRIPTION = "subscription";
    private static final String CALLBACK = "callback";
    private static final String SOURCE_TIMESTAMP = "source_timestamp";
    private static final String TAKEN = "taken";
    private static final String SERVICE_HANDLE = "service_handle";
    private static final String RMW_SERVICE_HANDLE = "rmw_service_handle";
    private static final String SERVICE_NAME = "service_name";
    private static final String CLIENT_HANDLE = "client_handle";
    private static final String RMW_CLIENT_HANDLE = "rmw_client_handle";
    private static final String TIMER_HANDLE = "timer_handle";
    private static final String PERIOD = "period";
    private static final String SYMBOL = "symbol";
    private static final String IS_INTRA_PROCESS = "is_intra_process";
    private static final String STATE_MACHINE = "state_machine";
    private static final String START_LABEL = "start_label";
    private static final String GOAL_LABEL = "goal_label";
    private static final String TIMEOUT = "timeout";
    private static final String HANDLE = "handle";

    private static final String SUBS = "subs";
    private static final String PUBS = "pubs";

    private static final String GID_PREFIX = "gid_prefix";
    private static final String GID_ENTITY = "gid_entity";
    private static final String WRITER = "writer";
    private static final String DATA = "data";
    private static final String TIMESTAMP = "timestamp";
    private static final String READER = "reader";
    private static final String BUFFER = "buffer";

    /** Context */
    private static final String VPID = "context._vpid";
    private static final String VTID = "context._vtid";
    private static final String PROCNAME = "context._procname";
    private static final String PERF_THREAD_TASK_CLOCK = "context._perf_thread_task_clock";

    /**
     * Constructor
     */
    protected Ros2RollingEventLayout() {
        // Do nothing
    }

    private static final Ros2RollingEventLayout INSTANCE = new Ros2RollingEventLayout();

    /**
     * Get a singleton instance
     *
     * @return the instance
     */
    public static Ros2RollingEventLayout getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------
    // Events
    // ------------------------------------------------------------------------

    // rcl_init

    @Override
    public String eventRclInit() {
        return getProviderName() + RCL_INIT;
    }

    // rcl_node_init

    @Override
    public String eventRclNodeInit() {
        return getProviderName() + RCL_NODE_INIT;
    }

    // rmw_publisher_init

    @Override
    public String eventRmwPublisherInit() {
        return getProviderName() + RMW_PUBLISHER_INIT;
    }

    // rcl_publisher_init

    @Override
    public String eventRclPublisherInit() {
        return getProviderName() + RCL_PUBLISHER_INIT;
    }

    // rclcpp_publish

    @Override
    public String eventRclcppPublish() {
        return getProviderName() + RCLCPP_PUBLISH;
    }

    // rcl_publish

    @Override
    public String eventRclPublish() {
        return getProviderName() + RCL_PUBLISH;
    }

    // rmw_publish

    @Override
    public String eventRmwPublish() {
        return getProviderName() + RMW_PUBLISH;
    }

    // rmw_subscription_init

    @Override
    public String eventRmwSubscriptionInit() {
        return getProviderName() + RMW_SUBSCRIPTION_INIT;
    }

    // rcl_subscription_init

    @Override
    public String eventRclSubscriptionInit() {
        return getProviderName() + RCL_SUBSCRIPTION_INIT;
    }

    // rclcpp_subscription_init

    @Override
    public String eventRclcppSubscriptionInit() {
        return getProviderName() + RCLCPP_SUBSCRIPTION_INIT;
    }

    // rclcpp_subscription_callback_added

    @Override
    public String eventRclcppSubscriptionCallbackAdded() {
        return getProviderName() + RCLCPP_SUBSCRIPTION_CALLBACK_ADDED;
    }

    // rmw_take

    @Override
    public String eventRmwTake() {
        return getProviderName() + RMW_TAKE;
    }

    // rcl_take

    @Override
    public String eventRclTake() {
        return getProviderName() + RCL_TAKE;
    }

    // rclcpp_take

    @Override
    public String eventRclcppTake() {
        return getProviderName() + RCLCPP_TAKE;
    }

    // rcl_service_init

    @Override
    public String eventRclServiceInit() {
        return getProviderName() + RCL_SERVICE_INIT;
    }

    // rclcpp_service_callback_added

    @Override
    public String eventRclcppServiceCallbackAdded() {
        return getProviderName() + RCLCPP_SERVICE_CALLBACK_ADDED;
    }

    // rcl_client_init

    @Override
    public String eventRclClientInit() {
        return getProviderName() + RCL_CLIENT_INIT;
    }

    // rcl_timer_init

    @Override
    public String eventRclTimerInit() {
        return getProviderName() + RCL_TIMER_INIT;
    }

    // rclcpp_timer_callback_added

    @Override
    public String eventRclcppTimerCallbackAdded() {
        return getProviderName() + RCLCPP_TIMER_CALLBACK_ADDED;
    }

    // rclcpp_timer_link_node

    @Override
    public String eventRclcppTimerLinkNode() {
        return getProviderName() + RCLCPP_TIMER_LINK_NODE;
    }

    // rclcpp_callback_register

    @Override
    public String eventRclcppCallbackRegister() {
        return getProviderName() + RCLCPP_CALLBACK_REGISTER;
    }

    // callback_start

    @Override
    public String eventCallbackStart() {
        return getProviderName() + CALLBACK_START;
    }

    // callback_end

    @Override
    public String eventCallbackEnd() {
        return getProviderName() + CALLBACK_END;
    }

    // rcl_lifecycle_state_machine_init

    @Override
    public String eventRclLifecycleStateMachineInit() {
        return getProviderName() + RCL_LIFECYCLE_STATE_MACHINE_INIT;
    }

    // rcl_lifecycle_transition

    @Override
    public String eventRclLifecycleTransition() {
        return getProviderName() + RCL_LIFECYCLE_TRANSITION;
    }

    // rclcpp_executor_get_next_ready

    @Override
    public String eventRclcppExecutorGetNextReady() {
        return getProviderName() + RCLCPP_EXECUTOR_GET_NEXT_READY;
    }

    // rclcpp_executor_wait_for_work

    @Override
    public String eventRclcppExecutorWaitForWork() {
        return getProviderName() + RCLCPP_EXECUTOR_WAIT_FOR_WORK;
    }

    // rclcpp_executor_execute

    @Override
    public String eventRclcppExecutorExecute() {
        return getProviderName() + RCLCPP_EXECUTOR_EXECUTE;
    }

    // Message causal links

    // message_link_periodic_async

    @Override
    public String eventMessageLinkPeriodicAsync() {
        return getProviderName() + MESSAGE_LINK_PERIODIC_ASYNC;
    }

    // message_link_partial_sync

    @Override
    public String eventMessageLinkPartialSync() {
        return getProviderName() + MESSAGE_LINK_PARTIAL_SYNC;
    }

    // DDS

    // dds:create_writer

    @Override
    public String eventDdsCreateWriter() {
        return DDS_CREATE_WRITER;
    }

    // dds:write_pre

    @Override
    public String eventDdsWritePre() {
        return DDS_WRITE_PRE;
    }

    // dds:write

    @Override
    public String eventDdsWrite() {
        return DDS_WRITE;
    }

    // dds:create_reader

    @Override
    public String eventDdsCreateReader() {
        return DDS_CREATE_READER;
    }

    // dds:read

    @Override
    public String eventDdsRead() {
        return DDS_READ;
    }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    @Override
    public String fieldContextHandle() {
        return CONTEXT_HANDLE;
    }

    @Override
    public String fieldVersion() {
        return VERSION;
    }

    @Override
    public String fieldNodeHandle() {
        return NODE_HANDLE;
    }

    @Override
    public String fieldRmwHandle() {
        return RMW_HANDLE;
    }

    @Override
    public String fieldNodeName() {
        return NODE_NAME;
    }

    @Override
    public String fieldNamespace() {
        return NAMESPACE;
    }

    @Override
    public String fieldRmwPublisherHandle() {
        return RMW_PUBLISHER_HANDLE;
    }

    @Override
    public String fieldGid() {
        return GID;
    }

    @Override
    public String fieldPublisherHandle() {
        return PUBLISHER_HANDLE;
    }

    @Override
    public String fieldTopicName() {
        return TOPIC_NAME;
    }

    @Override
    public String fieldQueueDepth() {
        return QUEUE_DEPTH;
    }

    @Override
    public String fieldMessage() {
        return MESSAGE;
    }

    @Override
    public String fieldRmwSubscriptionHandle() {
        return RMW_SUBSCRIPTION_HANDLE;
    }

    @Override
    public String fieldSubscriptionHandle() {
        return SUBSCRIPTION_HANDLE;
    }

    @Override
    public String fieldSubscription() {
        return SUBSCRIPTION;
    }

    @Override
    public String fieldCallback() {
        return CALLBACK;
    }

    @Override
    public String fieldSourceTimestamp() {
        return SOURCE_TIMESTAMP;
    }

    @Override
    public String fieldTaken() {
        return TAKEN;
    }

    @Override
    public String fieldServiceHandle() {
        return SERVICE_HANDLE;
    }

    @Override
    public String fieldRmwServiceHandle() {
        return RMW_SERVICE_HANDLE;
    }

    @Override
    public String fieldServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String fieldClientHandle() {
        return CLIENT_HANDLE;
    }

    @Override
    public String fieldRmwClientHandle() {
        return RMW_CLIENT_HANDLE;
    }

    @Override
    public String fieldTimerHandle() {
        return TIMER_HANDLE;
    }

    @Override
    public String fieldPeriod() {
        return PERIOD;
    }

    @Override
    public String fieldSymbol() {
        return SYMBOL;
    }

    @Override
    public String fieldIsIntraProcess() {
        return IS_INTRA_PROCESS;
    }

    @Override
    public String fieldStateMachine() {
        return STATE_MACHINE;
    }

    @Override
    public String fieldStartLabel() {
        return START_LABEL;
    }

    @Override
    public String fieldGoalLabel() {
        return GOAL_LABEL;
    }

    @Override
    public String fieldTimeout() {
        return TIMEOUT;
    }

    @Override
    public String fieldHandle() {
        return HANDLE;
    }

    // Message causal links

    @Override
    public String fieldSubs() {
        return SUBS;
    }

    @Override
    public String fieldPubs() {
        return PUBS;
    }

    // DDS

    @Override
    public String fieldGidPrefix() {
        return GID_PREFIX;
    }

    @Override
    public String fieldGidEntity() {
        return GID_ENTITY;
    }

    @Override
    public String fieldWriter() {
        return WRITER;
    }

    @Override
    public String fieldData() {
        return DATA;
    }

    @Override
    public String fieldTimestamp() {
        return TIMESTAMP;
    }

    @Override
    public String fieldReader() {
        return READER;
    }

    @Override
    public String fieldBuffer() {
        return BUFFER;
    }

    // ------------------------------------------------------------------------
    // Context fields
    // Note: The CTF parser exposes contexts as fields called "context._<name>"
    // ------------------------------------------------------------------------

    @Override
    public String contextVpid() {
        return VPID;
    }

    @Override
    public String contextVtid() {
        return VTID;
    }

    @Override
    public String contextProcname() {
        return PROCNAME;
    }

    @Override
    public String contextPerfThreadTaskClock() {
        return PERF_THREAD_TASK_CLOCK;
    }
}
