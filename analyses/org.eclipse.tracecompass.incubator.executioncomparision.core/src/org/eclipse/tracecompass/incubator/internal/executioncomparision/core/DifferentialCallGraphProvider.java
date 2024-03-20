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

import java.util.Collection;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeSet;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;

/**
 *
 * @author Fateme Faraji Daneshgar
 */
public class DifferentialCallGraphProvider extends DifferentialWeightedTreeProvider<ICallStackSymbol> {

    private final ICallGraphProvider fOriginalTree;

    /**
     * Constructor
     *
     * @param instrumentedCallStackAnalysis
     *            the original tree
     * @param trees
     *            the other trees to compare
     */
    public DifferentialCallGraphProvider(IWeightedTreeProvider<ICallStackSymbol, ICallStackElement, AggregatedCallSite> instrumentedCallStackAnalysis,
            Collection<DifferentialWeightedTree<ICallStackSymbol>> trees) {
        this(instrumentedCallStackAnalysis, DifferentialWeightedTreeSet.<ICallStackSymbol> create(trees));
    }

    /**
     * Constructor
     *
     * @param originalTree
     *            The original tree provider, used to get information for texts
     *            and metrics.
     * @param treeSet
     *            The differential tree set
     */
    @SuppressWarnings("unchecked")
    public DifferentialCallGraphProvider(
            IWeightedTreeProvider<ICallStackSymbol, ICallStackElement, ? extends WeightedTree<ICallStackSymbol>> originalTree,
            DifferentialWeightedTreeSet<ICallStackSymbol> treeSet) {
        super((IWeightedTreeProvider<ICallStackSymbol, ICallStackElement, WeightedTree<ICallStackSymbol>>)originalTree, treeSet);
        fOriginalTree = (ICallGraphProvider) originalTree;
    }

    @Override
    public IDataPalette getPalette() {
        return DifferentialFlamePalette.getInstance();
    }

    @SuppressWarnings("null")
    @Override
    public String toDisplayString(DifferentialWeightedTree<ICallStackSymbol> tree) {
        double difference = tree.getDifference();
        WeightedTree<ICallStackSymbol> originalTree = tree.getOriginalTree();
        String label = ""; //$NON-NLS-1$
        if (originalTree instanceof AggregatedCallSite) {
            label = fOriginalTree.toDisplayString((AggregatedCallSite) originalTree);
        }
        else {
            label = String.valueOf(originalTree.getObject().resolve(null));
        }
        if (Double.isFinite(difference)) {
            return String.format("(%#.02f %% ) %s", difference * 100, label); //$NON-NLS-1$
        }
        return label;

    }

}
