/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;


/**
 * Utility class to operate on {@link WeightedTree} objects
 *
 * @author Fateme Faraji Daneshgar
 */
@SuppressWarnings("restriction")
public final class ParametricWeightedTreeUtils {

    /*
     * Constructor
     */
    private ParametricWeightedTreeUtils() {
        // Nothing to do
    }

    /**
     * Does the differential between 2 weighted trees, i.e what happened in
     * tree2 that is different compared to tree1. The base weight comes from the
     * second tree and the differential value will show the difference with the
     * first tree.
     *
     * @param <T>
     *            The type of element in the tree
     * @param firstTree
     *            The tree that will be differentiated.
     * @param secondTree
     *            The tree to use as the base
     * @param statisticType
     *            Determines the statistic (duration or self time) that the
     *            flame graph will represent
     * @return The differential weighted tree
     */
    public static <@NonNull T> Collection<DifferentialWeightedTree<T>> diffTrees(Collection<WeightedTree<T>> firstTree, Collection<WeightedTree<T>> secondTree, @Nullable String statisticType) {
        List<DifferentialWeightedTree<T>> diffTrees = new ArrayList<>();
        for (WeightedTree<T> base : secondTree) {
            T object = base.getObject();
            // Find the equivalent tree in the first collection
            WeightedTree<T> other = findObject(firstTree, object);
            DifferentialWeightedTree<T> diffTree = calculateDiffTree(object, base, other, statisticType);
            diffTrees.add(diffTree);

            // Make the differential of the children
            for (WeightedTree<T> childTree : diffTrees(other == null ? Collections.<WeightedTree<T>> emptyList() : other.getChildren(), base.getChildren(), statisticType)) {
                diffTree.addChild(childTree);
            }
        }
        return diffTrees;
    }

    private static <T> DifferentialWeightedTree<@NonNull T> calculateDiffTree(@NonNull T object, WeightedTree<@NonNull T> base, @Nullable WeightedTree<@NonNull T> other, @Nullable String statisticType) {
        double diffWeight;
        double nullDiff = Double.NaN;
        DifferentialWeightedTree<@NonNull T> diffTree;
        if (statisticType == null) {
            diffWeight = other == null ? nullDiff : (double) (base.getWeight() - other.getWeight()) / other.getWeight();
            diffTree = new DifferentialWeightedTree<>(base, object, base.getWeight(), diffWeight);

        } else {
            long baseWeight = 0;
            long otherWeight = 0;
            if (base instanceof AggregatedCalledFunction) {
                long[] weightsArray = calculateWeights(base, other, statisticType);
                baseWeight = weightsArray[0];
                otherWeight = weightsArray[1];
            } else {
                baseWeight = base.getWeight();
                otherWeight = other == null ? 0 : other.getWeight();
            }
            if (other == null) {
                diffWeight = nullDiff;
            } else {
                diffWeight = calculateDiffWeight(baseWeight, otherWeight);
            }
            diffTree = new DifferentialWeightedTree<>(base, object, base.getWeight(), diffWeight);

        }
        return diffTree;
    }

    private static double calculateDiffWeight(long baseWeight, long otherWeight) {
        double diffWeight;
        double nullDiff = Double.NaN;
        if (otherWeight != 0) {
            diffWeight = (double) (baseWeight - otherWeight) / otherWeight;
        } else {
            diffWeight = nullDiff;
        }
        return diffWeight;

    }

    private static <T> long[] calculateWeights(WeightedTree<@NonNull T> base, @Nullable WeightedTree<@NonNull T> other, String statisticType) {
        long baseWeight = 0;
        long otherWeight = 0;
        long[] weightsArray = new long[2];
        switch (statisticType) {
        case "Self Time": //$NON-NLS-1$
        {
            baseWeight = ((AggregatedCalledFunction) base).getSelfTime();
            otherWeight = other == null ? 0 : ((AggregatedCalledFunction) other).getSelfTime();
            break;
        }
        case "Duration": //$NON-NLS-1$
        {
            baseWeight = ((AggregatedCalledFunction) base).getWeight();
            otherWeight = other == null ? 0 : ((AggregatedCalledFunction) other).getWeight();
            break;
        }
        default:
            break;
        }
        weightsArray[0] = baseWeight;
        weightsArray[1] = otherWeight;
        return weightsArray;
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
