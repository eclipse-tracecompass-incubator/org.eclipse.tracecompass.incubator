/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.ros.core.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.RosTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

/**
 * Test the {@link RosTrace} ROS traces
 *
 * @author Christophe Bedard
 */
public class RosTraceTest {

    private static final String TRACE_PATH = "traces/pub-sub-10";

    /**
     * Test correct validation of ROS trace
     */
    @Test
    public void testValidate() {
        ITmfTrace trace = new RosTrace();
        try {
            IStatus validate = trace.validate(null, TRACE_PATH);
            assertTrue(validate.getMessage(), validate.isOK());
        } finally {
            trace.dispose();
        }
    }
}
