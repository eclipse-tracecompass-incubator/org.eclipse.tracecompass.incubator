/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.security;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Perform various security tests for the BinaryFTraceFileParserImp
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFileParserTest {
    private static String injectedTraceHeaderPageSection;
    private static String injectedTraceCPU;

    /**
     * Initialize the paths to the trace files
     *
     * @throws IOException
     *             If an error occurred while getting the trace paths
     */
    @BeforeClass
    public static void init() throws IOException {
        injectedTraceHeaderPageSection = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_BAD_HEADER_PAGE_SECTION_SIZE);
        injectedTraceCPU = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_BAD_CPU_SECTION_SIZE);
    }

    /**
     * A03 Injection
     */
    /**
     * Test parsing a trace injected with a bad Header Page section size
     *
     * @throws TmfTraceException
     *             If the parser fails to parse the trace
     */
    @Test(expected = TmfTraceException.class)
    public void testBadHeaderPageSectionSize() throws TmfTraceException {
        BinaryFTraceFileParser.parse(injectedTraceHeaderPageSection);
    }

    /**
     * Test parsing a trace injected with a bad CPU section size
     *
     * @throws TmfTraceException
     *             If the parser fails to parse the trace
     */
    @Test(expected = TmfTraceException.class)
    public void testBadCpuSectionSize() throws TmfTraceException {
        BinaryFTraceFileParser.parse(injectedTraceCPU);
    }
}
