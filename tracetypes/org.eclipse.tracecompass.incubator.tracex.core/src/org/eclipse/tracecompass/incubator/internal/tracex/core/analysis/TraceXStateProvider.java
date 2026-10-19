/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXObjectType;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistry;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistryRecord;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/** Builds CPU, context, and nested-interrupt execution intervals. */
public final class TraceXStateProvider extends AbstractTmfStateProvider {

    /** State-system CPU root attribute. */
    public static final String CPU = "CPU 0"; //$NON-NLS-1$
    /** CPU execution attribute. */
    public static final String EXECUTION = "Execution"; //$NON-NLS-1$
    /** State-system context root attribute. */
    public static final String CONTEXTS = "Contexts"; //$NON-NLS-1$
    /** Hidden implementation attributes. */
    public static final String INTERNAL = "Internal"; //$NON-NLS-1$
    /** Diagnostic attribute root. */
    public static final String DIAGNOSTICS = "Diagnostics"; //$NON-NLS-1$
    /** Unmatched interrupt-exit diagnostic. */
    public static final String UNMATCHED_ISR_EXITS = "Unmatched ISR exits"; //$NON-NLS-1$

    static final int VERSION = 3;
    private static final int THREAD_RESUME = 1;
    private static final int THREAD_SUSPEND = 2;
    private static final int ISR_ENTER = 3;
    private static final int ISR_EXIT = 4;
    private static final int TIME_SLICE = 5;
    private static final int RUNNING = 6;
    private static final int THREAD_RELINQUISH = 109;

    private final TraceXRegistry fRegistry;
    private final TraceXKernelState fKernel;
    private final Deque<TraceXExecutionContext> fIsrStack = new ArrayDeque<>();
    private @Nullable TraceXExecutionContext fSchedulerContext;
    private @Nullable TraceXExecutionContext fActiveContext;
    private boolean fInitialized;
    private long fUnmatchedIsrExits;

    /**
     * Create a provider for a TraceX trace.
     *
     * @param trace
     *            initialized TraceX trace
     */
    public TraceXStateProvider(TraceXTrace trace) {
        super(trace, TraceXAnalysis.ID);
        fKernel = new TraceXKernelState(trace);
        try {
            fRegistry = trace.getRegistry();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the TraceX registry", e); //$NON-NLS-1$
        }
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new TraceXStateProvider((TraceXTrace) getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent tmfEvent) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        Object value = tmfEvent.getContent().getValue();
        if (ss == null || !(value instanceof TraceXEvent)) {
            return;
        }
        TraceXEvent event = (TraceXEvent) value;
        long timestamp = tmfEvent.getTimestamp().toNanos();
        if (!fInitialized) {
            initialize(ss, timestamp, event);
            fInitialized = true;
        }
        fKernel.observePriority(ss, timestamp, event.getContext());
        switch (event.getNormalizedEventId()) {
        case THREAD_RESUME:
        case THREAD_SUSPEND:
            selectScheduler(ss, timestamp, event.getInformationWord(4));
            break;
        case TIME_SLICE:
            selectScheduler(ss, timestamp, event.getInformationWord(1));
            break;
        case THREAD_RELINQUISH:
            selectScheduler(ss, timestamp, event.getInformationWord(2));
            break;
        case ISR_ENTER:
            enterIsr(ss, timestamp, event.getInformationWord(2));
            break;
        case ISR_EXIT:
            exitIsr(ss, timestamp);
            break;
        case RUNNING:
            establishRunning(ss, timestamp, event.getContext());
            break;
        default:
            break;
        }
    }

    private void initialize(ITmfStateSystemBuilder ss, long timestamp,
            TraceXEvent event) {
        createKnownContextAttributes(ss);
        fKernel.seed(ss, timestamp);
        TraceXContext context = event.getContext();
        if (context.getKind() == TraceXContext.Kind.ISR) {
            Long interrupted = context.getInterruptedThreadAddress();
            fSchedulerContext = interrupted == null
                    ? TraceXExecutionContext.unknown(-1, null)
                    : TraceXExecutionContext.scheduler(interrupted.longValue(), fRegistry);
            if (event.getNormalizedEventId() != ISR_ENTER) {
                TraceXExecutionContext unknownIsr = TraceXExecutionContext.isr(-1);
                fIsrStack.push(unknownIsr);
                fKernel.enterInterrupt(ss, timestamp, 0, -1);
                setStackSlot(ss, timestamp, 0, unknownIsr);
                switchActive(ss, timestamp, unknownIsr);
            } else {
                switchActive(ss, timestamp, fSchedulerContext);
            }
        } else {
            fSchedulerContext = TraceXExecutionContext.fromEventContext(context);
            switchActive(ss, timestamp, fSchedulerContext);
        }
        writeScheduler(ss, timestamp);
    }

