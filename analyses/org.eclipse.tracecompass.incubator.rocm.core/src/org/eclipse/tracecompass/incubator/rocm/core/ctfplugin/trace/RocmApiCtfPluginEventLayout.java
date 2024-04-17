package org.eclipse.tracecompass.incubator.rocm.core.ctfplugin.trace;

import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout.IApiEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public abstract class RocmApiCtfPluginEventLayout implements IApiEventLayout {

    @Override
    public boolean isBeginEvent(ITmfEvent event) {
        return event.getName().endsWith("_begin") || event.getName().endsWith("Begin");
    }

    @Override
    public String getEventName(ITmfEvent event) {
        if (isBeginEvent(event)) {
            if (event.getName().startsWith("hip")) {
                return event.getName().substring(0, event.getName().length() - 5);
            }
            return event.getName().substring(0, event.getName().length() - 6);
        }
        if (event.getName().startsWith("hip")) {
            return event.getName().substring(0, event.getName().length() - 3);
        }
        return event.getName().substring(0, event.getName().length() - 4);
    }
}
