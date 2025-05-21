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

package org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.alloc.free.rate;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the {@link MempoolAllocFreeRateViewer} view
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.alloc.free.rate.messages"; //$NON-NLS-1$
    /** Title of the view */
    public static @Nullable String MempoolAllocFreeRateView_Title;
    /** X axis caption */
    public static @Nullable String MempoolAllocFreeRateViewer_XAxis;
    /** Y axis caption */
    public static @Nullable String MempoolAllocFreeRateViewer_YAxis;
    /** Mempool Name column */
    public static @Nullable String MempoolAllocFreeRateTreeViewer_MempoolName;
    /** Legend Column */
    public static @Nullable String MempoolAllocFreeRateTreeViewer_Legend;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
