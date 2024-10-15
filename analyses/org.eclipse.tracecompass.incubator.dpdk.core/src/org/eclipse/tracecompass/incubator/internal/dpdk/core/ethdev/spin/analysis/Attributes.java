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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

/**
 * This interface defines all the attribute names used in the state system.
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings({ "nls" })
public interface Attributes {

    /* First-level attributes */

    /** Root attribute for DPDK Ethdev Nics */
    String POLL_THREADS = "Threads";
    /** Reception Queues */
    /** */
    String SPIN_STATUS = "Spin";
    /** */
    String ACTIVE_STATUS = "Active";
}
