/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view;

import org.eclipse.osgi.util.NLS;

/**
 * TMF message bundle
 *
 * @author Ivan Grinenko
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.messages"; //$NON-NLS-1$

    public static String Action_Set;

    public static String Dialog_SetDataProviderName;
    public static String Dialog_ListLabel;

    public static String ProviderType_XY;
    public static String ProviderType_TimeGraph;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
