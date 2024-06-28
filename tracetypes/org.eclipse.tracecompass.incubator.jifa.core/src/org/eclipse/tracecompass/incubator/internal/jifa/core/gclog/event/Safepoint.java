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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCEventType;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant;

public class Safepoint extends GCEvent {
    private double timeToEnter = Constant.UNKNOWN_DOUBLE;

    public Safepoint() {
        this.setEventType(GCEventType.SAFEPOINT);
    }

    public double getTimeToEnter() {
        return timeToEnter;
    }

    public void setTimeToEnter(double timeToEnter) {
        this.timeToEnter = timeToEnter;
    }

    @Override
    protected void appendClassSpecificInfo(StringBuilder sb) {
        sb.append(String.format("Total time for which application threads were stopped: " +
                "%.3f seconds, Stopping threads took: %.3f seconds", getDuration(), getTimeToEnter()));
    }

    @Override
    public String toDebugString(GCModel model) {
        StringBuilder sb = new StringBuilder();
        appendStartTimestamp(sb, model.getStartTime());
        appendStartTime(sb);
        appendClassSpecificInfo(sb);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendStartTime(sb);
        appendClassSpecificInfo(sb);
        return sb.toString();
    }
}
