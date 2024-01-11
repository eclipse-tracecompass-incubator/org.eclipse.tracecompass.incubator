/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.WeightedTreeUtils;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * Utility methods to operate on {@link WeightedTree} objects
 *
 * @author Fateme Faraji Daneshgar
 */
@SuppressWarnings("restriction")
public final class ParametricWeightedTreeUtils {

    private ParametricWeightedTreeUtils() {
        // Nothing to do
    }

    /**
     * Does the differential between 2 weighted trees, ie what happened in tree2
     * differently than in tree1. The base weight come from the second tree and
     * the differential value will show the difference with the first tree.
     *
     * @param <T>
     *            The type of element in the tree
     * @param first
     *            The tree that will be differentiated.
     * @param second
     *            The tree to use as the base
     * @param statisticType
     *            it determine the statistic (duration or self time) that flame graph will represent
     * @return The differential weighted tree
     */
    public static <@NonNull T> Collection<DifferentialWeightedTree<T>> diffTrees(Collection<WeightedTree<T>> first, Collection<WeightedTree<T>> second, @Nullable String statisticType) {
        List<DifferentialWeightedTree<T>> diffTrees = new ArrayList<>();
        double nullDiff = Double.NaN;

        for (WeightedTree<T> base : second) {
            T object = base.getObject();
            // Find the equivalent tree in the first collection
            WeightedTree<T> other = findObject(first, object);
            double diffWeight;
            DifferentialWeightedTree<@NonNull T> diffTree;
            if (statisticType == null) {
                diffWeight = other == null ? nullDiff : (double) (base.getWeight() - other.getWeight()) / other.getWeight();
                diffTree = new DifferentialWeightedTree<>(base, object, base.getWeight(), diffWeight);

            } else {
                long baseWeight;
                long otherWeight;
                if (base instanceof AggregatedCalledFunction) {
                    baseWeight = ((AggregatedCalledFunction) base).getWeight(statisticType);
                    otherWeight = other == null ? 0 : ((AggregatedCalledFunction) other).getWeight(statisticType);

                } else {
                    baseWeight = base.getWeight();
                    otherWeight = other == null ? 0 : other.getWeight();
                }
                diffWeight = other == null ? nullDiff : (double) (baseWeight - otherWeight) / otherWeight;
                diffTree = new DifferentialWeightedTree<>(base, object, base.getWeight(), diffWeight);

            }
            diffTrees.add(diffTree);

            // Make the differential of the children
            for (WeightedTree<T> childTree : diffTrees(other == null ? Collections.emptyList() : other.getChildren(), base.getChildren(), statisticType)) {
                diffTree.addChild(childTree);
            }
        }
        return diffTrees;
    }

    /**
     * Does the differential between 2 weighted tree sets, ie for each
     * comparable elements, what happened in tree set 2 differently than in tree
     * set 1. The base weight come from the second tree set and the differential
     * value will show the difference with the first tree.
     * <p>
     * Calling this method assumes the tree sets are comparable. It is the
     * caller's responsibility to make sure the parameters make sense. If the 2
     * tree sets come from different {@link IWeightedTreeProvider}, they should
     * be of similar types, otherwise, the comparison may not make sense.
     * </p>
     * <p>
     * The elements of each tree set are paired as follows:
     * </p>
     * <p>
     * 1- If there is only one element in each tree, they will be paired.
     * </p>
     * <p>
     * 2- If the same elements are present in both tree sets, they will be
     * paired, all other elements are ignored
     * </p>
     * <p>
     * 3- If no elements are paired, they will be paired by name (and
     * hierarchical names, if it applies). Unmatched elements will be ignored.
     * </p>
     * <p>
     * If elements are not paired at this point, it will return
     * <code>null</code>
     * </p>
     *
     * @param provider
     *            The base provider of one of the trees, it will be used by the
     *            differential weighted tree provider to display the metrics and
     *            titles, etc.. It could be the provider of the second tree set,
     *            as it serves as the base values.
     * @param first
     *            The first treeset to compare to
     * @param second
     *            The second treeset to compare.
     * @return A differential weighted tree provider wrapping the resulting tree
     *         set, or <code>null</code> if the 2 treesets have no elements in
     *         common
     */
    public static @Nullable DifferentialWeightedTreeProvider<ICallStackSymbol> diffTreeSets(ICallGraphProvider2 provider,
            IWeightedTreeSet<ICallStackSymbol, @NonNull ?, WeightedTree<ICallStackSymbol>> first,
            IWeightedTreeSet<ICallStackSymbol, @NonNull ?, WeightedTree<ICallStackSymbol>> second) {
        Collection<Pair<@NonNull ?, @NonNull ?>> pairedElements = pairElementsFromTrees(first, second);
        if (pairedElements.isEmpty()) {
            return null;
        }
        DifferentialWeightedTreeSet<ICallStackSymbol> treeSet = new DifferentialWeightedTreeSet<>();
        for (Pair<@NonNull ?, @NonNull ?> pair : pairedElements) {
            Collection<WeightedTree<ICallStackSymbol>> trees1 = first.getTreesFor(pair.getFirst());
            Collection<WeightedTree<ICallStackSymbol>> trees2 = second.getTreesFor(pair.getSecond());
            Collection<DifferentialWeightedTree<ICallStackSymbol>> diffTrees = WeightedTreeUtils.diffTrees(trees1, trees2);
            for (DifferentialWeightedTree<ICallStackSymbol> tree : diffTrees) {
                treeSet.addWeightedTree(pair.getFirst(), tree);
            }
        }

        return new DifferentialWeightedTreeProvider(provider, treeSet);
    }

