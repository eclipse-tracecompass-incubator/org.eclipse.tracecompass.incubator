/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Cédric Biancheri - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.lxc.LxcModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.qemukvm.QemuKvmStrings;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.qemukvm.QemuKvmVmModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.ImmutableMap;

/**
 * State provider for the Fused Virtual Machine analysis. It is based on the
 * version 16 of the kernel state provider.
 *
 * @author Cedric Biancheri
 */
public class FusedVirtualMachineStateProvider extends AbstractTmfStateProvider {
    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 2;

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    private final Map<String, VMKernelEventHandler> fEventNames;
    private final Map<ITmfTrace, LayoutHandler> fLayouts = new HashMap<>();
    private QemuKvmVmModel fKvmModel;
    private LxcModel fContainerModel;
    private int fCurrentThreadNode; // quark to current thread node
    private boolean fAllRolesFound = false;

    // ------------------------------------------------------------------------
    // Layout handling class and methods
    // ------------------------------------------------------------------------
    private final Map<IKernelAnalysisEventLayout, LayoutHandler> fMap = new HashMap<>();

    private LayoutHandler getForLayout(IKernelAnalysisEventLayout layout, Map<String, VMKernelEventHandler> builder) {
        LayoutHandler layoutHandler = fMap.get(layout);
        if (layoutHandler == null) {
            layoutHandler = new LayoutHandler(layout);
            fMap.put(layout, layoutHandler);
            addEventNames(builder, layout);
        }
        return layoutHandler;
    }

    private class LayoutHandler {

        protected final IKernelAnalysisEventLayout fLayout;
        protected final VMKernelEventHandler fSysEntryHandler;
        protected final VMKernelEventHandler fSysExitHandler;
        protected final VMKernelEventHandler fKvmEntryHandler;
        protected final VMKernelEventHandler fKvmExitHandler;
        protected final VMKernelEventHandler fKvmNestedVmExitInjectHandler;
        protected final VMKernelEventHandler fKvmMmuGetPageHandler;

        public LayoutHandler(IKernelAnalysisEventLayout layout) {
            fLayout = layout;
            fSysEntryHandler = new SysEntryHandler(layout, FusedVirtualMachineStateProvider.this);
            fSysExitHandler = new SysExitHandler(layout, FusedVirtualMachineStateProvider.this);
            fKvmEntryHandler = new KvmEntryHandler(layout, FusedVirtualMachineStateProvider.this);
            fKvmExitHandler = new KvmExitHandler(layout, FusedVirtualMachineStateProvider.this);
            fKvmMmuGetPageHandler = new KvmMmuGetPageHandler(layout, FusedVirtualMachineStateProvider.this);
            fKvmNestedVmExitInjectHandler = new KvmNestedVmExitInjectHandler(layout, FusedVirtualMachineStateProvider.this);
        }
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Instantiate a new state provider plugin.
     *
     * @param experiment
     *            The experiment that will be analyzed.
     */
    public FusedVirtualMachineStateProvider(TmfExperiment experiment) {
        super(experiment, "Virtual Machine State Provider"); //$NON-NLS-1$

        Map<String, VMKernelEventHandler> builder = new HashMap<>();

        for (ITmfTrace trace : TmfTraceManager.getTraceSet(experiment)) {
            if (trace instanceof IKernelTrace) {
                IKernelAnalysisEventLayout layout = ((IKernelTrace) trace).getKernelEventLayout();
                fLayouts.put(trace, getForLayout(layout, builder));
            }
        }
        fEventNames = ImmutableMap.copyOf(builder);
        fKvmModel = QemuKvmVmModel.get(experiment);
        fContainerModel = new LxcModel();
    }

    // ------------------------------------------------------------------------
    // Event names management
    // ------------------------------------------------------------------------

