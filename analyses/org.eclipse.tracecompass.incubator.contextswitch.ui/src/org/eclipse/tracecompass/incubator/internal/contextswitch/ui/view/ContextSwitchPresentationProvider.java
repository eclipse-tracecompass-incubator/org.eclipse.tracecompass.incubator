/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexis Cabana-Loriaux - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.contextswitch.ui.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * PresentationProvider used in the context switch view
 *
 * @author Alexis Cabana-Loriaux
 */
public class ContextSwitchPresentationProvider extends TimeGraphPresentationProvider {

    private enum State {
        /** Critical level, deep sky blue*/
        CRITICAL(new RGB(0, 191, 255)),
        /** High level, neon sky blue */
        HIGH(new RGB(0, 207, 255)),
        /** Moderate, medium sky blue */
        MODERATE(new RGB(128, 218, 235)),
        /** Low level, light sky blue */
        LOW(new RGB(135, 206, 250)),
        /** Used for when there's no context switch */
        NONE(new RGB(0, 0, 0));

        public final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /** Default Constructor */
    public ContextSwitchPresentationProvider() {
        super();
    }

    private static State[] getStateValues() {
        return State.values();
    }

    private static State getEventState(TimeEvent event) {
        if (event instanceof ContextSwitchTimeEvent) {
            ContextSwitchTimeEvent tevent = (ContextSwitchTimeEvent) event;
            ContextSwitchEntry entry = (ContextSwitchEntry) event.getEntry();
            if (entry.hasId()) {
                if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.NONE) {
                    return State.NONE;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.LOW) {
                    return State.LOW;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.MODERATE) {
                    return State.MODERATE;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.HIGH) {
                    return State.HIGH;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.CRITICAL) {
                    return State.CRITICAL;
                }
            }
        }
        return null;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        ContextSwitchEntry entry = (ContextSwitchEntry) event.getEntry();
        if (!entry.hasId()) {
            return TRANSPARENT;
        }
        State state = getEventState((TimeEvent) event);
        if (state == State.NONE) {
            return INVISIBLE;
        }
        if (state != null) {
            return state.ordinal();
        }
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return TRANSPARENT;
    }

    @Override
    public StateItem[] getStateTable() {
        State[] states = getStateValues();
        StateItem[] stateTable = new StateItem[states.length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = states[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public Map<String, Object> getSpecificEventStyle(ITimeEvent event) {
        Map<String, Object> specificEventStyle = super.getSpecificEventStyle(event);
        if (event instanceof ContextSwitchTimeEvent) {
            ContextSwitchTimeEvent csEvent = (ContextSwitchTimeEvent) event;
            if (csEvent.getEntry() instanceof ContextSwitchEntry) {
                ContextSwitchEntry csEntry = (ContextSwitchEntry) csEvent.getEntry();
                double mean = csEntry.getMean();
                if (mean != 0.0) {
                    Map<String, Object> retVal = new HashMap<>();
                    int count = csEvent.getCount();
                    float heightFactor = (float) (csEvent.getValue() / mean / count * 0.33);
                    heightFactor = (float) Math.max(0.1f, Math.min(heightFactor, 1.0));
                    retVal.put(ITimeEventStyleStrings.heightFactor(), heightFactor);
                    return retVal;
                }
            }
        }
        return specificEventStyle;
    }

    @Override
    public String getEventName(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.toString();
        }
        if (event instanceof NullTimeEvent) {
            return null;
        }
        return Messages.ContextSwitchPresentationProvider_CPU;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> retMap = new LinkedHashMap<>();
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tEvent = (TimeEvent) event;
            ContextSwitchEntry entry = (ContextSwitchEntry) event.getEntry();

            if (entry.hasId()) {
                retMap.put(Messages.ContextSwitchPresentationProvider_NumberOfContextSwitch, Integer.toString(tEvent.getValue()));
            }

        }
        return retMap;
    }
}
