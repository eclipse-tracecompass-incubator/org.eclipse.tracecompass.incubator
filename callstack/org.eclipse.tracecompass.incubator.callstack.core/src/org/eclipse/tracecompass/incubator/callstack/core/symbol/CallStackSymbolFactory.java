/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.symbol;

import java.util.regex.Pattern;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.symbol.ResolvableSymbol;
import org.eclipse.tracecompass.incubator.internal.callstack.core.symbol.StringSymbol;

/**
 * A factory that creates the appropriate symbol for a given element
 *
 * @author Geneviève Bastien
 */
@Deprecated(since="0.10.0", forRemoval=true)
public final class CallStackSymbolFactory {

    private static final Pattern IS_NUMBER = Pattern.compile("[0-9A-Fa-f]+");

    private CallStackSymbolFactory() {
        // Utility class, not to be instantiated
    }

    /**
     * Create a callstack symbol for a given element
     *
     * @param symbol
     *            the symbol, could be an number address, a string, or really
     *            anything
     * @param element
     *            the element to lookup
     * @param timestamp
     *            the time to lookup
     * @return an {@link ICallStackSymbol} with the data to be shown.
     */
    public static ICallStackSymbol createSymbol(Object symbol, ICallStackElement element, long timestamp) {
        if (symbol instanceof Long || symbol instanceof Integer) {
            long longAddress = ((Long) symbol).longValue();
            return new ResolvableSymbol(longAddress, element.getSymbolKeyAt(timestamp), timestamp);
        }
        if (symbol instanceof String) {
            String strSymbol = (String) symbol;
            if (IS_NUMBER.matcher(strSymbol).matches()) {
                try {
                    long longAddress = Long.parseUnsignedLong(strSymbol, 16);
                    return new ResolvableSymbol(longAddress, element.getSymbolKeyAt(timestamp), timestamp);
                } catch (NumberFormatException e) {
                    // Not a long number, use a string symbol
                }
            }
        }
        return new StringSymbol(symbol);
    }
}
