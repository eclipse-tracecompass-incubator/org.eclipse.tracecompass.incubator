package org.eclipse.tracecompass.incubator.rocm.core.exatracer.trace;

import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout.IApiEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public abstract class RocmApiEventLayout implements IApiEventLayout {

    @Override
    public boolean isBeginEvent(ITmfEvent event) {
        return event.getName().endsWith("_entry");
    }

    @Override
    public String getEventName(ITmfEvent event) {
        if (isBeginEvent(event)) {
            return event.getName().substring(4, event.getName().length() - 6);
        }
        return event.getName().substring(4, event.getName().length() - 5);
    }
}
