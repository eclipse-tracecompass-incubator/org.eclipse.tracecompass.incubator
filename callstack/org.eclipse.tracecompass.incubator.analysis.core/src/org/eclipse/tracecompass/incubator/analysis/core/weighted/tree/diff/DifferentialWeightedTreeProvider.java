/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;

/**
 * Weighted tree provider that provides a differential weighted tree. Since any
 * tree can be differentiated with other trees, this class is mostly a wrapper
 * around the original tree provider, only the elements and trees are specific
 * to this class.
 *
 * @author Geneviève Bastien
 */
public class DifferentialWeightedTreeProvider implements IWeightedTreeProvider<Object, String, DifferentialWeightedTree<Object>> {

    private static final String DEFAULT_ELEMENT = "diff"; //$NON-NLS-1$
    private static final Format FORMAT = new DecimalFormat("#.#"); //$NON-NLS-1$

    private static final Format DIFFERENTIAL_FORMAT = new Format() {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

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

    private class DifferentialWeightedTreeSet implements IWeightedTreeSet<Object, String, DifferentialWeightedTree<Object>> {

        private final Collection<DifferentialWeightedTree<Object>> fTrees;

        DifferentialWeightedTreeSet(Collection<DifferentialWeightedTree<Object>> trees) {
            fTrees = trees;
        }

        @Override
        public Collection<DifferentialWeightedTree<Object>> getTreesFor(String element) {
            if (element.equals(DEFAULT_ELEMENT)) {
                return fTrees;
            }
            return Collections.emptyList();
        }

        @Override
        public Collection<String> getElements() {
            return Collections.singleton(DEFAULT_ELEMENT);
        }
    }

    private static final List<MetricType> WEIGHT_TYPES = Collections.singletonList(new MetricType("Differential", DataType.OTHER, DIFFERENTIAL_FORMAT)); //$NON-NLS-1$

    private final IWeightedTreeSet<Object, String, DifferentialWeightedTree<Object>> fTreeSet;

    private final IWeightedTreeProvider<Object, ?, WeightedTree<Object>> fOriginalTree;

    /**
     * Constructor
     *
     * @param originalTree
     *            The original tree provider, used to get information for texts and metrics.
     * @param trees
     *            The differential tree
     */
    public DifferentialWeightedTreeProvider(IWeightedTreeProvider<Object, ?, WeightedTree<Object>> originalTree, Collection<DifferentialWeightedTree<Object>> trees) {
        fTreeSet = new DifferentialWeightedTreeSet(trees);
        fOriginalTree = originalTree;
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
    public String toDisplayString(DifferentialWeightedTree<Object> tree) {
        return fOriginalTree.toDisplayString(tree.getOriginalTree());
    }

    @Override
    public List<MetricType> getAdditionalMetrics() {
        return WEIGHT_TYPES;
    }

    @Override
    public Object getAdditionalMetric(DifferentialWeightedTree<Object> object, int metricIndex) {
        if (metricIndex == 0) {
            return object.getDifference();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public IWeightedTreeSet<Object, String, DifferentialWeightedTree<Object>> getTreeSet() {
        return fTreeSet;
    }

}
