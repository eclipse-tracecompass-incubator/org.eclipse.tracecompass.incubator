/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core.symbol;

import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;

/**
 * A resolved symbol that carries the backing library file in its display name,
 * so users can tell where a resolved function lives.
 */
public class TmfLibrarySymbol extends TmfResolvedSymbol {

    private final String fSourceFile;

    /**
     * Constructor
     *
     * @param address
     *            the address of this symbol
     * @param name
     *            the name this symbol resolves to
     * @param sourceFile
     *            the backing file of this symbol
     */
    public TmfLibrarySymbol(long address, String name, String sourceFile) {
        super(address, name);
        fSourceFile = sourceFile;
    }

    @Override
    public String getSymbolName() {
        return super.getSymbolName() + ' ' + '(' + fSourceFile + ')';
    }
}
