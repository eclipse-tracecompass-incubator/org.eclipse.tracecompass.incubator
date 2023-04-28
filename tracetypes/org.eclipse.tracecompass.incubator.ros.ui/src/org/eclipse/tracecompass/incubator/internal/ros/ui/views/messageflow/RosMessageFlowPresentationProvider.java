/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.messageflow;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

import com.google.common.collect.ImmutableMap;

/**
 * ROS Message Flow presentation provider
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowPresentationProvider extends TimeGraphPresentationProvider {

    private static String ARROW_STATE = "arrow"; //$NON-NLS-1$

    /**
     * The enumeration of possible states for the view
     */
    public enum State {
        /** Publisher queue segment */
        PUB_QUEUE_SEGMENT(new RGB(0x3c, 0xb4, 0x4b)),
        /** Subscriber queue segment */
        SUB_QUEUE_SEGMENT(new RGB(0x43, 0x63, 0xd8)),
        /** Callback segment */
        CALLBACK_SEGMENT(new RGB(0xf5, 0x82, 0x31)),
        /** Other segment */
        UNKNOWN_SEGMENT(new RGB(0x40, 0x3b, 0x33)),
        /** Network arrow (pub queue --> sub queue) */
        NETWORK_ARROW(new RGB(0x26, 0x26, 0x26)),
        /** Callback scheduling arrow (sub queue --> callback) */
        CALLBACK_SCHEDULING_ARROW(new RGB(0x66, 0x66, 0x66)),
        /** Publishing arrow (anything --> pub queue) */
        PUBLISH_ARROW(new RGB(0xa6, 0xa6, 0xa6)),
        /** Other arrow */
        UNKNOWN_ARROW(new RGB(0xcc, 0xcc, 0xcc));

        /** RGB color associated with a state */
        public final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    private static final StateItem[] STATE_TABLE;

    static {
        STATE_TABLE = new StateItem[State.values().length];
        for (int i = 0; i < STATE_TABLE.length; ++i) {
            State state = State.values()[i];

            RGB stateColor = state.rgb;
            String stateType = state.name().toLowerCase().contains(ARROW_STATE) ? StyleProperties.linkType() : StyleProperties.stateType();
            ImmutableMap<String, Object> styleMap = ImmutableMap.of(
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(stateColor.red, stateColor.green, stateColor.blue),
                    StyleProperties.STYLE_NAME, String.valueOf(state.toString()),
                    StyleProperties.itemTypeProperty(), stateType);
            STATE_TABLE[i] = new StateItem(styleMap);
        }
    }

    /**
     * Constructor
     */
    public RosMessageFlowPresentationProvider() {
        super(StringUtils.EMPTY);
        // Nothing to do
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            // Directly encoded in value
            return ((TimeEvent) event).getValue();
        }
        return TRANSPARENT;
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public @Nullable String getEventName(ITimeEvent event) {
        if (event instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent) event;
            return ev.getLabel();
        }
        return Messages.RosMessageFlowPresentationProvider_MultipleStates;
    }

    @Override
    public @Nullable Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        // TODO
        return super.getEventHoverToolTipInfo(event);
    }
}
