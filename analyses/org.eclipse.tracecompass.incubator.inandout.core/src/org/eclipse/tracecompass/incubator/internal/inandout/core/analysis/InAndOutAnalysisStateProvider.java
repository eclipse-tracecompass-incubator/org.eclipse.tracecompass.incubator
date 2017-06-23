/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.core.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.inandout.core.analysis.SegmentSpecifier.SegmentContext;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * In and Out analysis
 *
 * @author Matthew Khouzam
 */
public class InAndOutAnalysisStateProvider extends CallStackStateProvider {

    private List<@NonNull SegmentSpecifier> fList;
    private final Map<Object, Multimap<String, SegmentContext>> fTable = new HashMap<>();
    private SegmentContext fLast = null;
    private SegmentContext fFirst = null;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param list
     *            the list of segment contexts
     */
    public InAndOutAnalysisStateProvider(ITmfTrace trace, List<@NonNull SegmentSpecifier> list) {
        super(Objects.requireNonNull(trace));
        fList = list;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public CallStackStateProvider getNewInstance() {
        return new InAndOutAnalysisStateProvider(getTrace(), fList);
    }

    @Override
    protected boolean considerEvent(ITmfEvent event) {
        fFirst = null;
        fLast = null;
        for (SegmentSpecifier spec : fList) {
            SegmentContext context = spec.getSegmentContext(event);
            if (context != null) {
                Object classifier = context.getClassifier();
                if (classifier != null) {
                    Multimap<String, SegmentContext> map = fTable.computeIfAbsent(classifier, unused -> HashMultimap.create());
                    map.put(String.valueOf(context.getContext()), context);
                    fFirst = context;
                    return true;
                }
            }
            if (spec.matchesOutName(event)) {
                Object classifier = spec.getClassifier(event);
                if (classifier != null) {
                    Multimap<String, SegmentContext> contexts = fTable.get(classifier);
                    if (contexts != null && !contexts.isEmpty()) {
                        String outContext = spec.getOutContext(event);
                        Optional<SegmentContext> ctx = contexts.get(String.valueOf(outContext)).stream().findAny();
                        if (ctx.isPresent()) {
                            fLast = ctx.get();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected @Nullable String functionEntry(ITmfEvent event) {
        SegmentContext segmentContext = fFirst;
        if (segmentContext == null) {
            return null;
        }
        return segmentContext.getLabel();
    }

    @Override
    protected @Nullable String functionExit(ITmfEvent event) {
        if (fLast == null) {
            return null;
        }
        Multimap<String, SegmentContext> map = fTable.get(fLast.getClassifier());
        if (map != null) {
            Optional<SegmentContext> victim = map.get(fLast.getContext()).stream().findAny();
            if (victim.isPresent()) {
                map.remove(fLast.getContext(), victim.get());
            }
        }
        return fLast.getLabel();
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        int cls = classifierFrom(fFirst);
        if (cls > -1) {
            return cls;
        }
        cls = classifierFrom(fLast);
        if (cls > -1) {
            return cls;
        }
        return 0;
    }

    private static int classifierFrom(SegmentContext context) {
        Object clsPrime = null;
        if (context != null) {
            clsPrime = context.getClassifier();
        }
        if (clsPrime instanceof Number) {
            return ((Number) clsPrime).intValue();
        }
        if (clsPrime instanceof String) {
            try {
                return Integer.parseInt((String) clsPrime);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return -1;
    }

    @Override
    protected @Nullable String getProcessName(@NonNull ITmfEvent event) {
        if (fFirst != null) {
            return String.valueOf(fFirst.getClassifier());
        }
        if (fLast != null) {
            return String.valueOf(fLast.getClassifier());
        }
        return getThreadName(event);
    }

    @Override
    protected @Nullable String getThreadName(@NonNull ITmfEvent event) {
        if (fFirst != null) {
            return fFirst.getContext();
        }
        if (fLast != null) {
            return fLast.getContext();
        }
        return String.format("0x%08x", getThreadId(event)); //$NON-NLS-1$
    }

    /**
     * This is a best effort at the moment
     */
    @Override
    protected long getThreadId(ITmfEvent event) {
        try {
            if (fFirst != null) {
                return Integer.parseInt(fFirst.getContext());
            }
            if (fLast != null) {
                return Integer.parseInt(fLast.getContext());
            }
        } catch (NumberFormatException e) {
            // do nothing
        }
        return -1;
    }
}
