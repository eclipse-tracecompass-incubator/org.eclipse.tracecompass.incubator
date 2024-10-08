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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.rate.analysis;

/**
 * This file defines all the attribute names used in the handler. Both the
 * construction and query steps should use them.
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings({ "nls" })
public interface Attributes {

    /* First-level attributes */

    /** Root attribute for DPDK Ethdev Nics */
    String NICS = "NICs";
    /** Reception Queues */
    String RX_Q = "Rx_Q";
    /** Transmission Queues */
    String TX_Q = "Tx_Q";
}
