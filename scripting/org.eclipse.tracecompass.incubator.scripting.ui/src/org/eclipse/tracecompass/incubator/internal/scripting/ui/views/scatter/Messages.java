/*******************************************************************************
 * Copyright (c) 2020 VMware, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.scatter;

import org.eclipse.osgi.util.NLS;

/**
 * Messages related to the {@link ScriptedScatterView}
 *
 * @author Qing Chi
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.scripting.ui.views.scatter.messages"; //$NON-NLS-1$

    /** Default Viewer title */
    public static String ScriptedScatterTreeViewer_DefaultViewerTitle;
    /** default x axis title */
    public static String ScriptedScatterTreeViewer_DefaultXAxis;
    /** default y axis title */
    public static String ScriptedScatterTreeViewer_DefaultYAxis;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
