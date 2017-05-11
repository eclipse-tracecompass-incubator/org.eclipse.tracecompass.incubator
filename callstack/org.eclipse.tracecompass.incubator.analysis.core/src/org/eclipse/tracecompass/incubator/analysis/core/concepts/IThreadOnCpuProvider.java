/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.concepts;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface to be implemented by analyses who has information about the thread
 * running on a CPU at a given time.
 *
 * NOTE to developers: this interface is used with the composite host model but
 * won't be necessary anymore once the analyses populate the model directly.
 */
public interface IThreadOnCpuProvider {

    /**
     * Gets the current thread ID on a given CPU for a given time
     *
     * @param cpu
     *            the CPU
     * @param time
     *            the time in nanoseconds
     * @return the current TID at the time on the CPU or {@code null} if not
     *         known
     */
    @Nullable
    Integer getThreadOnCpuAtTime(int cpu, long time);

    /**
     * The list of host IDs for which this object providers information on
     * thread on the CPU
     *
     * @return The list of host IDs
     */
    Collection<String> getHostIds();
}
