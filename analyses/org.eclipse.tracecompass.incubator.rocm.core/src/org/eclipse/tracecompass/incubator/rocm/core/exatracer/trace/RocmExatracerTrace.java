package org.eclipse.tracecompass.incubator.rocm.core.exatracer.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.rocm.core.trace.RocmTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfEventFieldAspect;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

public class RocmExatracerTrace extends RocmTrace implements IGpuTrace {

    private static final int CONFIDENCE = 100;

    public RocmExatracerTrace() {
        super();
    }

    public static TmfEventFieldAspect fNameAspect = new TmfEventFieldAspect("Name", "name", ITmfEvent::getContent); //$NON-NLS-1$ //$NON-NLS-2$

    public static LinuxPidAspect fVpidAspect = new LinuxPidAspect() {

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            Long fieldValue = event.getContent().getFieldValue(Long.class, "context._vpid"); //$NON-NLS-1$
            if (fieldValue != null) {
                return fieldValue.intValue();
            }
            return null;
        }
    };
    public static LinuxTidAspect fVtidAspect = new LinuxTidAspect() {
        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            Long fieldValue = event.getContent().getFieldValue(Long.class, "context._vtid"); //$NON-NLS-1$
            if (fieldValue != null) {
                return fieldValue.intValue();
            }
            fieldValue = event.getContent().getFieldValue(Long.class, RocmExatracerTraceEventLayout.getInstance().fieldThreadId()); //$NON-NLS-1$
            if (fieldValue != null) {
                return fieldValue.intValue();
            }
            return null;
        }
    };
    public static TmfEventFieldAspect fBackendAspect = new TmfEventFieldAspect("Backend", "context._backend", ITmfEvent::getContent); //$NON-NLS-1$ //$NON-NLS-2$

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "kernel" in the trace's env vars */
            String domain = environment.get("tracer_name"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"lttng-ust\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "This trace was not recognized as a ROCm trace. You can update your rocprofiler version or you can change manually the tracer name to \"rocprof\" in the metadata file to force the validation."); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        Iterable<ITmfEventAspect<?>> oldAspects = super.getEventAspects();
        List<ITmfEventAspect<?>> aspects = new ArrayList<>();
        for (ITmfEventAspect<?> aspect : oldAspects) {
            aspects.add(aspect);
        }
        aspects.add(fNameAspect);
        aspects.add(fVpidAspect);
        aspects.add(fVtidAspect);
        aspects.add(fBackendAspect);
        return aspects;
    }

    @Override
    public @NonNull IGpuTraceEventLayout getGpuTraceEventLayout() {
        return RocmExatracerTraceEventLayout.getInstance();
    }
}
