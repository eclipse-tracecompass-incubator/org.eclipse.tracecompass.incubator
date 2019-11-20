/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.weighted.tree;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Group descriptor to represent all elements grouped together
 *
 * @author Geneviève Bastien
 */
public final class AllGroupDescriptor implements IWeightedTreeGroupDescriptor {

    private static final String ALL_NAME = "all"; //$NON-NLS-1$

    private static final IWeightedTreeGroupDescriptor INSTANCE = new AllGroupDescriptor();

    /**
     * Get the instance of the all group descriptor
     *
     * @return The instance of this group.
     */
    public static IWeightedTreeGroupDescriptor getInstance() {
        return INSTANCE;
    }

    private AllGroupDescriptor() {

    }

    @Override
    public @Nullable IWeightedTreeGroupDescriptor getNextGroup() {
        return null;
    }

    @Override
    public String getName() {
        return ALL_NAME;
    }

}
