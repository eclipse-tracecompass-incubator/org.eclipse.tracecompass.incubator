/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife;

import java.util.Objects;

import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

/**
 * Span event markers
 *
 * @author Matthew Khouzam
 */
class SpanMarkerEvent extends MarkerEvent {

    private final String fType;

    public SpanMarkerEvent(ITimeGraphEntry entry, long time, RGBA color, String type) {
        super(entry, time, 0, "logs", color, null, true); //$NON-NLS-1$
        fType = type;
    }

    public String getType() {
        return fType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fType);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && Objects.equals(fType, ((SpanMarkerEvent) obj).fType);
    }
}
