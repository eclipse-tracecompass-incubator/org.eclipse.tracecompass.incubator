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
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.util;

import java.util.List;

public class GCUtil {
    // JDK 8 default: ParallelScavenge + ParallelOld
    // CMS: ParNew + ConcurrentMarkSweep + SerialOld
    // G1: G1New + G1Old + SerialOld

    private static final List<String> PARALLEL_GC = List.of("G1New","ParNew","ParallelScavenge","ParallelOld");

    private static final List<String> CONCURRENT_GC = List.of(
        "G1Old",
        "ConcurrentMarkSweep");

    private static final List<String> SERIAL_GC = List.of("SerialOld");

    public static boolean isConcGC(String name) {
        return CONCURRENT_GC.contains(name);
    }

    public static boolean isParallelGC(String name) {
        return PARALLEL_GC.contains(name);
    }

    public static boolean isSerialGC(String name) {
        return SERIAL_GC.contains(name);
    }
}
