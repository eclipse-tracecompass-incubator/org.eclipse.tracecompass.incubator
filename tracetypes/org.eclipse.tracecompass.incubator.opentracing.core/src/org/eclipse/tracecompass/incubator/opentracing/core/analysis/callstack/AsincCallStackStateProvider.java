package org.eclipse.tracecompass.incubator.opentracing.core.analysis.callstack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Span Callstack state provider
 *
 * @author Fateme Faraji Daneshgar
 *
 */
public class AsincCallStackStateProvider extends AbstractTmfStateProvider {

    /**
     * Thread attribute
     *
     * @since 2.0
     */
    public static final String PROCESSES = "Processes"; //$NON-NLS-1$

    /**
     * Unknown process ID
     *
     * @since 2.0
     */
    public static final int UNKNOWN_PID = -1;

    /**
     * Unknown name
     *
     * @since 2.0
     */
    public static final String UNKNOWN = "UNKNOWN"; //$NON-NLS-1$


    private static final int MAX_STACK_DEPTH = 100000;

    private final Map<String, Integer> fStackDepthMap;
    private TreeMap<Long, List<Integer>> fPrevEvent;
    private int fStateQuark = 3;

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public AsincCallStackStateProvider(@NonNull ITmfTrace trace) {
        super(trace, OpenTracingCallstackAnalysis.ID);
        // fSpanMap = new HashMap<>();
        fStackDepthMap = new HashMap<>();
        fPrevEvent = new TreeMap<>();
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new AsincCallStackStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        handleSpan(event,ss);
    }

    private void handleSpan(ITmfEvent event, ITmfStateSystemBuilder ss) {
        long timestamp = event.getTimestamp().toNanos();
        if (timestamp == getTrace().getStartTime().toNanos()) {
            timestamp++;
        }
        Long duration = event.getContent().getFieldValue(Long.class, IOpenTracingConstants.DURATION);
        if (duration == null) {
            return;
        }
        while (!(fPrevEvent.isEmpty()) && (fPrevEvent.firstKey() < timestamp)) {
            long prevTime = fPrevEvent.firstKey();
            List<Integer> quarks = Objects.requireNonNull(fPrevEvent.get(fPrevEvent.firstKey()));
            ss.modifySpanAttribute(prevTime, (Object) null, quarks.get(0), quarks.get(1));
            fPrevEvent.remove(fPrevEvent.firstKey());
        }

        String processName = event.getContent().getFieldValue(String.class, IOpenTracingConstants.TRACE_ID);

        int processId = getProcessId(event);
        if (processName == null) {
            processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        }

        int pq = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);
        ss.updateOngoingState(TmfStateValue.newValueInt(processId), 1);

        String Opname = String.valueOf(TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), "Name", event)); //$NON-NLS-1$
        String spanId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.SPAN_ID);
        String parentId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.REFERENCES + "/CHILD_OF"); //$NON-NLS-1$

        int callStackQuark = ss.getQuarkRelativeAndAdd(pq, InstrumentedCallStackAnalysis.CALL_STACK);
        int stackDepth = getStackDepth(parentId);

        Object functionEntryName = functionEntry(spanId, parentId, Opname);
        stackDepth++;

        int spanQuark = ss.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(stackDepth));

        ss.modifySpanAttribute(timestamp, functionEntryName, fStateQuark, spanQuark);

        fStackDepthMap.put(spanId, stackDepth);

        List<Integer> quarksList = Arrays.asList(fStateQuark, spanQuark);
        fPrevEvent.put(timestamp + duration, quarksList);
        fStateQuark++;

    }

    protected @Nullable String getProcessName(ITmfEvent event) {

        Long fieldValue = event.getContent().getFieldValue(Long.class, "trace_id");
        if (fieldValue == null) {
            fieldValue = event.getContent().getFieldValue(Long.class, "trace_id_low");
        }

        return fieldValue == null ? "eduroam" : Long.toHexString(fieldValue);
    }

    protected int getProcessId(ITmfEvent event) {
        Long resolve = event.getContent().getFieldValue(Long.class, "trace_id");
        return resolve == null ? -1 : resolve.intValue();
    }

    private int getStackDepth(String parentId) {
        Integer stackDepth = fStackDepthMap.get(parentId);
        if (stackDepth == null) {
            stackDepth = 0;
        }
        if (stackDepth >= MAX_STACK_DEPTH) {
            /*
             * Limit stackDepth to 100000, to avoid having Attribute Trees grow
             * out of control due to buggy insertions
             */
            String message = " Stack limit reached, not pushing"; //$NON-NLS-1$
            throw new IllegalStateException(" Quark:" + parentId + message); //$NON-NLS-1$
        }
        return stackDepth;

    }

    protected @Nullable Object functionEntry(String spanId, String parentId, String name) {
        return new SpanCustomValue(spanId, (parentId == null) ? "0" : parentId, name);

    }

    protected Map<String, String> MessageHashMapExtractor(ITmfEventField value) {
        // split the string to creat key-value pairs
        Map<String, String> map = new HashMap<>();
        // iterate over the pairs
        for (ITmfEventField field : value.getFields()) {
            Objects.requireNonNull(field);
            map.put(field.getName(), field.getValue().toString().trim());
        }
        if (map.isEmpty()) {
            String valueString = (String) Objects.requireNonNull(value.getValue());
            String[] values = valueString.split(",");
            for (String tuple : values) {
                String[] parts = tuple.split("=");
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    @Override
    public void done() {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        while (!(fPrevEvent.isEmpty())) {
            long prevTime = fPrevEvent.firstKey();
            List<Integer> quarks = Objects.requireNonNull(fPrevEvent.get(fPrevEvent.firstKey()));
            ss.modifySpanAttribute(prevTime, (Object) null, quarks.get(0), quarks.get(1));
            fPrevEvent.remove(fPrevEvent.firstKey());
        }
    }

}
