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
package org.eclipse.tracecompass.incubator.jifa.core.tests.gclog;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment.GCLogAnalyzer;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.fragment.Metric;
import org.junit.Assert;
import org.junit.Test;

public class TestFragmentedParserToMetrics {
    public static final double DELTA = 1e-6;
    private static final String INSTANCEIDKEY = "instanceId", INSTANCEIDVALUE = "test-instanceId";

    private static List<Metric> parse(List<String> context, long startTime, long endTime) {
        GCLogAnalyzer gcLogAnalyzer = new GCLogAnalyzer();
        List<Metric> result = null;
        Map<String, String> instanceId = Map.of(INSTANCEIDKEY, INSTANCEIDVALUE);
        try {
            result = gcLogAnalyzer.parseToMetrics(context, instanceId, startTime, endTime);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return result;
    }

    // This is barely a template, fill the file set to evaluate analyse speed.
    public void testAnalyseSpeed() {
        Set<String> files = Collections.emptySet();
        GCLogAnalyzer gcLogAnalyzer = new GCLogAnalyzer();
        files.forEach(file -> {
            List<String> gclog = TestUtil.generateShuffledGCLog(file);
            Map<String, String> instanceId = Map.of(INSTANCEIDKEY, file);
            long beginTime = System.currentTimeMillis();
            List<Metric> result = null;
            try {
                result = gcLogAnalyzer.parseToMetrics(gclog, instanceId, 0, Long.MAX_VALUE);
                assertNotNull(result);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            double spendTime = (System.currentTimeMillis() - beginTime) / 1000.;
            assertNotNull(result);
            System.out.printf("file %s analysed，%d lines in total，%d metrics produced，duration %fs，%f lines processed per minute，producing %f metrics\n",
                    file, gclog.size(), result.size(), spendTime, gclog.size() / spendTime * 60, result.size() / spendTime * 60);
        });
    }

    @Test
    public void testMetricLength() {
        Map<String, Integer> metricSizeForEachGCLog = new HashMap<>();
        metricSizeForEachGCLog.put("11G1Parser.log", 50);
        metricSizeForEachGCLog.put("8CMSParser.log", 130);
        metricSizeForEachGCLog.put("8CMSPrintGC.log", 40);
        metricSizeForEachGCLog.put("8G1PrintGC.log", 47);
        metricSizeForEachGCLog.put("8ParallelGCParser.log", 75);
        metricSizeForEachGCLog.put("8G1GCParser.log", 77);
        metricSizeForEachGCLog.put("8G1GCParserAdaptiveSize.log", 28);
        metricSizeForEachGCLog.put("8ConcurrentPrintDateTimeStamp.log", 65);
        metricSizeForEachGCLog.put("8CMSCPUTime.log", 26);
        metricSizeForEachGCLog.put("8CMSPromotionFailed.log", 13);
        metricSizeForEachGCLog.put("8CMSScavengeBeforeRemark.log", 38);
        metricSizeForEachGCLog.put("8GenerationalGCInterleave.log", 13);
        metricSizeForEachGCLog.forEach((gclog, size) -> Assert.assertEquals(Integer.valueOf(parse(TestUtil.generateShuffledGCLog(gclog), 0, Long.MAX_VALUE).size()), size));
    }

    @Test
    public void testMetricContent() {
        List<Metric> metrics = parse(TestUtil.generateShuffledGCLog("8CMSCPUTime.log"), 1669618629000L, 1669618630000L);

        Map<String, Map<String, Double>> valueForEachMetric = Map.of("GC_CPU_USED", Map.of("USER", 180.0, "SYS", 30.0, "REAL", 50.0),
                "GC_PAUSE_TIME", Map.of("Young GC", 50.0),
                "BEFORE_GC_REGION_SIZE", Map.of("Young", 1761607680., "Old", 0., "Heap", 1761607680.),
                "AFTER_GC_REGION_SIZE", Map.of("Young", 36896768., "Old", 0., "Heap", 36896768.));

        Assert.assertEquals(metrics.size(), 12);
        valueForEachMetric.forEach((metricName, labelMap) -> labelMap.forEach((label, value) -> {
            List<Metric> actualMetrics = metrics.stream()
                    .filter(metric -> metric.getName().equals(metricName))
                    .filter(metric -> Objects.equals(metric.getLabel().get("type"), label))
                    .filter(metric -> metric.getTimestamp() == 1669618629974L)
                    .filter(metric -> Objects.equals(metric.getLabel().get("gc_type"), "CMS GC"))
                    .filter(metric -> Objects.equals(metric.getLabel().get(INSTANCEIDKEY), INSTANCEIDVALUE))
                    .collect(Collectors.toList());
            Assert.assertEquals(actualMetrics.size(), 1);
            Assert.assertEquals(actualMetrics.get(0).getValue(), value, DELTA);
        }));
    }

    @Test
    public void testMetricContentSubphase() {
        List<Metric> metrics = parse(TestUtil.generateShuffledGCLog("11CMSUpTime.log"), 0, Long.MAX_VALUE);
        Map<String, Double> subphaseMap = Map.of(
                "Initial Mark", 3.41,
                "Concurrent Mark", 136.57,
                "Concurrent Preclean", 4.724,
                "Concurrent Abortable preclean", 197.909,
                "Final Remark", 75.082,
                "Concurrent Sweep", 166.162,
                "Concurrent Reset", 609.769);

        subphaseMap.forEach((subphase, value) -> {
            List<Metric> actualMetrics = metrics.stream()
                    .filter(metric -> metric.getTimestamp() == 1693292160355L)
                    .filter(metric -> metric.getName().equals("GC_SUBPHASE_TIME"))
                    .filter(metric -> Objects.equals(metric.getLabel().get("gc_type"), "CMS GC"))
                    .filter(metric -> Objects.equals(metric.getLabel().get(INSTANCEIDKEY), INSTANCEIDVALUE))
                    .filter(metric -> Objects.equals(metric.getLabel().get("type"), "CMS"))
                    .filter(metric -> Objects.equals(metric.getLabel().get("subphase"), subphase))
                    .collect(Collectors.toList());
            Assert.assertEquals(actualMetrics.size(), 1);
            Assert.assertEquals(actualMetrics.get(0).getValue(), value, DELTA);
        });
    }
}
