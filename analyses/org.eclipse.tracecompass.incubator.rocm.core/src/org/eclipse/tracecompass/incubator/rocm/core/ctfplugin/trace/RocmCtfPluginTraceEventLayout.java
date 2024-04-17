package org.eclipse.tracecompass.incubator.rocm.core.ctfplugin.trace;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

public class RocmCtfPluginTraceEventLayout implements IGpuTraceEventLayout {

    private static @Nullable RocmCtfPluginTraceEventLayout INSTANCE;
    private static List<IApiEventLayout> fApiLayouts = List.of(new HsaApiEventLayout(), new HipApiEventLayout());

    /**
     * The instance of this event layout
     * <p>
     * This object is completely immutable, so no need to create additional
     * instances via the constructor.
     *
     * @return the instance
     */
    public static synchronized RocmCtfPluginTraceEventLayout getInstance() {
        RocmCtfPluginTraceEventLayout instance = INSTANCE;
        if (instance == null) {
            instance = new RocmCtfPluginTraceEventLayout();
            INSTANCE = instance;
        }
        return instance;
    }

    @Override
    public @NonNull Collection<IApiEventLayout> getApiLayouts() {
        return fApiLayouts;
    }

    @Override
    public @NonNull IApiEventLayout getCorrespondingApiLayout(ITmfEvent event) {
        if (event.getName().startsWith("hsa")) { //$NON-NLS-1$
            return fApiLayouts.get(0);
        }
        return fApiLayouts.get(1);
    }

    @Override
    public boolean isMemcpyBegin(ITmfEvent event) {
        return false;
    }

    @Override
    public boolean isLaunchBegin(ITmfEvent event) {
        return false;
    }

    @Override
    public boolean isApiEvent(ITmfEvent event) {
        String name = event.getName();
        return (name.startsWith("hsa") && !name.startsWith("hsa_op")) || (name.startsWith("hip") && !name.startsWith("hip_op")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public @NonNull String fieldThreadId() {
        return "context._thread_id"; //$NON-NLS-1$
    }

    @Override
    public @NonNull String fieldDuration() {
        return ""; //$NON-NLS-1$
    }

    public static class HsaApiEventLayout extends RocmApiCtfPluginEventLayout {
        @Override
        public String getApiName() {
            return "HSA"; //$NON-NLS-1$
        }
    }

    public static class HipApiEventLayout extends RocmApiCtfPluginEventLayout {
        @Override
        public String getApiName() {
            return "HIP"; //$NON-NLS-1$
        }
    }
}
