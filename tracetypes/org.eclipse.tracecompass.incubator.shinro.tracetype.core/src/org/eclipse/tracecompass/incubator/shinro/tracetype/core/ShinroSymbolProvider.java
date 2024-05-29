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
        // we'll probably need to override this
        return super.getSymbol(pid, timestamp, address);
    }

    @Override
    public @NonNull TmfResolvedSymbol getSymbol(long address) {
        // we'll probably need to override this
        return super.getSymbol(address);
    }
}
