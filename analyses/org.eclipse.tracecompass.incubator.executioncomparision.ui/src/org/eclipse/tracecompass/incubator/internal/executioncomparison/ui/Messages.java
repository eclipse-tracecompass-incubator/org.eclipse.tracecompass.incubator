/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.ui;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * @author Fateme Faraji Daneshgar
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.executioncomparison.ui.messages"; //$NON-NLS-1$

    /**
     * The label of GroupA
     */
    public static String MultipleDensityView_GroupA;

    /**
     * The label of GroupB
     */
    public static String MultipleDensityView_GroupB;

    /**
     * The label of Query Group
     */
    public static String MultipleDensityView_QueryGroup;

    /**
     * The label of Query expandable item
     */
    public static String MultipleDensityView_QueryExpandable;

    /**
     * The label for time range selection (from)
     */
    public static String MultipleDensityView_From;

    /**
     * The label for time range selection (to)
     */
    public static String MultipleDensityView_To;

    /**
     * The label for "compare to" in query
     */
    public static String MultipleDensityView_QueryCompare;

    /**
     * The Duration statistics
     */
    public static String MultipleDensityView_Duration;

    /**
     * The selfTime statistic
     */
    public static String MultipleDensityView_SelfTime;

    /**
     * The title of execution comparison view
     */
    public static String MultipleDensityView_title;

    /**
     * The action name for grouping
     */
    public static String FlameGraphView_GroupByName;

    /**
     * The action tooltip for selecting between Duration and SelfTime
     */
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