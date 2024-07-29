/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.modeInfo.VmOptions;
import org.junit.Assert;
import org.junit.Test;

public class TestVmOptions {
    @Test
    public void testVmOptions() {
        String optionString = "-server " +
                "-XX:+UseG1GC " +
                "-XX:-DisableExplicitGC " +
                "-verbose:gc " +
                "-Xloggc:gc.log " +
                "-XX:+PrintGCDetails " +
                "-XX:+PrintGCDateStamps " +
                "-XX:+HeapDumpOnOutOfMemoryError " +
                "-XX:HeapDumpPath=/home/admin/logs " +
                "-XX:ErrorFile=/home/admin/logs/hs_err_pid%p.log " +
                "-Xms4200m " +
                "-Xmx4200m " +
                "-XX:ParallelGCThreads=8 " +
                "-XX:MaxNewSize=1500m " +
                "-XX:InitiatingHeapOccupancyPercent=50 " +
                "-XX:G1HeapRegionSize=8m " +
                "-Xss512k " +
                "-XX:MetaspaceSize=10240 " +
                "-XX:MaxMetaspaceSize=512m\n";
        VmOptions options = new VmOptions(optionString);
        Assert.assertEquals(options.getOriginalOptionString(), optionString);
        Assert.assertNull(options.getOptionValue("Xmn"));
        Assert.assertEquals(4200L * 1024 * 1024 * 1024, (long) options.getOptionValue("Xmx"));
        Assert.assertTrue((boolean) options.getOptionValue("server"));
        Assert.assertFalse((boolean) options.getOptionValue("DisableExplicitGC"));
        Assert.assertEquals(50L, (long) options.getOptionValue("InitiatingHeapOccupancyPercent"));
        Assert.assertEquals(10240L, (long) options.getOptionValue("MetaspaceSize"));
        Assert.assertEquals("/home/admin/logs/hs_err_pid%p.log", options.getOptionValue("ErrorFile"));
        Assert.assertEquals("gc", options.getOptionValue("verbose"));
        Assert.assertEquals("gc.log", options.getOptionValue("Xloggc"));

        VmOptions.VmOptionResult result = options.getVmOptionResult();
        Assert.assertTrue(result.getOther().contains(new VmOptions.VmOptionVo("-XX:ErrorFile=/home/admin/logs/hs_err_pid%p.log")));
        Map<String, Integer> optionIndex = new HashMap<>();
        for (int i = 0; i < result.getGcRelated().size(); i++) {
            optionIndex.put(result.getGcRelated().get(i).getText(), i);
        }
        Assert.assertTrue(Objects.requireNonNull(optionIndex.get("-XX:+UseG1GC")) < Objects.requireNonNull(optionIndex.get("-Xms4200m")));
        Assert.assertTrue(Objects.requireNonNull(optionIndex.get("-Xms4200m")) < Objects.requireNonNull(optionIndex.get("-XX:ParallelGCThreads=8")));
        Assert.assertTrue(Objects.requireNonNull(optionIndex.get("-XX:ParallelGCThreads=8")) < Objects.requireNonNull(optionIndex.get("-XX:InitiatingHeapOccupancyPercent=50")));
        Assert.assertTrue(Objects.requireNonNull(optionIndex.get("-XX:InitiatingHeapOccupancyPercent=50")) < Objects.requireNonNull(optionIndex.get("-XX:-DisableExplicitGC")));
        Assert.assertTrue(Objects.requireNonNull(optionIndex.get("-XX:-DisableExplicitGC")) < Objects.requireNonNull(optionIndex.get("-XX:+PrintGCDetails")));
    }
}
