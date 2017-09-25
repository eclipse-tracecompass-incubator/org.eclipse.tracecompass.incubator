/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend.Attributes;

/**
 * Class that represents a state machine variable of timer class, of waitblocked
 * type
 *
 * @author Raphaël Beamonte
 */
public class StateMachineVariableTimerWaitBlocked extends StateMachineVariableTimer {

    /**
     * @param name
     *            The name of the variable
     * @param value
     *            The initial value of the variable
     */
    public StateMachineVariableTimerWaitBlocked(String name, Comparable<?> value) {
        super(name, value);
    }

    @Override
    public String getTimerAttribute() {
        return Attributes.TIMER_CPU_USAGE;
    }

    @Override
    public StateMachineVariableTimerWaitBlocked getCopy() {
        return new StateMachineVariableTimerWaitBlocked(getName(), getValue());
    }

}
