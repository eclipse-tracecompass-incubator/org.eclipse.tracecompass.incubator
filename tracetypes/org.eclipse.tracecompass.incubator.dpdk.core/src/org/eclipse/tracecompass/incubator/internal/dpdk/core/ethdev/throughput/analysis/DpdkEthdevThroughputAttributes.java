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

/**
 * This interface defines all the attribute names used in the state system.
 *
 * @author Adel Belkhiri
 */
public interface DpdkEthdevThroughputAttributes {

    /** Root attribute for DPDK Ethdev Nics */
    String NICS = "NICs"; //$NON-NLS-1$
    /** Reception queues */
    String RX_Q = "rx_qs"; //$NON-NLS-1$
    /** Transmission queues */
    String TX_Q = "tx_qs"; //$NON-NLS-1$
    /** Packets number */
    String PKT_COUNT = "pkt_cnt"; //$NON-NLS-1$
    /** Packets size */
    String PKT_SIZE_P = "pkt_size_p"; //$NON-NLS-1$
    /** Packets number provided by the profiling library events */
    String PKT_COUNT_P = "pkt_cnt_p"; //$NON-NLS-1$

}
