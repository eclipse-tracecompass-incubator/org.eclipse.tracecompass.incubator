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

package org.eclipse.tracecompass.incubator.analysis.core.concepts;

import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;

/**
 * @author Geneviève Bastien
 */
@Deprecated(since="0.10.0", forRemoval=true)
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
