/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart;

import org.eclipse.osgi.util.NLS;

/**
 * Messages related to the {@link ScriptedXYView}
 *
 * @author Benjamin Saint-Cyr
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart.messages"; //$NON-NLS-1$

    /** Default XY view title */
    public static String ScriptedXYTreeViewer_DefaultTitle;
    /** Name column */
    public static String ScriptedXYTreeViewer_Name;
    /** Legend Column */
    public static String ScriptedXYTreeViewer_Legend;
    /** Default Viewer title */
    public static String ScriptedXYTreeViewer_DefaultViewerTitle;
    /** default x axis title */
    public static String ScriptedXYTreeViewer_DefaultXAxis;
    /** default y axis title */
    public static String ScriptedXYTreeViewer_DefaultYAxis;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}