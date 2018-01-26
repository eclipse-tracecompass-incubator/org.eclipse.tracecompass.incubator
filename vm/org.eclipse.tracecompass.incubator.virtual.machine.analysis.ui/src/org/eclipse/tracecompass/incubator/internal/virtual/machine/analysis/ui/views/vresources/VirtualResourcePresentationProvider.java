/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVMInformationProvider;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVirtualMachineAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.trace.VirtualMachineExperiment;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources.VirtualResourceEntry.Type;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITmfTimeGraphDrawingHelper;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * @author Cedric Biancheri
 * @author Geneviève Bastien
 */
public class VirtualResourcePresentationProvider extends TimeGraphPresentationProvider {

    static final float FULL_HEIGHT = 1.0f;
    static final float REDUCED_HEIGHT = 0.25f;

    private long fLastThreadId = -1;
    private Color fColorWhite;
    private Color fColorGray;
    private Integer fAverageCharWidth;
    private volatile Object selectedFusedVMViewEntry;
    private String selectedMachine;
    private HostThread fSelectedThread;
    private int selectedCpu;
    private String selectedContainer;
//    private Map<Thread, Thread> fHighlightedThreads = new HashMap<>();
    private Map<String, Machine> fHighlightedMachines = new HashMap<>();
    private Multimap<String, Processor> fHighlightedCpus = HashMultimap.create();
    private Collection<Object> fSelectedElements = Collections.emptyList();

    private final Map<ITimeEvent, Float> fTimeEventHighlight = new HashMap<>();

