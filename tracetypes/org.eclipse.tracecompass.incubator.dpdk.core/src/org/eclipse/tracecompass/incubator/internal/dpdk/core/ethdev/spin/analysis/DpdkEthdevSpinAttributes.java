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
public interface DpdkEthdevSpinAttributes {

    /** Root attribute for DPDK PMD threads */
    String POLL_THREADS = "Threads";
    /** Thread is polling with no packets retrieved */
    String SPIN_STATUS = "Spin";
    /** Thread is polling and retrieving at least one packet */
    String ACTIVE_STATUS = "Active";
}
