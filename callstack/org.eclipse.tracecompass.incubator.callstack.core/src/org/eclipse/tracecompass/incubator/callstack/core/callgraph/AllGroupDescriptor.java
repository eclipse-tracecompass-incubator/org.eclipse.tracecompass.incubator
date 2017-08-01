/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callgraph;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;

/**
 * A group descriptor represents all the groups together
 *
 * @author Geneviève Bastien
 */
public final class AllGroupDescriptor implements ICallStackGroupDescriptor {

    private static final String ALL_NAME = "all"; //$NON-NLS-1$

    private static final ICallStackGroupDescriptor INSTANCE = new AllGroupDescriptor();

    /**
     * Get the instance of the all group descriptor
     *
     * @return The instance of this group.
     */
    public static ICallStackGroupDescriptor getInstance() {
        return INSTANCE;
    }

    private AllGroupDescriptor() {

    }

    @Override
    public @Nullable ICallStackGroupDescriptor getNextGroup() {
        return null;
    }

    @Override
    public boolean isSymbolKeyGroup() {
        return false;
    }

    @Override
    public String getName() {
        return ALL_NAME;
    }

//    @Override
//    public List<ICallStackElement> getElements(@Nullable ICallStackElement parent, @Nullable ICallStackElement symbolKeyElement) {
//        return Collections.emptyList();
//    }

}
