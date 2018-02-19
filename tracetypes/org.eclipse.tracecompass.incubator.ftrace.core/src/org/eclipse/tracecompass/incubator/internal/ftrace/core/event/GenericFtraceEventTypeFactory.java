/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.event;

import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class of Ftrace envent types
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class GenericFtraceEventTypeFactory {

    private static final Map<String, TmfEventType> TYPES = new HashMap<>();

    /**
     * Returns corresponding type for an event name
     *
     * @param eventName
     *            Name of the event
     * @return ITmfEventType corresponding to event
     */
    public static ITmfEventType get(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return null;
        }

        TmfEventType event = null;
        if (TYPES.containsKey(eventName)) {
            event = TYPES.get(eventName);
        } else {
            event = new TmfEventType(eventName, null);
            TYPES.put(eventName, event);
        }
        return event;
    }
}
