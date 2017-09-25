/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * Class to store a List of InterruptionEvent. More than just storing the List,
 * it also add methods to work on this list.
 *
 * @author Raphaël Beamonte
 */
public class InterruptionEventList {
    /** The list of interruption events */
    public final List<InterruptionEvent> events;

    /**
     * Constructor
     */
    public InterruptionEventList() {
        events = new ArrayList<>();
    }

    /**
     * @return The duration of the entirety of the event list
     */
    public ITmfTimestamp getDuration() {
        return events.get(events.size() - 1).getEnd().getDelta(events.get(0).getEvent().getTimestamp());
    }

    /**
     * @return A list of interruption durations computed from splitting the
     *         interruption event list
     */
    public List<InterruptionDuration> split() {
        List<InterruptionDuration> idList = new ArrayList<>();

        if (events.isEmpty()) {
            return idList;
        }

        for (InterruptionEvent ie : events) {
            InterruptionReason ir = ie.getInterruptionReason();
            double duration = ie.getDuration().getValue();
            TimestampInterval ti = new TimestampInterval(ie.getStart(), ie.getEnd());

            boolean added = false;
            for (InterruptionDuration id : idList) {
                if (id.getReason().fits(ir)) {
                    id.addDuration(duration);
                    id.addTsInterval(ti);
                    added = true;
                    break;
                }
            }
            if (!added) {
                InterruptionDuration id = new InterruptionDuration(ir, duration, ti);
                idList.add(id);
            }
        }

        return idList;
    }

    @Override
    public String toString() {
        String ret = "[\n"; //$NON-NLS-1$

        for (InterruptionEvent ie : events) {
            ret += String.format("\t%s,\n", ie); //$NON-NLS-1$
        }
        ret += "]"; //$NON-NLS-1$

        return ret;
    }
}