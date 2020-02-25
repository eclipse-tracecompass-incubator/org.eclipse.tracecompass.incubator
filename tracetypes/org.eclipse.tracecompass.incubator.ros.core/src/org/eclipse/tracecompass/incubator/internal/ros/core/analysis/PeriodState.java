/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis;

import java.text.DecimalFormat;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

/**
 * State for representing a time period. The period will be displayed in
 * seconds.
 *
 * TODO allow for other types of formatting
 *
 * @author Christophe Bedard
 */
public class PeriodState extends TimeGraphState {

    private static final String FORMAT = "#.####"; //$NON-NLS-1$
    private static final DecimalFormat FORMATTER = new DecimalFormat(FORMAT);
    private static final String TIME_RESOLUTION = "s"; //$NON-NLS-1$

    private final long fPeriod;

    /**
     * Constructor
     *
     * @param time
     *            the timestamp
     * @param duration
     *            the duration
     * @param period
     *            the period in nanoseconds
     */
    public PeriodState(long time, long duration, long period) {
        super(time, duration, 0, formatNanosecondstoSeconds(period));
        fPeriod = period;
    }

    /**
     * Get the period
     *
     * @return the period in nanoseconds
     */
    public long getPeriod() {
        return fPeriod;
    }

    private static @NonNull String formatNanosecondstoSeconds(long ns) {
        return Objects.requireNonNull(FORMATTER.format(ns / 1000000000.0) + " " + TIME_RESOLUTION); //$NON-NLS-1$
    }
}
