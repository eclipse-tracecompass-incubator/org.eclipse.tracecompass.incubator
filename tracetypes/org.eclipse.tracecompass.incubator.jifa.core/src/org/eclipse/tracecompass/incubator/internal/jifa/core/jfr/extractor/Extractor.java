package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.AnalysisResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.EventConstant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;

public abstract class Extractor extends EventVisitor {
    private static final Map<String, BiConsumer<Extractor, RecordedEvent>> DISPATCHER = new HashMap<>();
    static {
            DISPATCHER.put(EventConstant.GARBAGE_COLLECTION, EventVisitor::visitGarbageCollection);
            DISPATCHER.put(EventConstant.UNSIGNED_INT_FLAG, EventVisitor::visitUnsignedIntFlag);
            DISPATCHER.put(EventConstant.CPU_INFORMATION, EventVisitor::visitCPUInformation);
            DISPATCHER.put(EventConstant.CPC_RUNTIME_INFORMATION, EventVisitor::visitCPCRuntimeInformation);
            DISPATCHER.put(EventConstant.ENV_VAR, EventVisitor::visitEnvVar);
            DISPATCHER.put(EventConstant.ACTIVE_SETTING, EventVisitor::visitActiveSetting);
            DISPATCHER.put(EventConstant.THREAD_START, EventVisitor::visitThreadStart);
            DISPATCHER.put(EventConstant.THREAD_CPU_LOAD, EventVisitor::visitThreadCPULoad);
            DISPATCHER.put(EventConstant.PROCESS_CPU_LOAD, EventVisitor::visitProcessCPULoad);
            DISPATCHER.put(EventConstant.EXECUTION_SAMPLE, EventVisitor::visitExecutionSample);
            DISPATCHER.put(EventConstant.NATIVE_EXECUTION_SAMPLE, EventVisitor::visitNativeExecutionSample);
            DISPATCHER.put(EventConstant.EXECUTE_VM_OPERATION, EventVisitor::visitExecuteVMOperation);
            DISPATCHER.put(EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB, EventVisitor::visitObjectAllocationInNewTLAB);
            DISPATCHER.put(EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB, EventVisitor::visitObjectAllocationOutsideTLAB);
            DISPATCHER.put(EventConstant.OBJECT_ALLOCATION_SAMPLE, EventVisitor::visitObjectAllocationSample);

            DISPATCHER.put(EventConstant.FILE_FORCE, EventVisitor::visitFileForce);
            DISPATCHER.put(EventConstant.FILE_READ, EventVisitor::visitFileRead);
            DISPATCHER.put(EventConstant.FILE_WRITE, EventVisitor::visitFileWrite);

            DISPATCHER.put(EventConstant.SOCKET_READ, EventVisitor::visitSocketRead);
            DISPATCHER.put(EventConstant.SOCKET_WRITE, EventVisitor::visitSocketWrite);

            DISPATCHER.put(EventConstant.JAVA_MONITOR_ENTER, EventVisitor::visitMonitorEnter);
            DISPATCHER.put(EventConstant.THREAD_PARK, EventVisitor::visitThreadPark);

            DISPATCHER.put(EventConstant.CLASS_LOAD, EventVisitor::visitClassLoad);

            DISPATCHER.put(EventConstant.THREAD_SLEEP, EventVisitor::visitThreadSleep);
    }

    final JFRAnalysisContext context;

    private final List<String> interested;

    Extractor(JFRAnalysisContext context, List<String> interested) {
        this.context = context;
        this.interested = interested;
    }

    private boolean accept(RecordedEvent event) {
        return interested.contains(event.getEventType().getName());
    }

    public void process(RecordedEvent event) {
        if (accept(event)) {
            BiConsumer<Extractor, RecordedEvent> biConsumer = DISPATCHER.get(event.getEventType().getName());
            if (biConsumer != null) {
                biConsumer.accept(this, event);
            }
        }
    }

    public abstract void fillResult(AnalysisResult result);
}