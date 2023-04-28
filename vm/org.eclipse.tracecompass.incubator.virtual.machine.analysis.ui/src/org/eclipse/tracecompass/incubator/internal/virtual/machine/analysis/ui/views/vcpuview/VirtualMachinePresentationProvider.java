/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vcpuview;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.VcpuStateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vcpuview.VirtualMachineCommon.Type;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Presentation provider for the Virtual Machine view, based on the generic TMF
 * presentation provider.
 *
 * @author Mohamad Gebai
 */
public class VirtualMachinePresentationProvider extends TimeGraphPresentationProvider {

    private static final int ALPHA = 70;

    /*
     * TODO: Some of it is copy-pasted from the control flow presentation
     * provider because it actually is the same data as from the control flow
     * view. Ideally, we should reuse what is there instead of rewriting it here
     */
    enum VCPUStyle {

        UNKNOWN(Messages.VcpuStyles_unknow, 83, 156, 83, 255, 1.00f),
        IDLE(Messages.VcpuStyles_idle, 200, 200, 200, 255, 0.66f),
        USERMODE(Messages.VcpuStyles_vcpuUsermode, 0, 200, 0, 255, 1.00f),
        WAIT_VMM(Messages.VcpuStyles_waitVmm, 179, 6, 6, 255, 0.66f),
        VCPU_PREEMPTED(Messages.VcpuStyles_vcpuPreempted, 207, 127, 47, 255, 0.50f),

        THREAD_UNKNOWN(Messages.VcpuStyles_wait, 200, 200, 200, 255, 0.50f),
        THREAD_WAIT_BLOCKED(Messages.VcpuStyles_waitBlocked, 200, 200, 0, 255, 0.50f),
        THREAD_WAIT_FOR_CPU(Messages.VcpuStyles_waitForCPU, 200, 100, 0, 255, 0.50f),
        THREAD_USERMODE(Messages.VcpuStyles_usermode, 0, 200, 0, 255, 1.00f),
        THREAD_SYSCALL(Messages.VcpuStyles_systemCall, 0, 0, 200, 255, 1.00f),
        THREAD_INTERRUPTED(Messages.VcpuStyles_Interrupt, 200, 0, 100, 255, 0.75f);

        private final Map<String, Object> fMap;

        private VCPUStyle(@Nullable String label, int red, int green, int blue, int alpha, float heightFactor) {
            if (label == null) {
                throw new IllegalArgumentException("Label cannot be null"); //$NON-NLS-1$
            }
            if (red > 255 || red < 0) {
                throw new IllegalArgumentException("Red needs to be between 0 and 255"); //$NON-NLS-1$
            }
            if (green > 255 || green < 0) {
                throw new IllegalArgumentException("Green needs to be between 0 and 255"); //$NON-NLS-1$
            }
            if (blue > 255 || blue < 0) {
                throw new IllegalArgumentException("Blue needs to be between 0 and 255"); //$NON-NLS-1$
            }
            if (alpha > 255 || alpha < 0) {
                throw new IllegalArgumentException("alpha needs to be between 0 and 255"); //$NON-NLS-1$
            }
            if (heightFactor > 1.0 || heightFactor < 0) {
                throw new IllegalArgumentException("Height factor needs to be between 0 and 1.0, given hint : " + heightFactor); //$NON-NLS-1$
            }
            fMap = ImmutableMap.of(StyleProperties.STYLE_NAME, label,
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(red, green, blue),
                    StyleProperties.OPACITY, alpha / 255,
                    StyleProperties.HEIGHT, heightFactor);
        }

        public String getLabel() {
            return (String) fMap.get(StyleProperties.STYLE_NAME);
        }

        /**
         * Get a map of the values corresponding to the fields in
         * {@link StyleProperties}
         *
         * @return the map corresponding to the api defined in
         *         {@link StyleProperties}
         */
        public Map<String, Object> toMap() {
            return fMap;
        }
    }

    private static final List<StateItem> STATE_LIST;
    private static final StateItem[] STATE_TABLE;

    static {
        /*
         * DO NOT MODIFY AFTER
         */
        STATE_LIST = ImmutableList.of(new StateItem(VCPUStyle.UNKNOWN.toMap()),
                new StateItem(VCPUStyle.IDLE.toMap()),
                new StateItem(VCPUStyle.USERMODE.toMap()),
                new StateItem(VCPUStyle.WAIT_VMM.toMap()),
                new StateItem(VCPUStyle.VCPU_PREEMPTED.toMap()),
                new StateItem(VCPUStyle.THREAD_UNKNOWN.toMap()),
                new StateItem(VCPUStyle.THREAD_WAIT_BLOCKED.toMap()),
                new StateItem(VCPUStyle.THREAD_WAIT_FOR_CPU.toMap()),
                new StateItem(VCPUStyle.THREAD_USERMODE.toMap()),
                new StateItem(VCPUStyle.THREAD_SYSCALL.toMap()),
                new StateItem(VCPUStyle.THREAD_INTERRUPTED.toMap()));
        STATE_TABLE = STATE_LIST.toArray(new StateItem[STATE_LIST.size()]);
    }

