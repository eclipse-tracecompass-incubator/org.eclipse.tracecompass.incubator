package org.eclipse.tracecompass.incubator.rocm.core.ctfplugin.trace;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTrace;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmEventLayout;
import org.eclipse.tracecompass.incubator.rocm.core.trace.RocmTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocationInfo;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class RocmCtfPluginTrace extends RocmTrace implements IGpuTrace {

    private @Nullable Boolean fIsContainingKernelGpuActivity;

    private ImmutableList<ITmfEventAspect<?>> fAspects = ImmutableList.of(TID_ASPECT, FUNCTION_NAME_ASPECT);

    private static final int MAX_OPERATIONS_UNTIL_OPERATION_END = 100;
    private static final int CONFIDENCE = 100;

    @Override
    public @NonNull IGpuTraceEventLayout getGpuTraceEventLayout() {
        return RocmCtfPluginTraceEventLayout.getInstance();
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fAspects;
    }

    @Override
    @org.eclipse.jdt.annotation.NonNullByDefault({})
    public void initTrace(final IResource resource, final String path,
            final Class<? extends ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);

        ImmutableList.Builder<ITmfEventAspect<?>> builder = new Builder<>();
        builder.addAll(super.getEventAspects());
        fAspects = builder.build();
        lookForKernelGpuActivityMetadata();
    }

    @Override
    public @Nullable IStatus validate(final @Nullable IProject project, final @Nullable String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "kernel" in the trace's env vars */
            String domain = environment.get("tracer_name"); //$NON-NLS-1$
            boolean isRocprofilerVersionPresent = environment.get("rocprofiler_version") != null; //$NON-NLS-1$
            if (domain == null || !domain.equals("\"barectf\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                        "This trace was not recognized as a ROCm trace. You can update your rocprofiler version or you can change manually the tracer name to \"rocprof\" in the metadata file to force the validation."); //$NON-NLS-1$
            }
            if (isRocprofilerVersionPresent) {
                return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
            }
        }
        return status;
    }

    /**
     * Look for metadata indicating if the trace contains kernel gpu activity
     * events. Otherwise if it is requested, let the method
     * isContainingKernelGpuActivity to calculate this boolean if the metadata
     * is not present.
     */
    private void lookForKernelGpuActivityMetadata() {
        Map<String, String> environment = this.getEnvironment();
        /* Make sure the domain is "kernel" in the trace's env vars */
        String rocprofilerArgs = environment.get("args"); //$NON-NLS-1$
        if (rocprofilerArgs == null) {
            return;
        } else if (rocprofilerArgs.contains("--sys-trace") || rocprofilerArgs.contains("--hsa-trace")) { //$NON-NLS-1$ //$NON-NLS-2$
            fIsContainingKernelGpuActivity = true;
        }
        fIsContainingKernelGpuActivity = false;
    }

    /**
     * Look for kernel GPU activity in the trace and tries to correlate it with
     * API events.
     *
     * @return if the trace contains GPU kernel activity.
     */
    public boolean isContainingKernelGpuActivity() {
        if (fIsContainingKernelGpuActivity != null) {
            return fIsContainingKernelGpuActivity;
        }
        ITmfContext context = seekEvent(new CtfLocation(new CtfLocationInfo(0L, 0L)));
        ITmfEvent event = getNext(context);
        Long hipLaunchKernelEventCorrelationId = -1L;
        RocmEventLayout layout = new RocmEventLayout();
        Integer i = 0;
        while (event != null && i < MAX_OPERATIONS_UNTIL_OPERATION_END) {
            Long correlationId = event.getContent().getFieldValue(Long.class, layout.fieldCorrelationId());
            if (correlationId == null) {
                event = getNext(context);
                continue;
            }
            if (event.getName().equals(layout.hipLaunchKernelBegin())) {
                if (hipLaunchKernelEventCorrelationId == -1) {
                    hipLaunchKernelEventCorrelationId = correlationId;
                }
                i += 1;
            }
            if (event.getName().equals(layout.getHipOperationBegin()) && correlationId.equals(hipLaunchKernelEventCorrelationId)) {
                fIsContainingKernelGpuActivity = true;
                return fIsContainingKernelGpuActivity;
            }
            event = getNext(context);
        }
        fIsContainingKernelGpuActivity = false;
        return fIsContainingKernelGpuActivity;
    }

    private static final ITmfEventAspect<Integer> TID_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return "Thread ID"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable Integer resolve(ITmfEvent event) {
            return event.getContent().getFieldValue(Integer.class, RocmCtfPluginTraceEventLayout.getInstance().fieldThreadId());
        }
    };

    private static final ITmfEventAspect<String> FUNCTION_NAME_ASPECT = new ITmfEventAspect<>() {
        @Override
        public String getName() {
            return "Function name"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return ITmfEventAspect.EMPTY_STRING;
        }

        @Override
        public @Nullable String resolve(ITmfEvent event) {
            return RocmCtfPluginTraceEventLayout.getInstance().getCorrespondingApiLayout(event).getEventName(event);
        }
    };
}
