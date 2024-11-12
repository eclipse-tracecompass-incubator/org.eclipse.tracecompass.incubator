/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.dpdk.core.tests.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.dpdk.core.tests.stubs.DpdkTraceStub;
import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Wrapper for the LTTng Kernel test traces, instantiating
 * {@link DpdkTrace} objects from them.
 */
@NonNullByDefault
public final class DpdkTestTraceUtils extends CtfTmfTestTraceUtils {

    private static final Map<CtfTestTrace, CtfTmfTrace> LTTNG_KERNEL_TRACES = new HashMap<>();

    private DpdkTestTraceUtils() {
        super();
    }

    public static synchronized DpdkTrace getTrace(CtfTestTrace ctfTrace) {
        return (DpdkTrace) new DpdkTestTraceUtils().internalGetTrace(ctfTrace, LTTNG_KERNEL_TRACES, new DpdkTraceStub());
    }

    public static synchronized void dispose(CtfTestTrace ctfTrace) {
        new DpdkTestTraceUtils().internalDispose(ctfTrace, LTTNG_KERNEL_TRACES);
    }
}
