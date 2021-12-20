/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator;

/**
 * A response when reading a binary FTrace event.
 *
 * @author Hoang Thuan Pham
 *
 */
public enum BinaryFTraceResponse {
    /**
     * An event with payload is retrieved.
     */
    OK,
    /**
     * An error has occured when trying to get the next trace event with
     * payload.
     */
    ERROR,
    /**
     * An event with payload is retrieved and this is the last event of the
     * trace action.
     */
    FINISH
}
