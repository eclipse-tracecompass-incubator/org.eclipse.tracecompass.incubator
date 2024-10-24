/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.golang.core.trace;

import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.event.lookup.TmfCallsite;

/**
 * Callsite, enhanced
 *
 * @author Matthew Khouzam
 */
public class FullCallsite extends TmfCallsite {
    private final long fIp;
    private final String fFunction;

    /**
     * Constructor
     *
     * @param ip
     *            instruction pointer
     * @param function
     *            function name
     * @param file
     *            file name
     * @param line
     *            line
     */
    public FullCallsite(long ip, String function, String file, long line) {
        super(Objects.requireNonNull(file), line);
        fIp = ip;
        fFunction = function;
    }

    /**
     * @return the instruction pointer
     */
    public long getIp() {
        return fIp;
    }

    /**
     * @return the function
     */
    public String getFunction() {
        return fFunction;
    }
}