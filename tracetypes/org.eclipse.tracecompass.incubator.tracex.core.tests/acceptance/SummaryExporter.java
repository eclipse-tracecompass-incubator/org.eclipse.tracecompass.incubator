/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventIndex;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistryRecord;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;

/** Exports the complete logical model for independent acceptance comparisons. */
public final class SummaryExporter {
    /** Read one binary input and emit an unsigned, chronological JSON model. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Supply a binary trace and increasing or decreasing");
        }
        try (TraceXFile input = TraceXFile.open(Path.of(args[0]))) {
            TraceXTimerDirection direction = TraceXTimerDirection.parse(args[1]);
            TraceXEventIndex index = TraceXEventIndex.build(input, direction, () -> false);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("byte_order", input.getLayout().getByteOrder().toString());
            result.put("timer_mask", input.getLayout().getTimerValidMask());
            result.put("timer_direction", direction.getPropertyValue());
            result.put("event_count", index.getEventCount());
            result.put("diagnostics", input.getRegistry().getDiagnostics());
            List<Map<String, Object>> registry = new ArrayList<>();
            for (TraceXRegistryRecord r : input.getRegistry().getRecords()) {
                if (!r.isValid()) {
                    continue;
                }
                Map<String, Object> object = new LinkedHashMap<>();
                object.put("type", r.getTypeCode());
                object.put("address", r.getAddress());
                object.put("parameter1", r.getParameter1());
                object.put("parameter2", r.getParameter2());
                object.put("name", r.getName());
                registry.add(object);
            }
            result.put("registry", registry);
            List<Map<String, Object>> events = new ArrayList<>();
            for (long rank = 0; rank < index.getEventCount(); rank++) {
                TraceXEvent event = index.readEvent(input, rank);
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("rank", rank);
                e.put("context", event.getRawContextAddress());
                e.put("raw_priority", event.getRawPriority());
                e.put("raw_event_id", event.getRawEventId());
                e.put("event_id", event.getNormalizedEventId());
                e.put("masked_timestamp", event.getRawTimestamp() & input.getLayout().getTimerValidMask());
                e.put("raw_timestamp", event.getRawTimestamp());
                e.put("relative_ticks", index.getRelativeTicks(rank));
                e.put("info", new long[] { event.getInformationWord(1),
                        event.getInformationWord(2), event.getInformationWord(3),
                        event.getInformationWord(4) });
                e.put("fields", event.getFields());
                e.put("name", event.getMetadata().getName());
                events.add(e);
            }
            result.put("events", events);
            System.out.println(new Gson().toJson(result));
        }
    }
}
