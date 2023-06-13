/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.dependency;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.AbstractGpuEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.ApiEventHandler;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HostThreadIdentifier;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old.HostThreadIdentifier.KERNEL_CATEGORY;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmStrings;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * As the dependencies relies on specific information depending on the types of
 * event that are available, one class is implemented for each use case.
 *
 * In this case this dependency maker works in the case where both the HIP API
 * events and the HIP activity events are available.
 *
 * @author Arnaud Fiorini
 */
public class HipApiHipActivityDependencyMaker extends AbstractDependencyMaker {

    private final Map<Long, ITmfEvent> fApiEventCorrelationMap = new HashMap<>();
    private List<ITmfEvent> fWaitEventPerThread = new LinkedList<>();
    private final Queue<Pair<ITmfEvent, ITmfEvent>> fInFlightEvents = new PriorityQueue<>(new EventComparator());

    /**
     * This event comparator will sort pairs on API events and their
     * corresponding activity event. This is useful to derive forward
     * dependencies in the correct order.
     *
     * @author Arnaud Fiorini
     */
    public static class EventComparator implements Comparator<Pair<ITmfEvent, ITmfEvent>> {
        @Override
        public int compare(Pair<ITmfEvent, ITmfEvent> pair1, Pair<ITmfEvent, ITmfEvent> pair2) {
            Long endTime1 = AbstractGpuEventHandler.getEndTime(pair1.getSecond());
            Long endTime2 = AbstractGpuEventHandler.getEndTime(pair2.getSecond());
            if (endTime1 == null || endTime2 == null) {
                return -1;
            }
            return (int) (endTime1 - endTime2);
        }
    }

    @Override
    public void processEvent(ITmfEvent event, ITmfStateSystemBuilder ssb) {
        // Remove events that ended before this event
        removeInFlightEvents(event);
        // The eventName is the API function name where the event type name is
        // the API name (HIP/HSA)
        String eventName;
        switch (event.getName()) {
        case RocmStrings.HIP_API:
            eventName = ApiEventHandler.getFunctionApiName(event);
            // GPU Kernel dispatch
            if (eventName.equals(RocmStrings.KERNEL_LAUNCH)) {
                addGpuActivityDispatch(event, 7);
            }
            // Memory Copy dispatch
            else if (eventName.equals("hipMemcpy")) { //$NON-NLS-1$
                addGpuActivityDispatch(event, 4);
            }
            // Wait Api Events
            else if (eventName.equals(RocmStrings.HIP_DEVICE_SYNCHRONIZE)) {
                addWaitDependencies(event, ssb);
            }
            break;
        case RocmStrings.HIP_ACTIVITY:
            eventName = event.getContent().getFieldValue(String.class, RocmStrings.NAME);
            if (eventName != null && eventName.equals("KernelExecution")) { //$NON-NLS-1$
                addKernelDependency(event, ssb);
            } else if (eventName != null && eventName.startsWith("Copy")) { //$NON-NLS-1$
                addMemoryDependency(event, ssb);
            }
            Long correlationId = event.getContent().getFieldValue(Long.class, RocmStrings.CORRELATION_ID);
            fApiEventCorrelationMap.remove(correlationId);
            break;
        default:
        }
    }

    private void addGpuActivityDispatch(ITmfEvent event, int argPosition) {
        Long correlationId = Long.parseLong(ApiEventHandler.getArg(event.getContent(), argPosition));
        fApiEventCorrelationMap.put(correlationId, event);
    }

