/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
    /** Label for the object */
    public static String FlameGraph_Object;
    /** Label for the total time */
    public static String FlameGraph_Total;
    /** Label for average time */
    public static String FlameGraph_Average;
    /** Label for the minimum value */
    public static String FlameGraph_Min;
    /** Label for the maximum value */
    public static String FlameGraph_Max;
    /** Label for standard deviation */
    public static String FlameGraph_Deviation;
    /** Label for the goto min action */
    public static String FlameGraph_GoToMin;
    /** Label for the goto max action */
    public static String FlameGraph_GoToMax;
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