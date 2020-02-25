/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.hudson.maven.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.hudson.maven.core.trace.MavenEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Maven callstack state provider
 *
 * @author Matthew Khouzam
 */
public class MavenCallstackStateProvider extends CallStackStateProvider {

    private static final double NANOS_IN_SEC = 1e9;
    private static final int GROUP_DEPTH = 2;
    private static final int FINAL_DEPTH = 3;
    private static final int VERSION = 2;

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
        return VERSION;
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
        int processQuark = ssb.optQuarkAbsolute(PROCESSES, getProcessName(event));
        boolean isFirst = processQuark == ITmfStateSystem.INVALID_ATTRIBUTE;
        if (isFirst) {
            processQuark = ssb.getQuarkAbsoluteAndAdd(PROCESSES, getProcessName(event));
        }
        int threadQuark = ssb.getQuarkRelativeAndAdd(processQuark, getThreadName(event));
        int callStackQuark = ssb.getQuarkRelativeAndAdd(threadQuark, CallStackAnalysis.CALL_STACK);

        String groupAspect = MavenEvent.GROUP_ASPECT.resolve(event);
        if (event.getType().equals(MavenEvent.GOAL_TYPE)) {
            fSafeTime = Math.max(fSafeTime + 1, event.getTimestamp().toNanos());
            if (fDepth == FINAL_DEPTH) {
                ssb.popAttribute(Math.min(fNextClose, fSafeTime), callStackQuark);
                fDepth--;
            }
            while (fDepth > GROUP_DEPTH) {
                fDepth--;
                ssb.popAttribute(fSafeTime, callStackQuark);
            }
            if (!isFirst) {
                ssb.popAttribute(fSafeTime - 1, callStackQuark);
            } else {
                ssb.modifyAttribute(fSafeTime - 1, Integer.valueOf(fDepth), callStackQuark);
            }
            // Change the element if necessary
            String element = MavenEvent.ELEMENT_ASPECT.resolve(event);
            updateStackTop(ssb, callStackQuark, String.valueOf(element));
            ssb.pushAttribute(fSafeTime, groupAspect, callStackQuark);
            fNextClose = Long.MAX_VALUE;
            fDepth = GROUP_DEPTH;
        } else if (event.getType().equals(MavenEvent.SUMMARY_TYPE)) {
            if (fNextClose != Long.MAX_VALUE) {
                while (fDepth > GROUP_DEPTH) {
                    fDepth--;
                    ssb.popAttribute(fNextClose, callStackQuark);
                }
            }
            fSafeTime = fNextClose != Long.MAX_VALUE ? fNextClose + 1 : fSafeTime;
            long duration = (long) Math.ceil(Objects.requireNonNull(MavenEvent.DURATION_ASPECT.resolve(event)) * NANOS_IN_SEC);
            ssb.pushAttribute(fSafeTime, groupAspect, callStackQuark);
            fNextClose = fSafeTime + duration;
            fDepth = FINAL_DEPTH;
        } else if (event.getType().equals(MavenEvent.TEST_TYPE)) {
            String fullGroup = Objects.requireNonNull(MavenEvent.FULL_GROUP_ASPECT.resolve(event));
            String group = fullGroup.substring(fullGroup.indexOf('(') + 1, fullGroup.length() - 1);
            if (fDepth == GROUP_DEPTH) {
                // Push the group for the first time
                ssb.pushAttribute(fSafeTime, group, callStackQuark);
                fDepth = FINAL_DEPTH;
            } else {
                String topGroup = peekStackTop(ssb, callStackQuark);
                if (!group.equals(topGroup)) {
                    // If the group of this test is not the same as the stack
                    // top, it means the test groups spans many test classes, so
                    // add a level
                    while (fDepth > GROUP_DEPTH + 1) {
                        fDepth--;
                        ssb.popAttribute(fSafeTime, callStackQuark);
                    }
                    ssb.pushAttribute(fSafeTime, group, callStackQuark);
                    fDepth = FINAL_DEPTH + 1;
                }
            }
            long testDuration = (long) Math.ceil(Objects.requireNonNull(MavenEvent.DURATION_ASPECT.resolve(event)) * NANOS_IN_SEC);
            fSafeTime++;
            ssb.pushAttribute(fSafeTime, groupAspect, callStackQuark);
            fSafeTime += testDuration;
            ssb.popAttribute(fSafeTime, callStackQuark);
        }

    }

    private void updateStackTop(ITmfStateSystemBuilder ssb, int callStackQuark, String value) {
        Object currentDepth = ssb.queryOngoing(callStackQuark);
        // If currentDepth returns null, we need to store stack level 1
        if (currentDepth == null) {
            currentDepth = 1;
        }
        int currentGroupQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(currentDepth));
        ssb.modifyAttribute(fSafeTime, value, currentGroupQuark);
    }

    private static @Nullable String peekStackTop(ITmfStateSystemBuilder ssb, int callStackQuark) {
        Object currentDepth = ssb.queryOngoing(callStackQuark);
        int currentGroupQuark = ssb.getQuarkRelativeAndAdd(callStackQuark, String.valueOf(currentDepth));
        Object currentTop = ssb.queryOngoing(currentGroupQuark);
        return (currentTop instanceof String) ? (String) currentTop : null;
    }

}
