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

import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.base.IDataPalette;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeSet;

/**
 * DifferentialCallGraphProvider provides a callgraphProvider. It is inherited
 * from DifferentialWeightedTreeProvider to edit the naming and color.
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 */
public class DifferentialCallGraphProvider extends DifferentialWeightedTreeProvider<ICallStackSymbol> {

    /**
     * Constructor
     *
     * @param instrumentedCallStackAnalyses
     *            the original tree
     * @param trees
     *            the other trees to compare to
     */
    public DifferentialCallGraphProvider(Collection<ICallGraphProvider2> instrumentedCallStackAnalyses,
            Collection<DifferentialWeightedTree<ICallStackSymbol>> trees) {
        this(instrumentedCallStackAnalyses, DifferentialWeightedTreeSet.<ICallStackSymbol> create(trees));
    }

    /**
     * Constructor
     *
     * @param instrumentedCallStackAnalyses
     *            The original tree provider, used to get information for texts
     *            and metrics.
     * @param treeSet
     *            The differential tree set
     */
    public DifferentialCallGraphProvider(
            Collection<ICallGraphProvider2> instrumentedCallStackAnalyses,
            DifferentialWeightedTreeSet<ICallStackSymbol> treeSet) {
        super(instrumentedCallStackAnalyses, treeSet);
    }

    @Override
    public IDataPalette getPalette() {
        return DifferentialFlamePalette.getInstance();
    }

    @Override
    public String toDisplayString(DifferentialWeightedTree<ICallStackSymbol> tree) {
        double difference = tree.getDifference();
        WeightedTree<ICallStackSymbol> originalTree = tree.getOriginalTree();
        String label = ""; //$NON-NLS-1$
        if (originalTree instanceof AggregatedCallSite) {
            for (ICallGraphProvider2 provider : fOriginalTrees) {
                label = provider.toDisplayString((AggregatedCallSite) originalTree);
                if (!label.startsWith("0x")) { //$NON-NLS-1$
                    break;
                }
            }
        } else {
            label = String.valueOf(originalTree.getObject().resolve(new ArrayList<>()));
        }
        if (Double.isFinite(difference)) {
            return String.format("(%#+.02f %% ) %s", difference * 100, label); //$NON-NLS-1$
        }
        return label;
    }

}
