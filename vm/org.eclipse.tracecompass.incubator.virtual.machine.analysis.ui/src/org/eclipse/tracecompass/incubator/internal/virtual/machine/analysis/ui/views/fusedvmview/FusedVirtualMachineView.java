/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.fusedvmview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVMInformationProvider;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVirtualMachineAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.trace.VirtualMachineExperiment;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.fusedvmview.FusedVMViewEntry.Type;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowView;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractStateSystemTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider2;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphSelectionListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphSelectionEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Cedric Biancheri
 */
public class FusedVirtualMachineView extends AbstractStateSystemTimeGraphView {

    /** View ID. */
    public static final String ID = "org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.vm.fusedvmview"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.FusedVMView_stateTypeName
    };

    // Timeout between updates in the build thread in ms
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    /** Button to select a machine to highlight */
    private final Action fHighlightMachine = new Action(Messages.FusedVMView_ButtonMachineSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
            FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
            if (machine == null) {
                setChecked(!isChecked());
                return;
            }
            machine.setHighlightedWithAllCpu(isChecked());
            machine.setHighlightedWithAllContainers(isChecked());
            fHighlightCPU.setChecked(isChecked());
            fHighlightContainer.setChecked(isChecked());
            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a CPU to highlight */
    private final Action fHighlightCPU = new Action(Messages.FusedVMView_ButtonCPUSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
            FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
            if (machine == null) {
                setChecked(!isChecked());
                return;
            }
            machine.setHighlightedCpu(presentationProvider.getSelectedCpu(), isChecked());
            fHighlightMachine.setChecked(machine.isOneCpuHighlighted());
            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a process to highlight */
    private final Action fHighlightProcess = new Action(Messages.FusedVMView_ButtonProcessSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
            FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
            if (isChecked()) {
                presentationProvider.addHighlightedThread();
            } else {
                presentationProvider.removeHighlightedThread();
            }
            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a container to highlight */
    private final Action fHighlightContainer = new Action(Messages.FusedVMView_ButtonContainerSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
            FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
            String container = presentationProvider.getSelectedContainer();
            if (machine == null) {
                setChecked(!isChecked());
                return;
            }
            machine.setHighlightedContainer(container, isChecked());
            fHighlightMachine.setChecked(machine.isOneContainerHighlighted());
            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    private Action fSelectMachineAction;

    /** The beginning of the selected time */
    private long beginSelectedTime;

    /** The end of the selected time */
    private long endSelectedTime;

    /**
     * Listener that handles a change in the selected time in the FusedVM View
     */
    private final ITimeGraphTimeListener fTimeListenerFusedVMView = new ITimeGraphTimeListener() {

        @Override
        public void timeSelected(TimeGraphTimeEvent event) {
            setBeginSelectedTime(event.getBeginTime());
            setEndSelectedTime(event.getEndTime());
            long begin = getBeginSelectedTime();
            long end = getEndSelectedTime();
            if (begin == end) {
                FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                Object o = presentationProvider.getSelectedFusedVMViewEntry();
                if (o == null) {
                    return;
                }
                if (!(o instanceof FusedVMViewEntry)) {
                    return;
                }
                FusedVMViewEntry entry = (FusedVMViewEntry) o;
                int cpuQuark = entry.getQuark();
                ITmfTrace trace = getTrace();
                if (trace == null) {
                    return;
                }
                final ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, FusedVirtualMachineAnalysis.ID);
                if (ssq == null) {
                    return;
                }
                String machineName = null;
                try {
                    ITmfStateInterval interval;
                    int machineNameQuark = ssq.getQuarkRelative(cpuQuark, Attributes.MACHINE_NAME);
                    interval = ssq.querySingleState(begin, machineNameQuark);
                    ITmfStateValue value = interval.getStateValue();
                    machineName = value.unboxStr();
                    presentationProvider.setSelectedMachine(machineName);

                    int threadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
                    interval = ssq.querySingleState(begin, threadQuark);
                    value = interval.getStateValue();
                    int threadID = value.unboxInt();

                    String threadAttributeName = FusedVMInformationProvider.buildThreadAttributeName(threadID, Integer.parseInt(ssq.getAttributeName(cpuQuark)));
                    int execNameQuark = ssq.getQuarkAbsolute(Attributes.THREADS, machineName, threadAttributeName, Attributes.EXEC_NAME);
                    interval = ssq.querySingleState(begin, execNameQuark);
                    value = interval.getStateValue();
                    String threadName = value.unboxStr();

                    presentationProvider.setSelectedThread(machineName, threadID, threadName);

                    int conditionQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CONDITION);
                    interval = ssq.querySingleState(begin, conditionQuark);
                    value = interval.getStateValue();
                    int condition = value.unboxInt();
                    List<Integer> list = ssq.getQuarks(cpuQuark, Attributes.VIRTUAL_CPU);
                    if (condition == StateValues.CONDITION_IN_VM && !list.isEmpty()) {
                        /*
                         * Trick to get the quark and don't generate an
                         * exception if it's not there
                         */
                        int machineVCpuQuark = list.get(0);
                        interval = ssq.querySingleState(begin, machineVCpuQuark);
                        value = interval.getStateValue();
                        int vcpu = value.unboxInt();
                        presentationProvider.setSelectedCpu(vcpu);
                    } else {
                        presentationProvider.setSelectedCpu(Integer.parseInt(ssq.getAttributeName(cpuQuark)));
                    }

                    /*
                     * To look for the namespace number we look at process 1 if
                     * we are on process 0
                     */
                    if (threadID == 0) {
                        threadID++;
                    }
                    int nsInumQuark = FusedVMInformationProvider.getNodeNsInum(ssq, begin, machineName, threadID);
                    interval = ssq.querySingleState(begin, nsInumQuark);
                    String container = Long.toString(interval.getStateValue().unboxLong());
                    presentationProvider.setSelectedContainer(container);

                } catch (AttributeNotFoundException e) {
                     Activator.getDefault().logError("Error in FusedVirtualMachineView, timestamp: " + FusedVMInformationProvider.formatTime(event.getBeginTime()), e); //$NON-NLS-1$
                } catch (StateSystemDisposedException e) {
                    /* Ignored */
                }

                updateButtonsSelection();
                updateToolTipTexts();

            } else {
                printInformations();
            }
        }
    };

    /** Listener that handles a click on an entry in the FusedVM View */
    private final ITimeGraphSelectionListener fSelListenerFusedVMView = new ITimeGraphSelectionListener() {

        @Override
        public void selectionChanged(TimeGraphSelectionEvent event) {
            ITimeGraphEntry entry = event.getSelection();
            if (entry instanceof FusedVMViewEntry) {
                FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                presentationProvider.setSelectedFusedVMViewEntry(entry);
            }

        }
    };

    /** Listener that handles a click on an entry in the Control Flow View */
    private final ISelectionListener fSelListenerControlFlowView = new ISelectionListener() {
        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            if (selection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) selection).getFirstElement();
                if (element instanceof ControlFlowEntry) {
                    FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                    ControlFlowEntry entry = (ControlFlowEntry) element;
                    String machineName = entry.getTrace().getName();
                    String threadName = entry.getName();
                    int threadID = entry.getThreadId();
                    presentationProvider.setSelectedControlFlowViewEntry(element);
                    presentationProvider.setSelectedMachine(machineName);
                    // TODO: Find a way to access to the id of the cpu running
                    // the process
                    presentationProvider.setSelectedThread(machineName, threadID, threadName);

                    updateButtonsSelection();
                    updateToolTipTexts();
                }
            }
        }
    };

    private final MouseWheelListener fWheelListener = new MouseWheelListener() {

        @Override
        public void mouseScrolled(MouseEvent e) {
            if ((e.stateMask & SWT.SHIFT) != 0) {
                FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                presentationProvider.modifySelectedThreadAlpha(e.count);
                presentationProvider.destroyTimeEventHighlight();
                refresh();
            }
        }
    };

    private Machine machineHierarchy;
    private HashMap<String, Machine> machines;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public FusedVirtualMachineView() {
        super(ID, new FusedVMViewPresentationProvider());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new FusedVMFilterLabelProvider());
        setEntryComparator(new FusedVMViewEntryComparator());
        registerListener();
        setAutoExpandLevel(1);

    }

    private static class FusedVMViewEntryComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            FusedVMViewEntry entry1 = (FusedVMViewEntry) o1;
            FusedVMViewEntry entry2 = (FusedVMViewEntry) o2;
            Type typeE1 = entry1.getType();
            Type typeE2 = entry2.getType();
            if ((typeE1 == Type.VM || typeE1 == Type.CONTAINER) && (typeE2 == Type.VM || typeE2 == Type.CONTAINER)) {
                /* sort trace entries alphabetically */
                return entry1.getName().compareTo(entry2.getName());
            }
            if (typeE1 == Type.NULL && typeE2 == Type.NULL) {
                if (entry1.getName() == Messages.FusedVMView_PhysicalCpusEntry) {
                    return -1;
                }
                if(entry2.getName() == Messages.FusedVMView_PhysicalCpusEntry) {
                    return 1;
                }
                if(entry1.getName() == Messages.FusedVMView_ContainersEntry) {
                    return 1;
                }
                if(entry2.getName() == Messages.FusedVMView_ContainersEntry) {
                    return -1;
                }
            }
            /* sort resource entries by their defined order */
            return entry1.compareTo(entry2);
        }
    }

    private static class FusedVMFilterLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            FusedVMViewEntry entry = (FusedVMViewEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            return ""; //$NON-NLS-1$
        }

    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected String getNextText() {
        return Messages.FusedVMView_nextResourceActionNameText;
    }

    @Override
    protected String getNextTooltip() {
        return Messages.FusedVMView_nextResourceActionToolTipText;
    }

    @Override
    protected String getPrevText() {
        return Messages.FusedVMView_previousResourceActionNameText;
    }

    @Override
    protected String getPrevTooltip() {
        return Messages.FusedVMView_previousResourceActionToolTipText;
    }

    @Override
    protected void buildEntryList(ITmfTrace trace, ITmfTrace parentTrace, final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }
        if (!(parentTrace instanceof VirtualMachineExperiment)) {
            return;
        }

        final ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(parentTrace, FusedVirtualMachineAnalysis.ID);
        if (ssq == null) {
            return;
        }
        Comparator<ITimeGraphEntry> comparator = new Comparator<ITimeGraphEntry>() {
            @Override
            public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
                return ((FusedVMViewEntry) o1).compareTo(o2);
            }
        };

        /* IF we don't wait we might don't see some machines */
        ssq.waitUntilBuilt();

        machines = new HashMap<>();
        machineHierarchy = createHierarchy(ssq);
        if (machineHierarchy == null) {
            return;
        }
        FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();

        /* All traces are highlighted by default. */
        /* Remove highlighted machines from other analysis. */
        presentationProvider.destroyHightlightedMachines();
        // TODO: do this part in createHierarchy
        for (String machineName : FusedVMInformationProvider.getMachinesTraced(ssq)) {
            ITmfStateValue machineType = FusedVMInformationProvider.getTypeMachine(ssq, machineName);
            if (machineType == null) {
                continue;
            }
            Machine machine = new Machine(machineName, FusedVMInformationProvider.getNbCPUs(ssq, machineName), machineType);
            /* Get all the containers for this machine */
            for (String containerID : FusedVMInformationProvider.getMachineContainers(ssq, machineName)) {
                machine.addContainer(Machine.createContainer(containerID, machine));
            }
            presentationProvider.getHighlightedMachines().put(machine.getMachineName(), machine);
        }

        Map<Integer, FusedVMViewEntry> entryMap = new HashMap<>();
        TimeGraphEntry traceEntry = null;

        long startTime = ssq.getStartTime();
        long start = startTime;
        setStartTime(Math.min(getStartTime(), startTime));
        boolean complete = false;
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }
            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
            long end = ssq.getCurrentEndTime();
            if (start == end && !complete) {
                // when complete execute one last time regardless of end time
                continue;
            }
            long endTime = end + 1;
            setEndTime(Math.max(getEndTime(), endTime));

            List<Integer> machinesQuarks = ssq.getQuarks(Attributes.MACHINES, "*"); //$NON-NLS-1$
            String hostName = null;
            for (int quark : machinesQuarks) {
                try {
                    if (ssq.querySingleState(trace.getStartTime().getValue(), quark).getStateValue().unboxInt() == StateValues.MACHINE_HOST) {
                        hostName = ssq.getAttributeName(quark);
                    }
                } catch (StateSystemDisposedException e) {
                    e.printStackTrace();
                }
            }

            if (traceEntry == null) {
                traceEntry = new FusedVMViewEntry(trace, hostName, startTime, endTime, Type.VM, 0);
                traceEntry.sortChildren(comparator);
                List<TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
                addToEntryList(parentTrace, ssq, entryList);
            } else {
                traceEntry.updateEndTime(endTime);
            }

            List<Integer> cpuQuarks = ssq.getQuarks(Attributes.CPUS, "*"); //$NON-NLS-1$
            createCpuEntriesWithQuark(trace, ssq, entryMap, traceEntry, startTime, endTime, cpuQuarks);

            /* Create entries for machines and containers */
            createMachineAndContainerEntries(trace, ssq, entryMap, machineHierarchy, traceEntry, startTime, endTime);

            if (parentTrace.equals(getTrace())) {
                refresh();
            }
            final List<? extends ITimeGraphEntry> traceEntryChildren = traceEntry.getChildren();
            final long resolution = Math.max(1, (endTime - ssq.getStartTime()) / getDisplayWidth());
            final long qStart = start;
            final long qEnd = end;
            queryFullStates(ssq, qStart, qEnd, resolution, monitor, new IQueryHandler() {
                @Override
                public void handle(List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState) {
                    for (ITimeGraphEntry child : traceEntryChildren) {
                        if (!populateEventsRecursively(fullStates, prevFullState, child).isOK()) {
                            return;
                        }
                    }
                }

                private IStatus populateEventsRecursively(@NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, ITimeGraphEntry entry) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    if (entry instanceof TimeGraphEntry) {
                        TimeGraphEntry timeGraphEntry = (TimeGraphEntry) entry;
                        List<ITimeEvent> eventList = getEventList(timeGraphEntry, ssq, fullStates, prevFullState, monitor);
                        if (eventList != null) {
                            for (ITimeEvent event : eventList) {
                                timeGraphEntry.addEvent(event);
                            }
                        }
                    }
                    for (ITimeGraphEntry child : entry.getChildren()) {
                        IStatus status = populateEventsRecursively(fullStates, prevFullState, child);
                        if (!status.isOK()) {
                            return status;
                        }
                    }
                    return Status.OK_STATUS;
                }
            });

            start = end;
        }

    }

    private static void createCpuEntriesWithQuark(@NonNull ITmfTrace trace, final ITmfStateSystem ssq, Map<Integer, FusedVMViewEntry> entryMap, TimeGraphEntry traceEntry, long startTime, long endTime, List<Integer> cpuQuarks) {
        Integer cpusQuark = ssq.getQuarks(Attributes.CPUS).get(0);
        FusedVMViewEntry physicalCpusEntry = entryMap.get(cpusQuark);
        if (physicalCpusEntry == null) {
            physicalCpusEntry = new FusedVMViewEntry(cpusQuark, trace, Messages.FusedVMView_PhysicalCpusEntry, startTime, endTime, Type.NULL, cpusQuark);
            entryMap.put(cpusQuark, physicalCpusEntry);
            traceEntry.addChild(physicalCpusEntry);
        } else {
            physicalCpusEntry.updateEndTime(endTime);
        }
        for (Integer cpuQuark : cpuQuarks) {
            final @NonNull String cpuName = ssq.getAttributeName(cpuQuark);
            int cpu = Integer.parseInt(cpuName);
            FusedVMViewEntry cpuEntry = entryMap.get(cpuQuark);
            if (cpuEntry == null) {
                cpuEntry = new FusedVMViewEntry(cpuQuark, trace, startTime, endTime, Type.CPU, cpu);
                entryMap.put(cpuQuark, cpuEntry);
                physicalCpusEntry.addChild(cpuEntry);
            } else {
                cpuEntry.updateEndTime(endTime);
            }
            List<Integer> irqQuarks = ssq.getQuarks(Attributes.CPUS, cpuName, Attributes.IRQS, "*"); //$NON-NLS-1$
            createCpuInterruptEntryWithQuark(trace, ssq, entryMap, startTime, endTime, physicalCpusEntry, cpuEntry, irqQuarks, Type.IRQ);
            List<Integer> softIrqQuarks = ssq.getQuarks(Attributes.CPUS, cpuName, Attributes.SOFT_IRQS, "*"); //$NON-NLS-1$
            createCpuInterruptEntryWithQuark(trace, ssq, entryMap, startTime, endTime, physicalCpusEntry, cpuEntry, softIrqQuarks, Type.SOFT_IRQ);
        }
    }

    private static void createMachineAndContainerEntries(@NonNull ITmfTrace trace, final ITmfStateSystem ssq, Map<Integer, FusedVMViewEntry> entryMap, Machine machine, TimeGraphEntry machineEntry, long startTime, long endTime) {
        Set<Machine> vms = machine.getVirtualMachines();
        Set<Machine> containers = machine.getContainers();
        Set<Processor> pcpus = machine.getPCpus();

        if (!vms.isEmpty()) {
            FusedVMViewEntry virtualMachinesEntry = entryMap.get(3 * vms.hashCode());
            if (virtualMachinesEntry == null) {
                virtualMachinesEntry = new FusedVMViewEntry(0, trace, Messages.FusedVMView_VirtualMachinesEntry, startTime, endTime, Type.NULL, 3 * vms.hashCode());
                entryMap.put(3 * vms.hashCode(), virtualMachinesEntry);
                machineEntry.addChild(virtualMachinesEntry);
            } else {
                virtualMachinesEntry.updateEndTime(endTime);
            }
            for (Machine vm : vms) {
                FusedVMViewEntry virtualMachineEntry = entryMap.get(vm.hashCode());
                if (virtualMachineEntry == null) {
                    virtualMachineEntry = new FusedVMViewEntry(0, trace, vm.getMachineName(), startTime, endTime, Type.VM, vm.hashCode());
                    entryMap.put(vm.hashCode(), virtualMachineEntry);
                    virtualMachinesEntry.addChild(virtualMachineEntry);
                } else {
                    virtualMachineEntry.updateEndTime(endTime);
                }
                createMachineAndContainerEntries(trace, ssq, entryMap, vm, virtualMachineEntry, startTime, endTime);
            }
        }

        if(!containers.isEmpty()) {
            FusedVMViewEntry containersEntry = entryMap.get(3 * containers.hashCode());
            if (containersEntry == null) {
                containersEntry = new FusedVMViewEntry(0, trace, Messages.FusedVMView_ContainersEntry, startTime, endTime, Type.NULL, 3 * containers.hashCode());
                entryMap.put(3 * containers.hashCode(), containersEntry);
                machineEntry.addChild(containersEntry);
            } else {
                containersEntry.updateEndTime(endTime);
            }
            for (Machine container : containers) {
                FusedVMViewEntry containerEntry = entryMap.get(container.hashCode());
                if (containerEntry == null) {
                    containerEntry = new FusedVMViewEntry(0, trace, container.getMachineName(), startTime, endTime, Type.CONTAINER, container.hashCode());
                    entryMap.put(container.hashCode(), containerEntry);
                    containersEntry.addChild(containerEntry);
                } else {
                    containerEntry.updateEndTime(endTime);
                }
                createMachineAndContainerEntries(trace, ssq, entryMap, container, containerEntry, startTime, endTime);
            }
        }

        if (!pcpus.isEmpty()) {
            FusedVMViewEntry pCpusEntry = entryMap.get(3 * pcpus.hashCode());
            if (pCpusEntry == null) {
                pCpusEntry = new FusedVMViewEntry(0, trace, Messages.FusedVMView_PhysicalCpusEntry, startTime, endTime, Type.NULL, 3 * pcpus.hashCode());
                entryMap.put(3 * pcpus.hashCode(), pCpusEntry);
                machineEntry.addChild(pCpusEntry);
            } else {
                pCpusEntry.updateEndTime(endTime);
            }
            for (Processor p : pcpus) {
                FusedVMViewEntry pCpuEntry = entryMap.get(p.hashCode());
                if (pCpuEntry == null) {
                    List<Integer> list = ssq.getQuarks(Attributes.CPUS, p.getNumber());
                    if (list.isEmpty()) {
                        return;
                    }
                    int pCpuQuark = list.get(0);
                    Type type = Type.NULL;
                    Type typeMachine = ((FusedVMViewEntry) machineEntry).getType();
                    if (typeMachine == Type.VM) {
                        type = Type.PCPU_VM;
                    } else if (typeMachine == Type.CONTAINER) {
                        type = Type.PCPU_CONTAINER;
                    }
                    pCpuEntry = new FusedVMViewEntry(pCpuQuark, trace, startTime, endTime, type, Integer.parseInt(p.getNumber()));
                    entryMap.put(p.hashCode(), pCpuEntry);
                    pCpusEntry.addChild(pCpuEntry);
                } else {
                    pCpuEntry.updateEndTime(endTime);
                }
            }
        }

    }

    /**
     * Create and add execution contexts to a cpu entry. Also creates an
     * aggregate entry in the root trace entry. The execution context is
     * basically what the cpu is doing in its execution stack. It can be in an
     * IRQ, Soft IRQ. MCEs, NMIs, Userland and Kernel execution is not yet
     * supported.
     *
     * @param trace
     *            the trace
     * @param ssq
     *            the state system
     * @param entryMap
     *            the entry map
     * @param startTime
     *            the start time in nanoseconds
     * @param endTime
     *            the end time in nanoseconds
     * @param traceEntry
     *            the trace timegraph entry
     * @param cpuEntry
     *            the cpu timegraph entry (the entry under the trace entry
     * @param childrenQuarks
     *            the quarks to add to cpu entry
     * @param type
     *            the type of entry being added
     */
    private static void createCpuInterruptEntryWithQuark(@NonNull ITmfTrace trace,
            final ITmfStateSystem ssq, Map<Integer, FusedVMViewEntry> entryMap,
            long startTime, long endTime,
            TimeGraphEntry traceEntry, FusedVMViewEntry cpuEntry,
            List<Integer> childrenQuarks, Type type) {
        for (Integer quark : childrenQuarks) {
            final @NonNull String resourceName = ssq.getAttributeName(quark);
            int resourceId = Integer.parseInt(resourceName);
            FusedVMViewEntry interruptEntry = entryMap.get(quark);
            if (interruptEntry == null) {
                interruptEntry = new FusedVMViewEntry(quark, trace, startTime, endTime, type, resourceId);
                entryMap.put(quark, interruptEntry);
                cpuEntry.addChild(interruptEntry);
            } else {
                interruptEntry.updateEndTime(endTime);
            }
        }
    }

    @Override
    protected @Nullable List<ITimeEvent> getEventList(@NonNull TimeGraphEntry entry, ITmfStateSystem ssq,
            @NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull IProgressMonitor monitor) {
        FusedVMViewEntry fusedVMViewEntry = (FusedVMViewEntry) entry;
        int quark = fusedVMViewEntry.getQuark();

        if (fusedVMViewEntry.getType().equals(Type.CPU)) {
            return createCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.CPU);
        } else if ((fusedVMViewEntry.getType().equals(Type.IRQ) || fusedVMViewEntry.getType().equals(Type.SOFT_IRQ)) && (quark >= 0)) {
            return createIrqEventsList(entry, fullStates, prevFullState, monitor, quark);
        } else if (fusedVMViewEntry.getType().equals(Type.PCPU_VM)) {
            return createCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.PCPU_VM);
        } else if (fusedVMViewEntry.getType().equals(Type.PCPU_CONTAINER)) {
            return createCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.PCPU_CONTAINER);
        }

        return null;
    }

    private List<ITimeEvent> createCpuEventsList(TimeGraphEntry entry, ITmfStateSystem ssq, List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState, IProgressMonitor monitor, int quark, Type type) {
        List<ITimeEvent> eventList;
        int statusQuark;
        int machineQuark;
        int currentThreadQuark;
        String machineName = null;
        try {
            statusQuark = ssq.getQuarkRelative(quark, Attributes.STATUS);
            machineQuark = ssq.getQuarkRelative(quark, Attributes.MACHINE_NAME);
            currentThreadQuark = ssq.getQuarkRelative(quark, Attributes.CURRENT_THREAD);
        } catch (AttributeNotFoundException e) {
            /*
             * The sub-attribute "status" is not available. May happen if the
             * trace does not have sched_switch events enabled.
             */
            return null;
        }
        eventList = new ArrayList<>(fullStates.size());
        /*
         * In order to make the filter work, a time event must be generated for
         * each change of cpu status, current thread or current machine.
         */
        ITmfStateInterval lastStatusInterval = prevFullState == null || statusQuark >= prevFullState.size() ? null : prevFullState.get(statusQuark);
        ITmfStateInterval lastMachineInterval = prevFullState == null || machineQuark >= prevFullState.size() ? null : prevFullState.get(machineQuark);
        ITmfStateInterval lastCurrentThreadInterval = prevFullState == null || currentThreadQuark >= prevFullState.size() ? null : prevFullState.get(currentThreadQuark);
        long lastStatusStartTime = lastStatusInterval == null ? -1 : lastStatusInterval.getStartTime();
        long lastStatusEndTime = lastStatusInterval == null ? -1 : lastStatusInterval.getEndTime() + 1;
        long lastMachineStartTime = lastMachineInterval == null ? -1 : lastMachineInterval.getStartTime();
        long lastMachineEndTime = lastMachineInterval == null ? -1 : lastMachineInterval.getEndTime() + 1;
        long lastCurrentThreadStartTime = lastCurrentThreadInterval == null ? -1 : lastCurrentThreadInterval.getStartTime();
        long lastCurrentThreadEndTime = lastCurrentThreadInterval == null ? -1 : lastCurrentThreadInterval.getEndTime() + 1;
        /* So we intersect the three intervals. */
        long lastStartTime = max(lastStatusStartTime, max(lastMachineStartTime, lastCurrentThreadStartTime));
        long lastEndTime = min(lastStatusEndTime, min(lastMachineEndTime, lastCurrentThreadEndTime));
        for (List<ITmfStateInterval> fullState : fullStates) {
            if (monitor.isCanceled()) {
                return null;
            }
            if (statusQuark >= fullState.size()) {
                /* No information on this CPU (yet?), skip it for now */
                continue;
            }
            ITmfStateInterval statusInterval = fullState.get(statusQuark);
            ITmfStateInterval machineInterval = fullState.get(machineQuark);
            ITmfStateInterval currentThreadInterval = fullState.get(currentThreadQuark);

            if (type.equals(Type.CPU)) {
                /* Just keep going */
            } else if (type.equals(Type.PCPU_VM)) {
                // TODO: support vm's vms
                machineName = entry.getParent().getParent().getName();
//                if (!machineInterval.getStateValue().unboxStr().equals(machineName)) {
                if (!isInsideVM(machineInterval.getStateValue().unboxStr(), machineName)) {
                    /* Skip that interval, it's not related to the machine */
                    continue;
                }
            } else if (type.equals(Type.PCPU_CONTAINER)) {
                /* Get the entry of the machine containing the container */
                FusedVMViewEntry machineEntry = (FusedVMViewEntry) entry.getParent();
                while (machineEntry.getType() != Type.VM) {
                    machineEntry = (FusedVMViewEntry) machineEntry.getParent();
                }
                machineName = machineEntry.getName();
                if (machineName == null || !machineInterval.getStateValue().unboxStr().equals(machineName)) {
                    /* Other machine, skip the interval */
                    continue;
                }
                String containerID = entry.getParent().getParent().getName();
                if (containerID == null) {
                    continue;
                }
                int containerQuark = FusedVMInformationProvider.getContainerQuark(ssq, machineName, containerID);
                int threadID = currentThreadInterval.getStateValue().unboxInt();
                List<Integer> threadsQuarks = ssq.getQuarks(containerQuark, Attributes.THREADS, "*"); //$NON-NLS-1$
                boolean foundThread = false;
                for (Integer threadQuark : threadsQuarks) {
                    if (Integer.parseInt(ssq.getAttributeName(threadQuark)) == threadID) {
                        foundThread = true;
                        break;
                    }
                }
                if (!foundThread) {
                    /* Thread not inside container, skip interval */
                    continue;
                }
            }

            int status = statusInterval.getStateValue().unboxInt();
            long time = max(statusInterval.getStartTime(), max(machineInterval.getStartTime(), currentThreadInterval.getStartTime()));
            long duration = min(statusInterval.getEndTime(), min(machineInterval.getEndTime(), currentThreadInterval.getEndTime())) - time + 1;
            if (time == lastStartTime) {
                continue;
            }
            if (!statusInterval.getStateValue().isNull()) {
                if (lastEndTime != time && lastEndTime != -1) {
                    eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                }
                eventList.add(new TimeEvent(entry, time, duration, status));
            } else {
                eventList.add(new NullTimeEvent(entry, time, duration));
            }
            lastStartTime = time;
            lastEndTime = time + duration;
        }
        return eventList;
    }

    private static long min(long a, long b) {
        if (a == -1) {
            return b;
        } else if (b == -1) {
            return a;
        } else {
            return a < b ? a : b;
        }
    }

    private static long max(long a, long b) {
        if (a == -1) {
            return b;
        } else if (b == -1) {
            return a;
        } else {
            return a < b ? b : a;
        }
    }

    /**
     * Return true if machine1 is a submachine of machine2
     *
     * @param machine1
     * @param machine2
     * @return
     */
    private boolean isInsideVM(String machine1, String machine2) {
        if (machine1.equals(machine2)) {
            return true;
        }
        Machine m2 = machines.get(machine2);
        if (m2 == null) {
            return false;
        }
        for (Machine child : m2.getVirtualMachines()) {
            if (isInsideVM(machine1, child.getMachineName())) {
                return true;
            }
        }
        return false;
    }

    private static List<ITimeEvent> createIrqEventsList(TimeGraphEntry entry, List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState, IProgressMonitor monitor, int quark) {
        List<ITimeEvent> eventList;
        eventList = new ArrayList<>(fullStates.size());
        ITmfStateInterval lastInterval = prevFullState == null || quark >= prevFullState.size() ? null : prevFullState.get(quark);
        long lastStartTime = lastInterval == null ? -1 : lastInterval.getStartTime();
        long lastEndTime = lastInterval == null ? -1 : lastInterval.getEndTime() + 1;
        boolean lastIsNull = lastInterval == null ? false : lastInterval.getStateValue().isNull();
        for (List<ITmfStateInterval> fullState : fullStates) {
            if (monitor.isCanceled()) {
                return null;
            }
            if (quark >= fullState.size()) {
                /* No information on this IRQ (yet?), skip it for now */
                continue;
            }
            ITmfStateInterval irqInterval = fullState.get(quark);
            long time = irqInterval.getStartTime();
            long duration = irqInterval.getEndTime() - time + 1;
            if (time == lastStartTime) {
                continue;
            }
            if (!irqInterval.getStateValue().isNull()) {
                int cpu = irqInterval.getStateValue().unboxInt();
                eventList.add(new TimeEvent(entry, time, duration, cpu));
                lastIsNull = false;
            } else {
                if (lastEndTime != time && lastIsNull) {
                    /*
                     * This is a special case where we want to show IRQ_ACTIVE
                     * state but we don't know the CPU (it is between two null
                     * samples)
                     */
                    eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime, -1));
                }
                eventList.add(new NullTimeEvent(entry, time, duration));
                lastIsNull = true;
            }
            lastStartTime = time;
            lastEndTime = time + duration;
        }
        return eventList;
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        super.fillLocalToolBar(manager);
        IAction selectMachineAction = getSelectMachineAction();
        selectMachineAction.setText(Messages.FusedVMView_selectMachineText);
        selectMachineAction.setToolTipText(Messages.FusedVMView_selectMachineText);
        manager.add(selectMachineAction);
        manager.add(new Separator());
        manager.add(fHighlightMachine);
        manager.add(fHighlightCPU);
        manager.add(fHighlightProcess);
        manager.add(fHighlightContainer);


    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        getTimeGraphViewer().addTimeListener(fTimeListenerFusedVMView);
        getTimeGraphViewer().addSelectionListener(fSelListenerFusedVMView);

        getTimeGraphViewer().getTimeGraphControl().addMouseWheelListener(fWheelListener);
    }

    /**
     * Gets the beginning of the selected time
     *
     * @return the beginning of the selected time
     */
    public long getBeginSelectedTime() {
        return beginSelectedTime;
    }

    /**
     * Sets the beginning of the selected time
     *
     * @param begin
     *            the beginning of the selected time
     */
    public void setBeginSelectedTime(long begin) {
        beginSelectedTime = begin;
    }

    /**
     * Gets the end of the selected time
     *
     * @return the end of the selected time
     */
    public long getEndSelectedTime() {
        return endSelectedTime;
    }

    /**
     * Sets the end of the selected time
     *
     * @param end
     *            the end of the selected time
     */
    public void setEndSelectedTime(long end) {
        endSelectedTime = end;
    }

    /**
     * Getter to the presentation provider
     *
     * @return the FusedVMViewProvider
     */
    public FusedVMViewPresentationProvider getFusedVMViewPresentationProvider() {
        ITimeGraphPresentationProvider2 pp = getPresentationProvider();
        if (!(pp instanceof FusedVMViewPresentationProvider)) {
            return null;
        }
        return (FusedVMViewPresentationProvider) pp;
    }

    private void printInformations() {
        long begin = getBeginSelectedTime();
        long end = getEndSelectedTime();

        System.out.println("Begin time: " + Utils.formatTime(begin, TimeFormat.CALENDAR, Resolution.NANOSEC)); //$NON-NLS-1$
        System.out.println("End time: " + Utils.formatTime(end, TimeFormat.CALENDAR, Resolution.NANOSEC)); //$NON-NLS-1$
        System.out.println();

    }

    /**
     * Registers the listener that handles the click on a Control Flow View
     * entry
     */
    private void registerListener() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        IWorkbench wb = PlatformUI.getWorkbench();
        if (wb == null) {
            return;
        }
        IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
        if (wbw == null) {
            return;
        }
        final IWorkbenchPage activePage = wbw.getActivePage();
        if (activePage == null) {
            return;
        }

        /* Add the listener to the control flow view */
        IViewPart view = activePage.findView(ControlFlowView.ID);
        if (view != null) {
            view.getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(fSelListenerControlFlowView);
        }
    }

    /**
     * Updates the tooltip text of the buttons so it corresponds to the machine,
     * cpu and process selected
     */
    private void updateToolTipTexts() {
        FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
        fHighlightMachine.setToolTipText(presentationProvider.getSelectedMachine());
        fHighlightCPU.setToolTipText(Integer.toString((presentationProvider.getSelectedCpu())));
        fHighlightProcess.setToolTipText(Messages.FusedVMView_ButtonProcessSelected + ": " + //$NON-NLS-1$
                presentationProvider.getSelectedThreadName() + "\n" + //$NON-NLS-1$
                Messages.FusedVMView_ButtonHoverProcessSelectedTID + ": " + //$NON-NLS-1$
                Integer.toString(presentationProvider.getSelectedThreadID()));
        fHighlightContainer.setToolTipText(presentationProvider.getSelectedContainer());
    }

    /**
     * Sets the checked state of the buttons
     */
    private void updateButtonsSelection() {
        FusedVMViewPresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
        Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
        Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
        if (machine == null) {
            return;
        }

        fHighlightMachine.setChecked(machine.isHighlighted());
        fHighlightCPU.setChecked(machine.isCpuHighlighted(presentationProvider.getSelectedCpu()));
        fHighlightProcess.setChecked(presentationProvider.isThreadSelected(machine.getMachineName(), presentationProvider.getSelectedThreadID()));
        fHighlightContainer.setChecked(machine.isContainerHighlighted(presentationProvider.getSelectedContainer()));
    }

    /**
     * Get the select machine action.
     * @return The select machine action
     */
    public Action getSelectMachineAction() {
        if (fSelectMachineAction == null) {
            fSelectMachineAction = new Action() {
                @Override
                public void run() {
                    selectMachine();
                    FusedVMViewPresentationProvider presentationProvider = (FusedVMViewPresentationProvider) getPresentationProvider();
                    presentationProvider.destroyTimeEventHighlight();
                    redraw();
                }
            };
            fSelectMachineAction.setText(Messages.FusedVMView_SelectMachineActionNameText);
            fSelectMachineAction.setToolTipText(Messages.FusedVMView_SelectMachineActionToolTipText);
        }

        return fSelectMachineAction;
    }

    /**
     * Method called when the user clicks on the select machine button.
     */
    public void selectMachine() {
        Control dataViewer = getTimeGraphViewer().getControl();
        if (dataViewer == null || dataViewer.isDisposed()) {
            return;
        }

        SelectMachineDialog.open(dataViewer.getShell(), getFusedVMViewPresentationProvider());
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return Collections.singleton(trace);
    }

    private Machine createHierarchy(@NonNull ITmfStateSystem ssq) {
        /* Separate host from guests */
        Machine host = null;
        List<Machine> guests = new LinkedList<>();
        for (String machineName : FusedVMInformationProvider.getMachinesTraced(ssq)) {
            ITmfStateValue typeMachine = FusedVMInformationProvider.getTypeMachine(ssq, machineName);

            if (typeMachine != null) {
                if ((typeMachine.unboxInt() & StateValues.MACHINE_GUEST) == StateValues.MACHINE_GUEST) {
                    Machine machine = new Machine(machineName, typeMachine, FusedVMInformationProvider.getPCpusUsedByMachine(ssq, machineName));
                    machines.put(machine.getMachineName(), machine);
                    guests.add(machine);
                } else if (typeMachine.unboxInt() == StateValues.MACHINE_HOST) {
                    Machine machine = new Machine(machineName, typeMachine);
                    machines.put(machine.getMachineName(), machine);
                    host = machine;
                }
            }
        }
        if (host == null) {
            return null;
        }
        /* Complete construction for the host*/
        createContainersHierarchyForMachine(ssq, host);
        createMachineHierarchy(ssq, host, guests);
        /* Create container hierarchy for guests and add them to the host */
        for (Machine guest : guests) {
            createContainersHierarchyForMachine(ssq, guest);
        }
        return host;
    }

    private static void createMachineHierarchy(@NonNull ITmfStateSystem ssq, Machine host, List<Machine> guests) {
        for (Machine m : guests) {
            String parentName = FusedVMInformationProvider.getParentMachineName(ssq, m.getMachineName());
            if (parentName.equals(host.getMachineName())){
                m.setHost(host);
                host.addVirtualMachine(m);
            }
            for (Machine m2 : guests) {
                parentName = FusedVMInformationProvider.getParentMachineName(ssq, m2.getMachineName());
                if (parentName.equals(m.getMachineName())){
                    m2.setHost(m);
                    m.addVirtualMachine(m2);
                }
            }
        }
    }

    private static void createContainersHierarchyForMachine(@NonNull ITmfStateSystem ssq, Machine m) {
        String machineName = m.getMachineName();
        if (machineName == null) {
            return;
        }
        List<Integer> containersQuarks = FusedVMInformationProvider.getMachineContainersQuarks(ssq, machineName);
        /* Look for not nested containers */
        for (Integer quark : containersQuarks) {
            Long parentContainer = FusedVMInformationProvider.getParentContainer(ssq, quark);
            /* TODO: Externalize the root namespace ID */
            if (parentContainer.toString().equals("4026531836")) { //$NON-NLS-1$
                String containerName = ssq.getAttributeName(quark);
                List<String> pCpus = FusedVMInformationProvider.getPCpusUsedByContainer(ssq, quark);
                Machine container = new Machine(containerName, StateValues.MACHINE_CONTAINER_VALUE, pCpus);
                m.addContainer(container);
                /* Continue construction for these containers */
                createContainersHierarchyForContainer(ssq, container, containersQuarks);
            }
        }
    }

    private static void createContainersHierarchyForContainer(@NonNull ITmfStateSystem ssq, Machine container, List<Integer> containersQuarks) {
        Long containerName = Long.parseLong(container.getMachineName());
        for (int quark : containersQuarks) {
            if (FusedVMInformationProvider.getParentContainer(ssq, quark).equals(containerName)) {
                /* We found a child */
                String childName = ssq.getAttributeName(quark);
                List<String> pCpus = FusedVMInformationProvider.getPCpusUsedByContainer(ssq, quark);
                Machine child = new Machine(childName, StateValues.MACHINE_CONTAINER_VALUE, pCpus);
                container.addContainer(child);
                /* Look for child's childs */
                createContainersHierarchyForContainer(ssq, child, containersQuarks);
            }
        }
    }
}
