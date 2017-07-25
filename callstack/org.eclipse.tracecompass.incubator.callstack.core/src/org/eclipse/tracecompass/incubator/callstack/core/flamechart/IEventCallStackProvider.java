/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.flamechart;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * An interface that analyses can implement if they can provide a stack of
 * called function for a single event.
 *
 * @author Geneviève Bastien
 */
public interface IEventCallStackProvider {

    /**
     * Get the callstack from an event
     *
     * @param event
     *            The event for which to get the stack
     * @return The callstack for the event, grouped by some domain, where the
     *         first element of each collection is the root.
     */
    Map<String, Collection<Object>> getCallStack(ITmfEvent event);

}
