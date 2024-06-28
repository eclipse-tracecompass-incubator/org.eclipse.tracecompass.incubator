/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.AnalysisResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.DimensionResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.EventConstant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedStackTrace;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedThread;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.Task;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskAllocations;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskData;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.util.StackTraceUtil;

public class AllocationsExtractor extends Extractor {
    protected boolean useObjectAllocationSample;

    protected static final List<String> INTERESTED = List.of(EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB, EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB, EventConstant.OBJECT_ALLOCATION_SAMPLE);

    protected static class AllocTaskData extends TaskData {
        AllocTaskData(RecordedThread thread) {
            super(thread);
        }

        public long allocations;

        public long allocatedMemory;
    }

    protected final Map<Long, AllocTaskData> data = new HashMap<>();

    public AllocationsExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
        try {
            this.useObjectAllocationSample = this.context.getActiveSettingBool(EventConstant.OBJECT_ALLOCATION_SAMPLE, "enabled");
        } catch (Exception e) {
            this.useObjectAllocationSample = false;
        }
    }

    AllocTaskData getThreadData(RecordedThread thread) {
        return data.computeIfAbsent(thread.getJavaThreadId(), i -> new AllocTaskData(thread));
    }

    @Override
    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        if (useObjectAllocationSample) {
            return;
        }
        visitEvent(event);
    }

    @Override
    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        if (useObjectAllocationSample) {
            return;
        }
        this.visitEvent(event);
    }

    @Override
    void visitObjectAllocationSample(RecordedEvent event) {
        this.visitEvent(event);
    }

    void visitEvent(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            stackTrace = StackTraceUtil.DUMMY_STACK_TRACE;
        }

        AllocTaskData allocThreadData = getThreadData(event.getThread());
        if (allocThreadData.getSamples() == null) {
            allocThreadData.setSamples(new HashMap<>());
        }

        allocThreadData.getSamples().compute(stackTrace, (k, count) -> count == null ? 1 : count + 1);
        allocThreadData.allocations += 1;
    }

    private List<TaskAllocations> buildThreadAllocations() {
        List<TaskAllocations> taskAllocations = new ArrayList<>();
        for (AllocTaskData localData : this.data.values()) {
            if (localData.allocations == 0) {
                continue;
            }

            TaskAllocations threadAllocation = new TaskAllocations();
            Task ta = new Task();
            ta.setId(localData.getThread().getJavaThreadId());
            ta.setName(localData.getThread().getJavaName());
            threadAllocation.setTask(ta);

            if (localData.getSamples() != null) {
                threadAllocation.setAllocations(localData.allocations);
                threadAllocation.setSamples(localData.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum)));
            }

            taskAllocations.add(threadAllocation);
        }

        taskAllocations.sort((o1, o2) -> {
            long delta = o2.getAllocations() - o1.getAllocations();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return taskAllocations;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskAllocations> allocResult = new DimensionResult<>();
        allocResult.setList(buildThreadAllocations());
        result.setAllocations(allocResult);
    }
}
