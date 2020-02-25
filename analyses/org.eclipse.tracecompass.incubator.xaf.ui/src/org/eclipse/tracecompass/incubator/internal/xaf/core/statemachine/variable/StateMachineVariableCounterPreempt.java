/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.backend.Attributes;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.backend.BackendStateValue;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.exception.StateMachineUnexpectedEventException;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable.utils.InterruptionReason;
import org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.variable.utils.State;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * Class that represents a state machine variable of counter class, of preempt type
 * @author Raphaël Beamonte
 */
public class StateMachineVariableCounterPreempt extends StateMachineVariableCounter {

    /**
     * @param name The name of the variable
     * @param value The initial value of the variable
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariableCounterPreempt(String name, Comparable value) {
        super(name, value);
    }

    @Override
    public String getCounterAttribute() {
        return Attributes.COUNTER_PREEMPT;
    }

    @Override
    public StateMachineVariableCounterPreempt getCopy() {
        return new StateMachineVariableCounterPreempt(getName(), getValue());
    }

    @Override
    protected InterruptionReason getInterruptionReasonFromEvent(ITmfEvent e) throws StateMachineUnexpectedEventException {
        // Get the information of the preempting process
        // Get the analysis event layout
        IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
        if (!e.getName().equals(layout.eventSchedSwitch())) {
            throw new StateMachineUnexpectedEventException(
                    String.format("The event should be a '%s', but is a '%s' instead", //$NON-NLS-1$
                            layout.eventSchedSwitch(),
                            e.getName()
                    ));
        }
        // Get event content
        ITmfEventField content = e.getContent();
        // Get the name of the task that preempted ours
        String taskname = content.getField(layout.fieldNextComm()).getFormattedValue();
        // Get the TID of the task that preempted ours
        int tasktid = Integer.parseInt(content.getField(layout.fieldNextTid()).getFormattedValue());

        String data = taskname + " " + tasktid; //$NON-NLS-1$

        // Create a reason for the event
        State state = new State(
                BackendStateValue.PREEMPTED.getValue(),
                data);
        return state;
    }
}
