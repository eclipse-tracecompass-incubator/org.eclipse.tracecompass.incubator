/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.atrace.event;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEventTypeFactory;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.IGenericFtraceConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfSourceLookup;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Systrace process dump event
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class SystraceProcessDumpEvent extends PlatformObject implements ITmfEvent, ITmfSourceLookup {


    private final @NonNull String fName;
    private final @Nullable ITmfCallsite fCallsite;

    private final ITmfTrace fTrace;
    private final long fRank;
    private final @NonNull ITmfTimestamp fTimestamp;
    private final ITmfEventType fType;
    private final ITmfEventField fContent;


    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param rank
     *            the rank
     * @param timestamp
     *             timestamp of the event
     * @param field
     *            the event field, contains all the needed data
     */
    public SystraceProcessDumpEvent(ITmfTrace trace,
            long rank,
            ITmfTimestamp timestamp,
            SystraceProcessDumpEventField field) {
        fTrace = trace;
        fRank = rank;
        if (timestamp != null) {
            fTimestamp = timestamp;
        } else {
            fTimestamp = TmfTimestamp.ZERO;
        }
        fType = GenericFtraceEventTypeFactory.get(field.getName());
        fContent = field;
        fName = IGenericFtraceConstants.PROCESS_DUMP_EVENT_NAME;
        fCallsite = null;
    }

    /**
     * Get the name of the event
     *
     * @return the name of the event
     */
    @Override
    public @NonNull String getName() {
        return fName;
    }

    @Override
    public @Nullable ITmfCallsite getCallsite() {
        return fCallsite;
    }

    // ------------------------------------------------------------------------
    // ITmfEvent
    // ------------------------------------------------------------------------

    @Override
    public ITmfTrace getTrace() {
        ITmfTrace trace = fTrace;
        if (trace == null) {
            throw new IllegalStateException("Null traces are only allowed on special kind of events and getTrace() should not be called on them"); //$NON-NLS-1$
        }
        return trace;
    }

    @Override
    public long getRank() {
        return fRank;
    }

    @Override
    public ITmfTimestamp getTimestamp() {
        return fTimestamp;
    }

    @Override
    public ITmfEventType getType() {
        return fType;
    }

    @Override
    public ITmfEventField getContent() {
        return fContent;
    }
}
