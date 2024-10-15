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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages for {@link DpdkEthdevThroughputBpsDataProvider}
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis.messages"; //$NON-NLS-1$
    public static @Nullable String DpdkEthdev_ThroughputDataProvider_PORTS;
    public static @Nullable String DpdkEthdev_ThroughputDataProvider_TRAFFIC_RX;
    public static @Nullable String DpdkEthdev_ThroughputDataProvider_TRAFFIC_TX;
    public static @Nullable String DpdkEthdev_ThroughputBpsDataProvider_YAxis;
    public static @Nullable String DpdkEthdev_ThroughputPpsDataProvider_YAxis;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
