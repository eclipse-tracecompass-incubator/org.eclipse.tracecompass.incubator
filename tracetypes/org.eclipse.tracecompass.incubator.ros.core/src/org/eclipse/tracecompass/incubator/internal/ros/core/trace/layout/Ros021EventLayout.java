/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Definitions used in tracetools versions up to 0.2.1
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("nls")
@NonNullByDefault
public class Ros021EventLayout implements IRosEventLayout {

    /**
     * Constructor
     */
    protected Ros021EventLayout() {
        // Do nothing
    }

    private static final Ros021EventLayout INSTANCE = new Ros021EventLayout();

    /**
     * Get a singleton instance
     *
     * @return the instance
     */
    public static Ros021EventLayout getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------
    // init_node
    // ------------------------------------------------------------------------

    @Override
    public String eventInitNode() {
        return "roscpp:init_node";
    }

    // ------------------------------------------------------------------------
    // shutdown_node
    // ------------------------------------------------------------------------

    @Override
    public String eventShutdownNode() {
        return "roscpp:shutdown_node";
    }

    // ------------------------------------------------------------------------
    // new_connection
    // ------------------------------------------------------------------------

    @Override
    public String eventNewConnection() {
        return "roscpp:new_connection";
    }

    // ------------------------------------------------------------------------
    // subscriber_callback_added
    // ------------------------------------------------------------------------

    @Override
    public String eventSubCallbackAdded() {
        return "roscpp:subscriber_callback_added";
    }

    // ------------------------------------------------------------------------
    // publisher_message_queued
    // ------------------------------------------------------------------------

    @Override
    public String eventPubMsgQueued() {
        return "roscpp:publisher_message_queued";
    }

    // ------------------------------------------------------------------------
    // subscriber_link_message_write
    // ------------------------------------------------------------------------

    @Override
    public String eventSubLinkMsgWrite() {
        return "roscpp:subscriber_link_message_write";
    }

    // ------------------------------------------------------------------------
    // subscriber_link_message_dropped
    // ------------------------------------------------------------------------

    @Override
    public String eventSubLinkMsgDropped() {
        return "roscpp:subscriber_link_message_dropped";
    }

    // ------------------------------------------------------------------------
    // publisher_link_handle_message
    // ------------------------------------------------------------------------

    @Override
    public String eventPubLinkHandleMsg() {
        return "roscpp:publisher_link_handle_message";
    }

    // ------------------------------------------------------------------------
    // subscription_message_queued
    // ------------------------------------------------------------------------

    @Override
    public String eventSubMsgQueued() {
        return "roscpp:subscription_message_queued";
    }

    // ------------------------------------------------------------------------
    // subscription_message_dropped
    // ------------------------------------------------------------------------

    @Override
    public String eventSubMsgDropped() {
        return "roscpp:subscription_message_dropped";
    }

    // ------------------------------------------------------------------------
    // callback_start
    // ------------------------------------------------------------------------

    @Override
    public String eventCallbackStart() {
        return "roscpp:callback_start";
    }

    // ------------------------------------------------------------------------
    // subscriber_callback_start
    // ------------------------------------------------------------------------

    @Override
    public String eventSubCallbackStart() {
        return "roscpp:subscriber_callback_start";
    }

    // ------------------------------------------------------------------------
    // subscriber_callback_end
    // ------------------------------------------------------------------------

    @Override
    public String eventSubCallbackEnd() {
        return "roscpp:subscriber_callback_end";
    }

    // ------------------------------------------------------------------------
    // callback_end
    // ------------------------------------------------------------------------

    @Override
    public String eventCallbackEnd() {
        return "roscpp:callback_end";
    }

    // ------------------------------------------------------------------------
    // task_start
    // ------------------------------------------------------------------------

    @Override
    public String eventTaskStart() {
        return "roscpp:task_start";
    }

    // ------------------------------------------------------------------------
    // timer_added
    // ------------------------------------------------------------------------

    @Override
    public String eventTimerAdded() {
        return "roscpp:timer_added";
    }

    // ------------------------------------------------------------------------
    // timer_scheduled
    // ------------------------------------------------------------------------

    @Override
    public String eventTimerScheduled() {
        return "roscpp:timer_scheduled";
    }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    @Override
    public String fieldNodeName() {
        return "node_name";
    }

    @Override
    public String fieldRosCppVersion() {
        return "roscpp_version_compiletime";
    }

    @Override
    public String fieldQueueRef() {
        return "queue_ref";
    }

    @Override
    public String fieldCallbackRef() {
        return "callback_ref";
    }

    @Override
    public String fieldTypeInfo() {
        return "type_info";
    }

    @Override
    public String fieldDataType() {
        return "data_type";
    }

    @Override
    public String fieldSourceName() {
        return "source_name";
    }

    @Override
    public String fieldQueueSize() {
        return "queue_size";
    }

    @Override
    public String fieldTopic() {
        return "topic";
    }

    @Override
    public String fieldBufferRef() {
        return "buffer_ref";
    }

    @Override
    public String fieldDataRef() {
        return "data_ref";
    }

    @Override
    public String fieldTracingId() {
        return "tracing_id";
    }

    @Override
    public String fieldMsgRef() {
        return "message_ref";
    }

    @Override
    public String fieldReceiptTimeSec() {
        return "receipt_time_sec";
    }

    @Override
    public String fieldReceiptTimeNsec() {
        return "receipt_time_nsec";
    }

    @Override
    public String fieldIsLatchedMsg() {
        return "is_latched_msg";
    }

    @Override
    public String fieldTaskName() {
        return "task_name";
    }

    @Override
    public String fieldFunctionName() {
        return "function_name";
    }

    @Override
    public String fieldPeriodSec() {
        return "period_sec";
    }

    @Override
    public String fieldPeriodNsec() {
        return "period_nsec";
    }

    @Override
    public String fieldCallbackQueueCbRef() {
        return "callback_queue_cb_ref";
    }

    @Override
    public String fieldLocalHostport() {
        return "local_hostport";
    }

    @Override
    public String fieldRemoteHostport() {
        return "remote_hostport";
    }

    @Override
    public String fieldChannelRef() {
        return "channel_ref";
    }

    @Override
    public String fieldChannelType() {
        return "channel_type";
    }

    @Override
    public String fieldName() {
        return "name";
    }

    // ------------------------------------------------------------------------
    // Context fields
    // Note: The CTF parser exposes contexts as fields called "context._<name>"
    // ------------------------------------------------------------------------

    @Override
    public String contextVpid() {
        return "context._vpid";
    }

    @Override
    public String contextVtid() {
        return "context._vtid";
    }

    @Override
    public String contextProcname() {
        return "context._procname";
    }
}
