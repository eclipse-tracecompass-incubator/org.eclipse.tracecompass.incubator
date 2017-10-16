/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.concepts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;

import com.google.common.collect.ImmutableMap;

/**
 * Base class for aggregating call site data from either sampled or instrumented
 * call stacks.
 *
 * @author Geneviève Bastien
 */
public abstract class AggregatedCallSite {

    private final ICallStackSymbol fSymbol;
    private final Map<Object, AggregatedCallSite> fCallees = new HashMap<>();
    private final @Nullable AggregatedCallSite fCaller;

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol of the call site. It can eventually be resolved to
     *            a string using the symbol providers
     */
    public AggregatedCallSite(ICallStackSymbol symbol) {
        fSymbol = symbol;
        fCaller = null;
    }

    /**
     * Copy constructor
     *
     * @param copy
     *            The call site to copy
     */
    protected AggregatedCallSite(AggregatedCallSite copy) {
        fSymbol = copy.fSymbol;
        for (Entry<Object, AggregatedCallSite> entry : copy.fCallees.entrySet()) {
            fCallees.put(entry.getKey(), entry.getValue().copyOf());
        }
        fCaller = copy.fCaller;
    }

    /**
     * Get the aggregated value of this callsite. The units of this length will
     * depend on the time of callstack. Typically, for sampled, it will be the
     * number of times this symbol was hit, while for instrumented, it can be
     * the total time spent in this callstack element
     *
     * @return The aggregated value of this callsite
     */
    public abstract long getLength();

    /**
     * Make a copy of this callsite, with its statistics. Implementing classes
     * should make sure they copy all fields of the callsite, including the
     * statistics.
     *
     * @return A copy of this aggregated call site
     */
    public abstract AggregatedCallSite copyOf();

    /**
     * Get the symbol associated with this callsite
     *
     * @return The symbol for this callsite
     */
    public ICallStackSymbol getSymbol() {
        return fSymbol;
    }

    /**
     * Get the caller of this callsite (parent)
     *
     * @return The caller of this callsite
     */
    protected @Nullable AggregatedCallSite getCaller() {
        return fCaller;
    }

    /**
     * Get the callees of this callsite, ie the functions called by this one
     *
     * @return A collection of callees' callsites
     */
    public Collection<AggregatedCallSite> getCallees() {
        return fCallees.values();
    }

    /**
     * Add a callee to this callsite
     *
     * @param callee
     *            the call site of the callee
     */
    public void addCallee(AggregatedCallSite callee) {
        AggregatedCallSite callsite = fCallees.get(callee.getSymbol());
        if (callsite == null) {
            fCallees.put(callee.getSymbol(), callee);
            return;
        }
        callsite.merge(callee);
    }

    /**
     * Merge a callsite's data with this one. This method will modify the
     * current callsite.
     *
     * It will first call {@link #mergeData(AggregatedCallSite)} that needs to
     * be implemented for each implementation of this class.
     *
     * It will then merge the callees of both callsites by adding the other's
     * callees to this one.
     *
     * @param other
     *            The call site to merge. It has to have the same symbol as the
     *            current callsite otherwise it will throw an
     *            {@link IllegalArgumentException}
     */
    public final void merge(AggregatedCallSite other) {
        if (!other.getSymbol().equals(getSymbol())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        mergeData(other);
        mergeCallees(other);
    }

    /**
     * Merge the data of two callsites. This should modify the current
     * callsite's specific data. It is called by
     * {@link #merge(AggregatedCallSite)} and this method MUST NOT touch the
     * callees of the callsites.
     *
     * @param other
     *            The call site to merge to this one
     */
    protected abstract void mergeData(AggregatedCallSite other);

    /**
     * Merge the children callsites
     *
     * @param other
     *            The call site to merge to this one
     */
    private void mergeCallees(AggregatedCallSite other) {
        for (AggregatedCallSite otherChildSite : other.fCallees.values()) {
            Object childSymbol = otherChildSite.getSymbol();
            AggregatedCallSite childSite = fCallees.get(childSymbol);
            if (childSite == null) {
                fCallees.put(childSymbol, otherChildSite.copyOf());
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
        for (AggregatedCallSite callsite : fCallees.values()) {
            maxDepth = Math.max(maxDepth, callsite.getMaxDepth());
        }
        return maxDepth + 1;
    }

    /**
     * Get additional statistics for this call site
     *
     * @return A map of statistics title with statistics
     */
    public Map<String, IStatistics<?>> getStatistics() {
        return ImmutableMap.of();
    }

    @Override
    public String toString() {
        return "CallSite: " + fSymbol; //$NON-NLS-1$
    }

    /**
     * Get extra children sites that come with this callsite. For instance, an
     * instrumented callsite could return the kernel processes
     *
     * @return The extra children sites
     */
    public Iterable<AggregatedCallSite> getExtraChildrenSites() {
        return Collections.emptyList();
    }

}
