/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.contextswitch;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Matthew Khouzam
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.ui.views.contextswitch.messages"; //$NON-NLS-1$
    /**
     * Entry Title
     */
    public static String ContextSwitchPresentationProvider_CPU;
    /**
     * Tooltip message
     */
    public static String ContextSwitchPresentationProvider_NumberOfContextSwitch;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
