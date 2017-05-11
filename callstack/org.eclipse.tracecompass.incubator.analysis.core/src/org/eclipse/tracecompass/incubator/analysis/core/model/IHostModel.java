/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.model;

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
    int getThreadOnCpu(int cpu, long t);

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

}
