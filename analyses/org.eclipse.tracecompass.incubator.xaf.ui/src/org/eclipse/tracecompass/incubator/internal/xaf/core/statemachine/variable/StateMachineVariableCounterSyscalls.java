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

/**
 * Class that represents a state machine variable of counter class, of syscalls
 * type
 *
 * @author Raphaël Beamonte
 */
public class StateMachineVariableCounterSyscalls extends StateMachineVariableCounter {

    /**
     * @param name
     *            The name of the variable
     * @param value
     *            The initial value of the variable
     */
    @SuppressWarnings("rawtypes")
    public StateMachineVariableCounterSyscalls(String name, Comparable value) {
        super(name, value);
    }

    @Override
    public String getCounterAttribute() {
        return Attributes.COUNTER_SYSCALLS;
    }

    @Override
    public StateMachineVariableCounterSyscalls getCopy() {
        return new StateMachineVariableCounterSyscalls(getName(), getValue());
    }

    @Override
    protected InterruptionReason getInterruptionReasonFromEvent(ITmfEvent e) throws StateMachineUnexpectedEventException {
        // Get the information of the syscall
        // Get the analysis event layout
        IKernelAnalysisEventLayout layout = ((IKernelTrace) e.getTrace()).getKernelEventLayout();
        if (!e.getName().startsWith(layout.eventSyscallEntryPrefix())) {
            throw new StateMachineUnexpectedEventException(
                    String.format("The event should be a '%s', but is a '%s' instead", //$NON-NLS-1$
                            layout.eventSyscallEntryPrefix(),
                            e.getName()));
        }
        // Set the data as the name of the syscall
        String data = e.getName().substring(layout.eventSyscallEntryPrefix().length());

        // Create a reason for the event
        State state = new State(
                BackendStateValue.SYSCALL.getValue(),
                data);

        return state;
    }
}
