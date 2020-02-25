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
