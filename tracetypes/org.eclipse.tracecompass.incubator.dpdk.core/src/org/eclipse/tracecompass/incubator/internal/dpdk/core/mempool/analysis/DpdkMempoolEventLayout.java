/**********************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis;

/**
 * This class defines the names of DPDK mempool-related events and their fields.
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolEventLayout {
    /* Event names */
    private static final String MEMPOOL_CREATE = "lib.mempool.create"; //$NON-NLS-1$
    private static final String MEMPOOL_CREATE_EMPTY = "lib.mempool.create.empty"; //$NON-NLS-1$
    private static final String MEMPOOL_GENERIC_GET = "lib.mempool.generic.get"; //$NON-NLS-1$
    private static final String MEMPOOL_GENERIC_PUT = "lib.mempool.generic.put"; //$NON-NLS-1$
    private static final String MEMPOOL_FREE = "lib.mempool.free"; //$NON-NLS-1$

    /* Event field names */
    private static final String MEMPOOL_ID = "mempool"; //$NON-NLS-1$
    private static final String MEMPOOL_NAME = "name"; //$NON-NLS-1$
    private static final String MEMPOOL_NB_OBJS = "nb_objs"; //$NON-NLS-1$
    private static final String THREAD_NAME = "context.name"; //$NON-NLS-1$
    private static final String CPU_ID = "context.cpu_id"; //$NON-NLS-1$

    private DpdkMempoolEventLayout() {
        // do nothing
    }

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * This event is generated when a new mempool is created
     *
     * @return The event name
     */
    public static String eventMempoolCreate() {
        return MEMPOOL_CREATE;
    }

    /**
     * This event is generated when a new empty mempool is created
     *
     * @return The event name
     */
    public static String eventMempoolCreateEmpty() {
        return MEMPOOL_CREATE_EMPTY;
    }

    /**
     * This event is generated after getting an object from a mempool
     *
     * @return The event name
     */
    public static String eventMempoolGenericGet() {
        return MEMPOOL_GENERIC_GET;
    }

    /**
     * This event is generated after putting an object back to the mempool
     *
     * @return The event name
     */
    public static String eventMempoolGenericPut() {
        return MEMPOOL_GENERIC_PUT;
    }

    /**
     * This event is triggered when a mempool is freed
     *
     * @return The event name
     */
    public static String eventMempoolFree() {
        return MEMPOOL_FREE;
    }

    // ------------------------------------------------------------------------
    // Event field names
    // ------------------------------------------------------------------------

    /**
     * @return The name of the field specifying the ID of the mempool
     */
    public static String fieldMempoolId() {
        return MEMPOOL_ID;
    }

    /**
     * @return The name of the field specifying the name of the mempool
     */
    public static String fieldMempoolName() {
        return MEMPOOL_NAME;
    }

    /**
     * @return The name of the field specifying the number of objects retrieved
     *         or returned back to the mempool
     */
    public static String fieldMempoolNbObjs() {
        return MEMPOOL_NB_OBJS;
    }

    /**
     * @return The name of the field indicating the name of the thread
     *         performing the get/put operation
     */
    public static String fieldThreadName() {
        return THREAD_NAME;
    }

    /**
     * @return The name of the field indicating the ID of the CPU on which the
     *         DPDK event was recorded
     */
    public static String fieldCpuId() {
        return CPU_ID;
    }
}