    private static <@NonNull N> Collection<Pair<@NonNull ?, @NonNull ?>> pairElementsFromTrees(IWeightedTreeSet<N, @NonNull ?, WeightedTree<N>> first, IWeightedTreeSet<N, @NonNull ?, WeightedTree<N>> second) {
        Collection<@NonNull ?> elements1 = first.getElements();
        Collection<@NonNull ?> elements2 = second.getElements();
        // If there is only one element and it is not a tree, pair it
        if (elements1.size() == 1 && elements2.size() == 1) {
            @NonNull
            Object element1 = elements1.iterator().next();
            @NonNull
            Object element2 = elements2.iterator().next();
            if (!(element1 instanceof ITree) && !(element2 instanceof ITree)) {
                return ImmutableList.of(new Pair(element1, element2));
            }
        }

        // Try to find equal elements in both trees
        Collection<Pair<@NonNull ?, @NonNull ?>> pairedElements = pairEqualElements(elements1, elements2);
        if (!pairedElements.isEmpty()) {
            return pairedElements;
        }

        // Compare ITree elements by names
        pairedElements = pairSameNameElements(elements1, elements2);
        return pairedElements;
    }

    private static Collection<Pair<@NonNull ?, @NonNull ?>> pairEqualElements(Collection<@NonNull ?> elements1, Collection<@NonNull ?> elements2) {
        List<Pair<@NonNull ?, @NonNull ?>> pairedElements = new ArrayList<>();
        for (@NonNull
        Object element1 : elements1) {
            for (@NonNull
            Object element2 : elements2) {
                if (element1.equals(element2)) {
                    pairedElements.add(new Pair<>(element1, element1));
                    if (element1 instanceof ITree && element2 instanceof ITree) {
                        pairedElements.addAll(pairEqualElements(((ITree) element1).getChildren(), ((ITree) element2).getChildren()));
                    }
                    break;
                }
            }
        }
        return pairedElements;
    }

    private static Collection<Pair<@NonNull ?, @NonNull ?>> pairSameNameElements(Collection<@NonNull ?> elements1, Collection<?> elements2) {
        List<Pair<@NonNull ?, @NonNull ?>> pairedElements = new ArrayList<>();
        for (@NonNull
        Object element1 : elements1) {
            if (!(element1 instanceof ITree)) {
                continue;
            }
            for (@NonNull
            Object element2 : elements2) {
                if (!(element2 instanceof ITree)) {
                    continue;
                }
                if (((ITree) element1).getName().equals(((ITree) element2).getName())) {
                    pairedElements.add(new Pair<>(element1, element2));
                    pairedElements.addAll(pairSameNameElements(((ITree) element1).getChildren(), ((ITree) element2).getChildren()));
                    break;
                }
            }
        }
        return pairedElements;
    }

    private static @Nullable <@NonNull T> WeightedTree<T> findObject(Collection<WeightedTree<T>> tree, @NonNull T object) {
        for (WeightedTree<T> other : tree) {
            if (other.getObject().equals(object)) {
                return other;
            }
        }
        return null;
    }

}
