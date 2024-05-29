package org.eclipse.tracecompass.incubator.shinro.tracetype.core;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Symbol provider factory for Shinro traces
 */
public class ShinroSymbolProviderFactory implements ISymbolProviderFactory {

    @Override
    public @Nullable ISymbolProvider createProvider(@NonNull ITmfTrace trace) {
        if (trace instanceof ShinroTrace) {
            return new ShinroSymbolProvider((ShinroTrace) trace);
        }
        return null;
    }
}
