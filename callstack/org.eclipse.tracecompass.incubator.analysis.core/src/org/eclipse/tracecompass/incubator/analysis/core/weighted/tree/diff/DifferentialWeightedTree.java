/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;

/**
 * A class that represents a differential weighted tree. The weight is the base
 * weight of one of the differentiated tree[set] and a differencial value is
 * used to represent how it differs with the weight value of another tree.
 *
 * @author Geneviève Bastien
 *
 * @param <T>
 *            The type of object this tree uses
 */
public class DifferentialWeightedTree<@NonNull T> extends WeightedTree<@NonNull T> {

    private final double fDifference;
    private final WeightedTree<@NonNull T> fOriginalTree;

    /**
     * Constructor
     *
     * @param originalTree
     *            The base tree from which this differential tree was computed.
     *            Used for additional metrics and texts.
     * @param object
     *            The object this tree is linked to
     * @param initialWeight
     *            The initial weight of this tree
     * @param diffWeight
     *            The differential weight with the base
     */
    public DifferentialWeightedTree(WeightedTree<T> originalTree, T object, long initialWeight, double diffWeight) {
        super(object, initialWeight);
        fDifference = diffWeight;
        fOriginalTree = originalTree;
    }

    /**
     * Get the differential value for this object. This value is the relative
     * difference between the value of a first tree and the second tree
     * compared. A positive value means the object's value is superior than the
     * previous value. Conversely a negative difference means the second value
     * is inferior to the initial value. 0 means there was no difference.
     *
     * It can also be {@link Double#NaN} if the value did not exist before
     *
     * @return The differential value
     */
    public double getDifference() {
        return fDifference;
    }

    /**
     * Get the base tree from which this differential weighted tree was computed
     *
     * @return The original base tree
     */
    public WeightedTree<@NonNull T> getOriginalTree() {
        return fOriginalTree;
    }

}
