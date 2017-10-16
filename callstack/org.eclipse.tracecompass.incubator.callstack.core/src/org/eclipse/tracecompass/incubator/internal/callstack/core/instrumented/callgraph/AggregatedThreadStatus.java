/*******************************************************************************
 * Copyright (c) 2016, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph;

import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.internal.callstack.core.symbol.StringSymbol;

/**
 * Class to calculate statistics for an aggregated function.
 *
 * @author Geneviève Bastien
 */
public class AggregatedThreadStatus extends AggregatedCallSite {

    private final ProcessStatus fStatus;
    private long fLength = 0;

    /**
     * Constructor
     * @param status
     */
    public AggregatedThreadStatus(ProcessStatus status) {
        super(new StringSymbol(status));
        fStatus = status;
    }

    public AggregatedThreadStatus(AggregatedThreadStatus status) {
        super(status.getSymbol());
        fStatus = status.fStatus;
        fLength = status.fLength;
    }

    public void update(ProcessStatusInterval interval) {
        fLength += interval.getLength();
    }

    public ProcessStatus getProcessStatus() {
        return fStatus;
    }

    @Override
    public String toString() {
        return "Aggregated Thread status for " + fStatus + ": " + fLength; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public long getLength() {
        return fLength;
    }

    @Override
    public AggregatedCallSite copyOf() {
        return new AggregatedThreadStatus(this);
    }

    @Override
    protected void mergeData(AggregatedCallSite other) {
        if (other instanceof AggregatedThreadStatus) {
            fLength += ((AggregatedThreadStatus) other).getLength();
        }
    }

}
