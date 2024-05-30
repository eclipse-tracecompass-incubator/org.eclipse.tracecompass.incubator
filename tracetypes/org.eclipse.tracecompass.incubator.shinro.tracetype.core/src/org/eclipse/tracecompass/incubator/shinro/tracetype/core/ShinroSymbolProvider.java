package org.eclipse.tracecompass.incubator.shinro.tracetype.core;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.symbols.DefaultSymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;

/**
 * Symbol provider for Shinro traces
 */
public class ShinroSymbolProvider extends DefaultSymbolProvider {
    /**
     * Constructor that is parameter-wise consistent with superclass
     *
     * @param trace
     */
    public ShinroSymbolProvider(ShinroTrace trace) {
        super(trace);
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(int pid, long timestamp, long address) {
        return getSymbol(address);
    }

    @Override
    public @NonNull TmfResolvedSymbol getSymbol(long address) {
        // dummy implementation; temporary
        String name = "function_" + Long.toHexString(address);
        return new TmfResolvedSymbol(address, name);
    }
}
