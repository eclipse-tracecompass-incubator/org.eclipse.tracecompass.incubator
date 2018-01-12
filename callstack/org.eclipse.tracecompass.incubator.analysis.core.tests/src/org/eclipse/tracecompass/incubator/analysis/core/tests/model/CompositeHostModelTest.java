/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICpuTimeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.IThreadOnCpuProvider;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.internal.analysis.core.model.CompositeHostModel;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.tests.stubs.analysis.TestAnalysis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link CompositeHostModel} class
 *
 * @author Geneviève Bastien
 */
public class CompositeHostModelTest {

    private static final @NonNull String HOST_ID = "test host";
    private static final @NonNull String HOST_ID2 = "test host 2";

    /* A stub CPU time provider, no trace required */
    private static class CpuTimeProvider extends TestAnalysis implements ICpuTimeProvider {

        @Override
        public long getCpuTime(int tid, long start, long end) {
            // For the test, return the full duration if tid == 1, otherwise 0
            if (tid == 1) {
                return (end - start);
            }
            return 0;
        }

        @Override
        public @NonNull Collection<@NonNull String> getHostIds() {
            return Collections.singleton(HOST_ID);
        }

    }

    /* A stub thread on CPU provider, no trace required */
    private static class ThreadOnCpuProvider extends TestAnalysis implements IThreadOnCpuProvider {

        @Override
        public @NonNull Collection<@NonNull String> getHostIds() {
            return Collections.singleton(HOST_ID);
        }

        @Override
        public @Nullable Integer getThreadOnCpuAtTime(int cpu, long time, boolean block) {
            // On CPU 0 is tid 1, otherwise, return null
            if (cpu == 0) {
                return 1;
            }
            return null;
        }

    }

    private IThreadOnCpuProvider fThreadOnCpu;
    private ICpuTimeProvider fCpuTime;

    /**
     * Create an instance of each of the modules
     */
    @Before
    public void setupTest() {
        ThreadOnCpuProvider threadOnCpu = new ThreadOnCpuProvider();
        TmfAnalysisManager.analysisModuleCreated(threadOnCpu);
        fThreadOnCpu = threadOnCpu;

        CpuTimeProvider cpuTime = new CpuTimeProvider();
        TmfAnalysisManager.analysisModuleCreated(cpuTime);
        fCpuTime = cpuTime;
    }

    /**
     * Dispose of the modules
     */
    @After
    public void cleanup() {
        IThreadOnCpuProvider threadOnCpu = fThreadOnCpu;
        if (threadOnCpu != null) {
            ((IAnalysisModule) threadOnCpu).dispose();
        }
        ICpuTimeProvider cpuTime = fCpuTime;
        if (cpuTime != null) {
            ((IAnalysisModule) cpuTime).dispose();
        }
    }

    /**
     * Test that the CPU time provider was picked up and returns the right
     * results
     */
    @Test
    public void testCpuTimeProvider() {
        IHostModel model = ModelManager.getModelFor(HOST_ID);
        assertTrue(model instanceof CompositeHostModel);

        long cpuTime = model.getCpuTime(1, 10, 17);
        assertEquals(7, cpuTime);
        cpuTime = model.getCpuTime(2, 10, 17);
        assertEquals(0, cpuTime);
    }

    /**
     * Test that the thread on CPU provider was picked up and returns the right
     * results
     */
    @Test
    public void testThreadOnCpuProvider() {
        IHostModel model = ModelManager.getModelFor(HOST_ID);
        assertTrue(model instanceof CompositeHostModel);

        int threadOnCpu = model.getThreadOnCpu(0, 20);
        assertEquals(1, threadOnCpu);
        threadOnCpu = model.getThreadOnCpu(1, 20);
        assertEquals(IHostModel.UNKNOWN_TID, threadOnCpu);

    }

    /**
     * Test the default values when no model exists for a host
     */
    @Test
    public void testNoModel() {
        IHostModel model = ModelManager.getModelFor(HOST_ID2);
        assertTrue(model instanceof CompositeHostModel);
        long cpuTime = model.getCpuTime(1, 12, 15);
        assertEquals(IHostModel.TIME_UNKNOWN, cpuTime);
        int threadOnCpu = model.getThreadOnCpu(1, 20);
        assertEquals(IHostModel.UNKNOWN_TID, threadOnCpu);
    }

}
