/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.weighted.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;

/**
 * Utility methods to operate on {@link WeightedTree} objects
 *
 * @author Geneviève Bastien
 */
public final class WeightedTreeUtils {

    private WeightedTreeUtils() {
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
     * @return The differential weighted tree
     */
    public static <@NonNull T> Collection<DifferentialWeightedTree<T>> diffTrees(Collection<WeightedTree<T>> first, Collection<WeightedTree<T>> second) {
        List<DifferentialWeightedTree<T>> diffTrees = new ArrayList<>();
        for (WeightedTree<T> base : second) {
            T object = base.getObject();
            // Find the equivalent tree in the first collection
            WeightedTree<T> other = findObject(first, object);
            double diffWeight = other == null ? Double.NaN : (double) (base.getWeight() - other.getWeight()) / other.getWeight();
            DifferentialWeightedTree<@NonNull T> diffTree = new DifferentialWeightedTree<>(base, object, base.getWeight(), diffWeight);
            diffTrees.add(diffTree);

            // Make the differential of the children
            for (DifferentialWeightedTree<T> childTree : diffTrees(other == null ? Collections.emptyList() : other.getChildren(), base.getChildren())) {
                diffTree.addChild(childTree);
            }
        }
        return diffTrees;
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
