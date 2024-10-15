/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.AbstractDpdkStateProvider;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;

/**
 * State provider for the {@link DpdkEthdevSpinAnalysisModule} analysis
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinStateProvider extends AbstractDpdkStateProvider {

    private static final int VERSION = 1;

    /** Map events needed for this analysis with their handler functions */
    private @Nullable Map<String, IDpdkEventHandler> fEventNames;
    /** Events layout */
    private final DpdkEthdevEventLayout fLayout;

    /**
     * Constructor
     *
     * @param trace
     *            trace
     * @param layout
     *            layout
     * @param id
     *            id
     */
    protected DpdkEthdevSpinStateProvider(ITmfTrace trace, DpdkEthdevEventLayout layout, String id) {
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
        return new DpdkEthdevSpinStateProvider(this.getTrace(), fLayout, DpdkEthdevSpinAnalysisModule.ID);
    }

    @Override
    protected @Nullable IDpdkEventHandler getEventHandler(String eventName) {
        if (fEventNames == null) {
            ImmutableMap.Builder<String, IDpdkEventHandler> builder = ImmutableMap.builder();
            IDpdkEventHandler ethdevEventHandler = new DpdkEthdevSpinEventHandler(fLayout);
            builder.put(fLayout.eventEthdevRxBurstEmpty(), ethdevEventHandler);
            builder.put(fLayout.eventEthdevRxBurstNonEmpty(), ethdevEventHandler);
            fEventNames = builder.build();
        }
        if (fEventNames != null) {
            return fEventNames.get(eventName);
        }
        return null;
    }
}
