/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the call stack analysis module
 *
 * @author Sonia Farrah
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    /**
     * Name of the group by "All" descriptors
     */
    public static @Nullable String CallGraphAnalysis_AllDescriptors;
    /**
     * Analysis description for the help
     */
    public static @Nullable String CallGraphAnalysis_Description;
    /**
     * Prefix for the name of the analysis
     */
    public static @Nullable String CallGraphAnalysis_NamePrefix;
    /**
     * The call stack event's name
     */
    public static @Nullable String CallStack_FunctionName;
    /**
     * Querying state system error's message
     */
    public static @Nullable String QueringStateSystemError;
    /**
     * Segment's start time exceeding its end time Error message
     */
    public static @Nullable String TimeError;
    /** Duration statistics title */
    public static @Nullable String CallGraphStats_Duration;
    /** Self time statistics title */
    public static @Nullable String CallGraphStats_SelfTime;
    /** Cpu time statistics title */
    public static @Nullable String CallGraphStats_CpuTime;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
