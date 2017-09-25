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
 * The direction for a constraint
 * @author Raphaël Beamonte
 */
public enum Direction {
    /** The variable must be greater than (or greater or equal to) the value */
    VARIABLE_OVER_VALUE,
    /** The variable must be less than (or less or equal to) the value */
    VARIABLE_UNDER_VALUE,
    /** The variable must be equal to (or different than) the value */
    VARIABLE_EQ_VALUE,
}