    private void addKernelDependency(ITmfEvent hipActivityEvent, ITmfStateSystemBuilder ssb) {
        // Add Kernel Launch Dependency
        Integer queueId = hipActivityEvent.getContent().getFieldValue(Integer.class, RocmStrings.QUEUE_ID);
        Long correlationId = hipActivityEvent.getContent().getFieldValue(Long.class, RocmStrings.CORRELATION_ID);
        Long gpuId = hipActivityEvent.getContent().getFieldValue(Long.class, RocmStrings.DEVICE_ID);
        if (correlationId != null && queueId != null && gpuId != null) {
            ITmfEvent hipEvent = fApiEventCorrelationMap.get(correlationId);
            if (hipEvent == null) {
                return; // no correlation
            }
            addInFlightEvent(hipEvent, hipActivityEvent, ssb);
            Integer tid = hipEvent.getContent().getFieldValue(Integer.class, RocmStrings.TID);
            if (tid == null) {
                return;
            }
            int hipStreamId = Integer.parseInt(ApiEventHandler.getArg(hipEvent.getContent(), 4));
            HostThreadIdentifier srcHostThreadIdentifier = new HostThreadIdentifier(hipEvent, tid);
            HostThreadIdentifier dstStreamHostThreadIdentifier = new HostThreadIdentifier(hipStreamId, KERNEL_CATEGORY.STREAM, gpuId.intValue());
            HostThreadIdentifier dstQueueHostThreadIdentifier = new HostThreadIdentifier(queueId.intValue(), KERNEL_CATEGORY.QUEUE, gpuId.intValue());
            // HostThreads
            HostThread src = new HostThread(hipEvent.getTrace().getHostId(), srcHostThreadIdentifier.hashCode());
            HostThread destStream = new HostThread(hipEvent.getTrace().getHostId(), dstStreamHostThreadIdentifier.hashCode());
            HostThread destQueue = new HostThread(hipEvent.getTrace().getHostId(), dstQueueHostThreadIdentifier.hashCode());
            // Arrows
            Long hipStreamEventEndTimestamp = AbstractGpuEventHandler.getEndTime(hipEvent);
            if (hipStreamEventEndTimestamp != null) {
                addArrow(ssb, hipStreamEventEndTimestamp - 1, hipActivityEvent.getTimestamp().getValue(),
                        Math.toIntExact(correlationId), src, destQueue);
                addArrow(ssb, hipStreamEventEndTimestamp - 1, hipActivityEvent.getTimestamp().getValue(),
                        Math.toIntExact(correlationId), src, destStream);
            }
        }
    }

    private void addMemoryDependency(ITmfEvent hipActivityEvent, ITmfStateSystemBuilder ssb) {
        Long correlationId = hipActivityEvent.getContent().getFieldValue(Long.class, RocmStrings.CORRELATION_ID);
        if (correlationId == null) {
            return;
        }
        ITmfEvent hipEvent = fApiEventCorrelationMap.get(correlationId);
        if (hipEvent == null) {
            return; // no correlation
        }
        addInFlightEvent(hipEvent, hipActivityEvent, ssb);
        Integer tid = hipEvent.getContent().getFieldValue(Integer.class, RocmStrings.TID);
        if (tid == null) {
            return;
        }
        HostThreadIdentifier srcHostThreadIdentifier = new HostThreadIdentifier(hipEvent, tid);
        HostThreadIdentifier dstHostThreadIdentifier = new HostThreadIdentifier();
        // HostThreads
        HostThread src = new HostThread(hipEvent.getTrace().getHostId(), srcHostThreadIdentifier.hashCode());
        HostThread dst = new HostThread(hipEvent.getTrace().getHostId(), dstHostThreadIdentifier.hashCode());
        // Arrow
        addArrow(ssb, hipEvent.getTimestamp().getValue(), hipActivityEvent.getTimestamp().getValue(),
                Math.toIntExact(correlationId), src, dst);
    }

    private void addInFlightEvent(ITmfEvent hipApiEvent, ITmfEvent hipActivityEvent, ITmfStateSystemBuilder ssb) {
        fInFlightEvents.add(new Pair<>(hipApiEvent, hipActivityEvent));

        Long beginTs = hipApiEvent.getTimestamp().getValue();
        Long endTs = AbstractGpuEventHandler.getEndTime(hipApiEvent);
        // Check all waiting events to see if we are already waiting for this
        // operation.
        Iterator<ITmfEvent> waitEventIterator = fWaitEventPerThread.iterator();
        while (waitEventIterator.hasNext()) {
            ITmfEvent waitEvent = waitEventIterator.next();
            Long dependencyBeginTs = waitEvent.getTimestamp().getValue();
            Long dependencyEndTs = AbstractGpuEventHandler.getEndTime(waitEvent);
            if (beginTs > dependencyEndTs) {
                // The wait event has finished without any other activity so
                // there can be no wait dependency beyond this point.
                waitEventIterator.remove();
                continue;
            }
            if (beginTs > dependencyBeginTs) {
                // The wait event had already begun before the operation was
                // queued.
                // Therefore there is no dependency.
                continue;
            }
            if (beginTs < dependencyBeginTs && endTs > dependencyBeginTs) {
                // AFAIK, this case should not be possible.
                Activator.getInstance().logError("If you see this message, the wait dependencies behavior should be changed."); //$NON-NLS-1$
            }
            addWaitDependencies(waitEvent, ssb);
        }
    }

