/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.GCEvent;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo.MemoryArea;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;

public class GCModelConverter {
    public List<Metric> toMetrics(GCModel gcModel, Map<String, String> instanceId, long startTime, long endTime) {
        List<Metric> result = new ArrayList<>();
        Map<String, String> sharedLabels = new HashMap<>(instanceId);
        sharedLabels.put("gc_type", gcModel.getCollectorType().getName());
        for (GCEvent gcEvent : gcModel.getGcEvents()) {
            long timestamp = (long) (gcModel.getReferenceTimestamp() + gcEvent.getStartTime());
            if (timestamp >= startTime && timestamp < endTime) {
                result.addAll(new GCEventConverter().toMetrics(gcEvent, timestamp, sharedLabels));
            }
        }
        return result;
    }

    private class GCEventConverter {
        private GCEvent fGcEvent;
        private long fTimestamp;
        private Map<String, String> fSharedLabels;
        private List<Metric> fResult = new ArrayList<>();

        public List<Metric> toMetrics(GCEvent gcEvent, long timestamp, Map<String, String> sharedLabels) {
            this.fGcEvent = gcEvent;
            this.fTimestamp = timestamp;
            this.fSharedLabels = sharedLabels;
            addMetricCpuTime();
            addMetricDuration();
            addMetricMemoryUsage();
            addMetricPause();
            addMetricPromotion();
            addMetricSubphase();
            return fResult;
        }

        private Map<String, String> buildLabel() {
            return fSharedLabels;
        }

        private Map<String, String> buildLabel(String key, String value) {
            Map<String, String> label = new HashMap<>(fSharedLabels);
            label.put(key, value);
            return label;
        }

        private Map<String, String> buildLabel(String key1, String value1, String key2, String value2) {
            Map<String, String> label = new HashMap<>(fSharedLabels);
            label.put(key1, value1);
            label.put(key2, value2);
            return label;
        }

        private void addMetricDuration() {
            if (fGcEvent.getDuration() == Constant.UNKNOWN_DOUBLE) {
                return;
            }
            fResult.add(new Metric(fTimestamp, buildLabel("type", fGcEvent.getEventType().getName()), "GC_COST_TIME", fGcEvent.getDuration()));
        }

        private void addMetricPause() {
            if (fGcEvent.getPause() == Constant.UNKNOWN_DOUBLE) {
                return;
            }
            fResult.add(new Metric(fTimestamp, buildLabel("type", fGcEvent.getEventType().getName()), "GC_PAUSE_TIME", fGcEvent.getPause()));
        }

        private void addMetricPromotion() {
            if (fGcEvent.getPromotion() == Constant.UNKNOWN_INT) {
                return;
            }
            fResult.add(new Metric(fTimestamp, buildLabel(), "GC_PROMOTION", fGcEvent.getPromotion()));
        }

        private void addMetricCpuTime() {
            if (fGcEvent.getCpuTime() == null) {
                return;
            }
            final Set<String> typeSet = Set.of("USER", "SYS", "REAL");
            typeSet.forEach(type -> fResult.add(new Metric(fTimestamp, buildLabel("type", type), "GC_CPU_USED", fGcEvent.getCpuTime().getValue(type))));
        }

        private void addMetricMemoryUsage() {
            Map<String, MemoryArea> memoryAreaMap = Map.of("Young", MemoryArea.YOUNG, "Old", MemoryArea.OLD, "Humongous", MemoryArea.HUMONGOUS, "Heap", MemoryArea.HEAP, "Metaspace", MemoryArea.METASPACE);
            memoryAreaMap.forEach((key, value) -> {
                if (fGcEvent.getMemoryItem(value) != null) {
                    fResult.add(new Metric(fTimestamp, buildLabel("type", key), "BEFORE_GC_REGION_SIZE", fGcEvent.getMemoryItem(value).getPreUsed()));
                    fResult.add(new Metric(fTimestamp, buildLabel("type", key), "AFTER_GC_REGION_SIZE", fGcEvent.getMemoryItem(value).getPostUsed()));
                }
            });
        }

        private void addMetricSubphase() {
            if (fGcEvent.getPhases() == null) {
                return;
            }
            fGcEvent.getPhases().forEach(subphase -> fResult.add(new Metric(fTimestamp,
                    buildLabel("subphase", subphase.getEventType().getName(), "type", fGcEvent.getEventType().getName()),
                    "GC_SUBPHASE_TIME", subphase.getDuration())));
        }
    }
}
