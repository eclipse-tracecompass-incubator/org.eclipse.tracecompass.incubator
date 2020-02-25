/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event Handler parameter, provides a state system, an event, the tid and the
 * time
 *
 * @author Matthew Khouzam
 */
public class HandlerParameter {
    private final ITmfEvent fEvent;
    private final ITmfStateSystemBuilder fSsb;
    private final Integer fTid;
    private final long fTime;

    /**
     * Constructor
     *
     * @param ssb
     *            the state system builder
     * @param event
     *            the event
     * @param tid
     *            the threadID
     */
    public HandlerParameter(ITmfStateSystemBuilder ssb, ITmfEvent event, Integer tid) {
        fEvent = event;
        fSsb = ssb;
        fTid = tid;
        fTime = event.getTimestamp().toNanos();
    }

    /**
     * Get the event
     *
     * @return the event
     */
    public ITmfEvent getEvent() {
        return fEvent;
    }

    /**
     * Get the state system builder
     *
     * @return the state system builder
     */
    public ITmfStateSystemBuilder getSsb() {
        return fSsb;
    }

    /**
     * Get the thread id
     *
     * @return the thread id
     */
    public Integer getTid() {
        return fTid;
    }

    /**
     * Get the current time in ns
     *
     * @return the current time in ns
     */
    public long getTime() {
        return fTime;
    }
}