/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callstack;

import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;

/**
 * Interface that can be implemented by components who provide call stacks as
 * part of their data.
 *
 * @author Geneviève Bastien
 */
public interface ICallStackProvider extends IAnalysisModule {

    /**
     * Get all the callstacks provided by this analysis.
     *
     * @return A collection of call stacks
     */
    Collection<CallStackSeries> getCallStackSeries();

    /**
     * Get the ID of the host this callstack provider is for
     *
     * @return The ID of the host
     */
    String getHostId();
}
