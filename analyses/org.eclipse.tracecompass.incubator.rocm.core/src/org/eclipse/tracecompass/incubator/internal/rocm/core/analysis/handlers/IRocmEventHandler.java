/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers;

import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Interface to use for the event handlers.
 *
 * @author Arnaud Fiorini
 */
public interface IRocmEventHandler {

    /**
     * Default function to handle events.
     *
     * @param event
     *            The event to handle
     * @param ssb
     *            The state system builder
     * @param layout
     *            The layout of the events
     */
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, RocmEventLayout layout);
}
