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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

/**
 * This file contains the number of bytes reserved for different sections in the
 * header of a binary ftrace file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceHeaderElementSize {

    private BinaryFTraceHeaderElementSize() {
        // Do nothing
    }

    /**
     * Each string in the header is terminated by /0. The size of the
     * termination character is 1 byte
     */
    public static final int STRING_TERMINATOR = 1;

    /**
     * The size of the page header of a binary FTrace CPU data page in bytes
     */
    public static final int PAGE_HEADER_SIZE = 16;
}
