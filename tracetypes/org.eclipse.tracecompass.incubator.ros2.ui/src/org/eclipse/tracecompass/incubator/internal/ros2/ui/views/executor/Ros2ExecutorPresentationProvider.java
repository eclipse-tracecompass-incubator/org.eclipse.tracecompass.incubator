/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.executor;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.executor.Ros2ExecutorTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance.ExecutorState;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * ROS 2 Executor presentation provider.
 *
 * @author Christophe Bedard
 */
public class Ros2ExecutorPresentationProvider extends TimeGraphPresentationProvider {

    private static final int NUM_COLORS = 3;
    private static final StateItem[] STATE_TABLE;

    static {
        STATE_TABLE = new StateItem[NUM_COLORS];
        // Colours taken from the Control Flow view to feel more familiar
        // Red: #C80064 / 200, 0, 100
        STATE_TABLE[ExecutorState.GET_NEXT_READY.ordinal()] = new StateItem(new RGB(200, 0, 100), ExecutorState.GET_NEXT_READY.toString());
        // Orange: #C86400 / 200, 100, 0
        STATE_TABLE[ExecutorState.WAIT_FOR_WORK.ordinal()] = new StateItem(new RGB(200, 100, 0), ExecutorState.WAIT_FOR_WORK.toString());
        STATE_TABLE[ExecutorState.WAIT_FOR_WORK.ordinal()].getStyleMap().put(StyleProperties.HEIGHT, 0.5f);
        // Green: #00C800 / 0, 200, 0
        STATE_TABLE[ExecutorState.EXECUTE.ordinal()] = new StateItem(new RGB(0, 200, 0), ExecutorState.EXECUTE.toString());
    }

    /** The event cache */
    protected final LoadingCache<NamedTimeEvent, Optional<String>> fTimeEventNames = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(new CacheLoader<NamedTimeEvent, Optional<String>>() {
                @SuppressWarnings("null")
                @Override
                public Optional<String> load(NamedTimeEvent event) {
                    return Optional.ofNullable(event.getLabel());
                }
            });

    /**
     * Constructor
     */
    public Ros2ExecutorPresentationProvider() {
        super(StringUtils.EMPTY);
    }

    @Override
    public StateItem[] getStateTable() {
        return STATE_TABLE;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof NamedTimeEvent) {
            NamedTimeEvent namedEvent = (NamedTimeEvent) event;
            ExecutorState executorState = (ExecutorState) namedEvent.getMetadata().get("state").iterator().next(); //$NON-NLS-1$
            return executorState.ordinal();
        }
        return INVISIBLE;
    }

    @Override
    public @Nullable String getEventName(ITimeEvent event) {
        return null;
    }

    @Override
    public String getStateTypeName(ITimeGraphEntry entry) {
        if (!(entry instanceof TimeGraphEntry)) {
            return null;
        }
        TimeGraphEntry timeGraphEntry = (TimeGraphEntry) entry;

        TimeGraphEntry parentTimeGraphEntry = timeGraphEntry.getParent();
        if (null == parentTimeGraphEntry) {
            return null;
        }

        TimeGraphEntry grandParentTimeGraphEntry = parentTimeGraphEntry.getParent();
        if (null == grandParentTimeGraphEntry) {
            return "Trace"; //$NON-NLS-1$
        }

        TimeGraphEntry greatGrandParentTimeGraphEntry = grandParentTimeGraphEntry.getParent();
        if (null == greatGrandParentTimeGraphEntry) {
            return "PID"; //$NON-NLS-1$
        }

        return "TID"; //$NON-NLS-1$
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();

        Multimap<@NonNull String, @NonNull Object> metadata = event.getMetadata();
        ExecutorState executorState = (ExecutorState) metadata.get(Ros2ExecutorTimeGraphState.KEY_STATE).iterator().next();
        builder.put("State", executorState.toString()); //$NON-NLS-1$
        // Handle info might not be available
        Long handle = (Long) metadata.get(Ros2ExecutorTimeGraphState.KEY_HANDLE).iterator().next();
        builder.put("Handle", handle != 0 ? "0x" + Long.toHexString(handle) : "-"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return builder.build();
    }
}