    private void addWaitDependencies(ITmfEvent hipWaitEvent, ITmfStateSystemBuilder ssb) {
        String apiFunctionName = ApiEventHandler.getFunctionApiName(hipWaitEvent);

        Integer waitTid = hipWaitEvent.getContent().getFieldValue(Integer.class, RocmStrings.TID);
        if (waitTid == null) {
            return;
        }
        // Add the wait event for already queued operation that we cannot make
        // dependencies for yet.
        if (!fWaitEventPerThread.contains(hipWaitEvent)) {
            fWaitEventPerThread.add(hipWaitEvent);
        }
        if (apiFunctionName.equals(RocmStrings.HIP_DEVICE_SYNCHRONIZE)) {
            // Make arrows for current activity
            // TODO register deviceId per thread
            int waitingForDevice = 0;
            for (Pair<ITmfEvent, ITmfEvent> inFlightEventPair : fInFlightEvents) {
                Integer deviceId = inFlightEventPair.getSecond().getContent().getFieldValue(Integer.class, RocmStrings.DEVICE_ID);
                if (deviceId != null && deviceId == waitingForDevice) {
                    int hipStreamId = Integer.parseInt(ApiEventHandler.getArg(inFlightEventPair.getFirst().getContent(), 4));
                    addWaitArrow(ssb, inFlightEventPair.getSecond(), hipWaitEvent, waitTid, waitingForDevice, hipStreamId);
                }
            }
        }
    }

    private static void addWaitArrow(ITmfStateSystemBuilder ssb, ITmfEvent deviceEvent, ITmfEvent waitEvent, int waitTid, int deviceId, int hipStreamId) {
        Integer queueId = deviceEvent.getContent().getFieldValue(Integer.class, RocmStrings.QUEUE_ID);
        if (queueId == null) {
            return;
        }
        HostThreadIdentifier srcHostThreadIdentifier = new HostThreadIdentifier(waitEvent, waitTid);
        HostThreadIdentifier dstStreamHostThreadIdentifier = new HostThreadIdentifier(hipStreamId, KERNEL_CATEGORY.STREAM, deviceId);
        HostThreadIdentifier dstQueueHostThreadIdentifier = new HostThreadIdentifier(queueId, KERNEL_CATEGORY.QUEUE, deviceId);
        // HostThreads
        HostThread destThread = new HostThread(waitEvent.getTrace().getHostId(), srcHostThreadIdentifier.hashCode());
        HostThread srcStream = new HostThread(deviceEvent.getTrace().getHostId(), dstStreamHostThreadIdentifier.hashCode());
        HostThread srcQueue = new HostThread(deviceEvent.getTrace().getHostId(), dstQueueHostThreadIdentifier.hashCode());
        // Arrows
        Long hipStreamEventEndTimestamp = AbstractGpuEventHandler.getEndTime(waitEvent) - 1;
        Long correlationId = deviceEvent.getContent().getFieldValue(Long.class, RocmStrings.CORRELATION_ID);
        if (correlationId != null) {
            addArrow(ssb, AbstractGpuEventHandler.getEndTime(deviceEvent) - 1, hipStreamEventEndTimestamp,
                    Math.toIntExact(correlationId), srcStream, destThread);
            addArrow(ssb, AbstractGpuEventHandler.getEndTime(deviceEvent) - 1, hipStreamEventEndTimestamp,
                    Math.toIntExact(correlationId), srcQueue, destThread);
        }
    }

    private void removeInFlightEvents(ITmfEvent event) {
        Long currentTime = event.getTimestamp().getValue();
        Pair<ITmfEvent, ITmfEvent> inFlightEvent = fInFlightEvents.peek();
        while (inFlightEvent != null && (currentTime >= AbstractGpuEventHandler.getEndTime(inFlightEvent.getSecond()))) {
            fInFlightEvents.remove();
            inFlightEvent = fInFlightEvents.peek();
        }
    }

    @Override
    public Map<Long, ITmfEvent> getApiEventCorrelationMap() {
        return fApiEventCorrelationMap;
    }
}
