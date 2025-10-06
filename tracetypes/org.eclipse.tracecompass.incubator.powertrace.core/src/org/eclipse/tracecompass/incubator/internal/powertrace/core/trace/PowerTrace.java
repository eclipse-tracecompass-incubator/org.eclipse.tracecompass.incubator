package org.eclipse.tracecompass.incubator.internal.powertrace.core.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
            if (domain == null || !domain.equals("\"PowerTrace25\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "not andy"); //$NON-NLS-1$
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        List<ITmfEventAspect<?>> current = new ArrayList<>();
        Map<String, String> environment = super.getEnvironment();
        String source_names = environment.get("source_names");
        Map<Integer, String> source_id_name_pairs = new HashMap<>();
        if (source_names != null) {
            source_names = source_names.replaceAll("^\"|\"$", "");//remove leading and trailing ""
            String[] pairs = source_names.split(",");
            for (int index = 0; index < pairs.length; index ++) {
                String[] line = pairs[index].split(":");
                int id;
                if (line[0] != null && line[0].matches("[-+]?\\d+")) {//check if string is numerical
                    id = Integer.parseInt(line[0]);
                    if (line[1] != null) {
                        source_id_name_pairs.put(id, line[1]);
                    }
                }
            }
        }


        for (ITmfEventAspect<?> eventAspect : super.getEventAspects()) {
            current.add(eventAspect);
        }
        for (int index = 0; index < 16; index++) {
            final int i = index;
            String fieldName = "sensors " + i;
            String label = "Sensors " + i;
            if (source_id_name_pairs.containsKey(i)) {
                label = source_id_name_pairs.get(i);
            }
            CounterAspect aspect = new CounterAspect(fieldName, label, CounterType.DOUBLE) {
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
        CounterAspect aspect = new CounterAspect("CPU power total", "CPU power total", CounterType.DOUBLE) {
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
