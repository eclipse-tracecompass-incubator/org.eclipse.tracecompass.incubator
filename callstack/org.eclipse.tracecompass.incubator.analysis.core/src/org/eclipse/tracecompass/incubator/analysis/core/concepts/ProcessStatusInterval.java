/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.concepts;

import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * A segment representing the status of a process for a certain duration
 *
 * @author Geneviève Bastien
 */
public class ProcessStatusInterval implements ISegment {

    /**
     * Generated serial version
     */
    private static final long serialVersionUID = -4842178205345826699L;

    private final long fStartTime;
    private final long fEndTime;
    private final ProcessStatus fStatus;

    /**
     * Constructor
     *
     * @param start
     *            The start time of this interval
     * @param end
     *            The end time of this interval
     * @param status
     *            The status of this interval
     */
    public ProcessStatusInterval(long start, long end, ProcessStatus status) {
        fStartTime = start;
        fEndTime = end;
        fStatus = status;
    }

    @Override
    public long getStart() {
        return fStartTime;
    }

    @Override
    public long getEnd() {
        return fEndTime;
    }

    /**
     * Get the process status represented by this interval
     *
     * @return The status of this interval
     */
    public ProcessStatus getProcessStatus() {
        return fStatus;
    }

}
