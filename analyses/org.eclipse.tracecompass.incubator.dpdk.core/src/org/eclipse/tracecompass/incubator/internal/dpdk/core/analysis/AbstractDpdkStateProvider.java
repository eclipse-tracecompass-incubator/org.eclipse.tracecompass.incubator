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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Base state provider that will be used in all other analyses in this plugin
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public abstract class AbstractDpdkStateProvider extends AbstractTmfStateProvider {

    /**
     * Default constructor
     *
     * @param trace
     *            The trace to analyze
     * @param id
     *            The id of the analysis module
     */
    public AbstractDpdkStateProvider(ITmfTrace trace, String id) {
        super(trace, id);
    }

    /**
     * Dispatch required events to their handler while processing the trace.
     *
     * @param event
     *            Event being processed.
     */
    @Override
    protected void eventHandle(ITmfEvent event) {
        final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());
        IDpdkEventHandler eventHandler = getEventHandler(event.getName());
        if (eventHandler != null) {
            eventHandler.handleEvent(ss, event);
        }
    }

    /**
     * Get the correct event handler depending on the event name.
     *
     * @param eventName
     *            The event type in CTF
     * @return Event handler
     */
    protected abstract @Nullable IDpdkEventHandler getEventHandler(String eventName);

}
