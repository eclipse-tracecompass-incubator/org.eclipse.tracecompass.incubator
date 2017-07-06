/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.contextswitch.ui.view;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Context switch entry, has the mean of the trace and an ID to lookup
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class ContextSwitchEntry extends TimeGraphEntry {

    private static final int NO_ID = -666;

    private final int fId;
    private final ITmfTrace fTrace;

    private double fMean;

    /**
     * Constructor with no id
     *
     * @param trace
     *            the trace
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     */
    public ContextSwitchEntry(ITmfTrace trace, long startTime, long endTime) {
        this(trace, String.valueOf(trace.getName()), startTime, endTime, NO_ID);
    }

    /**
     * Constructor with id
     *
     * @param trace
     *            the trace
     * @param name
     *            the name of the entry
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param id
     *            the id of the entry (typically a CPU number)
     */
    public ContextSwitchEntry(ITmfTrace trace, String name, long startTime, long endTime, int id) {
        super(name, startTime, endTime);
        fId = id;
        fTrace = trace;
    }

    /**
     * Get the ID
     *
     * @return the ID of the entry
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the trace of the entry
     *
     * @return the trace associated with the entry
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Has an ID been set?
     *
     * @return has the ID been set?
     */
    public boolean hasId() {
        return fId != NO_ID;
    }

    /**
     * Get the mean value
     *
     * @return the mean value
     */
    public double getMean() {
        return fMean;
    }

    void setMean(double mean) {
        fMean = mean;
        for (TimeGraphEntry child : getChildren()) {
            if (child instanceof ContextSwitchEntry) {
                ((ContextSwitchEntry) child).setMean(mean);
            }
        }

    }

}