    /**
     * Default constructor
     */
    public VirtualMachinePresentationProvider() {
        super();
    }

    private static VCPUStyle getStateForVcpu(int value) {
        if ((value & VcpuStateValues.VCPU_PREEMPT) > 0) {
            return VCPUStyle.VCPU_PREEMPTED;
        } else if ((value & VcpuStateValues.VCPU_VMM) > 0) {
            return VCPUStyle.WAIT_VMM;
        } else if (value == 2) {
            return VCPUStyle.USERMODE;
        } else if (value == 1) {
            return VCPUStyle.IDLE;
        } else {
            return VCPUStyle.UNKNOWN;
        }
    }

    private static @Nullable VCPUStyle getStateForThread(int value) {
        if (value == VcpuStateValues.VCPU_PREEMPT) {
            return null;
        }
        switch (value) {
        case StateValues.PROCESS_STATUS_RUN_USERMODE:
            return VCPUStyle.THREAD_USERMODE;
        case StateValues.PROCESS_STATUS_RUN_SYSCALL:
            return VCPUStyle.THREAD_SYSCALL;
        case StateValues.PROCESS_STATUS_WAIT_FOR_CPU:
            return VCPUStyle.THREAD_WAIT_FOR_CPU;
        case StateValues.PROCESS_STATUS_WAIT_BLOCKED:
            return VCPUStyle.THREAD_WAIT_BLOCKED;
        case StateValues.PROCESS_STATUS_INTERRUPTED:
            return VCPUStyle.THREAD_INTERRUPTED;
        case StateValues.PROCESS_STATUS_UNKNOWN:
        case StateValues.PROCESS_STATUS_WAIT_UNKNOWN:
            return VCPUStyle.THREAD_UNKNOWN;
        default:
            return null;
        }
    }

    private static @Nullable VCPUStyle getEventState(TimeEvent event) {
        if (event.hasValue()) {
            VirtualMachineViewEntry entry = (VirtualMachineViewEntry) event.getEntry();
            int value = event.getValue();

            if (entry.getType() == Type.VCPU) {
                return getStateForVcpu(value);
            } else if (entry.getType() == Type.THREAD) {
                return getStateForThread(value);
            }
        }
        return null;
    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event == null) {
            return TRANSPARENT;
        }
        VCPUStyle state = getEventState((TimeEvent) event);
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
        return STATE_TABLE;
    }

    @Override
    public @Nullable String getEventName(@Nullable ITimeEvent event) {
        if (event == null) {
            return null;
        }
        VCPUStyle state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.toString();
        }
        if (event instanceof NullTimeEvent) {
            return null;
        }
        return Messages.VmView_multipleStates;
    }

    @Override
    public void postDrawEvent(@Nullable ITimeEvent event, @Nullable Rectangle bounds, @Nullable GC gc) {
        if (bounds == null || gc == null || !(event instanceof TimeEvent)) {
            return;
        }
        boolean visible = bounds.width == 0 ? false : true;
        if (!visible) {
            return;
        }
        TimeEvent ev = (TimeEvent) event;
        /*
         * FIXME: There seems to be a bug when multiple events should be drawn
         * under a alpha event. See FIXME comment in
         * VirtualMachineView#getEventList
         */
        if (ev.hasValue()) {
            VirtualMachineViewEntry entry = (VirtualMachineViewEntry) event.getEntry();

            if (entry.getType() == Type.THREAD) {
                int value = ev.getValue();
                if ((value & VcpuStateValues.VCPU_PREEMPT) != 0) {
                    /*
                     * If the status was preempted at this time, draw an alpha
                     * over this state
                     */
                    Color alphaColor = Display.getDefault().getSystemColor(SWT.COLOR_RED);

                    int alpha = gc.getAlpha();
                    Color background = gc.getBackground();
                    // fill all rect area
                    gc.setBackground(alphaColor);
                    gc.setAlpha(ALPHA);
                    gc.fillRectangle(bounds);

                    gc.setBackground(background);
                    gc.setAlpha(alpha);
                }
            }
        }
    }

}