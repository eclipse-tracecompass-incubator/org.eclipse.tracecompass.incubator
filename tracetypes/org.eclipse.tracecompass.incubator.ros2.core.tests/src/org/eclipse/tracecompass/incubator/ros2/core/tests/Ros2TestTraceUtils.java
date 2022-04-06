/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.ros2.core.tests;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.internal.ros2.core.trace.Ros2Trace;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

/**
 * Wrapper for the ROS 2 test traces, instantiating {@link Ros2Trace} objects
 * from them.
 *
 * @author Christophe Bedard
 */
@NonNullByDefault
public class Ros2TestTraceUtils extends CtfTmfTestTraceUtils {

    private static final Map<CtfTestTrace, CtfTmfTrace> ROS2_TRACES = new HashMap<>();

    private Ros2TestTraceUtils() {
        super();
    }

    /**
     * Return a Ros2TraceStub object of this test trace. It will be already
     * initTrace()'ed.
     *
     * After being used by unit tests, traces should be properly disposed by
     * calling the {@link #dispose(CtfTestTrace)} method.
     *
     * @param ctfTrace
     *            The test trace to initialize
     * @return A Ros2TraceStub reference to this trace
     */
    public static synchronized Ros2Trace getTrace(CtfTestTrace ctfTrace) {
        return (Ros2Trace) new Ros2TestTraceUtils().internalGetTrace(ctfTrace, ROS2_TRACES, new Ros2TraceStub());
    }

    /**
     * Dispose of the trace
     *
     * @param ctfTrace
     *            Trace to dispose
     */
    public static synchronized void dispose(CtfTestTrace ctfTrace) {
        new Ros2TestTraceUtils().internalDispose(ctfTrace, ROS2_TRACES);
    }
}
