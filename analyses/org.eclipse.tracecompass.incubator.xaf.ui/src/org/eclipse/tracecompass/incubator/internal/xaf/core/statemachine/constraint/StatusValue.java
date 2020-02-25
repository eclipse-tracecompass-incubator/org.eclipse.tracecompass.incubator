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
 * The status of a constraint
 * @author Raphaël Beamonte
 */
public enum StatusValue {
    /** The constraint is valid */
    VALID,
    /** The constraint is uncertain */
    UNCERTAIN,
    /** The constraint is invalid */
    INVALID,
    /** The constraint is adaptive */
    ADAPTIVE
}