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

package org.eclipse.tracecompass.incubator.ros2.core.tests.trace;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.trace.Ros2Trace;
import org.eclipse.tracecompass.incubator.ros2.core.tests.Ros2TestTraceUtils;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link Ros2Trace} class
 *
 * @author Christophe Bedard
 */
public class Ros2TraceTest {

    private static final @NonNull CtfTestTrace TEST_TRACE = CtfTestTrace.ROS2;

    private ITmfTrace fTrace;

    /**
     * Perform pre-class initialization.
     */
    @Before
    public void setUp() {
        fTrace = Ros2TestTraceUtils.getTrace(TEST_TRACE);
    }

    /** Empty and delete a directory */
    private static void deleteDirectory(File dir) throws IOException {
        /* Assuming the dir only contains file or empty directories */
        for (File file : dir.listFiles()) {
            Files.delete(file.toPath());
        }
        Files.delete(dir.toPath());
    }

    /**
     * Perform post-class clean-up.
     *
     * @throws IOException
     *             failed to delete supplementaty files directory
     */
    @After
    public void tearDown() throws IOException {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            Ros2TestTraceUtils.dispose(TEST_TRACE);
            File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
            deleteDirectory(suppDir);
        }
    }

    /**
     * Test trace validation
     */
    @Test
    public void testTraceValidation() {
        Ros2Trace ros2Trace = new Ros2Trace();
        IStatus status = ros2Trace.validate(null, fTrace.getPath());
        assertTrue(status instanceof TraceValidationStatus);
        TraceValidationStatus traceStatus = (TraceValidationStatus) status;
        assertTrue(traceStatus.isOK());
    }
}
