/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core;

import org.eclipse.tracecompass.common.core.TraceCompassActivator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackPublicationInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2MessageTransportInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TimerCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2NodeObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;

/**
 * Activator
 */
@SuppressWarnings("restriction")
public class Activator extends TraceCompassActivator {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "org.eclipse.tracecompass.incubator.ros2.core"; //$NON-NLS-1$

    /**
     * The constructor
     */
    public Activator() {
        super(PLUGIN_ID);
    }

    /**
     * Returns the instance of this plug-in
     *
     * @return The plugin instance
     */
    public static TraceCompassActivator getInstance() {
        return TraceCompassActivator.getInstance(PLUGIN_ID);
    }

    @Override
    protected void startActions() {
        // General
        CustomStateValue.registerCustomFactory(HostProcessPointer.CUSTOM_TYPE_ID_HOST_PROCESS_POINTER, HostProcessPointer.HOST_PROCESS_POINTER_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2ObjectHandle.CUSTOM_TYPE_ID_ROS2_OBJECT_HANDLE, Ros2ObjectHandle.ROS2_OBJECT_HANDLE_VALUE_FACTORY);
        // Objects analysis
        CustomStateValue.registerCustomFactory(Ros2NodeObject.CUSTOM_TYPE_ID, Ros2NodeObject.ROS2_NODE_OBJECT_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2PublisherObject.CUSTOM_TYPE_ID, Ros2PublisherObject.ROS2_PUBLISHER_OBJECT_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2SubscriptionObject.CUSTOM_TYPE_ID, Ros2SubscriptionObject.ROS2_SUBSCRIPTION_OBJECT_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2TimerObject.CUSTOM_TYPE_ID, Ros2TimerObject.ROS2_TIMER_OBJECT_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2CallbackObject.CUSTOM_TYPE_ID, Ros2CallbackObject.ROS2_CALLBACK_OBJECT_VALUE_FACTORY);
        // Instances (for messages analysis)
        CustomStateValue.registerCustomFactory(Ros2PubInstance.CUSTOM_TYPE_ID, Ros2PubInstance.ROS2_PUB_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2TakeInstance.CUSTOM_TYPE_ID, Ros2TakeInstance.ROS2_TAKE_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2CallbackInstance.CUSTOM_TYPE_ID, Ros2CallbackInstance.ROS2_CALLBACK_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2SubCallbackInstance.CUSTOM_TYPE_ID, Ros2SubCallbackInstance.ROS2_SUB_CALLBACK_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2TimerCallbackInstance.CUSTOM_TYPE_ID, Ros2TimerCallbackInstance.ROS2_TIMER_CALLBACK_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2MessageTransportInstance.CUSTOM_TYPE_ID, Ros2MessageTransportInstance.ROS2_MESSAGE_TRANSPORT_INSTANCE_VALUE_FACTORY);
        CustomStateValue.registerCustomFactory(Ros2CallbackPublicationInstance.CUSTOM_TYPE_ID, Ros2CallbackPublicationInstance.ROS2_CALLBACK_PUBLICATION_INSTANCE_VALUE_FACTORY);
        // Executor analysis
        CustomStateValue.registerCustomFactory(Ros2ExecutorStateInstance.CUSTOM_TYPE_ID, Ros2ExecutorStateInstance.ROS2_EXECUTOR_STATE_INSTANCE_VALUE_FACTORY);
    }

    @Override
    protected void stopActions() {
        // Do nothing
    }
}
