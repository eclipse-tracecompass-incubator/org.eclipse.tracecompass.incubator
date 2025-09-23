package org.eclipse.tracecompass.incubator.internal.powertrace.core.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.CounterType;
import org.eclipse.tracecompass.analysis.counters.core.aspects.CounterAspect;
import org.eclipse.tracecompass.incubator.internal.powertrace.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

public class PowerTrace extends CtfTmfTrace {
    private static final int CONFIDENCE = 420;

    @Override
    public IStatus validate(IProject project, String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "kernel" in the trace's env vars */
            String domain = environment.get("tracer"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"AndyTracer\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "not andy"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        List<ITmfEventAspect<?>> current = new ArrayList<>();
        for (ITmfEventAspect<?> eventAspect : super.getEventAspects()) {
            current.add(eventAspect);
        }
        for (int index = 0; index < 16; index++) {
            final int i = index;
            CounterAspect aspect = new CounterAspect("sensors " + i, "Sensor " + i, CounterType.DOUBLE) {
                @Override
                public @Nullable Number resolve(@NonNull ITmfEvent event) {
                    ITmfEventField array = event.getContent().getField("sensors");

                    ITmfEventField field = array.getField("sensors[" + String.valueOf(i) + "]");
                    if (field != null) {
                        return field.getFieldValue(Double.class, "value");
                    }
                    return null;
                }
            };
            current.add(aspect);
        }
        CounterAspect aspect = new CounterAspect("CPU Power", "CPU POWAH", CounterType.DOUBLE) {
            @Override
            public @Nullable Number resolve(@NonNull ITmfEvent event) {
                ITmfEventField array = event.getContent().getField("sensors");

                ITmfEventField field0 = array.getField("sensors[8]");
                ITmfEventField field1 = array.getField("sensors[9]");
                if (field0 != null) {
                    Double fieldValue = field1.getFieldValue(Double.class, "value");
                    Double fieldValue2 = field0.getFieldValue(Double.class, "value");
                    if (fieldValue != null && fieldValue2 != null) {
                        return fieldValue2 + fieldValue;
                    }
                }
                return null;
            }

        };
        current.add(aspect);
        return current;

    }
}
