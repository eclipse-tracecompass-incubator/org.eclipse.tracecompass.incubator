/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.event.matching.ITmfMatchEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching.Direction;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TmfEventTypeCollectionHelper;

/**
 * Class to match vmsync events between host and guest.
 * Uses vmsync_hg_host/guest and vmsync_gh_host/guest events
 * with cnt and vm_uid fields for synchronization
 *
 * @author Francois Belias
 */
public class VmSyncLttngEventMatching implements ITmfMatchEventDefinition {

    private static final Set<String> REQUIRED_EVENTS = new HashSet<>();
    static {
        REQUIRED_EVENTS.add("vmsync_hg_host"); //$NON-NLS-1$
        REQUIRED_EVENTS.add("vmsync_hg_guest"); //$NON-NLS-1$
        REQUIRED_EVENTS.add("vmsync_gh_host"); //$NON-NLS-1$
        REQUIRED_EVENTS.add("vmsync_gh_guest"); //$NON-NLS-1$
    }

    /** Use a weak hash map so that traces can be garbage collected */
    private static final Map<ITmfTrace, IKernelAnalysisEventLayout> TRACE_LAYOUTS = new WeakHashMap<>();

    @Override
    public boolean canMatchTrace(ITmfTrace trace) {
        // Get the events that this trace needs to have
        if (!(trace instanceof IKernelTrace)) {
            return false;
        }
        IKernelAnalysisEventLayout layout = ((IKernelTrace) trace).getKernelEventLayout();
        TRACE_LAYOUTS.put(trace, layout);

        if (!(trace instanceof ITmfTraceWithPreDefinedEvents)) {
            // No predefined events, suppose events are present
            return true;
        }
        ITmfTraceWithPreDefinedEvents ktrace = (ITmfTraceWithPreDefinedEvents) trace;

        Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(ktrace.getContainedEventTypes());
        traceEvents.retainAll(REQUIRED_EVENTS);
        return !traceEvents.isEmpty();
    }

    @Override
    public Direction getDirection(ITmfEvent event) {
        String evname = event.getName();

        // Host to Guest events
        if ("vmsync_hg_host".equals(evname)) { //$NON-NLS-1$
            return Direction.CAUSE;  // Host sends
        } else if ("vmsync_hg_guest".equals(evname)) { //$NON-NLS-1$
            return Direction.EFFECT; // Guest receives
        }
        // Guest to Host events
        else if ("vmsync_gh_guest".equals(evname)) { //$NON-NLS-1$
            return Direction.CAUSE;  // Guest sends
        } else if ("vmsync_gh_host".equals(evname)) { //$NON-NLS-1$
            return Direction.EFFECT; // Host receives
        }

        return null;
    }


    @Override
    public IEventMatchingKey getEventKey(ITmfEvent event) {
        String evname = event.getName();
        if (!REQUIRED_EVENTS.contains(evname)) {
            return null;
        }

        ITmfEventField content = event.getContent();

        Long cnt = content.getFieldValue(Long.class, "cnt"); //$NON-NLS-1$
        Long vmUid = content.getFieldValue(Long.class, "vm_uid"); //$NON-NLS-1$

        if (cnt == null || vmUid == null) {
            return null;
        }

        return new VmSyncEventKey(cnt, vmUid);
    }


}
