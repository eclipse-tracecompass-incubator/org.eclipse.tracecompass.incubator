/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;

import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

public class TimeRange extends TmfTimeRange {

    public TimeRange(double startTime, double endTime) {
        super(TmfTimestamp.fromNanos((long) (startTime * 1e6)), TmfTimestamp.fromNanos((long) (endTime * 1e6)));
        start = startTime;
        end = endTime;
    }

    // unit is ms
    private double start = UNKNOWN_DOUBLE;
    private double end = UNKNOWN_DOUBLE;

    public boolean isValid() {
        return start >= 0 && end >= 0 && start < end;
    }

    public double length() {
        if (isValid()) {
            return end - start;
        }
        return UNKNOWN_DOUBLE;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return start + " ~ " + end; //$NON-NLS-1$
    }
}
