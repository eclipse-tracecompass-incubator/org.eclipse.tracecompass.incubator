/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.trace;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.jsontrace.core.job.SortingJob;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Sorting job particular to trace event traces
 *
 * @author Katherine Nadeau
 *
 */
public class TraceEventSortingJob extends SortingJob {

    /**
     * Constructor
     *
     * @param trace
     *            the trace to be sort
     * @param path
     *            the path to the trace file
     */
    public TraceEventSortingJob(ITmfTrace trace, String path) {
        super(trace, path, "\"ts\":", 1); //$NON-NLS-1$
    }

    @Override
    protected void processMetadata(ITmfTrace trace, String dir) throws IOException {
        // No metadata to be read in trace event traces
    }

}
