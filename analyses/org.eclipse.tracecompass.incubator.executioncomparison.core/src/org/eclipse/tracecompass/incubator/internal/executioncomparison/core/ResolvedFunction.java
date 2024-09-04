/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.core;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;

/**
 * A pre-resolved function. Basically has an overridden equals on the name
 */
@SuppressWarnings("restriction")
public final class ResolvedFunction extends AggregatedCalledFunction {
    private static class CSS implements ICallStackSymbol {
        private String fLabel;

        public CSS(String label) {
            fLabel = label;
        }

        @Override
        public String resolve(Collection<ISymbolProvider> providers) {
            return fLabel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fLabel);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CSS other = (CSS) obj;
            return Objects.equals(fLabel, other.fLabel);
        }

    }

    private final String fLabel;

    ResolvedFunction(String label, AggregatedCalledFunction toCopy) {
        super(toCopy);
        fLabel = label;
    }

    @Override
    public ICallStackSymbol getObject() {
        return new CSS(fLabel);
    }
}