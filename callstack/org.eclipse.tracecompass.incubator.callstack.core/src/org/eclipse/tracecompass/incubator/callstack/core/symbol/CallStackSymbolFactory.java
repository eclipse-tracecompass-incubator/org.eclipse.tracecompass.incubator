/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.symbol;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.symbol.ResolvableSymbol;
import org.eclipse.tracecompass.incubator.internal.callstack.core.symbol.StringSymbol;

/**
 * A factory that creates the appropriate symbol for a given element
 *
 * @author Geneviève Bastien
 */
public final class CallStackSymbolFactory {

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
            try {
                long longAddress = Long.parseUnsignedLong((String) symbol, 16);
                return new ResolvableSymbol(longAddress, element.getSymbolKeyAt(timestamp), timestamp);
            } catch (NumberFormatException e) {
                // Not a long number, use a string symbol
            }
        }
        return new StringSymbol(symbol);
    }
}
