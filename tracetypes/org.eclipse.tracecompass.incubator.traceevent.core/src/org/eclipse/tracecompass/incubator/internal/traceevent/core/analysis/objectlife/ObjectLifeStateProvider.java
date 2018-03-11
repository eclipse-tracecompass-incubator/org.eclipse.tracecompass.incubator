/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.objectlife;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventAspects;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventPhases;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Object life state provider
 *
 * @author Matthew Khouzam
 *
 */
public class ObjectLifeStateProvider extends AbstractTmfStateProvider {

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public ObjectLifeStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ObjectLifeAnalysis.ID);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ObjectLifeStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        String phase = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        if (phase == null) {
            return;
        }
        long t = event.getTimestamp().toNanos();
        int quark = ITmfStateSystem.INVALID_ATTRIBUTE;
        Object id = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TraceEventAspects.ID_ASPECT.getClass(), event);
        String name = String.valueOf(TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), "Name", event)); //$NON-NLS-1$
        if (id == null) {
            return;
        }
        switch (phase) {
        case TraceEventPhases.OBJECT_CREATED:
            quark = ss.getQuarkAbsoluteAndAdd(name, String.valueOf(id));
            ss.modifyAttribute(t, "", quark); //$NON-NLS-1$
            break;
        case TraceEventPhases.OBJECT_SNAPSHOT:
            quark = ss.getQuarkAbsoluteAndAdd(name, String.valueOf(id));
            ss.modifyAttribute(t, String.valueOf(TraceEventAspects.ARGS_ASPECT.resolve(event)), quark);
            break;
        case TraceEventPhases.OBJECT_DESTROYED:
            quark = ss.getQuarkAbsoluteAndAdd(name, String.valueOf(id));
            ss.modifyAttribute(t, (Object)null, quark);
            break;
        default:
            break;
        }
    }

}
