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

package org.eclipse.tracecompass.incubator.internal.dpdk.ui.ethdev.throughput.pps;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Translatable strings for the {@link NicQueueThroughputPpsView} View
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.ui.ethdev.throughput.pps.messages"; //$NON-NLS-1$
    /** Title of the DPDK Ethernet rate view */
    public static @Nullable String EthdevThroughputPpsView_Title;
    /** Title of the DPDK Ethernet rate viewer */
    public static @Nullable String EthdevThroughputPpsViewer_Title;
    /** X axis caption */
    public static @Nullable String EthdevThroughputPpsViewer_XAxis;
    /** Port ID column */
    public static @Nullable String EthdevThroughputPpsTreeViewer_PortName;
    /** Legend Column */
    public static @Nullable String EthdevThroughputPpsTreeViewer_Legend;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
