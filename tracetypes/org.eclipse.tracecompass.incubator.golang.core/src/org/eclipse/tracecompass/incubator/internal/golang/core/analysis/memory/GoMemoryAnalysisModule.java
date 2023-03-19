/**********************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.golang.core.analysis.memory;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;


/**
 * This analysis module creates a stateprovider that keeps track of the memory
 * allocated and deallocated by the kernel
 *
 * @author Samuel Gagnon
 * @since 2.0
 */
public class GoMemoryAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * Analysis ID, it should match that in the plugin.xml file
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.analysis.golang.memory.analysis"; //$NON-NLS-1$

    /**
     * Each thread attribute in the tree has an attribute for keeping the lowest memory
     * value for that thread during the trace. (Those values can be negative because we
     * don't have access to a memory dump before the trace)
     */
    public static final @NonNull String THREAD_LOWEST_MEMORY_VALUE = "lowestMemory"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new GoMemoryStateProvider(Objects.requireNonNull(getTrace()));
    }
}