    private void addEventNames(Map<String, VMKernelEventHandler> builder, IKernelAnalysisEventLayout layout) {

        builder.put(layout.eventIrqHandlerEntry(), new IrqEntryHandler(layout, this));
        builder.put(layout.eventIrqHandlerExit(), new IrqExitHandler(layout, this));
        builder.put(layout.eventSoftIrqEntry(), new SoftIrqEntryHandler(layout, this));
        builder.put(layout.eventSoftIrqExit(), new SoftIrqExitHandler(layout, this));
        builder.put(layout.eventSoftIrqRaise(), new SoftIrqRaiseHandler(layout, this));
        builder.put(layout.eventSchedSwitch(), new SchedSwitchHandler(layout, this));
        builder.put(layout.eventSchedPiSetprio(), new PiSetprioHandler(layout, this));
        builder.put(layout.eventSchedProcessFork(), new ProcessForkContainerHandler(layout, this));
        builder.put(layout.eventSchedProcessExit(), new ProcessExitHandler(layout, this));
        builder.put(layout.eventSchedProcessFree(), new ProcessFreeHandler(layout, this));

        final String eventStatedumpProcessState = layout.eventStatedumpProcessState();
        if (eventStatedumpProcessState != null) {
            builder.put(eventStatedumpProcessState, new StateDumpContainerHandler(layout, this));
        }

        for (String eventSchedWakeup : layout.eventsSchedWakeup()) {
            builder.put(eventSchedWakeup, new SchedWakeupHandler(layout, this));
        }
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public TmfExperiment getTrace() {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof TmfExperiment) {
            return (TmfExperiment) trace;
        }
        throw new IllegalStateException("FusedVirtualMachineStateProvider: The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public FusedVirtualMachineStateProvider getNewInstance() {
        return new FusedVirtualMachineStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpu == null) {
            /* We couldn't find any CPU information, ignore this event */
            return;
        }

        VirtualMachine host = null;
        // FIXME: Add a test with 2 sets of machines connected by network and see where it fails
        if (!allRolesFound()) {
            host = getCurrentMachineAndAdd(event);
        } else {
            host = getCurrentMachine(event);
        }

        String traceHost = event.getTrace().getHostId();
        LayoutHandler layoutHandler = fLayouts.get(event.getTrace());
        if (layoutHandler == null) {
            return;
        }
        /*
         * Have the hypervisor models handle the event first.
         */
        fKvmModel.handleEvent(event, layoutHandler.fLayout);

        /*
         * Continue even if host is unknown if the event is required for
         * container analysis
         */
        // if (host == null &&
        // !fContainerModel.getRequiredEvents().contains(event.getName()) &&
        // !allRolesFound()) {
        // return;
        // }

        // What is this condition?
        if (!fContainerModel.getRequiredEvents(layoutHandler.fLayout).contains(event.getName()) && !allRolesFound()) {
            return;
        }

        Integer currentVCpu = -1;
        if (host != null) {
            /* Associate the cpu to its machine */
            VirtualCPU.getVirtualCPU(host, cpu.longValue());
            if (host.isGuest()) {
                /*
                 * If the event is from a vm we have to find on which physical
                 * cpu it is running.
                 */
                currentVCpu = cpu;
                cpu = getPhysicalCPU(host, cpu);
                if (cpu == null) {
                    return;
                }
            }
        }

        final String eventName = event.getName();
        final long ts = event.getTimestamp().getValue();

        final ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        /*
         * Do this block only all machines have their roles
         */
        if (allRolesFound()) {
            /* Shortcut for the "current CPU" attribute node */
            int currentCPUNode = ss.getQuarkRelativeAndAdd(getNodeCPUs(ss), String.valueOf(cpu));

            /*
             * Add in the state system the state of the cpu (in or out vm).
             */
            int quarkCondition = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CONDITION);
            ITmfStateValue valueCondition = StateValues.CONDITION_UNKNOWN_VALUE;
            int quarkMachines = FusedVMEventHandlerUtils.getMachinesNode(ss);
            int machineHostQuark = ss.getQuarkRelativeAndAdd(quarkMachines, traceHost);
            // if (inVM) {
            if (host != null && host.isGuest()) {
                valueCondition = StateValues.CONDITION_IN_VM_VALUE;
                int quarkVCpu = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.VIRTUAL_CPU);
                ITmfStateValue valueVCpu = TmfStateValue.newValueInt(currentVCpu);
                ss.modifyAttribute(ts, valueVCpu, quarkVCpu);

                /*
                 * This part is used to remember how many cpus a machine has
                 */
                // if (host != null && host.isGuest()) {
                ss.getQuarkRelativeAndAdd(machineHostQuark, FusedAttributes.CPUS, currentVCpu.toString());
                // }
                /* Remember that this VM is using this pcpu. */
                int quarkPCPUs = FusedVMEventHandlerUtils.getMachinepCPUsNode(ss, traceHost);
                ss.getQuarkRelativeAndAdd(quarkPCPUs, cpu.toString());
            } else {
                /*
                 * We still need to check here if we are a guest because the
                 * guest's trace can be longer than the host's and we might be
                 * in a vm even if inVM == false //
                 */
                // if (host != null && host.isGuest()) {
                // ss.getQuarkRelativeAndAdd(machineNameQuark, Attributes.CPUS,
                // currentVCpu.toString());
                // } else {
                ss.getQuarkRelativeAndAdd(quarkMachines, traceHost, FusedAttributes.CPUS, cpu.toString());
                // }
                valueCondition = StateValues.CONDITION_OUT_VM_VALUE;
            }
            /*
             * Add the role of the machine in the state system
             */
            setMachinesRoles(ss);
            setMachinesParents(ss);

