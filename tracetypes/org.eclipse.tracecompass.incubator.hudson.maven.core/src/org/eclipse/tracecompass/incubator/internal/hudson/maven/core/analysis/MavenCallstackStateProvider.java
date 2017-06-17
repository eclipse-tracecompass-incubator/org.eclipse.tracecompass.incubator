/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.hudson.maven.core.trace.MavenEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Maven callstack state provider
 *
 * @author Matthew Khouzam
 */
public class MavenCallstackStateProvider extends CallStackStateProvider {

    private static final double NANOS_IN_SEC = 1e9;

    long fSafeTime = 0;

    private long fNextClose = Long.MAX_VALUE;

    private int fDepth = 0;

    /**
     * Constructor with a trace
     *
     * @param trace
     *            the trace to analyze
     */
    public MavenCallstackStateProvider(@NonNull ITmfTrace trace) {
        super(trace);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public CallStackStateProvider getNewInstance() {
        return new MavenCallstackStateProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(ITmfEvent event) {
        return event instanceof MavenEvent;
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(ITmfEvent event) {
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(ITmfEvent event) {
        return null;
    }

    @Override
    protected int getProcessId(ITmfEvent event) {
        return 0;
    }

    @Override
    protected long getThreadId(ITmfEvent event) {
        return 0;
    }

    @Override
    protected @Nullable String getProcessName(ITmfEvent event) {
        return "CI"; //$NON-NLS-1$
    }

    @Override
    protected @Nullable String getThreadName(ITmfEvent event) {
        return "Maven Job"; //$NON-NLS-1$
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        int processQuark = ssb.getQuarkAbsoluteAndAdd(PROCESSES, getProcessName(event));
        int threadQuark = ssb.getQuarkRelativeAndAdd(processQuark, getThreadName(event));
        int callStackQuark = ssb.getQuarkRelativeAndAdd(threadQuark, CALL_STACK);

        TmfStateValue stateValue = TmfStateValue.newValueString(MavenEvent.GROUP_ASPECT.resolve(event));
        if (event.getType().equals(MavenEvent.GOAL_TYPE)) {
            fSafeTime = Math.max(fSafeTime + 1, event.getTimestamp().toNanos());
            if (fDepth == 2) {
                ssb.popAttribute(Math.min(fNextClose, fSafeTime), callStackQuark);
                fDepth--;
            }
            while (fDepth > 1) {
                fDepth--;
                ssb.popAttribute(fSafeTime, callStackQuark);
            }
            ssb.popAttribute(fSafeTime - 1, callStackQuark);
            ssb.pushAttribute(fSafeTime, stateValue, callStackQuark);
            fNextClose = Long.MAX_VALUE;
            fDepth = 1;
        } else if (event.getType().equals(MavenEvent.SUMMARY_TYPE)) {
            if (fNextClose != Long.MAX_VALUE) {
                ssb.popAttribute(fNextClose, callStackQuark);
            }
            fSafeTime = fNextClose != Long.MAX_VALUE ? fNextClose + 1 : fSafeTime;
            long duration = (long) Math.ceil(Objects.requireNonNull(MavenEvent.DURATION_ASPECT.resolve(event)) * NANOS_IN_SEC);
            ssb.pushAttribute(fSafeTime, stateValue, callStackQuark);
            fNextClose = fSafeTime + duration;
            fDepth = 2;
        } else if (event.getType().equals(MavenEvent.TEST_TYPE)) {
            if (fDepth == 1) {
                // attempt to re-create a group
                String fullGroup = Objects.requireNonNull(MavenEvent.FULL_GROUP_ASPECT.resolve(event));
                ssb.pushAttribute(fSafeTime, TmfStateValue.newValueString(fullGroup.substring(fullGroup.indexOf('(') + 1, fullGroup.length() - 1)), callStackQuark);
            }
            long testDuration = (long) Math.ceil(Objects.requireNonNull(MavenEvent.DURATION_ASPECT.resolve(event)) * NANOS_IN_SEC);
            fSafeTime++;
            ssb.pushAttribute(fSafeTime, stateValue, callStackQuark);
            fSafeTime += testDuration;
            ssb.popAttribute(fSafeTime, callStackQuark);
            fDepth = 2;
        }

    }
}
