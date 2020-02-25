/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Alexis Cabana-Loriaux - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.contextswitch;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Class used to represent a bucket on the timegraph in the context siwtch view.
 * The only addition is that a TimeEvent has a rate for better classification.
 *
 * @author Alexis Cabana-Loriaux
 */
class ContextSwitchTimeEvent extends TimeEvent {

    /**
     * Possible context switch rates of a given TimeEvent
     *
     * The rate of context switching for a given TimeEvent
     *
     * @author Alexis Cabana-Loriaux
     */
    public enum ContextSwitchRate {
        NONE, LOW, MODERATE, HIGH, CRITICAL
    }

    /** The rate of context switching for this TimeEvent */
    public ContextSwitchRate fRate;
    private int fCount;

    /**
     * Default constructor
     *
     * @param entry
     *            the entry
     * @param time
     *            the start time
     * @param duration
     *            the duration
     * @param value
     *            the number of context switch
     */
    public ContextSwitchTimeEvent(ITimeGraphEntry entry, long time, long duration, int value) {
        this(entry, time, duration, value, 1);
    }

    public ContextSwitchTimeEvent(ITimeGraphEntry entry, long time, long duration, int value, int count) {
        super(entry, time, duration, value);
        fRate = ContextSwitchRate.NONE;
        fCount = count;
    }

    @Override
    public ITimeEvent splitBefore(long splitTime) {
        ContextSwitchTimeEvent event = (splitTime > fTime ? new ContextSwitchTimeEvent(fEntry, fTime, Math.min(fDuration, splitTime - fTime), getValue()) : null);
        if (event != null) {
            event.fRate = this.fRate;
        }
        return event;
    }

    @Override
    public ITimeEvent splitAfter(long splitTime) {
        ContextSwitchTimeEvent event = (splitTime < fTime + fDuration ? new ContextSwitchTimeEvent(fEntry, Math.max(fTime, splitTime), fDuration - Math.max(0, splitTime - fTime),
                getValue()) : null);
        if (event != null) {
            event.fRate = this.fRate;
        }
        return event;
    }

    public int getCount() {
        return fCount;
    }
}
