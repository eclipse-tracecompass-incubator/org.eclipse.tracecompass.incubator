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

package org.eclipse.tracecompass.incubator.scripting.core.callstack;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeGroupBy;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeUtils;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.core.flamegraph.FlameGraphDataProvider;
import org.eclipse.tracecompass.incubator.internal.scripting.core.data.provider.ScriptingDataProviderManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Provide an API to manipulate callstacks and weighted trees. This modules uses
 * objects of class {@link IWeightedTreeProvider} that can be obtained from a
 * trace by requesting an analysis that implements the interface.
 * <p>
 * For example, the following scriptlet would get the LTTng-UST CallStack
 * (Incubator) analysis, which implements the weighted tree provider, for the
 * active LTTng UST trace. The object can then be used as parameter for the
 * methods of this module.
 * </p>
 *
 * <pre>
 * trace = getActiveTrace()
 * wtProvider = getTraceAnalysis(trace, "org.eclipse.tracecompass.incubator.callstack.core.lttng.ust");
 * </pre>
 *
 * @author Geneviève Bastien
 */
public class CallStackScriptingModule {

    /**
     * Group the trees of the provider's main treeset, up to the requested
     * level. For example, if a treeset's elements are grouped in a hierarchical
     * manner with the following element hierarchy:
     *
     * <pre>
     * element 1
     *    {@literal -->} element 2
     *    {@literal -->} element 3
     * element 4
     *    {@literal -->} element 5
     * </pre>
     *
     * this method with a level of <code>1</code> would result in a treeset with
     * 2 elements: <code>element 1</code> and <code>element 4</code>, each
     * containing a merge of the weighted trees of their respective children
     * elements.
     *
     * @param <N>
     *            The type of objects represented by each node in the tree
     * @param <E>
     *            The type of elements used to group the trees
     * @param provider
     *            The {@link IWeightedTreeProvider} object containing the
     *            treeset for which to group trees.
     * @param level
     *            The hierarchical level after which the elements' trees will be
     *            merged together. A value of <code>0</code> would group all the
     *            weighted trees together in a single element.
     * @return The weighted tree set with weighted grouped at the requested
     *         level.
     */
    @WrapToScript
    public <@NonNull N, E> IWeightedTreeSet<N, ?, WeightedTree<N>> groupTreesBy(IWeightedTreeProvider<N, E, WeightedTree<N>> provider, int level) {
        IWeightedTreeSet<N, E, WeightedTree<N>> treeSet = provider.getTreeSet();
        IWeightedTreeGroupDescriptor groupDescriptor = getGroupDescriptor(provider, level);
        if (groupDescriptor != null) {
            return WeightedTreeGroupBy.groupWeightedTreeBy(groupDescriptor, treeSet, provider);
        }
        return treeSet;
    }

    private static <@NonNull N> @Nullable IWeightedTreeGroupDescriptor getGroupDescriptor(IWeightedTreeProvider<N, ?, WeightedTree<N>> provider, int level) {
        IWeightedTreeGroupDescriptor groupDescriptor = provider.getGroupDescriptor();
        if (level == 0) {
            return AllGroupDescriptor.getInstance();
        }
        int i = 1;
        while (groupDescriptor != null && i < level) {
            groupDescriptor = groupDescriptor.getNextGroup();
            i++;
        }
        return groupDescriptor;
    }

    /**
     * Differentiates 2 treesets
     *
     * @param provider
     *            The original weighted tree provider, whose values will be used
     *            for the metrics, palettes, etc
     * @param first
     *            The base treeset for comparison
     * @param second
     *            The treeset to compare to
     * @param minSignificantValue
     *            The value as a percentage (between 0 and 100), under which is
     *            difference should not be highlighted
     * @param maxSignificantValue
     *            The value as a percentage (between 0 and 100), above which the
     *            difference should be highlighted at the maximal value.
     * @param <N>
     *            The type of data that goes in the trees
     * @return The resulting differential weighted tree provider containing the
     *         result of the difference between the 2 trees
     */
    @WrapToScript
    public <@NonNull N> @Nullable DifferentialWeightedTreeProvider<N> diffTreeSets(IWeightedTreeProvider<N, ?, WeightedTree<N>> provider,
            IWeightedTreeSet<N, @NonNull ?, WeightedTree<N>> first,
            IWeightedTreeSet<N, @NonNull ?, WeightedTree<N>> second,
            @ScriptParameter(defaultValue = "-1") int minSignificantValue,
            @ScriptParameter(defaultValue = "-1") int maxSignificantValue) {
        DifferentialWeightedTreeProvider<@NonNull N> diffTrees = WeightedTreeUtils.diffTreeSets(provider, first, second);
        if (diffTrees != null && minSignificantValue >= 0) {
            diffTrees.setHeatThresholds(minSignificantValue, maxSignificantValue);
        }
        return diffTrees;
    }

    /**
     * Get a flame graph data provider for a weighted tree provider. The
     * weighted tree provider can be the result of the {@link #diffTreeSets}
     * method
     *
     * @param <N>
     *            The type of data that goes in the trees
     * @param <E>
     *            The type of elements used to group the trees
     * @param <T>
     *            The type of the tree provided
     * @param trace
     *            The trace for which to get the data provider. It is required
     *            for the data provider base class.
     * @param provider
     *            The weighted tree provider that will provide the data
     * @param id
     *            The ID of the data provider
     * @return The resulting flame graph data provider
     */
    @SuppressWarnings("restriction")
    @WrapToScript
    public <@NonNull N, E, @NonNull T extends WeightedTree<N>> FlameGraphDataProvider<N, E, T> getFlameGraphDataProvider(ITmfTrace trace, IWeightedTreeProvider<N, E, T> provider, String id) {
        FlameGraphDataProvider<@NonNull N, E, @NonNull T> dataProvider = new FlameGraphDataProvider<>(trace, provider, ScriptingDataProviderManager.PROVIDER_ID + ':' + id);
        ScriptingDataProviderManager.getInstance().registerDataProvider(trace, dataProvider);
        return dataProvider;
    }

}