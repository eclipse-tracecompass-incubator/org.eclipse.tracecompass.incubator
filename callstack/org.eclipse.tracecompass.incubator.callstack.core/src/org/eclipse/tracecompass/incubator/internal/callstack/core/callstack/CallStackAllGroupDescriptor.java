/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callstack;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.instrumented.Messages;

/**
 * Group descriptor that represents an aggregation of all the groups of a series
 *
 * @author Geneviève Bastien
 */
public class CallStackAllGroupDescriptor implements ICallStackGroupDescriptor {

    private CallStackSeries fSeries;

    /**
     * Constructor
     *
     * @param series
     *            The series this all group descriptor is for
     */
    public CallStackAllGroupDescriptor(CallStackSeries series) {
        fSeries = series;
    }

    @Override
    public @Nullable ICallStackGroupDescriptor getNextGroup() {
        return fSeries.getRootGroup();
    }

    @Override
    public @NonNull List<@NonNull ICallStackElement> getElements(@Nullable ICallStackElement parent, int baseQuark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdProvider) {
        return Collections.emptyList();
    }

    @Override
    public boolean isSymbolKeyGroup() {
        return false;
    }

    @Override
    public @NonNull String getName() {
        return String.valueOf(Messages.CallGraphAnalysis_AllDescriptors);
    }

}
