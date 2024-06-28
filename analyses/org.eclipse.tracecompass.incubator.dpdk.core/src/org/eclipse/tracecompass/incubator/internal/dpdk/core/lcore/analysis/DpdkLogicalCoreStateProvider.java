/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.AbstractDpdkStateProvider;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;

/**
 * State provider for Logical Core events and analysis
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkLogicalCoreStateProvider extends AbstractDpdkStateProvider {

    private static final int VERSION = 1;
    /** Map events needed for this analysis with their handler functions */
    private @Nullable Map<String, IDpdkEventHandler> fEventNames;
    /** Events layout */
    private final DpdkLogicalCoreEventLayout fLayout;

    /**
     * @param trace
     *            trace
     * @param layout
     *            layout
     * @param id
     *            id
     */
    protected DpdkLogicalCoreStateProvider(ITmfTrace trace, DpdkLogicalCoreEventLayout layout, String id) {
        super(trace, id);
        fLayout = layout;
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
        return new DpdkLogicalCoreStateProvider(this.getTrace(), this.fLayout, DpdkLogicalCoreAnalysisModule.ID);
    }

    @Override
    protected @Nullable IDpdkEventHandler getEventHandler(String eventName) {
        if (fEventNames == null) {
            ImmutableMap.Builder<String, IDpdkEventHandler> builder = ImmutableMap.builder();
            IDpdkEventHandler logicalCoreEventHandler = new DpdkLogicalCoreEventHandler(fLayout);
            builder.put(fLayout.eventLcoreStateChange(), logicalCoreEventHandler);
            builder.put(fLayout.eventServiceLcoreStart(), logicalCoreEventHandler);
            builder.put(fLayout.eventServiceLcoreStop(), logicalCoreEventHandler);
            builder.put(fLayout.eventThreadLcoreStopped(), logicalCoreEventHandler);
            builder.put(fLayout.eventThreadLcoreRunning(), logicalCoreEventHandler);
            builder.put(fLayout.eventThreadLcoreReady(), logicalCoreEventHandler);
            IDpdkEventHandler serviceEventHandler = new DpdkServiceEventHandler(fLayout);
            builder.put(fLayout.eventServiceComponentRegister(), serviceEventHandler);
            builder.put(fLayout.eventServiceMapLcore(), serviceEventHandler);
            builder.put(fLayout.eventServiceRunBegin(), serviceEventHandler);
            builder.put(fLayout.eventServiceRunEnd(), serviceEventHandler);
            builder.put(fLayout.eventServiceRunStateSet(), serviceEventHandler);
            fEventNames = builder.build();
        }
        if (fEventNames != null) {
            return fEventNames.get(eventName);
        }
        return null;
    }
}
