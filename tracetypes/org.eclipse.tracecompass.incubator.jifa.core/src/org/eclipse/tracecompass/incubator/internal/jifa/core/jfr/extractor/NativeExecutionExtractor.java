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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.AnalysisResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.DimensionResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.EventConstant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.Task;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskCount;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.util.StackTraceUtil;

public class NativeExecutionExtractor extends CountExtractor {

    protected static final List<String> INTERESTED = List.of(EventConstant.NATIVE_EXECUTION_SAMPLE);

    public NativeExecutionExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitNativeExecutionSample(RecordedEvent event) {
        visitEvent(event);
    }

    private List<TaskCount> buildTaskExecutionSamples() {
        List<TaskCount> nativeSamples = new ArrayList<>();

        for (TaskCountData localData : this.data.values()) {
            if (localData.count == 0) {
                continue;
            }

            TaskCount threadSamples = new TaskCount();
            Task ta = new Task();
            ta.setId(localData.getThread().getJavaThreadId());
            ta.setName(localData.getThread().getJavaName());
            threadSamples.setTask(ta);

            if (localData.getSamples() != null) {
                threadSamples.setCount(localData.count);
                threadSamples.setSamples(localData.getSamples().entrySet().stream().collect(
                        Collectors.toMap(
                                e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                                Map.Entry::getValue,
                                Long::sum
                        )
                ));
            }

            nativeSamples.add(threadSamples);
        }

        nativeSamples.sort((o1, o2) -> {
            long delta = o2.getCount() - o1.getCount();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return nativeSamples;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> nativeResult = new DimensionResult<>();
        nativeResult.setList(buildTaskExecutionSamples());
        result.setNativeExecutionSamples(nativeResult);
    }
}
