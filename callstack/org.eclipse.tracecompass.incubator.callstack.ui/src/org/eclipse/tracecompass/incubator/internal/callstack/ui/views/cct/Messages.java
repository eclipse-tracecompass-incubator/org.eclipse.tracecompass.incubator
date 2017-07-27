/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.cct;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the calling context tree view
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.callstack.ui.views.cct.messages"; //$NON-NLS-1$
    public static @Nullable String CallingContextTreeViewer_CallSite;
    public static @Nullable String CallingContextTreeViewer_CpuTime;
    public static @Nullable String CallingContextTreeViewer_Duration;
    public static @Nullable String CallingContextTreeViewer_NbCalls;
    public static @Nullable String CallingContextTreeViewer_SelfTime;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
