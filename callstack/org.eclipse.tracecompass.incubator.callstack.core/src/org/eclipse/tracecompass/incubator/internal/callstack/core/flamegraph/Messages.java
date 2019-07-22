/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.core.flamegraph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Message bundle for the flame graph view
 *
 * @author Sonia Farrah
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$
    /** Title of the data provider */
    public static String FlameGraphDataProvider_Title;
    /** Label for the symbol */
    public static String FlameGraph_Symbol;
    /** Label for the CPU times */
    public static String FlameGraph_Total;
    /** Label for total CPU time */
    public static String FlameGraph_Average;
    /** Label for average CPU time */
    public static String FlameGraph_Min;
    /** Label for minimum CPU time */
    public static String FlameGraph_Max;
    /** Label for maximum CPU time */
    public static String FlameGraph_Deviation;
    /**
     * The number of calls of a function
     */
    public static String FlameGraph_NbCalls;
    /**
     * The depth of a function
     */
    public static String FlameGraph_Depth;
    /**
     * Percentage text
     */
    public static String FlameGraph_Percentage;

    /** Title of kernel status rows */
    public static @Nullable String FlameGraph_KernelStatusTitle;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}