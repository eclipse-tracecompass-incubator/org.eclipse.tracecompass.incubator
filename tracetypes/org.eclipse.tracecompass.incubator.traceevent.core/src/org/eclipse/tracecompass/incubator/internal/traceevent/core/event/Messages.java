/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Matthew Khouzam
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.traceevent.core.event.messages"; //$NON-NLS-1$
    /**
     * Arguments
     */
    public static String TraceCompassScopeLogAspects_Args;
    /**
     * Arguments Description
     */
    public static String TraceCompassScopeLogAspects_ArgsD;
    /**
     * Callsite
     */
    public static String TraceCompassScopeLogAspects_Callsite;
    /**
     * Callsite Description
     */
    public static String TraceCompassScopeLogAspects_CallsiteD;
    /**
     * Category
     */
    public static String TraceCompassScopeLogAspects_Category;
    /**
     * Category Description
     */
    public static String TraceCompassScopeLogAspects_CategoryD;
    /**
     * ID
     */
    public static String TraceCompassScopeLogAspects_Id;
    /**
     * ID Description
     */
    public static String TraceCompassScopeLogAspects_IdD;
    /**
     * Log Level
     */
    public static String TraceCompassScopeLogAspects_LogLevel;
    /**
     * Log Level Description
     */
    public static String TraceCompassScopeLogAspects_LogLevelD;
    /**
     * Name
     */
    public static String TraceCompassScopeLogAspects_Name;
    /**
     * Name Description
     */
    public static String TraceCompassScopeLogAspects_NameD;
    /**
     * Phase
     */
    public static String TraceCompassScopeLogAspects_Phase;
    /**
     * Phase Description
     */
    public static String TraceCompassScopeLogAspects_PhaseD;
    /**
     * Thread Id
     */
    public static String TraceCompassScopeLogAspects_ThreadId;
    /**
     * Thread Id Description
     */
    public static String TraceCompassScopeLogAspects_ThreadIdD;
    /**
     * Duration
     */
    public static String TraceEventAspects_Duration;
    /**
     * Duration Description
     */
    public static String TraceEventAspects_DurationD;
    /**
     * Process Id
     */
    public static String TraceEventAspects_Pid;
    /**
     * Process Id Description
     */
    public static String TraceEventAspects_PidD;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
