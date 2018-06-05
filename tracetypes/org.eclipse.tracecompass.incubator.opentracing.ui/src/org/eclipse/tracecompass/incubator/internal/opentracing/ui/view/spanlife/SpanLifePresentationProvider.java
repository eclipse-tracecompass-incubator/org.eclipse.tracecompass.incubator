/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Object lifespan presentation provider
 *
 * @author Katherine Nadeau
 */
public class SpanLifePresentationProvider extends TimeGraphPresentationProvider {

    /**
     * Only state available
     */
    private static final StateItem[] STATE_TABLE = { new StateItem(new RGB(0, 0, 140), "Active") }; //$NON-NLS-1$

    /**
     * Constructor
     */
    public SpanLifePresentationProvider() {
        super("Span"); //$NON-NLS-1$
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        return (event instanceof TimeEvent && ((TimeEvent) event).getValue() != Integer.MIN_VALUE) ? 0 : -1;
    }
}
