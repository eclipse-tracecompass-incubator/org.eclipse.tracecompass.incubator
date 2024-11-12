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
package org.eclipse.tracecompass.incubator.dpdk.core.tests.lcore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.tracecompass.incubator.dpdk.core.tests.utils.DpdkTestTraceUtils;
import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.DpdkLogicalCoreAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link DpdkLogicalCoreAnalysisModule} class
 *
 * @author Arnaud Fiorini
 */
public class LcoreStateSystemTest {

    private DpdkTrace fTrace;
    private DpdkLogicalCoreAnalysisModule fDpdkLogicalCoreAnalysisModule;

    /**
     * Set-up the test
     */
    @Before
    public void setUp() {
        fDpdkLogicalCoreAnalysisModule = new DpdkLogicalCoreAnalysisModule();
        fTrace = DpdkTestTraceUtils.getTrace(CtfTestTrace.DPDK_SERVICE_CORES);
    }

    /**
     * Dispose test objects
     */
    @After
    public void tearDown() {
        fDpdkLogicalCoreAnalysisModule.dispose();
        DpdkTestTraceUtils.dispose(CtfTestTrace.DPDK_SERVICE_CORES);
        fTrace = null;
        fDpdkLogicalCoreAnalysisModule = null;
    }

    /**
     * Test that the module as a callstack provider produces the same results as
     * the callstack analysis module under it
     */
    @Test
    public void testCallStackProvider() {
        fDpdkLogicalCoreAnalysisModule.setId("test");
        ITmfTrace trace = fTrace;
        assertNotNull(trace);
        try {
            assertTrue(fDpdkLogicalCoreAnalysisModule.setTrace(trace));
        } catch (TmfAnalysisException e) {
            fail(e.getMessage());
        }
        // Assert the state system has not been initialized yet
        ITmfStateSystem ss = fDpdkLogicalCoreAnalysisModule.getStateSystem();
        assertNull(ss);

        assertTrue(TmfTestHelper.executeAnalysis(fDpdkLogicalCoreAnalysisModule));

        ss = fDpdkLogicalCoreAnalysisModule.getStateSystem();
        assertNotNull(ss);

        List<Integer> quarks = ss.getQuarks("*");
        assertFalse(quarks.isEmpty());
    }

    /**
     * Test the canExecute method on valid and invalid traces
     */
    @Test
    public void testCanExecute() {
        /* Test with a valid dpdk trace */
        assertNotNull(fTrace);
        assertTrue(fDpdkLogicalCoreAnalysisModule.canExecute(fTrace));

        /* Test with a CTF trace that does not have required events */
        CtfTmfTrace trace = CtfTmfTestTraceUtils.getTrace(CtfTestTrace.CYG_PROFILE);
        assertFalse(fDpdkLogicalCoreAnalysisModule.canExecute(trace));
        CtfTmfTestTraceUtils.dispose(CtfTestTrace.CYG_PROFILE);
    }
}
