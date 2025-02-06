/*******************************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.usage;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the {@link MempoolUsageViewer} view
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.usage.messages"; //$NON-NLS-1$
    /** Title of the view */
    public static @Nullable String MempoolThreadUsageView_Title;
    /** X axis caption */
    public static @Nullable String MempoolThreadUsageViewer_XAxis;
    /** Y axis caption */
    public static @Nullable String MempoolThreadUsageViewer_YAxis;
    /** Mempool name column */
    public static @Nullable String MempoolThreadUsageTreeViewer_MempoolName;
    /** Legend column */
    public static @Nullable String MempoolThreadUsageTreeViewer_Legend;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
