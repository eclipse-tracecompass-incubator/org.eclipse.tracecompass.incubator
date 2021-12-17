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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

/**
 * Represent the binary FTrace file type.
 *
 * @author Hoang Thuan Pham
 */
public enum BinaryFTraceFileType {
    /**
     * An ASCII text file
     */
    LATENCY,
    /**
     * Binary file
     */
    FLY_RECORD
}
