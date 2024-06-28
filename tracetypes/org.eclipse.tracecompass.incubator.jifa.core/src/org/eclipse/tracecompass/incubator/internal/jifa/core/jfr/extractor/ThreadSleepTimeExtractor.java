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
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.AnalysisResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.DimensionResult;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.EventConstant;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.RecordedEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model.TaskSum;

public class ThreadSleepTimeExtractor extends SumExtractor {
    protected static final List<String> INTERESTED = List.of(EventConstant.THREAD_SLEEP);

    public ThreadSleepTimeExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitThreadSleep(RecordedEvent event) {
        visitEvent(event);
    }

    private void visitEvent(RecordedEvent event) {
        long eventValue = event.getLong("time") * 1000 * 1000;
        visitEvent(event, eventValue);
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskSum> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskSums());
        result.setThreadSleepTime(tsResult);
    }
}
