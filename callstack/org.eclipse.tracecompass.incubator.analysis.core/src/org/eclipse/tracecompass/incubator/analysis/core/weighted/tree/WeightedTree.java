/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.weighted.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A Weighted Tree class to describe hierarchical data with a weight. This class
 * is a concrete class to describe a simple weighted tree, but it is also meant
 * to be extended to support other metrics associated with each tree, apart from
 * the weight.
 *
 * Note that the weight is such that the sum of the weight of the children is
 * smaller or equal to the weight of the parent. Failure to comply to this will
 * result in undefined behaviors when viewing the results.
 *
 * Also, if a child is added to the weighted tree for an object that is already
 * present in the children of this tree, their data will be merged.
 *
 * @author Geneviève Bastien
 * @param <T>
 *            The type of objects in this tree
 */
public class WeightedTree<@NonNull T> implements Comparable<WeightedTree<T>> {

    private final T fObject;
    private final Map<Object, WeightedTree<T>> fChildren = new HashMap<>();
    private @Nullable WeightedTree<T> fParent;
    private long fWeight = 0;

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol of the call site. It can eventually be resolved to
     *            a string using the symbol providers
     */
    public WeightedTree(T symbol) {
        this(symbol, 0);
    }

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol of the call site. It can eventually be resolved to
     *            a string using the symbol providers
     * @param initialWeight
     *            The initial length of this object
     */
    public WeightedTree(T symbol, long initialWeight) {
        fObject = symbol;
        fParent = null;
        fWeight = initialWeight;
    }

    /**
     * Copy constructor
     *
     * @param copy
     *            The call site to copy
     */
    protected WeightedTree(WeightedTree<T> copy) {
        fObject = copy.fObject;
        for (WeightedTree<T> entry : copy.fChildren.values()) {
            fChildren.put(entry.getObject(), entry.copyOf());
        }
        fParent = copy.fParent;
        fWeight = copy.fWeight;
    }

    /**
     * Get the aggregated value of this callsite. The units of this length will
     * depend on the time of callstack. Typically, for sampled, it will be the
     * number of times this symbol was hit, while for instrumented, it can be
     * the total time spent in this callstack element
     *
     * @return The aggregated value of this callsite
     */
    public long getWeight() {
        return fWeight;
    }

    /**
     * Make a copy of this callsite, with its statistics. Implementing classes
     * should make sure they copy all fields of the callsite, including the
     * statistics.
     *
     * @return A copy of this aggregated call site
     */
    public WeightedTree<T> copyOf() {
        return new WeightedTree<>(this);
    }

    /**
     * Get the symbol associated with this callsite
     *
     * @return The symbol for this callsite
     */
    public T getObject() {
        return fObject;
    }

    /**
     * Get the caller of this callsite (parent)
     *
     * @return The caller of this callsite
     */
    protected @Nullable WeightedTree<T> getParent() {
        return fParent;
    }

    /**
     * Sets the parent of this tree
     *
     * @param parent
     *            The parent tree
     */
    protected void setParent(WeightedTree<T> parent) {
        fParent = parent;
    }

    /**
     * Get the callees of this callsite, ie the functions called by this one
     *
     * @return A collection of callees' callsites
     */
    public Collection<WeightedTree<T>> getChildren() {
        return fChildren.values();
    }

    /**
     * Add value to the length of this callsite
     *
     * @param weight
     *            the amount to add to the length
     */
    public void addToWeight(long weight) {
        fWeight += weight;
    }

    /**
     * Add a callee to this callsite. If a callee for the same object already
     * exists, the data for both callees will be merged.
     *
     * @param callee
     *            the call site of the callee
     */
    public void addChild(WeightedTree<T> callee) {
        WeightedTree<T> callsite = fChildren.get(callee.getObject());
        if (callsite == null) {
            callee.setParent(this);
            fChildren.put(callee.getObject(), callee);
            return;
        }
        callsite.merge(callee);
    }

    /**
     * Merge a callsite's data with this one. This method will modify the
     * current callsite.
     *
     * It will first call {@link #mergeData(WeightedTree)} that needs to be
     * implemented for each implementation of this class.
     *
     * It will then merge the callees of both callsites by adding the other's
     * callees to this one.
     *
     * @param other
     *            The call site to merge. It has to have the same symbol as the
     *            current callsite otherwise it will throw an
     *            {@link IllegalArgumentException}
     */
    public final void merge(WeightedTree<T> other) {
        if (!other.getObject().equals(getObject())) {
            throw new IllegalArgumentException("AggregatedStackTraces: trying to merge stack traces of different symbols"); //$NON-NLS-1$
        }
        fWeight += other.fWeight;
        mergeData(other);
        mergeChildren(other);
    }

    /**
     * Merge the data of two callsites. This should modify the current
     * callsite's specific data. It is called by {@link #merge(WeightedTree)}
     * and this method MUST NOT touch the callees of the callsites.
     *
     * @param other
     *            The call site to merge to this one
     */
    protected void mergeData(WeightedTree<T> other) {
        // Nothing to do in main class
    }

    /**
     * Merge the children callsites
     *
     * @param other
     *            The call site to merge to this one
     */
    private void mergeChildren(WeightedTree<T> other) {
        for (WeightedTree<T> otherChildSite : other.fChildren.values()) {
            T childSymbol = otherChildSite.getObject();
            WeightedTree<T> childSite = fChildren.get(childSymbol);
            if (childSite == null) {
                fChildren.put(childSymbol, otherChildSite.copyOf());
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
        for (WeightedTree<T> child : getChildren()) {
            maxDepth = Math.max(maxDepth, child.getMaxDepth());
        }
        return maxDepth + 1;
    }

    /**
     * Get other children of this tree that are not its direct descendants. It
     * can be used for instance to represent extra data, for example kernel
     * statuses for a callstack.
     * <p>
     * A weighted tree provider will advertise those potential children data
     * that come with this tree, and consumers can then call this method with
     * the index of this extra type, if the tree has more than one extra data
     * set
     *
     * @param index
     *            The index of this extra children set, as provided by the
     *            {@link IWeightedTreeProvider#getExtraDataSets()} method.
     *
     * @return The extra children sites
     */
    public Collection<WeightedTree<@NonNull T>> getExtraDataTrees(int index) {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "WeightedTreeNode: " + fObject; //$NON-NLS-1$
    }

    @Override
    public int compareTo(WeightedTree<@NonNull T> o) {
        return Long.compare(fWeight, o.fWeight);
    }

}
