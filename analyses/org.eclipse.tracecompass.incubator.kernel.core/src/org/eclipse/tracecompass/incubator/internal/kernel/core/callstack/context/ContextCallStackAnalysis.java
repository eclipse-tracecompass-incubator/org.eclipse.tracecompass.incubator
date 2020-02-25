/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.callstack.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph.ProfilingCallGraphAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * This class analyzes the kernel and userspace callstack contexts of lttng
 * kernel traces
 *
 * @author Geneviève Bastien
 */
public class ContextCallStackAnalysis extends ProfilingCallGraphAnalysisModule {

    /**
     * ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.kernel.core.callstack.core.context"; //$NON-NLS-1$

    private static final long UNDEFINED_SYMBOL = -1L;

    private static final String KERNEL_CALLSTACK_FIELD = "context._callstack_kernel"; //$NON-NLS-1$
    private static final String USER_CALLSTACK_FIELD = "context._callstack_user"; //$NON-NLS-1$
    private static final String KERNEL_STACK_NAME = "kernel"; //$NON-NLS-1$
    private static final String USER_STACK_NAME = "user"; //$NON-NLS-1$

    private final CallStackGroupDescriptor fEventDescriptor;
    private final CallStackGroupDescriptor fProcessDescriptor;
    private final CallStackGroupDescriptor fThreadDescriptor;

    /**
     * Constructor
     */
    public ContextCallStackAnalysis() {
        // Create group descriptors
        fEventDescriptor = new CallStackGroupDescriptor(Objects.requireNonNull(Messages.ContextCallStackAnalysis_GroupEvents), null, false);
        fProcessDescriptor = new CallStackGroupDescriptor(Objects.requireNonNull(Messages.ContextCallStackAnalysis_GroupProcess), fEventDescriptor, false);
        fThreadDescriptor = new CallStackGroupDescriptor(Objects.requireNonNull(Messages.ContextCallStackAnalysis_GroupThread), fProcessDescriptor, false);
    }

    @Override
    protected @Nullable Pair<ICallStackElement, AggregatedCallSite> getProfiledStackTrace(@NonNull ITmfEvent event) {
        Map<String, Collection<Object>> callStack = getCallStack(event);
        if (callStack.isEmpty()) {
            return null;
        }
        ICallStackElement element = getElement(event);

        Collection<Object> kernelCs = callStack.get(KERNEL_STACK_NAME);
        if (kernelCs == null) {
            kernelCs = Collections.emptyList();
        }
        Collection<Object> userCs = callStack.get(USER_STACK_NAME);
        if (userCs == null) {
            userCs = Collections.emptyList();
        }
        if (kernelCs.size() + userCs.size() == 0) {
            long[] stack = new long[1];
            stack[0] = 0;
            return new Pair<>(element, getCallSite(element, stack, event.getTimestamp().getValue()));
        }
        long[] stack = new long[userCs.size() + kernelCs.size()];
        int i = 0;
        for (Object call : userCs) {
            stack[i] = (call instanceof Long) ? (long) call : UNDEFINED_SYMBOL;
            i++;
        }
        for (Object call : kernelCs) {
            stack[i] = (call instanceof Long) ? (long) call : UNDEFINED_SYMBOL;
            i++;
        }
        return new Pair<>(element, getCallSite(element, stack, event.getTimestamp().getValue()));

    }

    private ICallStackElement getElement(ITmfEvent event) {
        // Find a root elements with the same PID
        Collection<ICallStackElement> rootElements = getRootElements();
        String name = event.getName();
        Optional<ICallStackElement> events = rootElements.stream()
                .filter(e -> e.getName().equals(String.valueOf(name)))
                .findFirst();
        Integer threadId = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        int tid = (threadId == null) ? -1 : threadId;
        Integer pId = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxPidAspect.class, event);
        int pid = (pId == null) ? -1 : pId.intValue();
        if (events.isPresent()) {
            ICallStackElement eventEl = events.get();

            // Process exists, find a thread element under it or create it
            Optional<ICallStackElement> process = eventEl.getChildrenElements().stream()
                    .filter(e -> e.getName().equals(String.valueOf(pid)))
                    .findFirst();

            ICallStackElement processEl;
            if (process.isPresent()) {
                processEl = process.get();
            } else {
                processEl = new CallStackElement(String.valueOf(pid), fProcessDescriptor, fThreadDescriptor, eventEl) {

                    @Override
                    protected int retrieveSymbolKeyAt(long time) {
                        return pid;
                    }

                };
                processEl.setSymbolKeyElement(processEl);
                eventEl.addChild(processEl);
            }

            // Process exists, find a thread element under it or create it
            Optional<ICallStackElement> thread = processEl.getChildrenElements().stream()
                    .filter(e -> e.getName().equals(String.valueOf(tid)))
                    .findFirst();

            if (thread.isPresent()) {
                return thread.get();
            }
            ICallStackElement threadEl = new CallStackElement(String.valueOf(tid), fThreadDescriptor, null, processEl);
            processEl.addChild(threadEl);
            return threadEl;
        }
        ICallStackElement eventEl = new CallStackElement(name, fEventDescriptor, null, null);
        ICallStackElement processEl = new CallStackElement(String.valueOf(pid), fProcessDescriptor, null, eventEl);
        processEl.setSymbolKeyElement(processEl);
        ICallStackElement threadEl = new CallStackElement(String.valueOf(tid), fThreadDescriptor, null, processEl);
        addRootElement(eventEl);
        return threadEl;
    }

    /**
     * Get CPU
     *
     * @param event
     *            The event containing the cpu
     *
     * @return the CPU number (null for not set)
     */
    public static @Nullable Integer getCpu(ITmfEvent event) {
        Integer cpuObj = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpuObj == null) {
            /* We couldn't find any CPU information, ignore this event */
            return null;
        }
        return cpuObj;
    }

    @Override
    public Map<String, Collection<Object>> getCallStack(ITmfEvent event) {
        Map<String, Collection<Object>> map = new HashMap<>();
        ITmfEventField content = event.getContent();
        ITmfEventField field = content.getField(KERNEL_CALLSTACK_FIELD);
        if (field != null) {
            map.put(KERNEL_STACK_NAME, getCallstack(field));
        }
        field = content.getField(USER_CALLSTACK_FIELD);
        if (field != null) {
            map.put(USER_STACK_NAME, getCallstack(field));
        }
        return map;
    }

    private static Collection<Object> getCallstack(ITmfEventField field) {
        Object value = field.getValue();
        if (!(value instanceof long[])) {
            return Collections.emptyList();
        }
        long[] callstack = (long[]) value;
        List<Object> longList = new ArrayList<>();
        for (long callsite : callstack) {
            longList.add(callsite);
        }
        Collections.reverse(longList);
        return longList;

    }

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        return ImmutableList.of(fEventDescriptor);
    }

}
