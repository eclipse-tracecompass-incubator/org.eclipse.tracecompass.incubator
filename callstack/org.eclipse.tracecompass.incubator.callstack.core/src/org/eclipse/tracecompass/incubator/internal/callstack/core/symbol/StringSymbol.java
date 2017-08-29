/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.symbol;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;

/**
 * A simple string symbol, to be used when the symbol is already available and
 * does not have to be resolved with providers
 *
 * @author Geneviève Bastien
 */
public class StringSymbol implements ICallStackSymbol {

    private final Object fSymbol;

    /**
     * Constructor
     *
     * @param symbol
     *            The string symbol
     */
    public StringSymbol(Object symbol) {
        fSymbol = symbol;
    }

    @Override
    public @NonNull String resolve(Collection<ISymbolProvider> providers) {
        return String.valueOf(fSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fSymbol);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof StringSymbol)) {
            return false;
        }
        StringSymbol other = (StringSymbol) obj;
        return (fSymbol.equals(other.fSymbol));
    }

    @Override
    public String toString() {
        return "String Symbol: " + fSymbol; //$NON-NLS-1$
    }
}
