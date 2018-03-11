/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.ui.view.life;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;

/**
 * Object lifespan presentation provider
 *
 * @author Matthew Khouzam
 */
public class ObjectLifePresentationProvider extends TimeGraphPresentationProvider {

    /**
     * Only state available
     */
    private static final StateItem THE_ONE_TRUE_STATE = new StateItem(new RGB(174, 174, 51), "Allocated"); //$NON-NLS-1$

    /**
     *
     */
    private static final StateItem[] THE_ONE_TRUE_STATE_TABLE = { THE_ONE_TRUE_STATE };

    @Override
    public StateItem[] getStateTable() {
        return THE_ONE_TRUE_STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        return (event instanceof NamedTimeEvent) ? 0 : -1;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        Map<String, String> retMap = super.getEventHoverToolTipInfo(event);
        if (retMap == null) {
            retMap = new LinkedHashMap<>(1);
        }
        if (event instanceof NamedTimeEvent) {
            retMap.put("Snapshot", ((NamedTimeEvent) event).getLabel());
        }
        return retMap;
    }
}
