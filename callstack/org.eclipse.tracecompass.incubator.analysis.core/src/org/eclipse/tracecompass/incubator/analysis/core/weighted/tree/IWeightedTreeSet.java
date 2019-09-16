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
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A structure that keeps elements and their weighted trees together for a given
 * time range or grouping. It is the class that contains the actual data that
 * the {@link IWeightedTreeProvider} provides.
 *
 * The tree set contains the data, but a {@link IWeightedTreeProvider} object
 * is necessary to describe it, its format, their names, etc.
 *
 * @author Geneviève Bastien
 *
 * @param <N>
 *            The type of objects represented by each node in the tree
 * @param <E>
 *            The type of elements used to group the trees
 * @param <T>
 *            The type of the tree provided
 */
public interface IWeightedTreeSet<@NonNull N, E, @NonNull T extends WeightedTree<N>> {

    /**
     * Get the elements under which are the weighted trees. It can be a single
     * constant element if this treeset does not have the concept of grouping
     * the trees.
     *
     * @return The elements used to group the trees
     */
    Collection<E> getElements();

    /**
     * Get the weighted trees for a given element
     *
     * @param element
     *            The element for which to get the trees
     * @return A collection of weighted trees for the requested element
     */
    Collection<T> getTreesFor(E element);

    /**
     * Return a list of additional data sets' titles. These sets will be
     * available by calling {@link WeightedTree#getExtraDataTrees(int)} on the
     * trees, where the index in the list is the parameter that the children set
     * should match
     *
     * @return The title of each child set
     */
    default List<String> getExtraDataSets() {
        return Collections.emptyList();
    }
}
