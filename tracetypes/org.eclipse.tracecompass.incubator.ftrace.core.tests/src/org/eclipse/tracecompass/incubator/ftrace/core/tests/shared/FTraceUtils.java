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
package org.eclipse.tracecompass.incubator.ftrace.core.tests.shared;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;

/**
 * A utility class to get trace absolute paths and initialized trace from
 * {@link FtraceTestTrace} for tests.
 *
 * @author Hoang Thuan Pham
 *
 */
public class FTraceUtils {
    /**
     * Constructor
     */
    protected FTraceUtils() {
    }

    /**
     * Obtain the absolute path to a trace file.
     *
     * @param testTrace
     *            The {@link FtraceTestTrace} represent the trace file.
     * @return The absolute path to the trace file.
     * @throws IOException
     *             If there is an error getting the test trace.
     */
    public static synchronized String getTraceAbsolutePath(FtraceTestTrace testTrace) throws IOException {
        return FileUtils.toFile(FileLocator.toFileURL(testTrace.getTraceURL())).getAbsolutePath();
    }
}