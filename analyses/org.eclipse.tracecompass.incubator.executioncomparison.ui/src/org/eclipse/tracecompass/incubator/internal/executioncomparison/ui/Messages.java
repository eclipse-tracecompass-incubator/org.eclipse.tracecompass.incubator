/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
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
 * Message bundle class
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.executioncomparison.ui.messages"; //$NON-NLS-1$

    /**
     * The color definition for the tool background.
     */
    public static String background;

    /**
     * The label for the data selection portion of the execution comparison ui
     */
    public static String dataSelection;

    /**
     * The label of GroupA
     */
    public static String multipleDensityViewGroupA;

    /**
     * The label of GroupB
     */
    public static String multipleDensityViewGroupB;

    /**
     * The label of Query Group
     */
    public static String multipleDensityViewQueryGroup;

    /**
     * The label of Query expandable item
     */
    public static String multipleDensityViewQueryExpandable;

    /**
     * The label for time range selection (from)
     */
    public static String multipleDensityViewFrom;

    /**
     * The label for time range selection (to)
     */
    public static String multipleDensityViewTo;

    /**
     * The label for "compare to" in query
     */
    public static String multipleDensityViewQueryCompare;

    /**
     * The Duration statistics
     */
    public static String multipleDensityViewDuration;

    /**
     * The selfTime statistic
     */
    public static String multipleDensityViewSelfTime;

    /**
     * The title of execution comparison view
     */
    public static String multipleDensityViewTitle;

    /**
     * The id of execution comparison view
     */
    public static String multipleDensityViewId;

    /**
     * The color definition for the tool foreground.
     */
    public static String foreground;

    /**
     * The action name for grouping
     */
    public static String flameGraphViewGroupByName;

    /**
     * The action tooltip for selecting between Duration and SelfTime
     */
    public static String flameGraphViewStatisticTooltip;

    /**
     * Execution of the callGraph Analysis
     */
    public static String flameGraphViewRetrievingData;

    /**
     * Name of the traces
     */
    public static String traceName;

    /**
     * The context of the TmfView
     */
    public static String tmfViewUiContext;

    /**
     * Name of the statistics
     */
    public static String statisticName;

    /**
     * Label for the Y Axis
     */
    public static String yAxisLabel;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
