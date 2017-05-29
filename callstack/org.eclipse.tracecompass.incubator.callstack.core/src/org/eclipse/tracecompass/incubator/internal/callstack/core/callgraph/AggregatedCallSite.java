/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICalledFunction;

import com.google.common.collect.ImmutableMap;

/**
 * @author Geneviève Bastien
 */
public abstract class AggregatedCallSite {

    private final Object fSymbol;
    private final Map<Object, AggregatedCallSite> fChildren = new HashMap<>();
    private final @Nullable AggregatedCallSite fParent = null;
    // private final AggregatedCalledFunctionStatistics fStatistics;

    public AggregatedCallSite(Object symbol) {
        fSymbol = symbol;
    }

    public abstract long getLength();

    public Object getSymbol() {
        return fSymbol;
    }

    protected @Nullable AggregatedCallSite getParent() {
        return fParent;
    }

    public Collection<AggregatedCallSite> getChildren() {
        return fChildren.values();
    }

    public void addChild(AggregatedCallSite child) {
        AggregatedCallSite callsite = fChildren.get(child.getSymbol());
        if (callsite == null) {
            fChildren.put(child.getSymbol(), child);
            return;
        }
        if (fChildren.containsKey(child.getSymbol())) {
            callsite.merge(child);
        }
    }

    public final void merge(AggregatedCallSite other) {
        if (!other.getSymbol().equals(getSymbol())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        mergeData(other);
        mergeChildren(other);
    }

    protected abstract void mergeData(AggregatedCallSite other);

    protected void mergeChildren(AggregatedCallSite other) {
        for (AggregatedCallSite otherChildSite : other.fChildren.values()) {
            Object childSymbol = otherChildSite.getSymbol();
            AggregatedCallSite childSite = fChildren.get(childSymbol);
            if (childSite == null) {
                fChildren.put(childSymbol, otherChildSite);
            } else {
                // combine children
                childSite.merge(otherChildSite);
            }
        }
    }

    /**
     * Get the maximum depth under and including this aggregated callsite. A
     * depth of 1 means there is one element under and including this element.
     *
     * @return The maximum depth under and including this aggregated call site.
     *         The minimal value for the depth is 1.
     */
    public int getMaxDepth() {
        int maxDepth = 0;
        for (AggregatedCallSite callsite : fChildren.values()) {
            maxDepth = Math.max(maxDepth, callsite.getMaxDepth());
        }
        return maxDepth + 1;
    }

    /**
     * @return
     */
    public Map<String, IStatistics<ICalledFunction>> getStatistics() {
        return ImmutableMap.of();
    }

}
