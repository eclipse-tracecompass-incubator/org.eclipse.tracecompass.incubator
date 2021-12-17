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
 * A mapping of function name to address.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFunctionAddressNameMapping {
    private final String fFunctionAddress;
    private final BinaryFTraceFunctionType fFunctionType; // Refer to man nm
    private final String fFunctionName;

    /**
     * Constructor
     *
     * @param functionAddress
     *            The memory address of the function used in the trace file
     * @param functionType
     *            The function type, refer to man nm for documentation
     * @param functionName
     *            The name of the function
     */
    public BinaryFTraceFunctionAddressNameMapping(String functionAddress, BinaryFTraceFunctionType functionType, String functionName) {
        fFunctionAddress = functionAddress;
        fFunctionType = functionType;
        fFunctionName = functionName;
    }

    /**
     * Get the function address
     *
     * @return The function address
     */
    public String getFunctionAddress() {
        return fFunctionAddress;
    }

    /**
     * Get the function type of this mapping
     *
     * @return the function type of this mapping
     */
    public BinaryFTraceFunctionType getFunctionType() {
        return fFunctionType;
    }

    /**
     * Get the function name
     *
     * @return The name of the function
     */
    public String getFunctionName() {
        return fFunctionName;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("Function address: ").append(fFunctionAddress) //$NON-NLS-1$
                .append("; Function type: ").append(fFunctionType) //$NON-NLS-1$
                .append("; Function name: ").append(fFunctionName); //$NON-NLS-1$

        return strBuilder.toString();
    }
}
