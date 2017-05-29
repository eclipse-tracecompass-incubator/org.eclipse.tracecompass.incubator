/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;

public class LeafGroupNode extends GroupNode {

    private final List<AggregatedCallSite> fCallSites = new ArrayList<>();

    public LeafGroupNode(String name, ICallStackGroupDescriptor descriptor) {
        super(name, descriptor);
    }

    @Override
    public @NonNull Collection<AggregatedCallSite> getAggregatedData() {
        return fCallSites;
    }

    public void addAggregatedData(AggregatedCallSite ags) {
        for (AggregatedCallSite site : fCallSites) {
            if (site.getSymbol().equals(ags.getSymbol())) {
                site.merge(ags);
                return;
            }
        }
        fCallSites.add(ags);
    }

    @Override
    public final boolean isLeaf() {
        return true;
    }
}
