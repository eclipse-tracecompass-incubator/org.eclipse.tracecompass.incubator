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

package org.eclipse.tracecompass.incubator.internal.xaf.core.statemachine.constraint;

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