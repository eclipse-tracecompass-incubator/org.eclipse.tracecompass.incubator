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

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.AbstractDpdkStateProvider;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;

/**
 *
 * <p>
 * Dpdk Mempool State Provider
 * </p>
 * <p>
 * In the state system, there is a single root attribute with the following
 * structure:
 * </p>
 *
 * <p>
 * Mempools: Contains information for each mempool. Each mempool has an
 * <em>ID</em>, a name, and a <em>Threads</em> sub-attribute. Under
 * <em>Threads</em>, each thread’s name is listed, along with two
 * sub-attributes, the <em>alloc</em> and <em>free</em>, which track the total
 * number of mempool objects that have been allocated or freed so far through
 * get/put operations.
 * </p>
 *
 * <pre>
 * Mempools
 *  | - <ID>
 *       | - name  -> Mempool name
 *       | - Threads  -> List of DPDK threads using the mempool
 *             | - <Thread Name> -> Name of the DPDK thread
 *                  | - alloc -> Current number of mempool objects being allocated
 *                  | - free -> Current number of mempool objects being freed
 * </pre>
 *
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolStateProvider extends AbstractDpdkStateProvider {

    private static final int VERSION = 1;
    /** Map events needed for this analysis with their handler functions */
    private @Nullable Map<String, IDpdkEventHandler> fEventNames;

    /**
     * Constructor
     *
     * @param trace
     *            trace
     * @param id
     *            id
     */
    protected DpdkMempoolStateProvider(ITmfTrace trace, String id) {
        super(trace, id);
    }

    /**
     * Get the version of this state provider
     */
    @Override
    public int getVersion() {
        return VERSION;
    }

    /**
     * Get a new instance
     */
    @Override
    public ITmfStateProvider getNewInstance() {
        return new DpdkMempoolStateProvider(this.getTrace(), DpdkMempoolAnalysisModule.ID);
    }

    @Override
    protected @Nullable IDpdkEventHandler getEventHandler(String eventName) {
        if (fEventNames == null) {
            ImmutableMap.Builder<String, IDpdkEventHandler> builder = ImmutableMap.builder();
            IDpdkEventHandler mempoolEventHandler = new DpdkMempoolEventHandler();
            builder.put(DpdkMempoolEventLayout.eventMempoolCreate(), mempoolEventHandler);
            builder.put(DpdkMempoolEventLayout.eventMempoolCreateEmpty(), mempoolEventHandler);
            builder.put(DpdkMempoolEventLayout.eventMempoolGenericGet(), mempoolEventHandler);
            builder.put(DpdkMempoolEventLayout.eventMempoolGenericPut(), mempoolEventHandler);
            builder.put(DpdkMempoolEventLayout.eventMempoolFree(), mempoolEventHandler);

            fEventNames = builder.build();
        }
        if (fEventNames != null) {
            return fEventNames.get(eventName);
        }
        return null;
    }
}
