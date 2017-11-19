/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.flamechart;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLinkEvent;

import com.google.common.base.Objects;

/**
 * Span link, a link with an ID. Helps in graph creation as it can provide a
 * quick filter.
 *
 * @author Matthew Khouzam
 */
public class SpanLinkEvent extends TimeLinkEvent {

    private final int fId;

    /**
     * Span link builder
     *
     * @param src
     *            source entry
     * @param dst
     *            destination entry
     * @param start
     *            start time
     * @param duration
     *            duration
     * @param id
     *            span id
     * @return the link event
     */
    public static @NonNull ILinkEvent create(ITimeGraphEntry src, ITimeGraphEntry dst, long start, long duration, int id) {
        return id == -1 ? new TimeLinkEvent(src, dst, start, duration) : new SpanLinkEvent(src, dst, start, duration, id);
    }

    /**
     * Constructor
     *
     * @param src
     *            source entry
     * @param dst
     *            destination entry
     * @param start
     *            start time
     * @param duration
     *            duration
     * @param id
     *            span id
     */
    private SpanLinkEvent(ITimeGraphEntry src, ITimeGraphEntry dst, long start, long duration, int id) {
        super(src, dst, start, duration);
        fId = id;
    }

    /**
     * Get the span id
     *
     * @return the span id
     */
    public int getId() {
        return fId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), fId);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((SpanLinkEvent) obj).fId == fId;
    }

}
