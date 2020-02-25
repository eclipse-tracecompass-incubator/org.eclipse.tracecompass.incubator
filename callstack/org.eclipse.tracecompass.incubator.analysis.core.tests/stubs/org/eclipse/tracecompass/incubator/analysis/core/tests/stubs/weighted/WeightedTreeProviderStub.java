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

package org.eclipse.tracecompass.incubator.analysis.core.tests.stubs.weighted;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTreeSet;

/**
 * A weighted tree provider stub, without pre-defined data
 *
 * @author Geneviève Bastien
 *
 * @param <N> The type of data in the nodes
 * @param <E> The type of elements
 */
public class WeightedTreeProviderStub<@NonNull N, @NonNull E> implements IWeightedTreeProvider<N, E, WeightedTree<N>> {

    @Override
    public IWeightedTreeSet<@NonNull N, E, WeightedTree<N>> getTreeSet() {
        return new WeightedTreeSet<>();
    }

    @Override
    public String getTitle() {
        return "Empty weighted tree provider stub for unit tests";
    }

}
