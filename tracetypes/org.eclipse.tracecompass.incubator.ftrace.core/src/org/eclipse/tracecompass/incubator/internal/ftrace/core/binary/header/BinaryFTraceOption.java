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
 * Represent an option in the option section in the header of the binary FTrace
 * file. The options are just text metadata about the file, so they do not
 * require any special handling.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceOption {
    private final short fOptionType;
    private final String fOptionContent;

    /**
     * Constructor
     *
     * @param optionType
     *            The ID of the option
     * @param optionContent
     *            The content of the option as text
     */
    public BinaryFTraceOption(short optionType, String optionContent) {
        fOptionType = optionType;
        fOptionContent = optionContent;
    }

    /**
     * Get the ID of option
     *
     * @return The ID of the option
     */
    public short getOptionType() {
        return fOptionType;
    }

    /**
     * Get the content of this option
     *
     * @return The content of this option
     */
    public String getOptionContent() {
        return fOptionContent;
    }

    @Override
    public String toString() {
        return "Option ID: " + fOptionType + "; OptionContent: " + fOptionContent; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
