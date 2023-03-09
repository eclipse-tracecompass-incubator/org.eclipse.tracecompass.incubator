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

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Represents every event handler to store those in maps and used across all
 * analysis to handle events.
 *
 * @author Arnaud Fiorini
 */
public interface IDpdkEventHandler {

    /**
     * @param ssb
     *            The state system builder
     * @param event
     *            The event to handle
     */
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event);
}
