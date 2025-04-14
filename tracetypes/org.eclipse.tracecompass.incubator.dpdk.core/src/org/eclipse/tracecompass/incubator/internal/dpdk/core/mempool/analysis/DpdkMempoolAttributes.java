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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis;

/**
 * This class defines all the attribute names used in the state system.
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolAttributes {
    /** Root attribute for DPDK Mempools */
    public static final String MEMPOOLS = "Mempools"; //$NON-NLS-1$
    /** Name of the mempool */
    public static final String MEMPOOL_NAME = "name"; //$NON-NLS-1$
    /** List of threads using the mempool */
    public static final String THREADS = "threads"; //$NON-NLS-1$
    /** Number of of objects allocated by a worker thread */
    public static final String THREAD_OBJ_ALLOC = "alloc"; //$NON-NLS-1$
    /** Number of of objects deallocated by a worker thread */
    public static final String THREAD_OBJ_FREE = "free"; //$NON-NLS-1$

    private DpdkMempoolAttributes() {
        // do nothing
    }
}