    // TODO: Will this class be needed ?
//    private class Thread {
//        private String machineName;
//        private int threadID;
//        private String threadName;
//        private float fHeight;
//
//        public Thread(String m, int t) {
//            machineName = m;
//            threadID = t;
//            threadName = null;
//            fHeight = FULL_HEIGHT;
//        }
//
//        public Thread(String m, int t, String n) {
//            machineName = m;
//            threadID = t;
//            threadName = n;
//            fHeight = FULL_HEIGHT;
//        }
//
//        public String getMachineName() {
//            return machineName;
//        }
//
//        public int getThreadID() {
//            return threadID;
//        }
//
//        public void modifyHeight(float delta) {
//            fHeight += delta;
//            if (fHeight < 0) {
//                fHeight = 0;
//            } else if (fHeight > FULL_HEIGHT) {
//                fHeight = FULL_HEIGHT;
//            }
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o instanceof Thread) {
//                Thread t = (Thread) o;
//                return (t.getMachineName().equals(machineName)) && (t.getThreadID() == threadID);
//            }
//            return false;
//        }
//
//        @Override
//        public int hashCode() {
//            int hash = 1;
//            hash = hash * 31 + machineName.hashCode();
//            hash = hash * 31 + threadID;
//            return hash;
//        }
//
//        /**
//         * @return the threadName
//         */
//        public String getThreadName() {
//            return threadName;
//        }
//
//        // public float getHeightFactor() {
//        // return fHeight;
//        // }
//    }

    private enum State {
        IDLE(new RGB(200, 200, 200)),
        USERMODE(new RGB(0, 200, 0)),
        SYSCALL(new RGB(0, 0, 200)),
        IRQ(new RGB(200, 0, 100)),
        SOFT_IRQ(new RGB(200, 150, 100)),
        IRQ_ACTIVE(new RGB(200, 0, 100)),
        SOFT_IRQ_RAISED(new RGB(200, 200, 0)),
        SOFT_IRQ_ACTIVE(new RGB(200, 150, 100)),
        IN_VM(new RGB(200, 0, 200));

        public final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Default constructor
     */
    public VirtualResourcePresentationProvider() {
        super();
    }

    private static State[] getStateValues() {
        return State.values();
    }

    static private State getEventState(TimeEvent event) {
        if (event.hasValue()) {
            VirtualResourceEntry entry = (VirtualResourceEntry) event.getEntry();
            int value = event.getValue();

            if (entry.getType() == Type.CPU || entry.getType() == Type.PCPU_VM || entry.getType() == Type.PCPU_CONTAINER) {
                State state = null;
                if (value == StateValues.CPU_STATUS_IDLE) {
                    state = State.IDLE;
                } else if (value == StateValues.CPU_STATUS_RUN_USERMODE) {
                    state = State.USERMODE;
                } else if (value == StateValues.CPU_STATUS_RUN_SYSCALL) {
                    state = State.SYSCALL;
                } else if (value == StateValues.CPU_STATUS_IRQ) {
                    state = State.IRQ;
                } else if (value == StateValues.CPU_STATUS_SOFTIRQ) {
                    state = State.SOFT_IRQ;
                } else if (value == StateValues.CPU_STATUS_SOFT_IRQ_RAISED) {
                    state = State.SOFT_IRQ_RAISED;
                } else if (value == StateValues.CPU_STATUS_IN_VM) {
                    state = State.IN_VM;
                }
                if (state != null) {
                    return state;
                }
            } else if (entry.getType() == Type.IRQ) {
                return State.IRQ_ACTIVE;
            } else if (entry.getType() == Type.SOFT_IRQ) {
                if (value == StateValues.CPU_STATUS_SOFT_IRQ_RAISED) {
                    return State.SOFT_IRQ_RAISED;
                }
                return State.SOFT_IRQ_ACTIVE;
            }
        }
        return null;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.ordinal();
        }
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        /*
         * TODO: find why some time events are created even if there is nothing
         * for some VMs. Normally the filter by VM name in the
         * createCpuEventsList in the FVMView should prevent the creation of
         * those events.
         */
        return INVISIBLE;
        // return TRANSPARENT;
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
    public String getEventName(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.toString();
        }
        if (event instanceof NullTimeEvent) {
            return null;
        }
        return Messages.FusedVMView_multipleStates;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {

        Map<String, String> retMap = new LinkedHashMap<>();
        if (!(event instanceof TimeEvent && ((TimeEvent) event).hasValue())) {
            return retMap;
        }

        TimeEvent tcEvent = (TimeEvent) event;
        VirtualResourceEntry entry = (VirtualResourceEntry) event.getEntry();

        ITmfTrace exp = entry.getTrace();
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(exp, FusedVirtualMachineAnalysis.ID);
        if (ss == null) {
            return retMap;
        }

        /* Here we get the name of the host or the vm. */
        int cpuQuark = entry.getQuark();
        String machineName = null;
        try {
            ITmfStateInterval interval;
            List<ITmfStateInterval> fullState = ss.queryFullState(hoverTime);
            int machineNameQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.MACHINE_NAME);
            if (machineNameQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                interval = fullState.get(machineNameQuark);
                ITmfStateValue value = interval.getStateValue();
                machineName = value.unboxStr();
                retMap.put(Messages.FusedVMView_TooltipVirtualMachine, machineName);
            }

            int conditionQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.CONDITION);
            if (conditionQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                interval = fullState.get(conditionQuark);
                ITmfStateValue value = interval.getStateValue();
                int condition = value.unboxInt();
                if (condition == StateValues.CONDITION_IN_VM) {
                    int machineVCpuQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.VIRTUAL_CPU);
                    if (machineVCpuQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                        interval = fullState.get(machineVCpuQuark);
                        value = interval.getStateValue();
                        int vcpu = value.unboxInt();
                        retMap.put(Messages.FusedVMView_TooltipVirtualCpu, String.valueOf(vcpu));
                    }
                }
            }

            // Check for IRQ or Soft_IRQ type
            if (entry.getType().equals(Type.IRQ) || entry.getType().equals(Type.SOFT_IRQ)) {

                // Get CPU of IRQ or SoftIRQ and provide it for the tooltip
                // display
                int cpu = tcEvent.getValue();
                if (cpu >= 0) {
                    retMap.put(Messages.FusedVMView_attributeCpuName, String.valueOf(cpu));
                }
            }

            // Check for type CPU
            else if (entry.getType().equals(Type.CPU) || entry.getType().equals(Type.PCPU_VM) || entry.getType().equals(Type.PCPU_CONTAINER)) {
                int status = tcEvent.getValue();

                if (status == StateValues.CPU_STATUS_IRQ) {
                    // In IRQ state get the IRQ that caused the interruption
                    int cpu = entry.getId();

                    List<Integer> irqQuarks = ss.getQuarks(FusedAttributes.CPUS, Integer.toString(cpu), FusedAttributes.IRQS, "*"); //$NON-NLS-1$

                    for (int irqQuark : irqQuarks) {
                        if (fullState.get(irqQuark).getStateValue().unboxInt() == cpu) {
                            ITmfStateInterval value = fullState.get(irqQuark);
                            if (!value.getStateValue().isNull()) {
                                int irq = Integer.parseInt(ss.getAttributeName(irqQuark));
                                retMap.put(Messages.FusedVMView_attributeIrqName, String.valueOf(irq));
                            }
                            break;
                        }
                    }

                } else if (status == StateValues.CPU_STATUS_SOFTIRQ) {
                    // In SOFT_IRQ state get the SOFT_IRQ that caused the
                    // interruption
                    int cpu = entry.getId();

                    List<Integer> softIrqQuarks = ss.getQuarks(FusedAttributes.CPUS, Integer.toString(cpu), FusedAttributes.SOFT_IRQS, "*"); //$NON-NLS-1$

                    for (int softIrqQuark : softIrqQuarks) {
                        if (fullState.get(softIrqQuark).getStateValue().unboxInt() == cpu) {
                            ITmfStateInterval value = fullState.get(softIrqQuark);
                            if (!value.getStateValue().isNull()) {
                                int softIrq = Integer.parseInt(ss.getAttributeName(softIrqQuark));
                                retMap.put(Messages.FusedVMView_attributeSoftIrqName, String.valueOf(softIrq));
                            }
                            break;
                        }
                    }

                } else if (status == StateValues.CPU_STATUS_RUN_USERMODE || status == StateValues.CPU_STATUS_RUN_SYSCALL) {
                    // In running state get the current tid

                    retMap.put(Messages.FusedVMView_attributeHoverTime, Utils.formatTime(hoverTime, TimeFormat.CALENDAR, Resolution.NANOSEC));
                    cpuQuark = entry.getQuark();
                    int currentThreadQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.CURRENT_THREAD);
                    if (currentThreadQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        return retMap;
                    }
                    interval = fullState.get(currentThreadQuark);
                    if (!interval.getStateValue().isNull()) {
                        ITmfStateValue value = interval.getStateValue();
                        int currentThreadId = value.unboxInt();
                        retMap.put(Messages.FusedVMView_attributeTidName, Integer.toString(currentThreadId));

                        /*
                         * Special case for tid == 0, there is no NS_MAX_LEVEL
                         * node. So we look at tid 1. It should not be inside a
                         * container.
                         */
                        int saveTID = currentThreadId;
                        if (currentThreadId == 0) {
                            currentThreadId++;
                        }
                        int nsMaxLevelQuark = ss.optQuarkAbsolute(FusedAttributes.THREADS, machineName, Integer.toString(currentThreadId), FusedAttributes.NS_MAX_LEVEL);
                        if (nsMaxLevelQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                            return retMap;
                        }
                        currentThreadId = saveTID;
                        interval = fullState.get(nsMaxLevelQuark);
                        int nsMaxLevel = interval.getStateValue().unboxInt();
                        if (nsMaxLevel > 1) {
                            int actualLevel = 1;
                            int virtualTIDQuark = ss.optQuarkAbsolute(FusedAttributes.THREADS, machineName, Integer.toString(currentThreadId), FusedAttributes.VTID);
                            if (virtualTIDQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                                return retMap;
                            }
                            actualLevel++;
                            while (actualLevel < nsMaxLevel) {
                                virtualTIDQuark = ss.optQuarkRelative(virtualTIDQuark, FusedAttributes.VTID);
                                if (virtualTIDQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                                    break;
                                }
                                actualLevel++;
                            }
                            int vtid = fullState.get(virtualTIDQuark).getStateValue().unboxInt();
                            int namespaceIDQuark = ss.optQuarkRelative(virtualTIDQuark, FusedAttributes.NS_INUM);
                            if (namespaceIDQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                                return retMap;
                            }
                            long namespaceID = fullState.get(namespaceIDQuark).getStateValue().unboxLong();
                            retMap.put(Messages.FusedVMView_TooltipRecVtid, Integer.toString(vtid));
                            retMap.put(Messages.FusedVMView_TooltipRecContainer, Long.toString(namespaceID));
                        }

                        int execNameQuark = ss.optQuarkAbsolute(FusedAttributes.THREADS, machineName, Integer.toString(currentThreadId), FusedAttributes.EXEC_NAME);
                        if (execNameQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                            return retMap;
                        }
                        interval = fullState.get(execNameQuark);
                        if (!interval.getStateValue().isNull()) {
                            value = interval.getStateValue();
                            retMap.put(Messages.FusedVMView_attributeProcessName, value.unboxStr());
                        }
                        if (status == StateValues.CPU_STATUS_RUN_SYSCALL) {
                            int syscallQuark = ss.optQuarkAbsolute(FusedAttributes.THREADS, machineName, Integer.toString(currentThreadId), FusedAttributes.SYSTEM_CALL);
                            if (syscallQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                                return retMap;
                            }
                            interval = fullState.get(syscallQuark);
                            if (!interval.getStateValue().isNull()) {
                                value = interval.getStateValue();
                                retMap.put(Messages.FusedVMView_attributeSyscallName, value.unboxStr());
                            }
                        }
                    }
                }
            }
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }

        return retMap;
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (fColorGray == null) {
            fColorGray = gc.getDevice().getSystemColor(SWT.COLOR_GRAY);
        }
        if (fColorWhite == null) {
            fColorWhite = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
        }
        if (fAverageCharWidth == null) {
            fAverageCharWidth = gc.getFontMetrics().getAverageCharWidth();
        }

        ITmfTimeGraphDrawingHelper drawingHelper = getDrawingHelper();
        if (bounds.width <= fAverageCharWidth) {
            return;
        }

        if (!(event instanceof TimeEvent)) {
            return;
        }
        TimeEvent tcEvent = (TimeEvent) event;
        if (!tcEvent.hasValue()) {
            return;
        }

        VirtualResourceEntry entry = (VirtualResourceEntry) event.getEntry();
        if (!(entry.getType().equals(Type.CPU) || entry.getType().equals(Type.PCPU_VM) || entry.getType().equals(Type.PCPU_CONTAINER))) {
            return;
        }

        int status = tcEvent.getValue();
        if (status != StateValues.CPU_STATUS_RUN_USERMODE && status != StateValues.CPU_STATUS_RUN_SYSCALL) {
            return;
        }

        ITmfTrace exp = entry.getTrace();
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(exp, FusedVirtualMachineAnalysis.ID);
        if (ss == null) {
            return;
        }
        long time = event.getTime();

        /* Here we get the name of the host or the vm. */
        int cpuQuark = entry.getQuark();
        String machineName = null;
        try {
            ITmfStateInterval interval;
            int machineNameQuark = ss.getQuarkRelative(cpuQuark, FusedAttributes.MACHINE_NAME);
            interval = ss.querySingleState(time, machineNameQuark);
            ITmfStateValue value = interval.getStateValue();
            machineName = value.unboxStr();
        } catch (AttributeNotFoundException e) {
            Activator.getDefault().logError("Error in FusedVMViewPresentationProvider, timestamp: " + FusedVMInformationProvider.formatTime(event.getTime()), e); //$NON-NLS-1$
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }

        try {
            while (time < event.getTime() + event.getDuration()) {
                cpuQuark = entry.getQuark();
                int currentThreadQuark = ss.getQuarkRelative(cpuQuark, FusedAttributes.CURRENT_THREAD);
                ITmfStateInterval tidInterval = ss.querySingleState(time, currentThreadQuark);
                long startTime = Math.max(tidInterval.getStartTime(), event.getTime());
                int x = Math.max(drawingHelper.getXForTime(startTime), bounds.x);
                if (x >= bounds.x + bounds.width) {
                    break;
                }
                if (!tidInterval.getStateValue().isNull()) {
                    ITmfStateValue value = tidInterval.getStateValue();
                    int currentThreadId = value.unboxInt();
                    long endTime = Math.min(tidInterval.getEndTime() + 1, event.getTime() + event.getDuration());
                    int xForEndTime = drawingHelper.getXForTime(endTime);
                    if (xForEndTime > bounds.x) {
                        int width = Math.min(xForEndTime, bounds.x + bounds.width) - x - 1;
                        if (width > 0) {
                            String attribute = null;
                            int beginIndex = 0;
                            if (status == StateValues.CPU_STATUS_RUN_USERMODE && currentThreadId != fLastThreadId) {
                                attribute = FusedAttributes.EXEC_NAME;
                            } else if (status == StateValues.CPU_STATUS_RUN_SYSCALL) {
                                attribute = FusedAttributes.SYSTEM_CALL;
                                /*
                                 * Remove the "sys_" or "syscall_entry_" or
                                 * similar from what we draw in the rectangle.
                                 * This depends on the trace's event layout.
                                 */
                                ITmfTrace trace = entry.getTrace();
                                if (trace instanceof IKernelTrace) {
                                    IKernelAnalysisEventLayout layout = ((IKernelTrace) trace).getKernelEventLayout();
                                    beginIndex = layout.eventSyscallEntryPrefix().length();
                                } else if (trace instanceof VirtualMachineExperiment) {
                                    IKernelAnalysisEventLayout layout = ((IKernelTrace) trace.getChild(0)).getKernelEventLayout();
                                    beginIndex = layout.eventSyscallEntryPrefix().length();
                                }
                            }
                            if (attribute != null) {
                                int quark = ss.getQuarkAbsolute(FusedAttributes.THREADS, machineName, Integer.toString(currentThreadId), attribute);
                                ITmfStateInterval interval = ss.querySingleState(time, quark);
                                if (!interval.getStateValue().isNull()) {
                                    value = interval.getStateValue();
                                    gc.setForeground(fColorWhite);
                                    int drawn = Utils.drawText(gc, value.unboxStr().substring(beginIndex), x + 1, bounds.y - 2, width, bounds.height, true, true);
                                    if (drawn > 0) {
                                        fLastThreadId = currentThreadId;
                                    }
                                }
                            }
                            if (xForEndTime < bounds.x + bounds.width) {
                                gc.setForeground(fColorGray);
                                gc.drawLine(xForEndTime, bounds.y + 1, xForEndTime, bounds.y + bounds.height - 2);
                            }
                        }
                    }
                }
                // make sure next time is at least at the next pixel
                time = Math.max(tidInterval.getEndTime() + 1, drawingHelper.getTimeAtX(x + 1));
            }
        } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
            Activator.getDefault().logError("Error in FusedVMViewPresentationProvider, timestamp: " + FusedVMInformationProvider.formatTime(event.getTime()), e); //$NON-NLS-1$
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
    }

    @Override
    public void postDrawEntry(ITimeGraphEntry entry, Rectangle bounds, GC gc) {
        fLastThreadId = -1;
    }

    //
    // Functions used to decide if the area is highlighted or not.
    //

    /**
     * Says for a specific event if the related machine is highlighted
     *
     * @throws StateSystemDisposedException
     */
    private boolean isMachineAndCpuHighlighted(ITimeEvent event) throws StateSystemDisposedException {
        Map<String, Machine> map = getHighlightedMachines();
        if (map.isEmpty()) {
            // There is no specific machine selected, so we highlight everything
            // if there is no thread selected
            return fSelectedThread == null;
        }

        // Get the machine of this event
        VirtualResourceEntry entry = (VirtualResourceEntry) event.getEntry();
        ITmfTrace trace = entry.getTrace();
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(trace, FusedVirtualMachineAnalysis.ID);
        int cpuQuark = entry.getQuark();
        // Query the state system at the end of this event, as we may have more
        // information at this time than at the beginning
        long time = event.getTime() + event.getDuration() - 1;
        if (ss == null) {
            return false;
        }

        List<String> allMachines = FusedVMInformationProvider.getAllMachines(ss, Integer.valueOf(ss.getAttributeName(cpuQuark)), time);
        allMachines.retainAll(map.keySet());

        for (String machineName : allMachines) {
            Machine highlighted = map.get(machineName);
            // See if the cpu is selected
            Collection<Processor> highlightedCpus = getHighlightedCpusFor(machineName);

            // If the machine is selected, and no specific processor is, return
            // true
            if (highlighted != null && highlightedCpus.isEmpty()) {
                return true;
            }
            // Otherwise look at the specific processor

            // Get the CPU of this event
            int conditionQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.CONDITION);
            if (conditionQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return false;
            }
            ITmfStateInterval interval = ss.querySingleState(time, conditionQuark);
            if (!interval.getStateValue().isNull()) {
                ITmfStateValue valueInVM = interval.getStateValue();
                int cpu = -1;
                int inVM = valueInVM.unboxInt();
                switch (inVM) {
                case StateValues.CONDITION_IN_VM:
                    int machineVCpuQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.VIRTUAL_CPU);
                    if (machineVCpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                        return false;
                    }
                    interval = ss.querySingleState(time, machineVCpuQuark);
                    ITmfStateValue value = interval.getStateValue();
                    cpu = value.unboxInt();
                    break;
                case StateValues.CONDITION_OUT_VM:
                    cpu = Integer.parseInt(ss.getAttributeName(cpuQuark));
                    break;
                default:
                    return true;
                }
                if (cpu != -1) {
                    for (Processor proc : highlightedCpus) {
                        if (proc.getNumber() == cpu) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Says for a specific event if the related process is highlighted
     *
     * @throws StateSystemDisposedException
     */
    private boolean isProcessHighlighted(ITimeEvent event) throws StateSystemDisposedException {
        HostThread selectedThread = fSelectedThread;
        if (selectedThread == null) {
            return false;
        }

        VirtualResourceEntry entry = (VirtualResourceEntry) event.getEntry();
        ITmfTrace trace = entry.getTrace();
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(trace, FusedVirtualMachineAnalysis.ID);
        int cpuQuark = entry.getQuark();
        long time = event.getTime();
        if (ss == null) {
            // return false;
            return false;
        }
        ITmfStateInterval interval;
        int machineHostQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.MACHINE_NAME);
        int currentThreadQuark = ss.optQuarkRelative(cpuQuark, FusedAttributes.CURRENT_THREAD);
        if (machineHostQuark == ITmfStateSystem.INVALID_ATTRIBUTE || currentThreadQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return false;
        }
        interval = ss.querySingleState(time, machineHostQuark);
        ITmfStateValue value = interval.getStateValue();
        String machineHost = value.unboxStr();

        interval = ss.querySingleState(time, currentThreadQuark);
        value = interval.getStateValue();
        int currentThreadID = value.unboxInt();

        return selectedThread.equals(new HostThread(machineHost, currentThreadID));
    }

    //
    // Getters, setter, and some short useful methods
    //

    /**
     * Sets the selected entry in FusedVM View
     *
     * @param o
     *            the FusedVMView entry selected
     */
    public void setSelectedFusedVMViewEntry(Object o) {
        selectedFusedVMViewEntry = o;
    }

    /**
     * Gets the selected entry in FusedVM View
     *
     * @return the selectedFusedVMViewEntry
     */
    public Object getSelectedFusedVMViewEntry() {
        return selectedFusedVMViewEntry;
    }

    /**
     * Gets the selected machine
     *
     * @return the selectedMachine
     */
    public String getSelectedMachine() {
        return selectedMachine;
    }

    /**
     * Sets the selected machine
     *
     * @param machine
     *            the selectedMachine to set
     */
    public void setSelectedMachine(String machine) {
        selectedMachine = machine;
    }

    /**
     * Gets the selected cpu
     *
     * @return the selectedCpu
     */
    public int getSelectedCpu() {
        return selectedCpu;
    }

    /**
     * Sets the selected cpu
     *
     * @param cpu
     *            the selectedCpu to set
     */
    public void setSelectedCpu(int cpu) {
        selectedCpu = cpu;
    }

    /**
     * @param ht
     *            The host thread to select
     */
    public void setSelectedThread(@Nullable HostThread ht) {
        if (ht == null) {
            fSelectedThread = null;
        } else {
            HostThread selectedThread = fSelectedThread;
            if (ht.equals(selectedThread)) {
                fSelectedThread = null;
            } else {
                fSelectedThread = ht;
            }
        }
        resetTimeEventHighlight();
    }

    /**
     * Sets the selected container;
     *
     * @param container
     *            the selected container
     */
    public void setSelectedContainer(String container) {
        selectedContainer = container;
    }

    /**
     * Gets the selected container
     *
     * @return the selected container
     */
    public String getSelectedContainer() {
        return selectedContainer;
    }


//    /**
//     * Adds the selected thread to the list of highlighted threads
//     */
//    public void addHighlightedThread() {
//        fHighlightedThreads.put(fSelectedThread, fSelectedThread);
//    }
//
//    /**
//     * Removes the selected thread of the list of highlighted threads
//     */
//    public void removeHighlightedThread() {
//        fHighlightedThreads.remove(fSelectedThread);
//    }

    private Map<String, Machine> getHighlightedMachines() {
        return fHighlightedMachines;
    }

    private Collection<Processor> getHighlightedCpusFor(String machine) {
        return fHighlightedCpus.get(machine);
    }

    /**
     * Resets the time event highlights
     */
    public void resetTimeEventHighlight() {
        fTimeEventHighlight.clear();
    }

    @Override
    public Map<String, Object> getSpecificEventStyle(ITimeEvent event) {
        float heightFactor = FULL_HEIGHT;
        Float b = fTimeEventHighlight.get(event);
        if (b != null) {
            return ImmutableMap.of(ITimeEventStyleStrings.heightFactor(), b);
        }
        try {
            Type typeEntry = ((VirtualResourceEntry) event.getEntry()).getType();
            if (typeEntry == Type.IRQ || typeEntry == Type.SOFT_IRQ) {
                heightFactor = FULL_HEIGHT;
            } else {
                if (isProcessHighlighted(event) || isMachineAndCpuHighlighted(event)) {
                    heightFactor = FULL_HEIGHT;
                } else {
                    heightFactor = REDUCED_HEIGHT;
                }
            }
        } catch (StateSystemDisposedException e) {
            heightFactor = FULL_HEIGHT;
        }
        fTimeEventHighlight.put(event, heightFactor);
        return ImmutableMap.of(ITimeEventStyleStrings.heightFactor(), heightFactor);
    }

    /**
     * @param delta The difference in thread alpha
     */
    public void modifySelectedThreadAlpha(int delta) {
        // TODO: implement this method
//        if (fSelectedThread != null) {
//            fSelectedThread.modifyHeight(delta / 100);
//        }
    }

    Collection<Object> getSelectedElements() {
        return fSelectedElements;
    }

    void setSelectedElements(Collection<Object> elements) {
        fSelectedElements = elements;
        fHighlightedMachines.clear();
        fHighlightedCpus.clear();
        for (Object obj : elements) {
            if (obj instanceof Machine) {
                Machine machine = (Machine) obj;
                // FIXME: Containers could have same ID in different physical machine, need the host ID as well
                fHighlightedMachines.put(machine.isContainer() ? machine.getMachineName() : machine.getHostId(), machine);
            } else if (obj instanceof Processor) {
                Processor processor = (Processor) obj;
                Machine machine = processor.getMachine();
                fHighlightedCpus.put(machine.isContainer() ? machine.getMachineName() : machine.getHostId(), processor);
            }
        }
    }

}
