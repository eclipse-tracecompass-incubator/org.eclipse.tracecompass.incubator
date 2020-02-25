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
