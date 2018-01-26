/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfThreadSelectedSignal;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVMInformationProvider;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVirtualMachineAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualMachineModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.trace.VirtualMachineExperiment;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources.VirtualResourceEntry.Type;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
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

/**
 * A view showing the virtual resources / CPUs for traces and their contained
 * traces and containers
 *
 * @author Cedric Biancheri
 * @author Geneviève Bastien
 */
public class VirtualResourcesView extends AbstractStateSystemTimeGraphView {

    /** View ID. */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.virtualresources"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.FusedVMView_stateTypeName
    };

    /** Button to select a machine to highlight */
    private final Action fHighlightMachine = new Action(Messages.FusedVMView_ButtonMachineSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
//            VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
//            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
//            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
//            if (machine == null) {
//                setChecked(!isChecked());
//                return;
//            }
//            machine.setHighlightedWithAllCpu(isChecked());
//            machine.setHighlightedWithAllContainers(isChecked());
//            fHighlightCPU.setChecked(isChecked());
//            fHighlightContainer.setChecked(isChecked());
//            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a CPU to highlight */
    private final Action fHighlightCPU = new Action(Messages.FusedVMView_ButtonCPUSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
//            VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
//            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
//            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
//            if (machine == null) {
//                setChecked(!isChecked());
//                return;
//            }
//            machine.setHighlightedCpu(presentationProvider.getSelectedCpu(), isChecked());
//            fHighlightMachine.setChecked(machine.isOneCpuHighlighted());
//            presentationProvider.destroyTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a process to highlight */
    private final Action fHighlightProcess = new Action(Messages.FusedVMView_ButtonProcessSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
            VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
//            if (isChecked()) {
//                presentationProvider.addHighlightedThread();
//            } else {
//                presentationProvider.removeHighlightedThread();
//            }
            presentationProvider.resetTimeEventHighlight();
            refresh();
        }
    };

    /** Button to select a container to highlight */
    private final Action fHighlightContainer = new Action(Messages.FusedVMView_ButtonContainerSelected, IAction.AS_CHECK_BOX) {
        @Override
        public void run() {
//            VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
//            Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
//            Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
//            String container = presentationProvider.getSelectedContainer();
//            if (machine == null) {
//                setChecked(!isChecked());
//                return;
//            }
//            machine.setHighlightedContainer(container, isChecked());
//            fHighlightMachine.setChecked(machine.isOneContainerHighlighted());
//            presentationProvider.destroyTimeEventHighlight();
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
                VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                Object o = presentationProvider.getSelectedFusedVMViewEntry();
                if (o == null) {
                    return;
                }
                if (!(o instanceof VirtualResourceEntry)) {
                    return;
                }
                VirtualResourceEntry entry = (VirtualResourceEntry) o;
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
                    int machineNameQuark = ssq.getQuarkRelative(cpuQuark, FusedAttributes.MACHINE_NAME);
                    interval = ssq.querySingleState(begin, machineNameQuark);
                    ITmfStateValue value = interval.getStateValue();
                    machineName = value.unboxStr();
                    presentationProvider.setSelectedMachine(machineName);

                    int threadQuark = ssq.getQuarkRelative(cpuQuark, FusedAttributes.CURRENT_THREAD);
                    interval = ssq.querySingleState(begin, threadQuark);
                    value = interval.getStateValue();
                    int threadID = value.unboxInt();

                    presentationProvider.setSelectedThread(new HostThread(machineName, threadID));

                    int conditionQuark = ssq.getQuarkRelative(cpuQuark, FusedAttributes.CONDITION);
                    interval = ssq.querySingleState(begin, conditionQuark);
                    value = interval.getStateValue();
                    int condition = value.unboxInt();
                    List<Integer> list = ssq.getQuarks(cpuQuark, FusedAttributes.VIRTUAL_CPU);
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
            if (entry instanceof VirtualResourceEntry) {
                VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                presentationProvider.setSelectedFusedVMViewEntry(entry);
            }

        }
    };

    private final MouseWheelListener fWheelListener = new MouseWheelListener() {

        @Override
        public void mouseScrolled(MouseEvent e) {
            if ((e.stateMask & SWT.SHIFT) != 0) {
                VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
                presentationProvider.modifySelectedThreadAlpha(e.count);
                presentationProvider.resetTimeEventHighlight();
                refresh();
            }
        }
    };

    private final Map<ITmfTrace, Machine> fPhysicalMachines = new HashMap<>();
    private Map<String, Machine> fMachines;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public VirtualResourcesView() {
        super(ID, new VirtualResourcePresentationProvider());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new FusedVMFilterLabelProvider());
        setEntryComparator(new FusedVMViewEntryComparator());
    }

    private static class FusedVMViewEntryComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            VirtualResourceEntry entry1 = (VirtualResourceEntry) o1;
            VirtualResourceEntry entry2 = (VirtualResourceEntry) o2;
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
                if (entry2.getName() == Messages.FusedVMView_PhysicalCpusEntry) {
                    return 1;
                }
                if (entry1.getName() == Messages.FusedVMView_ContainersEntry) {
                    return 1;
                }
                if (entry2.getName() == Messages.FusedVMView_ContainersEntry) {
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
            VirtualResourceEntry entry = (VirtualResourceEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            return ""; //$NON-NLS-1$
        }

    }

    private static final Comparator<ITimeGraphEntry> ENTRY_COMPARATOR = new Comparator<ITimeGraphEntry>() {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (!((o1 instanceof VirtualResourceEntry) && (o2 instanceof VirtualResourceEntry))) {
                return 0;
            }
            return ((VirtualResourceEntry) o1).compareTo(o2);
        }
    };

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

        /* if we don't wait we might don't see some machines */
        /* TODO: make the view built incrementally with the analysis */
        ssq.waitUntilBuilt();

        /* All machines should are highlighted by default. */
        /* Remove highlighted machines from other analysis. */
        VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
        presentationProvider.setSelectedElements(Collections.emptySet());

        fMachines = new HashMap<>();
        // TODO: In an experiment, there can be multiple host machines
        Machine physicalMachine = createHierarchy(ssq);

        if (physicalMachine == null) {
            return;
        }
        fPhysicalMachines.put(trace, physicalMachine);

        TimeGraphEntry traceEntry = null;

        long startTime = ssq.getStartTime();
        long start = startTime;
        setStartTime(Math.min(getStartTime(), startTime));

        // FIXME: Here would start the while(!complete) loop
        if (monitor.isCanceled()) {
            return;
        }
        long end = ssq.getCurrentEndTime();
        long endTime = end + 1;
        setEndTime(Math.max(getEndTime(), endTime));

        List<Integer> machinesQuarks = ssq.getQuarks(FusedAttributes.HOSTS, "*"); //$NON-NLS-1$
        String hostName = null;
        for (int quark : machinesQuarks) {
            try {
                if (ssq.querySingleState(trace.getStartTime().getValue(), quark).getStateValue().unboxInt() == StateValues.MACHINE_HOST) {
                    hostName = ssq.getAttributeName(quark);
                }
            } catch (StateSystemDisposedException e) {
                return;
            }
        }

        traceEntry = new VirtualResourceEntry(trace, hostName, startTime, endTime, Type.VM, 0);
        traceEntry.sortChildren(ENTRY_COMPARATOR);
        List<@NonNull TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
        addToEntryList(parentTrace, ssq, entryList);

        createPhysicalCpuEntries(trace, ssq, traceEntry, startTime, endTime);

        /* Create entries for machines and containers */
        createMachineAndContainerEntries(trace, ssq, physicalMachine, traceEntry, startTime, endTime);

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

    private void createPhysicalCpuEntries(@NonNull ITmfTrace trace, final ITmfStateSystem ssq, TimeGraphEntry traceEntry, long startTime, long endTime) {
        List<Integer> cpuQuarks = ssq.getQuarks(FusedAttributes.CPUS, "*"); //$NON-NLS-1$
        int cpusQuark = ssq.optQuarkAbsolute(FusedAttributes.CPUS);
        VirtualResourceEntry physicalCpusEntry = new VirtualResourceEntry(cpusQuark, trace, Messages.FusedVMView_PhysicalCpusEntry, startTime, endTime, Type.NULL, cpusQuark);
        traceEntry.addChild(physicalCpusEntry);

        for (Integer cpuQuark : cpuQuarks) {
            String cpuName = ssq.getAttributeName(cpuQuark);
            int cpu = Integer.parseInt(cpuName);
            VirtualResourceEntry cpuEntry = new VirtualResourceEntry(cpuQuark, trace, startTime, endTime, Type.CPU, cpu);
            physicalCpusEntry.addChild(cpuEntry);

            List<Integer> irqQuarks = ssq.getQuarks(FusedAttributes.CPUS, cpuName, FusedAttributes.IRQS, "*"); //$NON-NLS-1$
            createCpuInterruptEntryWithQuark(trace, ssq, startTime, endTime, cpuEntry, irqQuarks, Type.IRQ);
            List<Integer> softIrqQuarks = ssq.getQuarks(FusedAttributes.CPUS, cpuName, FusedAttributes.SOFT_IRQS, "*"); //$NON-NLS-1$
            createCpuInterruptEntryWithQuark(trace, ssq, startTime, endTime, cpuEntry, softIrqQuarks, Type.SOFT_IRQ);

            Display.getDefault().asyncExec(() -> {
                getTimeGraphViewer().setExpandedState(cpuEntry, false);
            });
        }
    }

    private void createMachineAndContainerEntries(@NonNull ITmfTrace trace, final ITmfStateSystem ssq, Machine machine, TimeGraphEntry parentEntry, long startTime, long endTime) {
        Machine physicalMachine = fPhysicalMachines.get(trace);
        Collection<Machine> vms = machine.getVirtualMachines();
        Collection<Machine> containers = machine.getContainers();
        Collection<Processor> pcpus = machine.getPhysicalCpus();

        if (!vms.isEmpty()) {
            VirtualResourceEntry virtualMachinesEntry = new VirtualResourceEntry(0, trace, Messages.FusedVMView_VirtualMachinesEntry, startTime, endTime, Type.NULL, 3 * vms.hashCode());
            parentEntry.addChild(virtualMachinesEntry);

            for (Machine vm : vms) {
                VirtualResourceEntry virtualMachineEntry = new VirtualResourceEntry(0, trace, vm.getMachineName(), startTime, endTime, Type.VM, vm.hashCode());
                virtualMachinesEntry.addChild(virtualMachineEntry);
                createMachineAndContainerEntries(trace, ssq, vm, virtualMachineEntry, startTime, endTime);
            }
        }

        if (!containers.isEmpty()) {
            VirtualResourceEntry containersEntry = new VirtualResourceEntry(0, trace, Messages.FusedVMView_ContainersEntry, startTime, endTime, Type.NULL, 3 * containers.hashCode());
            parentEntry.addChild(containersEntry);

            for (Machine container : containers) {
                VirtualResourceEntry containerEntry = new VirtualResourceEntry(0, trace, container.getMachineName(), startTime, endTime, Type.CONTAINER, container.hashCode());
                containersEntry.addChild(containerEntry);
                createMachineAndContainerEntries(trace, ssq, container, containerEntry, startTime, endTime);
            }
        }

        if (!pcpus.isEmpty() && (machine != physicalMachine)) {
            VirtualResourceEntry pCpusEntry = new VirtualResourceEntry(0, trace, Messages.FusedVMView_PhysicalCpusEntry, startTime, endTime, Type.NULL, 3 * pcpus.hashCode());
            parentEntry.addChild(pCpusEntry);

            for (Processor p : pcpus) {
                List<Integer> list = ssq.getQuarks(FusedAttributes.CPUS, String.valueOf(p.getNumber()));
                if (list.isEmpty()) {
                    return;
                }
                int pCpuQuark = list.get(0);
                Type type = Type.NULL;
                Type typeMachine = ((VirtualResourceEntry) parentEntry).getType();
                if (typeMachine == Type.VM) {
                    type = Type.PCPU_VM;
                } else if (typeMachine == Type.CONTAINER) {
                    type = Type.PCPU_CONTAINER;
                }
                VirtualResourceEntry pCpuEntry = new VirtualResourceEntry(pCpuQuark, trace, startTime, endTime, type, p.getNumber());
                pCpusEntry.addChild(pCpuEntry);
            }
        }
    }

    /*
     * Create and add execution contexts to a cpu entry. Also creates an
     * aggregate entry in the root trace entry. The execution context is
     * basically what the cpu is doing in its execution stack. It can be in an
     * IRQ, Soft IRQ. MCEs, NMIs, Userland and Kernel execution is not yet
     * supported.
     */
    private static void createCpuInterruptEntryWithQuark(@NonNull ITmfTrace trace,
            final ITmfStateSystem ssq,
            long startTime, long endTime, VirtualResourceEntry cpuEntry,
            List<Integer> childrenQuarks, Type type) {
        for (Integer quark : childrenQuarks) {
            final @NonNull String resourceName = ssq.getAttributeName(quark);
            int resourceId = Integer.parseInt(resourceName);
            VirtualResourceEntry interruptEntry = new VirtualResourceEntry(quark, trace, startTime, endTime, type, resourceId);
            cpuEntry.addChild(interruptEntry);
        }
    }

    @Override
    protected @Nullable List<ITimeEvent> getEventList(@NonNull TimeGraphEntry entry, ITmfStateSystem ssq,
            @NonNull List<List<ITmfStateInterval>> fullStates, @Nullable List<ITmfStateInterval> prevFullState, @NonNull IProgressMonitor monitor) {
        VirtualResourceEntry fusedVMViewEntry = (VirtualResourceEntry) entry;
        int quark = fusedVMViewEntry.getQuark();

        if (fusedVMViewEntry.getType().equals(Type.CPU)) {
            return getCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.CPU);
        } else if ((fusedVMViewEntry.getType().equals(Type.IRQ) || fusedVMViewEntry.getType().equals(Type.SOFT_IRQ)) && (quark >= 0)) {
            return getIrqEventsList(entry, fullStates, prevFullState, monitor, quark);
        } else if (fusedVMViewEntry.getType().equals(Type.PCPU_VM)) {
            return getCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.PCPU_VM);
        } else if (fusedVMViewEntry.getType().equals(Type.PCPU_CONTAINER)) {
            return getCpuEventsList(entry, ssq, fullStates, prevFullState, monitor, quark, Type.PCPU_CONTAINER);
        }

        return null;
    }

    private List<ITimeEvent> getCpuEventsList(TimeGraphEntry entry, ITmfStateSystem ssq, List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState, IProgressMonitor monitor, int quark, Type type) {
        List<ITimeEvent> eventList;
        int statusQuark;
        int machineQuark;
        int currentThreadQuark;
        String machineName = null;
        try {
            statusQuark = ssq.getQuarkRelative(quark, FusedAttributes.STATUS);
            machineQuark = ssq.getQuarkRelative(quark, FusedAttributes.MACHINE_NAME);
            currentThreadQuark = ssq.getQuarkRelative(quark, FusedAttributes.CURRENT_THREAD);
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
        long lastStatusEndTime = lastStatusInterval == null ? Long.MAX_VALUE : lastStatusInterval.getEndTime() + 1;
        long lastMachineStartTime = lastMachineInterval == null ? -1 : lastMachineInterval.getStartTime();
        long lastMachineEndTime = lastMachineInterval == null ? Long.MAX_VALUE : lastMachineInterval.getEndTime() + 1;
        long lastCurrentThreadStartTime = lastCurrentThreadInterval == null ? -1 : lastCurrentThreadInterval.getStartTime();
        long lastCurrentThreadEndTime = lastCurrentThreadInterval == null ? Long.MAX_VALUE : lastCurrentThreadInterval.getEndTime() + 1;
        /* So we intersect the three intervals. */
        long lastStartTime = Math.max(lastStatusStartTime, Math.max(lastMachineStartTime, lastCurrentThreadStartTime));
        long lastEndTime = Math.min(lastStatusEndTime, Math.min(lastMachineEndTime, lastCurrentThreadEndTime));
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
                // if
                // (!machineInterval.getStateValue().unboxStr().equals(machineName))
                // {
                if (!isInsideVM(machineInterval.getStateValue().unboxStr(), machineName)) {
                    /* Skip that interval, it's not related to the machine */
                    continue;
                }
            } else if (type.equals(Type.PCPU_CONTAINER)) {
                /* Get the entry of the machine containing the container */
                VirtualResourceEntry machineEntry = (VirtualResourceEntry) entry.getParent();
                if (machineEntry == null) {
                    continue;
                }
                while (machineEntry.getType() != Type.VM) {
                    machineEntry = (VirtualResourceEntry) machineEntry.getParent();
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
                if (containerQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    Activator.getDefault().logWarning("Container quark not found for " + containerID + " in machine " + machineName + ". This shouldn't happen."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    break;
                }
                int threadID = currentThreadInterval.getStateValue().unboxInt();
                List<Integer> threadsQuarks = ssq.getQuarks(containerQuark, FusedAttributes.THREADS, "*"); //$NON-NLS-1$
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
            long time = Math.max(statusInterval.getStartTime(), Math.max(machineInterval.getStartTime(), currentThreadInterval.getStartTime()));
            long duration = Math.min(statusInterval.getEndTime(), Math.min(machineInterval.getEndTime(), currentThreadInterval.getEndTime())) - time + 1;
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

    /**
     * Return true if machine1 is a submachine of machine2
     *
     * @param machine1
     * @param machine2
     * @return
     */
    private boolean isInsideVM(String machine1, String machine2) {
        Machine m2 = fMachines.get(machine2);
        if (m2 == null) {
            return false;
        }
        String machine2Host = m2.getHostId();
        if (machine1.equals(machine2Host)) {
            return true;
        }

        for (Machine child : m2.getVirtualMachines()) {
            if (isInsideVM(machine1, child.getHostId())) {
                return true;
            }
        }
        return false;
    }

    private static List<ITimeEvent> getIrqEventsList(TimeGraphEntry entry, List<List<ITmfStateInterval>> fullStates, List<ITmfStateInterval> prevFullState, IProgressMonitor monitor, int quark) {
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
//        manager.add(new Separator());
//        manager.add(fHighlightMachine);
//        manager.add(fHighlightCPU);
//        manager.add(fHighlightProcess);
//        manager.add(fHighlightContainer);

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
    public VirtualResourcePresentationProvider getFusedVMViewPresentationProvider() {
        ITimeGraphPresentationProvider2 pp = getPresentationProvider();
        if (!(pp instanceof VirtualResourcePresentationProvider)) {
            return null;
        }
        return (VirtualResourcePresentationProvider) pp;
    }

    private void printInformations() {
        long begin = getBeginSelectedTime();
        long end = getEndSelectedTime();

        System.out.println("Begin time: " + Utils.formatTime(begin, TimeFormat.CALENDAR, Resolution.NANOSEC)); //$NON-NLS-1$
        System.out.println("End time: " + Utils.formatTime(end, TimeFormat.CALENDAR, Resolution.NANOSEC)); //$NON-NLS-1$
        System.out.println();

    }

    /**
     * Updates the tooltip text of the buttons so it corresponds to the machine,
     * cpu and process selected
     */
    private void updateToolTipTexts() {
        VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
        fHighlightMachine.setToolTipText(presentationProvider.getSelectedMachine());
        fHighlightCPU.setToolTipText(Integer.toString((presentationProvider.getSelectedCpu())));
        // TODO: Add the name of the selected process
        fHighlightProcess.setToolTipText(Messages.FusedVMView_ButtonProcessSelected);
        fHighlightContainer.setToolTipText(presentationProvider.getSelectedContainer());
    }

    /**
     * Sets the checked state of the buttons
     */
    private void updateButtonsSelection() {
//        VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
//        Map<String, Machine> highlightedMachines = presentationProvider.getHighlightedMachines();
//        Machine machine = highlightedMachines.get(presentationProvider.getSelectedMachine());
//        if (machine == null) {
//            return;
//        }
//
//        fHighlightMachine.setChecked(machine.isHighlighted());
//        fHighlightCPU.setChecked(machine.isCpuHighlighted(presentationProvider.getSelectedCpu()));
//        fHighlightProcess.setChecked(presentationProvider.isThreadSelected(machine.getMachineName(), presentationProvider.getSelectedThreadID()));
//        fHighlightContainer.setChecked(machine.isContainerHighlighted(presentationProvider.getSelectedContainer()));
    }

    /**
     * Get the select machine action.
     *
     * @return The select machine action
     */
    public Action getSelectMachineAction() {
        if (fSelectMachineAction == null) {
            fSelectMachineAction = new Action() {
                @Override
                public void run() {
                    VirtualResourcePresentationProvider presentationProvider = (VirtualResourcePresentationProvider) getPresentationProvider();

                    Control dataViewer = getTimeGraphViewer().getControl();
                    if (dataViewer == null || dataViewer.isDisposed()) {
                        return;
                    }
                    ITmfTrace trace = getTrace();
                    if (trace == null) {
                        return;
                    }
                    Machine physicalMachine = fPhysicalMachines.get(trace);
                    if (physicalMachine == null) {
                        return;
                    }
                    SelectMachineDialog dialog = new SelectMachineDialog(dataViewer.getShell());
                    dialog.setInput(Collections.singleton(physicalMachine));
                    dialog.setInitialSelections(presentationProvider.getSelectedElements().toArray());
                    dialog.open();
                    Object[] result = dialog.getResult();
                    if (result != null) {
                        presentationProvider.setSelectedElements(Arrays.asList(result));
                        presentationProvider.resetTimeEventHighlight();
                        redraw();
                    }
                }
            };
            fSelectMachineAction.setText(Messages.FusedVMView_SelectMachineActionNameText);
            fSelectMachineAction.setToolTipText(Messages.FusedVMView_SelectMachineActionToolTipText);
        }

        return fSelectMachineAction;
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return Collections.singleton(trace);
    }

    private Machine createHierarchy(@NonNull ITmfStateSystem ssq) {
        /* Separate host from guests */
        Machine host = null;
        List<Machine> guests = new LinkedList<>();
        for (String machineHost : FusedVMInformationProvider.getMachinesTraced(ssq)) {
            ITmfStateValue typeMachine = FusedVMInformationProvider.getTypeMachine(ssq, machineHost);

            if (typeMachine.isNull()) {
                continue;
            }
            String machineName = FusedVMInformationProvider.getMachineName(ssq, machineHost);
            Machine machine = null;
            if ((typeMachine.unboxInt() & StateValues.MACHINE_GUEST) == StateValues.MACHINE_GUEST) {
                machine = new Machine(machineName, machineHost, typeMachine, FusedVMInformationProvider.getPhysicalCpusUsedByMachine(ssq, machineHost));
                fMachines.put(machine.getMachineName(), machine);
                guests.add(machine);
            } else if (typeMachine.unboxInt() == StateValues.MACHINE_HOST) {
                machine = new Machine(machineName, machineHost, typeMachine);
                for (String cpus : FusedVMInformationProvider.getCpusUsedByMachine(ssq, machineHost)) {
                    machine.addPCpu(cpus);
                }
                fMachines.put(machine.getMachineName(), machine);
                host = machine;
            }
            if (machine == null) {
                continue;
            }
        }
        if (host == null) {
            return null;
        }
        /* Complete construction for the host */
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
            String parentHostId = FusedVMInformationProvider.getParentMachineHostId(ssq, m.getHostId());
            if (parentHostId.equals(host.getHostId())) {
                m.setHost(host);
                host.addVirtualMachine(m);
            }
            for (Machine m2 : guests) {
                parentHostId = FusedVMInformationProvider.getParentMachineHostId(ssq, m2.getHostId());
                if (parentHostId.equals(m.getHostId())) {
                    m2.setHost(m);
                    m.addVirtualMachine(m2);
                }
            }
        }
    }

    private static void createContainersHierarchyForMachine(@NonNull ITmfStateSystem ssq, Machine m) {
        String machineName = m.getHostId();
        Collection<Integer> containersQuarks = FusedVMInformationProvider.getMachineContainersQuarks(ssq, machineName);
        /* Look for not nested containers */
        for (Integer quark : containersQuarks) {
            long parentContainer = FusedVMInformationProvider.getParentContainer(ssq, quark);
            if (parentContainer == IVirtualMachineModel.ROOT_NAMESPACE) {
                String containerName = ssq.getAttributeName(quark);
                List<String> pCpus = FusedVMInformationProvider.getPCpusUsedByContainer(ssq, quark);
                Machine container = m.createContainer(containerName, m.getHostId(), pCpus);
                /* Continue construction for these containers */
                createContainersHierarchyForContainer(ssq, container, containersQuarks);
            }
        }
    }

    private static void createContainersHierarchyForContainer(@NonNull ITmfStateSystem ssq, Machine container, Collection<Integer> containersQuarks) {
        Long containerName = Long.parseLong(container.getMachineName());
        for (int quark : containersQuarks) {
            if (FusedVMInformationProvider.getParentContainer(ssq, quark).equals(containerName)) {
                /* We found a child */
                String childName = ssq.getAttributeName(quark);
                List<String> pCpus = FusedVMInformationProvider.getPCpusUsedByContainer(ssq, quark);
                Machine child = container.createContainer(childName, container.getHostId(), pCpus);
                /* Look for child's childs */
                createContainersHierarchyForContainer(ssq, child, containersQuarks);
            }
        }
    }

    /**
     * Update the view when a thread is selected
     *
     * @param signal
     *            The thread selected signal
     */
    @TmfSignalHandler
    public void threadSelected(TmfThreadSelectedSignal signal) {
        int threadId = signal.getThreadId();
        VirtualResourcePresentationProvider presentationProvider = getFusedVMViewPresentationProvider();
        presentationProvider.setSelectedThread(new HostThread(Objects.requireNonNull(signal.getHostId()), threadId));

        updateButtonsSelection();
        updateToolTipTexts();
    }
}
