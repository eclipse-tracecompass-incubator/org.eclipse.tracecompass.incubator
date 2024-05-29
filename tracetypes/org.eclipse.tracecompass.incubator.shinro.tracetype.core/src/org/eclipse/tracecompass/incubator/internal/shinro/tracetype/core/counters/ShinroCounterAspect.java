package org.eclipse.tracecompass.incubator.internal.shinro.tracetype.core.counters;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * This exists just to override isCumulative
 */
public class ShinroCounterAspect extends CounterAspect {

    /**
     * @param fieldName name of the event field from which the counter value is taken
     * @param label label of the counter
     */
    public ShinroCounterAspect(@NonNull String fieldName, @NonNull String label) {
        super(fieldName, label);
    }

    @Override
    public boolean isCumulative() {
        return false;
    }

    @Override
    public @Nullable Number resolve(@NonNull ITmfEvent event) {
        Number result = null;
        if (event.getName().equals(this.getName())) {
            result = super.resolve(event);
        }
        return result;
    }
}
