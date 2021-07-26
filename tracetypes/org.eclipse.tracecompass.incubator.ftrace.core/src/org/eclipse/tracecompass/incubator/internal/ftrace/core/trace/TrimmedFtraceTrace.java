/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.trace;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;

/**
 * Trimmed Ftrace trace.
 *
 * @author Matthew Khouzam
 */
public class TrimmedFtraceTrace extends FtraceTrace {

    @Override
    public IStatus validate(IProject project, String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof TraceValidationStatus) {
            TraceValidationStatus traceValidationStatus = (TraceValidationStatus) status;
            status = new TraceValidationStatus(traceValidationStatus.getConfidence() - 1, traceValidationStatus.getPlugin());
        }
        return status;
    }

    @Override
    protected long getFileStart() throws IOException {
        long start = 0;
        try (RandomAccessFile fileInput = new BufferedRandomAccessFile(getFile(), "r")) { //$NON-NLS-1$
            fileInput.readLine();
            String line = fileInput.readLine();
            while (line != null) {
                if (line.trim().startsWith("#")) { //$NON-NLS-1$
                    start = fileInput.getFilePointer();
                }
                line = fileInput.readLine();
            }
        }
        return start;
    }
}
