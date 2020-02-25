/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.callstack.context;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the context callstack analysis
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.core.callstack.context.messages"; //$NON-NLS-1$
    /**
     * Group by events
     */
    public static @Nullable String ContextCallStackAnalysis_GroupEvents;
    /**
     * Group by process
     */
    public static @Nullable String ContextCallStackAnalysis_GroupProcess;
    /**
     * Group by thread
     */
    public static @Nullable String ContextCallStackAnalysis_GroupThread;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
