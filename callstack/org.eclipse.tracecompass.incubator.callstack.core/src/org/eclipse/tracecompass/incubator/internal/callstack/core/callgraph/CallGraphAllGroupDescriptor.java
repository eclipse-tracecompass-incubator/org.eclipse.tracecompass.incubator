/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;

public final class CallGraphAllGroupDescriptor implements ICallStackGroupDescriptor {

    private static final ICallStackGroupDescriptor INSTANCE = new CallGraphAllGroupDescriptor();

    public static ICallStackGroupDescriptor getInstance() {
        return INSTANCE;
    }

    private CallGraphAllGroupDescriptor() {

    }

    @Override
    public @Nullable ICallStackGroupDescriptor getNextGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ICallStackElement> getElements(@Nullable ICallStackElement parent, int baseQuark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdProvider) {
        return Collections.emptyList();
    }

    @Override
    public boolean isSymbolKeyGroup() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public @NonNull String getName() {
        return "all";
    }

}
