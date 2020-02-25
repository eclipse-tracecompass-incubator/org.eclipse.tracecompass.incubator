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

package org.eclipse.tracecompass.incubator.internal.ros.ui.views;

import org.eclipse.osgi.util.NLS;

/**
 * Messages for ROS presentation providers
 *
 * @author Christophe Bedard
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.ros.ui.views.messages"; //$NON-NLS-1$

    /** Connection channel type */
    public static String AbstractRosPresentationProvider_ConnectionChannelType;
    /** Connections local host */
    public static String AbstractRosPresentationProvider_ConnectionHostLocal;
    /** Connection remote host */
    public static String AbstractRosPresentationProvider_ConnectionHostRemote;
    /** Connection hosts */
    public static String AbstractRosPresentationProvider_ConnectionHosts;
    /** Connection name */
    public static String AbstractRosPresentationProvider_ConnectionName;
    /** Node name */
    public static String AbstractRosPresentationProvider_NodeName;
    /** PID */
    public static String AbstractRosPresentationProvider_PID;
    /** Queue position */
    public static String AbstractRosPresentationProvider_QueuePosition;
    /** Queue element reference */
    public static String AbstractRosPresentationProvider_QueueReference;
    /** Queue size */
    public static String AbstractRosPresentationProvider_QueueSize;
    /** Task name */
    public static String AbstractRosPresentationProvider_TaskName;
    /** Timer callback */
    public static String AbstractRosPresentationProvider_TimerCallback;
    /** Timer count */
    public static String AbstractRosPresentationProvider_TimerCount;
    /** Timer index */
    public static String AbstractRosPresentationProvider_TimerIndex;
    /** Timer period */
    public static String AbstractRosPresentationProvider_TimerPeriod;
    /** Timer queue reference */
    public static String AbstractRosPresentationProvider_TimerQueueReference;
    /** Topic name */
    public static String AbstractRosPresentationProvider_TopicName;
    /** Unknown value */
    public static String AbstractRosPresentationProvider_Unknown;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
