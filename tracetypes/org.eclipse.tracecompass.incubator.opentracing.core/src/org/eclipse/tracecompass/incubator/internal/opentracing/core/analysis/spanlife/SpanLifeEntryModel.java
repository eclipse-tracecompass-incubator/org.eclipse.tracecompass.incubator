/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.List;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * {@link TimeGraphEntryModel} for the Span life data provider
 *
 * @author Katherine Nadeau
 *
 */
public class SpanLifeEntryModel extends TimeGraphEntryModel {

    private final List<Long> fLogs;

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param name
     *            Entry name to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param logs
     *            Span logs timestamps
     */
    public SpanLifeEntryModel(long id, long parentId, String name, long startTime, long endTime, List<Long> logs) {
        super(id, parentId, name, startTime, endTime);
        fLogs = logs;
    }

    /**
     * Getter for the logs
     *
     * @return the logs timestamps
     */
    public List<Long> getLogs() {
        return fLogs;
    }

}
