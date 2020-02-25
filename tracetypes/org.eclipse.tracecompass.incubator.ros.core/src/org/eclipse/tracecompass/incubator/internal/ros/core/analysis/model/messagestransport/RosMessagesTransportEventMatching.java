/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.ConnectionEndpoint;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.TcpEventStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.event.matching.TcpLttngEventMatching;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TmfEventTypeCollectionHelper;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventField;

/**
 * Class to match TCP events with useful ROS connection context
 *
 * FIXME code duplication with TcpLttngEventMatching
 *
 * @author Christophe Bedard
 */
public class RosMessagesTransportEventMatching extends TcpLttngEventMatching {

    // FIXME get base path from layout instead
    private static final @NonNull String[] FIELD_PATH_TO_SADDR = { "network_header", CtfTmfEventField.FIELD_VARIANT_SELECTED, TcpEventStrings.SADDR }; //$NON-NLS-1$
    private static final @NonNull String[] FIELD_PATH_TO_DADDR = { "network_header", CtfTmfEventField.FIELD_VARIANT_SELECTED, TcpEventStrings.DADDR }; //$NON-NLS-1$
    private static final @NonNull String[] FIELD_PATH_TO_SOURCE_PORT = { "network_header", CtfTmfEventField.FIELD_VARIANT_SELECTED, "transport_header", "tcp", "source_port" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final @NonNull String[] FIELD_PATH_TO_DEST_PORT = { "network_header", CtfTmfEventField.FIELD_VARIANT_SELECTED, "transport_header", "tcp", "dest_port" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static final Map<IKernelAnalysisEventLayout, Set<String>> REQUIRED_EVENTS = new HashMap<>();

    /** Use a weak hash map so that traces can be garbage collected */
    private static final Map<ITmfTrace, IKernelAnalysisEventLayout> TRACE_LAYOUTS = new WeakHashMap<>();

    @Override
    public boolean canMatchTrace(ITmfTrace trace) {
        // Get the events that this trace needs to have
        if (!(trace instanceof IKernelTrace)) {
            // Not a kernel trace, we cannot know what events to use, return
            // false
            return false;
        }
        IKernelAnalysisEventLayout layout = ((IKernelTrace) trace).getKernelEventLayout();
        TRACE_LAYOUTS.put(trace, layout);

        Set<String> events = REQUIRED_EVENTS.get(layout);
        if (events == null) {
            events = new HashSet<>();
            events.addAll(layout.eventsNetworkSend());
            events.addAll(layout.eventsNetworkReceive());
            REQUIRED_EVENTS.put(layout, events);
        }

        if (!(trace instanceof ITmfTraceWithPreDefinedEvents)) {
            // No predefined events, suppose events are present
            return true;
        }
        ITmfTraceWithPreDefinedEvents ktrace = (ITmfTraceWithPreDefinedEvents) trace;

        Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(ktrace.getContainedEventTypes());
        traceEvents.retainAll(events);
        return !traceEvents.isEmpty();
    }

    @Override
    public IEventMatchingKey getEventKey(ITmfEvent event) {
        IKernelAnalysisEventLayout layout = TRACE_LAYOUTS.get(event.getTrace());
        if (layout == null) {
            return null;
        }

        TmfEventField content = (TmfEventField) event.getContent();

        Long sequence = content.getFieldValue(Long.class, layout.fieldPathTcpSeq());
        Long ack = content.getFieldValue(Long.class, layout.fieldPathTcpAckSeq());
        Long flags = content.getFieldValue(Long.class, layout.fieldPathTcpFlags());

        long[] saddr = content.getFieldValue(long[].class, FIELD_PATH_TO_SADDR);
        Long sport = content.getFieldValue(Long.class, FIELD_PATH_TO_SOURCE_PORT);
        long[] daddr = content.getFieldValue(long[].class, FIELD_PATH_TO_DADDR);
        Long dport = content.getFieldValue(Long.class, FIELD_PATH_TO_DEST_PORT);

        if (sequence == null || ack == null || flags == null || saddr == null || sport == null || daddr == null || dport == null) {
            return null;
        }

        ConnectionEndpoint sourceEndpoint = new ConnectionEndpoint(saddr, sport);
        ConnectionEndpoint destinationEndpoint = new ConnectionEndpoint(daddr, dport);
        NetworkConnection connection = new NetworkConnection(sourceEndpoint, destinationEndpoint);
        return new RosMessagesTransportEventKey(sequence, ack, flags, connection);
    }
}
