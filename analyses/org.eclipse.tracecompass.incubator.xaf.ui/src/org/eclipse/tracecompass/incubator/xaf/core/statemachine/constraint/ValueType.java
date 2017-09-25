/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.constraint;

/**
 * The value type for that constraint
 * @author Raphaël Beamonte
 */
public enum ValueType {
    /** The value is a constant */
    CONSTANT,
    /** The value is the name of a StateMachineVariable */
    VARIABLE
}