    private void createKnownContextAttributes(ITmfStateSystemBuilder ss) {
        contextQuark(ss, TraceXExecutionContext.initialization());
        contextQuark(ss, TraceXExecutionContext.idle());
        for (TraceXRegistryRecord record : fRegistry.getRecords()) {
            if (record.isValid() && record.getType() == TraceXObjectType.THREAD) {
                contextQuark(ss, TraceXExecutionContext.thread(record.getAddress(),
                        record.getName()));
            }
        }
    }

    private void establishRunning(ITmfStateSystemBuilder ss, long timestamp,
            TraceXContext context) {
        if (context.getKind() == TraceXContext.Kind.ISR) {
            return;
        }
        fSchedulerContext = TraceXExecutionContext.fromEventContext(context);
        writeScheduler(ss, timestamp);
        if (fIsrStack.isEmpty()) {
            switchActive(ss, timestamp, fSchedulerContext);
        }
    }

    private void selectScheduler(ITmfStateSystemBuilder ss, long timestamp,
            long address) {
        fSchedulerContext = TraceXExecutionContext.scheduler(address, fRegistry);
        writeScheduler(ss, timestamp);
        if (fIsrStack.isEmpty()) {
            switchActive(ss, timestamp, fSchedulerContext);
        }
    }

    private void enterIsr(ITmfStateSystemBuilder ss, long timestamp,
            long number) {
        TraceXExecutionContext isr = TraceXExecutionContext.isr(number);
        int depth = fIsrStack.size();
        fIsrStack.push(isr);
        fKernel.enterInterrupt(ss, timestamp, depth, number);
        setStackSlot(ss, timestamp, depth, isr);
        switchActive(ss, timestamp, isr);
    }

    private void exitIsr(ITmfStateSystemBuilder ss, long timestamp) {
        TraceXExecutionContext top = fIsrStack.peek();
        if (top == null) {
            fUnmatchedIsrExits++;
            int diagnostic = ss.getQuarkAbsoluteAndAdd(DIAGNOSTICS,
                    UNMATCHED_ISR_EXITS);
            ss.modifyAttribute(timestamp, fUnmatchedIsrExits, diagnostic);
            return;
        }
        int depth = fIsrStack.size() - 1;
        fIsrStack.pop();
        fKernel.exitInterrupt(ss, timestamp, depth);
        setStackSlot(ss, timestamp, depth, null);
        TraceXExecutionContext restored = fIsrStack.peek();
        switchActive(ss, timestamp,
                restored == null ? fSchedulerContext : restored);
    }

    private static void setStackSlot(ITmfStateSystemBuilder ss, long timestamp,
            int depth, @Nullable TraceXExecutionContext context) {
        int quark = ss.getQuarkAbsoluteAndAdd(INTERNAL, "ISR stack", //$NON-NLS-1$
                Integer.toString(depth));
        ss.modifyAttribute(timestamp,
                context == null ? null : context.serialize(), quark);
    }

    private void writeScheduler(ITmfStateSystemBuilder ss, long timestamp) {
        TraceXExecutionContext scheduler = fSchedulerContext;
        int quark = ss.getQuarkAbsoluteAndAdd(INTERNAL, "Scheduler"); //$NON-NLS-1$
        ss.modifyAttribute(timestamp,
                scheduler == null ? null : scheduler.serialize(), quark);
    }

    private void switchActive(ITmfStateSystemBuilder ss, long timestamp,
            @Nullable TraceXExecutionContext next) {
        TraceXExecutionContext previous = fActiveContext;
        if (same(previous, next)) {
            return;
        }
        if (previous != null) {
            ss.modifyAttribute(timestamp, (Object) null, contextQuark(ss, previous));
        }
        int cpu = ss.getQuarkAbsoluteAndAdd(CPU, EXECUTION);
        if (next == null) {
            ss.modifyAttribute(timestamp, (Object) null, cpu);
        } else {
            String serialized = next.serialize();
            ss.modifyAttribute(timestamp, serialized, contextQuark(ss, next));
            ss.modifyAttribute(timestamp, serialized, cpu);
        }
        fKernel.switchActive(ss, timestamp, next, fSchedulerContext);
        fActiveContext = next;
    }

    private static boolean same(@Nullable TraceXExecutionContext left,
            @Nullable TraceXExecutionContext right) {
        return left == right || left != null && right != null
                && left.serialize().equals(right.serialize());
    }

    private static int contextQuark(ITmfStateSystemBuilder ss,
            TraceXExecutionContext context) {
        return ss.getQuarkAbsoluteAndAdd(CONTEXTS,
                context.getType().getLabel(), context.getEntryName());
    }
}
