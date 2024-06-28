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
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskData;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskSum;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.util.StackTraceUtil;
import org.eclipse.tracecompass.internal.analysis.counters.core.Activator;


public class WallClockExtractor extends Extractor {
    private static final int ASYNC_PROFILER_DEFAULT_INTERVAL = 50 * 1000 * 1000;

    private static final List<String> INTERESTED = List.of(EventConstant.ACTIVE_SETTING,EventConstant.WALL_CLOCK_SAMPLE);

    static class TaskWallClockData extends TaskData {
        private long begin = 0;
        private long end = 0;
        private long sampleCount = 0;

        TaskWallClockData(RecordedThread thread) {
            super(thread);
        }

        void updateTime(long time) {
            if (begin == 0 || time < begin) {
                begin = time;
            }
            if (end == 0 || time > end) {
                end = time;
            }
        }

        long getDuration() {
            return end - begin;
        }
    }

    private final Map<Long, TaskWallClockData> data = new HashMap<>();
    private long methodSampleEventId = -1;
    private long interval; // nano

    private boolean isWallClockEvents = false;

    public WallClockExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);

        Long id = context.getEventTypeId(EventConstant.WALL_CLOCK_SAMPLE);
        if (id != null) {
            methodSampleEventId = context.getEventTypeId(EventConstant.WALL_CLOCK_SAMPLE);
        }
    }

    TaskWallClockData getThreadData(RecordedThread thread) {
        return data.computeIfAbsent(thread.getJavaThreadId(), i -> new TaskWallClockData(thread));
    }

    @Override
    void visitActiveSetting(RecordedEvent event) {
        if (EventConstant.EVENT.equals(event.getString("name")) && EventConstant.WALL.equals(event.getString("value"))) {
            this.isWallClockEvents = true;
        }

        if (event.getActiveSetting().eventId() == methodSampleEventId) {
            if (EventConstant.WALL.equals(event.getString("name"))) {
                this.isWallClockEvents = true;
                this.interval = Long.parseLong(event.getString("value")) * 1000 * 1000;
            }
            if (EventConstant.INTERVAL.equals(event.getString("name"))) {
                this.interval = Long.parseLong(event.getString("value")) * 1000 * 1000;
            }
        }
    }

    @Override
    void visitExecutionSample(RecordedEvent event) {
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return;
        }

        RecordedThread thread = event.getThread("eventThread");
        if (thread == null) {
            thread = event.getThread("sampledThread");
        }
        if (thread == null) {
            return;
        }
        TaskWallClockData taskWallClockData = getThreadData(thread);

        if (taskWallClockData.getSamples() == null) {
            taskWallClockData.setSamples(new HashMap<>());
        }
        taskWallClockData.updateTime(event.getStartTimeNanos());
        taskWallClockData.getSamples().compute(stackTrace, (k, count) -> count == null ? 1 : count + 1);
        taskWallClockData.sampleCount++;
    }

    private List<TaskSum> buildThreadWallClock() {
        List<TaskSum> taskSumList = new ArrayList<>();
        if (!isWallClockEvents) {
            return taskSumList;
        }

        if (this.interval <= 0) {
            this.interval = ASYNC_PROFILER_DEFAULT_INTERVAL;
            Activator.getInstance().logWarning("use default interval: " + ASYNC_PROFILER_DEFAULT_INTERVAL / 1000 / 1000 + " ms");
        }
        Map<Long, TaskSum> map = new HashMap<>();
        for (TaskWallClockData localData : this.data.values()) {
            if (localData.getSamples() == null) {
                continue;
            }
            TaskSum taskSum = new TaskSum();
            taskSum.setTask(context.getThread(localData.getThread()));
            taskSum.setSum(localData.sampleCount > 1 ? localData.getDuration() : this.interval);
            localData.getSamples().replaceAll((k, v) -> v * (taskSum.getSum() / localData.sampleCount));
            taskSum.setSamples(localData.getSamples().entrySet().stream().collect(
                    Collectors.toMap(
                            e -> StackTraceUtil.build(e.getKey(), context.getSymbols()),
                            Map.Entry::getValue,
                            Long::sum)
            ));
            map.put(localData.getThread().getJavaThreadId(), taskSum);
        }

        map.forEach((k, v) -> {
            taskSumList.add(v);
        });

        taskSumList.sort((o1, o2) -> {
            long delta = o2.getSum() - o1.getSum();
            return delta > 0 ? 1 : (delta == 0 ? 0 : -1);
        });

        return taskSumList;
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> wallClockResult = new DimensionResult<>();
        wallClockResult.setList(buildThreadWallClock());
        result.setWallClock(wallClockResult);
    }
}
