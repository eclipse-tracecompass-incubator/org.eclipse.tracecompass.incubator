/*******************************************************************************
 * Copyright (c) 2015, 2020 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.ui;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * @author Marc-Andre Laperle
 * @since 4.1
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.executioncomparision.ui.messages"; //$NON-NLS-1$


    /**
     * The label of GroupA
     */
    public static String AbstractMultipleDensityView_GroupA;


    /**
     *The label of GroupB
     */
    public static String AbstractMultipleDensityView_GroupB;


    /**
     *The Duration statistics
     */
    public static String AbstractMultipleDensityView_Duration;


    /**
     * The selfTime statistic
     */
    public static String AbstractMultipleDensityView_SelfTime;


    /**
     *The title of execution comparison view
     */
    public static String AbstractMultipleDensityView_title;


    /**
     * Label for the count axis of the density chart.
     */
    public static String ExecutionComparisionDurationViewer_TimeAxisLabel;

    /**
     * Label for the time axis of the density chart.
     */
    public static String ExecutionComparisionSelfTimeViewer_TimeAxisLabel;

    /** The action name for grouping */
    public static String FlameGraphView_GroupByName;
    /** The action tooltip for selecting between Duration and SelfTime */
    public static String FlameGraphView_StatisticTooltip;
    /**
     * Execution of the callGraph Analysis
     */
    public static String FlameGraphView_RetrievingData;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
