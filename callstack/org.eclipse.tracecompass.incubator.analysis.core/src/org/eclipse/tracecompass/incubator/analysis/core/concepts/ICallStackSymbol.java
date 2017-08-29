/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.concepts;

import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;

/**
 * @author Geneviève Bastien
 */
public interface ICallStackSymbol {

    /**
     * Resolve the current symbol to a string with the providers
     *
     * @param providers
     *            The collection of providers available
     * @return The resolved symbol
     */
    public String resolve(Collection<ISymbolProvider> providers);

}
