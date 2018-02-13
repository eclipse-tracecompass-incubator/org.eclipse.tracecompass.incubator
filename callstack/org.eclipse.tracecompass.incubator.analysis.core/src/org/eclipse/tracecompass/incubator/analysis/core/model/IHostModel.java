/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.model;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;

/**
 * This interface represents a host system, for example a machine running Linux,
 * and allows to access information on the state of the machine at some
 * time[range]. Typically, there would be one model instance per host and all
 * traces taken on the same host (machine) will share the same model.
 *
 * Users of the model do not need to know where the information comes from. For
 * each method providing information, a default value will be provided in case
 * the information is not available. See the method's javadoc for that value.
 *
 * How the host information is accessed is up to each implementations. It can
 * make use of the various analyses of the traces that compose this model.
 *
 * @author Geneviève Bastien
 */
public interface IHostModel {

    /**
     * Value to use for thread ID
     */
    int UNKNOWN_TID = -1;
    /**
     * Value to use when a duration or timestamp is not known
     */
    long TIME_UNKNOWN = -1;

    /**
     * Get which thread is running on the CPU at a given time
     *
     * @param cpu
     *            The CPU ID on which the thread is running
     * @param t
     *            The desired time
     * @return The ID of the thread running on the CPU, or {@link #UNKNOWN_TID}
     *         if it is not available
     */
    default int getThreadOnCpu(int cpu, long t) {
        return getThreadOnCpu(cpu, t, false);
    }

    /**
     * Get which thread is running on the CPU at a given time, but may blocks if the
     * answer is not available yet but may come later
     *
     * @param cpu
     *            The CPU ID on which the thread is running
     * @param t
     *            The desired time
     * @param block
     *            If <code>true</code>, the method will block until the providers
     *            have the data available.
     * @return The ID of the thread running on the CPU, or {@link #UNKNOWN_TID} if
     *         it is not available
     */
    int getThreadOnCpu(int cpu, long t, boolean block);

    /**
     * Get the amount of time a thread was active on the CPU (any CPU) during a
     * period.
     *
     * @param tid
     *            The ID of the thread
     * @param start
     *            The start of the period for which to get the time on CPU
     * @param end
     *            The end of the period for which to get the time on CPU
     * @return The time spent on the CPU by the thread in that duration or
     *         {@link #TIME_UNKNOWN} if it is not available
     */
    long getCpuTime(int tid, long start, long end);

    /**
     * Get the aggregated sample data for a thread in a time range.
     *
     * @param tid
     *            The ID of the thread
     * @param start
     *            The start of the period for which to get the time on CPU
     * @param end
     *            The end of the period for which to get the time on CPU
     * @return The collection of aggregated sampling data for the time range
     */
    Collection<AggregatedCallSite> getSamplingData(int tid, long start, long end);

    /**
     * Get an iterator over the status intervals of a thread
     *
     * @param tid
     *            The ID of the thread
     * @param start
     *            The start of the period for which to get the time on CPU
     * @param end
     *            The end of the period for which to get the time on CPU
     * @param resolution
     *            The resolution, ie the number of nanoseconds between kernel status
     *            queries. A value lower or equal to 1 will return all intervals.
     * @return An iterator over the status intervals for the thread
     */
    Iterator<ProcessStatusInterval> getThreadStatusIntervals(int tid, long start, long end, long resolution);

    /**
     * Get whether sampling data is available for this host
     *
     * @return <code>true</code> if sampling data is available, <code>false</code>
     *         otherwise
     */
    boolean isSamplingDataAvailable();

    /**
     * Get whether thread status information is available for this host
     *
     * @return <code>true</code> if thread status information is available,
     *         <code>false</code> otherwise
     */
    boolean isThreadStatusAvailable();

    /**
     * Dispose of the model when it is not needed anymore
     */
    void dispose();

}
