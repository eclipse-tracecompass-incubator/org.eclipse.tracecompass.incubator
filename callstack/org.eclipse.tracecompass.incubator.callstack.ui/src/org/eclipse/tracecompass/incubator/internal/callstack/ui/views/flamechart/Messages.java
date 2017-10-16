/*******************************************************************************
 * Copyright (c) 2012, 2016 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.flamechart;

import org.eclipse.osgi.util.NLS;

/**
 * TMF message bundle
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {

    private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static String CallStackPresentationProvider_Thread;
    public static String CallStackView_FunctionColumn;

    public static String CallStackView_DepthColumn;

    public static String CallStackView_EntryTimeColumn;

    public static String CallStackView_ExitTimeColumn;

    public static String CallStackView_DurationColumn;

    public static String CallStackView_ThreadColumn;

    public static String CallStackView_StackInfoNotAvailable;

    public static String CallStackView_SortByThreadName;

    public static String CallStackView_SortByThreadId;

    public static String CallStackView_SortByThreadTime;


    public static String CallStackView_ConfigureSymbolProvidersText;

    public static String CallStackView_ConfigureSymbolProvidersTooltip;


    public static String CallStackView_NextStateChangeHelp;

    public static String CallStackView_NextStateChangeText;

    public static String CallStackView_PreviousStateChangeHelp;

    public static String CallStackView_PreviousStateChangeText;

    public static String CallStackView_KernelStatus;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
