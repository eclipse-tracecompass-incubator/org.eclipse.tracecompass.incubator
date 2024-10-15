/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.ui.ethdev.spin;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for the {@link ThreadSpinStatisticsViewer} view
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.ui.ethdev.spin.messages"; //$NON-NLS-1$
    /** Title of the view */
    public static @Nullable String EthdevThreadSpinStatsView_Title;
    /** Title of the viewer */
    public static @Nullable String EthdevThreadSpinStatsViewer_Title;
    /** X axis caption */
    public static @Nullable String EthdevThreadSpinStatsViewer_XAxis;
    /** Y axis caption */
    public static @Nullable String EthdevThreadSpinStatsViewer_YAxis;
    /**  column */
    public static @Nullable String EthdevThreadSpinStatsTreeViewer_ThreadName;
    /** Legend Column*/
    public static @Nullable String EthdevThreadSpinStatsTreeViewer_Legend;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
