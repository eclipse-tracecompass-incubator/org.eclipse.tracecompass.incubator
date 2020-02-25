/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.criticalpath;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the aggregated critical path
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.core.criticalpath.messages"; //$NON-NLS-1$
    /** String for the aggregation by process name */
    public static @Nullable String CriticalPathWeighted_ByProcessName;
    /** String for the aggregation by status */
    public static @Nullable String CriticalPathWeighted_ByStatus;
    /** String for the aggregation by thread */
    public static @Nullable String CriticalPathWeighted_ByThread;
    /** Label for the other running process element */
    public static @Nullable String CriticalPathWeighted_OtherRunningProcess;
    /** String for the self worker */
    public static @Nullable String CriticalPathWeighted_SelfWorker;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
