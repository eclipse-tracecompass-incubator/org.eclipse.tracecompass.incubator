/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.variable;

import java.util.Comparator;

/**
 * @author Raphaël Beamonte
 *
 */
public class StateMachineVariableComparator implements Comparator<StateMachineVariable> {

    @Override
    public int compare(StateMachineVariable v1, StateMachineVariable v2) {
        return v1.getName().compareTo(v2.getName());
    }

}