            /*
             * Set the condition value in the state system (in or out vm)
             */
            if (host != null && host.isHost() && !host.isGuest()) {
                ss.modifyAttribute(ts, valueCondition, quarkCondition);
            }

            /*
             * Shortcut for the "current thread" attribute node. It requires
             * querying the current CPU's current thread.
             */
            int quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CURRENT_THREAD);

            ITmfStateValue value = ss.queryOngoingState(quark);
            int thread = value.isNull() ? -1 : value.unboxInt();

            fCurrentThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(ss, traceHost), String.valueOf(thread));

            /* Set the name of the machine running on the cpu */
            quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.MACHINE_NAME);
            value = TmfStateValue.newValueString(event.getTrace().getHostId());
            if (host != null && host.isHost() && !host.isGuest()) {
                ss.modifyAttribute(ts, value, quark);
            }
        }
        /*
         * Feed event to the history system if it's known to cause a state
         * transition.
         */
        VMKernelEventHandler handler = fEventNames.get(eventName);
        // TODO: maybe put the other handlers also in fEventNames
        if (handler == null) {
            IKernelAnalysisEventLayout layout = layoutHandler.fLayout;
            if (isSyscallExit(eventName, layout)) {
                handler = layoutHandler.fSysExitHandler;
            } else if (isSyscallEntry(eventName, layout)) {
                handler = layoutHandler.fSysEntryHandler;
            } else if (isKvmEntry(eventName)) {
                handler = layoutHandler.fKvmEntryHandler;
            } else if (isKvmExit(eventName)) {
                handler = layoutHandler.fKvmExitHandler;
            } else if (isKvmMmuGetPage(eventName)) {
                handler = layoutHandler.fKvmMmuGetPageHandler;
            } else if (isKvmNestedVmExitInject(eventName)) {
                handler = layoutHandler.fKvmNestedVmExitInjectHandler;
            }
        }
        if (handler != null) {
            handler.handleEvent(ss, event);
        }

    }

    // ------------------------------------------------------------------------
    // Convenience methods for commonly-used attribute tree locations
    // Either private or package private so they can be used by the handlers
    // ------------------------------------------------------------------------

    private static int getNodeCPUs(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(FusedAttributes.CPUS);
    }

    private static int getNodeThreads(ITmfStateSystemBuilder ssb, String machineName) {
        return ssb.getQuarkAbsoluteAndAdd(FusedAttributes.THREADS, machineName);
    }

    static int getCurrentThreadNode(Integer cpuNumber, ITmfStateSystemBuilder ss) {
        /*
         * Shortcut for the "current thread" attribute node. It requires
         * querying the current CPU's current thread.
         */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getCurrentCPUNode(cpuNumber, ss), FusedAttributes.CURRENT_THREAD);
        ITmfStateValue value = ss.queryOngoingState(quark);
        int thread = value.isNull() ? -1 : value.unboxInt();
        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getCurrentCPUNode(cpuNumber, ss), FusedAttributes.MACHINE_NAME);
        value = ss.queryOngoingState(quark);
        String machineName = value.unboxStr();
        return ss.getQuarkRelativeAndAdd(getNodeThreads(ss, machineName), String.valueOf(thread));
    }

    private static boolean isSyscallEntry(String eventName, IKernelAnalysisEventLayout layout) {
        return (eventName.startsWith(layout.eventSyscallEntryPrefix())
                || eventName.startsWith(layout.eventCompatSyscallEntryPrefix()));
    }

    private static boolean isSyscallExit(String eventName, IKernelAnalysisEventLayout layout) {
        return (eventName.startsWith(layout.eventSyscallExitPrefix()) ||
                eventName.startsWith(layout.eventCompatSyscallExitPrefix()));
    }

    int getCurrentThreadNode() {
        return fCurrentThreadNode;
    }

    @Nullable
    Integer getPhysicalCPU(VirtualMachine host, Integer cpu) {
        VirtualCPU vcpu = VirtualCPU.getVirtualCPU(host, cpu.longValue());
        Long physCpu = fKvmModel.getPhysicalCpuFromVcpu(host, vcpu);
        if (physCpu == null) {
            return null;
        }
        /* Replace the vcpu value by the physical one. */
        return physCpu.intValue();
    }

    @Nullable
    VirtualMachine getCurrentMachineAndAdd(ITmfEvent event) {
        return fKvmModel.getCurrentMachine(event);
    }

    @Nullable
    VirtualMachine getCurrentMachine(ITmfEvent event) {
        return getKnownMachines().get(event.getTrace().getHostId());
    }

    @Nullable
    VirtualMachine getCurrentContainer(ITmfEvent event) {
        return fContainerModel.getCurrentMachine(event);
    }

    @Nullable
    VirtualMachine getVmFromHostThread(HostThread ht) {
        return fKvmModel.getVmFromHostThread(ht);
    }

    @Nullable
    HostThread getHostThreadFromVCpu(VirtualCPU virtualCPU) {
        return fKvmModel.getHostThreadFromVCpu(virtualCPU);
    }

    @Nullable
    VirtualCPU getVirtualCpu(HostThread ht) {
        return fKvmModel.getVirtualCpu(ht);
    }

    @Nullable
    VirtualCPU getVCpuEnteringHypervisorMode(ITmfEvent event, HostThread ht, IKernelAnalysisEventLayout layout) {
        return fKvmModel.getVCpuEnteringHypervisorMode(event, ht, layout);
    }

    private static boolean isKvmEntry(String eventName) {
        return eventName.equals(QemuKvmStrings.KVM_ENTRY) || eventName.equals(QemuKvmStrings.KVM_X86_ENTRY);
    }

    private static boolean isKvmExit(String eventName) {
        return eventName.equals(QemuKvmStrings.KVM_EXIT) || eventName.equals(QemuKvmStrings.KVM_X86_EXIT);
    }

    private static boolean isKvmMmuGetPage(String eventName) {
        return eventName.equals(QemuKvmStrings.KVM_MMU_GET_PAGE);
    }

    private static boolean isKvmNestedVmExitInject(String eventName) {
        return eventName.equals(QemuKvmStrings.KVM_NESTED_VMEXIT_INJECT);
    }

    /**
     * Return the known machines
     *
     * @return The known machines
     */
    public Map<String, VirtualMachine> getKnownMachines() {
        return fKvmModel.getKnownMachines();
    }

    /**
     * Tell if all the roles of the machines were found
     *
     * TODO: What if we have UST traces? or traces that have no role in the
     * experiment but that are welcome there, we shouldn't rely on this method
     *
     * @return true if all roles were found
     */
    private boolean allRolesFound() {
        if (fAllRolesFound) {
            return fAllRolesFound;
        }
        int numberOfMachines = numberOfIdentifiedMachines();
        fAllRolesFound = getTrace().getTraces().size() == numberOfMachines;
        return fAllRolesFound;
    }

    private int numberOfIdentifiedMachines() {
        int numberOfMachines = 0;
        for (VirtualMachine machine : getKnownMachines().values()) {
            if (machine.isHost() && !machine.isGuest()) {
                numberOfMachines++;
                numberOfMachines += numberOfIdentifiedMachinesRec(machine);
            }
        }
        return numberOfMachines;
    }

    private int numberOfIdentifiedMachinesRec(VirtualMachine machine) {
        int nbMachines = 0;
        for (VirtualMachine child : machine.getChildren()) {
            child.setParent(machine);
            nbMachines++;
            nbMachines += numberOfIdentifiedMachinesRec(child);
        }
        return nbMachines;
    }

    private void setMachinesRoles(ITmfStateSystemBuilder ss) {
        Map<String, VirtualMachine> knownMachines = getKnownMachines();

        for (VirtualMachine machine : knownMachines.values()) {
            String machineHost = machine.getHostId();
            int machineQuark = ss.getQuarkAbsoluteAndAdd(FusedAttributes.HOSTS, machineHost);
            // Update the trace name for this machine
            int machineNameQuark = ss.optQuarkRelative(machineQuark, FusedAttributes.MACHINE_NAME);
            if (machineNameQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                machineNameQuark = ss.getQuarkRelativeAndAdd(machineQuark, FusedAttributes.MACHINE_NAME);
                ss.updateOngoingState(TmfStateValue.newValueString(machine.getTraceName()), machineNameQuark);
            }
            // Set the type
            ITmfStateValue machineType = ss.queryOngoingState(machineQuark);
            if (!machineType.isNull()) {
                continue;
            }
            ss.updateOngoingState(TmfStateValue.newValueInt(machine.getType()), machineQuark);
        }
    }

    private void setMachinesParents(ITmfStateSystemBuilder ss) {
        Map<String, VirtualMachine> knownMachines = getKnownMachines();
        for (VirtualMachine machine : knownMachines.values()) {
            String machineName = machine.getHostId();
            int parentQuark = ss.getQuarkAbsoluteAndAdd(FusedAttributes.HOSTS, machineName, FusedAttributes.PARENT);
            ITmfStateValue parent = ss.queryOngoingState(parentQuark);
            if (!parent.isNull()) {
                return;
            }
            VirtualMachine parentMachine = machine.getParent();
            if (parentMachine != null) {
                ss.modifyAttribute(getStartTime(), TmfStateValue.newValueString(parentMachine.getHostId()), parentQuark);
            }
        }
    }
}