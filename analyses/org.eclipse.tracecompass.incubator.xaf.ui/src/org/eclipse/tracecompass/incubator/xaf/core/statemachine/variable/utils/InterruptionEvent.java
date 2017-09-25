/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable.utils;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.BackendStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * Class representing a interruption event, that can then allow to get the
 * interruption reason behind the event
 *
 * @author Raphaël Beamonte
 */
public class InterruptionEvent {
    private ITmfEvent event;
    private ITmfTimestamp start;
    private ITmfTimestamp end;
    private InterruptionReason interruptionReason;

    /**
     * Constructor
     *
     * @param event
     *            The event
     * @param end
     *            The end timestamp
     */
    public InterruptionEvent(ITmfEvent event, ITmfTimestamp end) {
        this.event = event;
        this.start = event.getTimestamp();
        this.end = end;

        // Get layout and event content
        IKernelAnalysisEventLayout layout = ((IKernelTrace) event.getTrace()).getKernelEventLayout();
        ITmfEventField content = event.getContent();

        // Get the reason of the interruption
        int state = Integer.parseInt(content.getField(layout.fieldPrevState()).getFormattedValue());
        interruptionReason = new State(
                ((state == 0 || state == 1024) ? BackendStateValue.PREEMPTED : BackendStateValue.BLOCKED).getValue(),
                StateMachineVariableHelpers.getSchedSwitchNextProcessInformation(event));
    }

    /**
     * Constructor
     *
     * @param event
     *            The event
     * @param end
     *            The end timestamp
     * @param ir
     *            The interruption reason
     */
    public InterruptionEvent(ITmfEvent event, ITmfTimestamp end, InterruptionReason ir) {
        this.event = event;
        this.start = event.getTimestamp();
        this.end = end;
        interruptionReason = ir;
    }

    /**
     * Constructor
     *
     * @param event
     *            The event
     * @param start
     *            The start timestamp
     * @param end
     *            The end timestamp
     * @param ir
     *            The interruption reason
     */
    public InterruptionEvent(ITmfEvent event, ITmfTimestamp start, ITmfTimestamp end, InterruptionReason ir) {
        this.event = event;
        this.start = start;
        this.end = end;
        interruptionReason = ir;
    }

    /**
     * @return The event
     */
    public ITmfEvent getEvent() {
        return event;
    }

    /**
     * @return The start timestamp
     */
    public ITmfTimestamp getStart() {
        return start;
    }

    /**
     * @return The end timestamp
     */
    public ITmfTimestamp getEnd() {
        return end;
    }

    /**
     * @return The interruption reason
     */
    public InterruptionReason getInterruptionReason() {
        return interruptionReason;
    }

    /**
     * @return The duration
     */
    public ITmfTimestamp getDuration() {
        return this.end.getDelta(this.start);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", //$NON-NLS-1$
                interruptionReason.toString(),
                getDuration());
    }
}