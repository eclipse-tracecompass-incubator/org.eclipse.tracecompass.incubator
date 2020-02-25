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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.internal.analysis.core.weighted.tree.DifferentialPalette;

/**
 * Weighted tree provider that provides a differential weighted tree. Since any
 * tree can be differentiated with other trees, this class is mostly a wrapper
 * around the original tree provider, only the elements and trees are specific
 * to this class.
 *
 * @author Geneviève Bastien
 * @param <N>
 *            The type of objects represented by each node in the tree
 */
public class DifferentialWeightedTreeProvider<@NonNull N> implements IWeightedTreeProvider<N, Object, DifferentialWeightedTree<N>> {

    private static final Format FORMAT = new DecimalFormat("#.#"); //$NON-NLS-1$

    private static final Format DIFFERENTIAL_FORMAT = new Format() {

        /**
         * UUID for this format
         */
        private static final long serialVersionUID = 9150811551603074986L;

        @Override
        public @Nullable StringBuffer format(@Nullable Object obj, @Nullable StringBuffer toAppendTo, @Nullable FieldPosition pos) {
            StringBuffer buf = toAppendTo;
            if (buf == null) {
                buf = new StringBuffer();
            }
            if (obj instanceof Number) {
                double num = ((Number) obj).doubleValue();
                if (num == 0.0) {
                    return buf.append("No Difference"); //$NON-NLS-1$
                }
                return buf.append((num > 0 ? "+" : "")).append(FORMAT.format(num * 100)).append("%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return FORMAT.format(obj, toAppendTo, pos);
        }

        @Override
        public @Nullable Object parseObject(@Nullable String source, @Nullable ParsePosition pos) {
            return null;
        }

    };

    private static final List<MetricType> WEIGHT_TYPES = Collections.singletonList(new MetricType("Differential", DataType.OTHER, DIFFERENTIAL_FORMAT)); //$NON-NLS-1$

    private final IWeightedTreeSet<N, Object, DifferentialWeightedTree<N>> fTreeSet;

    private final IWeightedTreeProvider<N, ?, WeightedTree<N>> fOriginalTree;
    private final List<MetricType> fAdditionalMetrics = new ArrayList<>(WEIGHT_TYPES);
    private @Nullable IDataPalette fPalette = null;

    /**
     * Constructor
     *
     * @param originalTree
     *            The original tree provider, used to get information for texts and metrics.
     * @param trees
     *            The differential tree
     */
    public DifferentialWeightedTreeProvider(IWeightedTreeProvider<N, ?, WeightedTree<N>> originalTree, Collection<DifferentialWeightedTree<N>> trees) {
        this(originalTree, DifferentialWeightedTreeSet.create(trees));
    }

    /**
     * Constructor
     *
     * @param originalTree
     *            The original tree provider, used to get information for texts and metrics.
     * @param treeSet
     *            The differential tree set
     */
    public DifferentialWeightedTreeProvider(IWeightedTreeProvider<N, ?, WeightedTree<N>> originalTree, DifferentialWeightedTreeSet<N> treeSet) {
        fOriginalTree = originalTree;
        fTreeSet = treeSet;
        fAdditionalMetrics.addAll(fOriginalTree.getAdditionalMetrics());
    }

    /**
     * Set the differential threshold for this provider, ie views will highlight
     * the gradual heat of the differential value when the value is between min
     * and max threshold values.
     *
     * If the 2 values are identical, the default palette will be used.
     *
     * @param minThreshold
     *            Minimal threshold (in %, typically between 0 and 100) of
     *            significance for the heat (absolute value). Any percentage
     *            below this value (whether positive or negative) will be
     *            considered as equal.
     * @param maxThreshold
     *            Maximal threshold (in %, typically between 0 and 100) of
     *            significance for the heat (absolute value). Any percentage
     *            above this value (whether positive or negative) will be
     *            considered at maximum heat.
     */
    public void setHeatThresholds(int minThreshold, int maxThreshold) {
        if (minThreshold == maxThreshold) {
            fPalette = DifferentialPalette.getInstance();
        }
        fPalette = DifferentialPalette.create(minThreshold, maxThreshold);
    }

    @Override
    public String getTitle() {
        return "Differential tree"; //$NON-NLS-1$
    }

    @Override
    public @NonNull MetricType getWeightType() {
        return fOriginalTree.getWeightType();
    }

    @Override
    public String toDisplayString(DifferentialWeightedTree<N> tree) {
        return fOriginalTree.toDisplayString(tree.getOriginalTree());
    }

    @Override
    public List<MetricType> getAdditionalMetrics() {
        return fAdditionalMetrics;
    }

    @Override
    public Object getAdditionalMetric(DifferentialWeightedTree<N> object, int metricIndex) {
        if (metricIndex == 0) {
            return object.getDifference();
        }
        return fOriginalTree.getAdditionalMetric(object.getOriginalTree(), metricIndex - 1);
    }

    @Override
    public IWeightedTreeSet<N, Object, DifferentialWeightedTree<N>> getTreeSet() {
        return fTreeSet;
    }

    @Override
    public IDataPalette getPalette() {
        IDataPalette palette = fPalette;
        return palette == null ? DifferentialPalette.getInstance() : palette;
    }

}
