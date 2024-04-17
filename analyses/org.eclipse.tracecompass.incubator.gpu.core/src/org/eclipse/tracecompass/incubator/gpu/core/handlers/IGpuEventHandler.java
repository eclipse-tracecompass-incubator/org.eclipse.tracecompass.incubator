/*******************************************************************************
 * Copyright (c) 2024 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.handlers;

import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Interface to use for the event handlers.
 *
 * @author Arnaud Fiorini
 */
public interface IGpuEventHandler {

    /**
     * Default function to handle events.
     *
     * @param event
     *            The event to handle
     * @param ssb
     *            The state system builder
     * @param layout
     *            The layout of the events
     * @param stateProvider
     *            The state provider that calls this event handler
     */
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, IGpuTraceEventLayout layout, ITmfStateProvider stateProvider);
}
