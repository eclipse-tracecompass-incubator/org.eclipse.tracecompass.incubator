/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.lttng2.ust.extras.core.pthread;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.building.AbstractTraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.building.ITraceEventHandler;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.IOsExecutionGraphHandlerBuilder;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraphProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.legacy.OSEdgeContextState.OSEdgeContextEnum;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * An execution graph handler handling the userspace pthread spin lock events
 * and adding the proper links in the graph.
 *
 * @author Geneviève Bastien
 */
public class PThreadLockGraphHandler extends AbstractTraceEventHandler {

    private static final Pattern UNLOCK_EVENT = Pattern.compile("lttng_ust_pthread:pthread_.*_unlock"); //$NON-NLS-1$
    private static final Pattern ACQUIRE_LOCK_EVENT = Pattern.compile("lttng_ust_pthread:pthread_.*_lock_acq"); //$NON-NLS-1$
    private static final Pattern REQUEST_LOCK_EVENT = Pattern.compile("lttng_ust_pthread:pthread_.*_lock_req"); //$NON-NLS-1$
    private static final String MUTEX_FIELD = "mutex"; //$NON-NLS-1$

    private static class LastLockOwner {
        public final Integer fTid;
        public final ITmfVertex fVertex;

        /**
         * @param tid
         *            The ID of the thread who last released the lock
         * @param vertex
         *            The vertex at which the lock was removed by the worker
         */
        public LastLockOwner(Integer tid, ITmfVertex vertex) {
            fTid = tid;
            fVertex = vertex;
        }
    }

    private final OsExecutionGraphProvider fProvider;
    /** tid, mutex ID, vertex of the last lock request */
    private final Table<Integer, Long, ITmfVertex> fLastRequest;
    /** mutex ID, last lock owner */
    private final Map<Long, LastLockOwner> fLastLockOwner = new HashMap<>();

    /**
     * Constructor
     *
     * @param provider
     *            The graph provider
     * @param priority
     *            The priority of this handler
     */
    public PThreadLockGraphHandler(OsExecutionGraphProvider provider, int priority) {
        super(priority);
        fProvider = provider;
        fLastRequest = HashBasedTable.create();
    }

    /**
     * The handler builder for the event context handler
     */
    public static class HandlerBuilderPThreadLock implements IOsExecutionGraphHandlerBuilder {

        @Override
        public ITraceEventHandler createHandler(@NonNull OsExecutionGraphProvider provider, int priority) {
            return new PThreadLockGraphHandler(provider, priority);
        }

    }

    private OsWorker getOrCreateKernelWorker(ITmfEvent event, Integer tid) {
        HostThread ht = new HostThread(event.getTrace().getHostId(), tid);
        OsWorker worker = fProvider.getSystem().findWorker(ht, null);
        if (worker != null) {
            return worker;
        }
        worker = new OsWorker(ht, "kernel/" + tid, event.getTimestamp().getValue()); //$NON-NLS-1$
        worker.setStatus(ProcessStatus.RUN);
        fProvider.getSystem().addWorker(worker, null);
        return worker;
    }

    @Override
    public void handleEvent(ITmfEvent event) {
        String name = event.getName();
        if (UNLOCK_EVENT.matcher(name).matches()) {
            handleUnlockEvent(event);
        } else if (REQUEST_LOCK_EVENT.matcher(name).matches()) {
            handleRequestLockEvent(event);
        } else if (ACQUIRE_LOCK_EVENT.matcher(name).matches()) {
            handleAcquireLockEvent(event);
        }
    }

    private void handleAcquireLockEvent(ITmfEvent event) {
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            return;
        }
        Long fieldValue = event.getContent().getFieldValue(Long.class, MUTEX_FIELD);
        if (fieldValue == null) {
            return;
        }
        OsWorker worker = getOrCreateKernelWorker(event, tid);

        // Get the vertex for the last request
        ITmfVertex lastReqVertex = fLastRequest.get(tid, fieldValue);
        if (lastReqVertex == null) {
            return;
        }

        // Get the last lock owner
        LastLockOwner lastOwner = fLastLockOwner.get(fieldValue);
        if (lastOwner != null && lastOwner.fTid != tid && lastOwner.fVertex.getTimestamp() > lastReqVertex.getTimestamp()) {
            // This thread has been blocked, add the proper vertices and links
            ITmfGraph graph = Objects.requireNonNull(fProvider.getGraph());

            // First add a vertex at the time of lock request
            graph.append(lastReqVertex, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
            // Then add the blocked transition for the current worker
            ITmfVertex unblockVertex = graph.createVertex(worker, event.getTimestamp().toNanos());
            graph.append(unblockVertex, new OSEdgeContextState(OSEdgeContextEnum.BLOCKED));
            // And add the vertical link between the unlock and the acquisition
            // TODO: check if it's the correct replacement
//            lastOwner.fVertex.linkVertical(unblockVertex);
            graph.edgeVertical(lastOwner.fVertex, unblockVertex, new OSEdgeContextState(OSEdgeContextEnum.DEFAULT), MUTEX_FIELD);
        }
    }

    private void handleRequestLockEvent(ITmfEvent event) {
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            return;
        }
        Long fieldValue = event.getContent().getFieldValue(Long.class, MUTEX_FIELD);
        if (fieldValue == null) {
            return;
        }

        ITmfGraph graph = Objects.requireNonNull(fProvider.getGraph());
        OsWorker worker = getOrCreateKernelWorker(event, tid);

        // Don't add a state change to the worker just yet, let's keep the previous state until we know it's being blocked
        ITmfVertex vertex = graph.createVertex(worker, event.getTimestamp().toNanos());
        //TmfVertex stateChange = stateChange(worker, event.getTimestamp().toNanos(), EdgeType.RUNNING);
        fLastRequest.put(tid, fieldValue, vertex);
    }

    private void handleUnlockEvent(ITmfEvent event) {
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            return;
        }
        Long fieldValue = event.getContent().getFieldValue(Long.class, MUTEX_FIELD);
        if (fieldValue == null) {
            return;
        }
        OsWorker worker = getOrCreateKernelWorker(event, tid);
        // Set the previous state to running for the worker and add a vertex at this
        // event, so it can be used by any thread that was blocked
        ITmfGraph graph = Objects.requireNonNull(fProvider.getGraph());
        ITmfVertex vertex = graph.createVertex(worker, event.getTimestamp().toNanos());
        graph.append(vertex, new OSEdgeContextState(OSEdgeContextEnum.RUNNING));
        fLastLockOwner.put(fieldValue, new LastLockOwner(tid, vertex));
    }

}
