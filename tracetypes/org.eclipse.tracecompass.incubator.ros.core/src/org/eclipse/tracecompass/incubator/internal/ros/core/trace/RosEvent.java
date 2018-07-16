/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.trace;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Event type for use in ROS traces.
 *
 * @author Christophe Bedard
 */
@NonNullByDefault
public class RosEvent extends CtfTmfEvent {

    /**
     * Default constructor. Only for use by extension points. Should not be
     * called directly.
     */
    @Deprecated
    public RosEvent() {
        super();
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace to which this event belongs
     * @param rank
     *            The rank of the event
     * @param timestamp
     *            The timestamp
     * @param channel
     *            The CTF channel of this event
     * @param cpu
     *            The event's CPU
     * @param declaration
     *            The event declaration
     * @param eventDefinition
     *            The event definition
     */
    protected RosEvent(CtfTmfTrace trace, long rank, ITmfTimestamp timestamp,
            String channel, int cpu, IEventDeclaration declaration, IEventDefinition eventDefinition) {
        super(trace, rank, timestamp, channel, cpu, declaration, eventDefinition);
    }
}
