/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;

import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

/**
 * Abstract base class for constructing data series representing the throughput
 * of DPDK ports, measured in packets per second (PPS) and bits per second (BPS)
 */
abstract class AbstractPortQueueBuilder {
    private final long fId;
    private final String fName;
    protected final int fQueueQuark;
    protected final double[] fValues;
    protected double fPrevCount;

    static final double SECONDS_PER_NANOSECOND = 1E-9;

    /**
     * Constructor
     *
     * @param id
     *            The series Id
     * @param portQueueQuark
     *            The queue's quark
     * @param name
     *            The name of this series
     * @param length
     *            The length of the series
     */
    AbstractPortQueueBuilder(long id, int portQueueQuark, String name, int length) {
        fId = id;
        fQueueQuark = portQueueQuark;
        fName = name;
        fValues = new double[length];
    }

    void setPrevCount(double prevCount) {
        fPrevCount = prevCount;
    }

    /**
     * Update packet counts or packet bytes at the desired index
     *
     * @param pos
     *            The index to update
     * @param newCount
     *            The new count of bytes received or transmitted
     * @param deltaT
     *            The time difference to the previous value for interpolation
     */
    abstract void updateValue(int pos, double newCount, long deltaT);

    /**
     * Build a data series representing the network traffic throughput
     *
     * @param yAxisDescription
     *            Description for the Y axis
     * @return an IYModel
     */
    IYModel build(TmfXYAxisDescription yAxisDescription) {
        return new YModel(fId, fName, fValues, yAxisDescription);
    }
}
