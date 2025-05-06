/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.tests.aspects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.analysis.profiling.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.AnalysisThreadNameAspect;
import org.eclipse.tracecompass.internal.analysis.profiling.core.model.ModelManager;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.TmfTraceStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the model and thread name Aspect with a KernelAnalysisModule from a kernel trace
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class AnalysisThreadNameAspectTest {

    /*
     * For this trace, process 4111 was on CPU between 1332170688674418133 and
     * 1332170688674530927 and on CPU 1 was process 1344
     */
    private static final CtfTestTrace TRACE = CtfTestTrace.KERNEL;
    private static final long startTs = 1332170688674418133L;
    private static final int processCpu0 = 4111;
    private static final String execNameCpu0 = "threaded-ml";

    private @Nullable LttngKernelTrace fTrace;

    /**
     * Set-up the test
     */
    @Before
    public void setUp() {
        LttngKernelTrace trace = LttngKernelTestTraceUtils.getTrace(TRACE);
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        IAnalysisModule module = trace.getAnalysisModule(KernelAnalysisModule.ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());
        module = trace.getAnalysisModule(TidAnalysisModule.ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());
        fTrace = trace;
    }

    /**
     * Dispose test objects
     */
    @After
    public void tearDown() {
        LttngKernelTestTraceUtils.dispose(CtfTestTrace.KERNEL);
        fTrace = null;
    }

    /**
     * Test that the {@link KernelAnalysisModule} is added to the model of the
     * trace
     */
    @Test
    public void testModelExecName() {
        LttngKernelTrace trace = fTrace;
        assertNotNull(trace);

        IHostModel model = ModelManager.getModelFor(trace.getHostId());
        int threadOnCpu = model.getThreadOnCpu(0, startTs + 1);
        assertEquals(processCpu0, threadOnCpu);
        String execName = model.getExecName(threadOnCpu, startTs + 1);
        assertEquals(execNameCpu0, execName);
    }

    /**
     * Test that resolving the aspect for another trace with the same host ID
     * gets the result from the Kernel analysis module
     */
    @Test
    public void testAspectFromOtherTrace() {
        LttngKernelTrace trace = fTrace;
        assertNotNull(trace);

        ITmfTrace otherTrace = new TmfTraceStub() {

            @Override
            public @NonNull String getHostId() {
                return trace.getHostId();
            }

            private final Collection<ITmfEventAspect<?>> fEventAspects = ImmutableList.of(new LinuxTidAspect() {
                @Override
                public @Nullable Integer resolve(ITmfEvent event) {
                    return processCpu0;
                }
            });

            @Override
            public Iterable<ITmfEventAspect<?>> getEventAspects() {
                return fEventAspects;
            }

        };

        try {
            ITmfEvent event = new TmfEvent(otherTrace, 1, TmfTimestamp.fromNanos(startTs), new TmfEventType(ITmfEventType.DEFAULT_TYPE_ID, null), null);
            Object execName = TmfTraceUtils.resolveEventAspectOfClassForEvent(otherTrace, AnalysisThreadNameAspect.class, event);
            assertTrue(execName instanceof String);
            assertEquals(execNameCpu0, execName);
        } finally {
            otherTrace.dispose();
        }
    }

}
