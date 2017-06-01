/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.lxc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualMachineModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

import com.google.common.collect.ImmutableSet;

/**
 * @author Cédric Biancheri
 */
public class LxcModel implements IVirtualMachineModel {
    /* Maps a namespace ID to a container */
    /* Map the namespace ID to a container */
    private final Map<Long, VirtualMachine> fKnownContainers = new HashMap<>();
    /* Map a TID to a container */
    // private final Map<Integer, VirtualMachine> fTIDtoContainer = new
    // HashMap<>();

    // private final TmfExperiment fExperiment;

    static final ImmutableSet<String> REQUIRED_EVENTS = NonNullUtils.checkNotNull(ImmutableSet.of(
            "lttng_statedump_process_state", //$NON-NLS-1$
            "sched_process_fork")); //$NON-NLS-1$

    // public LxcModel(TmfExperiment exp) {
    // fExperiment = exp;
    // }

    @Override
    public @Nullable VirtualMachine getCurrentMachine(@NonNull ITmfEvent event) {
        //
        // Integer cpu =
        // TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(),
        // TmfCpuAspect.class, event);
        // if (cpu == null) {
        // /* We couldn't find any CPU information, ignore this event */
        // return null;
        // }
        //
        // /* Get the TID currently running */
        // long ts = event.getTimestamp().getValue();
        // String hostId = event.getTrace().getHostId();
        // KernelAnalysisModule module = getLttngKernelModuleFor(hostId);
        // Integer tid = KernelThreadInformationProvider.getThreadOnCpu(module,
        // cpu, ts);
        //
        //
        // String eventName = event.getName();
        // if (!getRequiredEvents().contains(eventName)) {
        // VirtualMachine container = fTIDtoContainer.get(tid);
        // if (container != null) {
        // return container;
        // }
        // return null;
        // }
        //
        // String fieldNamespaceID;
        // String fieldTID;
        // switch (eventName) {
        // case "lttng_statedump_process_state":
        // fieldNamespaceID = "ns_inum";
        // fieldTID = "tid";
        // break;
        // case "sched_process_fork":
        // fieldNamespaceID = "child_ns_inum";
        // fieldTID = "child_tid";
        // break;
        // default:
        // return null;
        // }
        // Long namespaceID =(Long)
        // event.getContent().getField(fieldNamespaceID).getValue();
        // Integer newTID = ((Long)
        // event.getContent().getField(fieldTID).getValue()).intValue();
        // VirtualMachine container = fKnownContainers.get(namespaceID);
        // if (container != null) {
        // return container;
        // }
        // container = VirtualMachine.newContainerMachine(namespaceID,
        // event.getTrace().getHostId(), event.getTrace().getName());
        // fKnownContainers.put(namespaceID, container);
        // fTIDtoContainer.put(newTID, container);
        return null;
    }

    @Override
    public @NonNull Set<@NonNull String> getRequiredEvents(@NonNull IKernelAnalysisEventLayout layout) {
        return REQUIRED_EVENTS;
    }

    @Override
    public @Nullable VirtualCPU getVCpuEnteringHypervisorMode(@NonNull ITmfEvent event, @NonNull HostThread ht, @NonNull IKernelAnalysisEventLayout layout) {
        // Not used
        return null;
    }

    @Override
    public @Nullable VirtualCPU getVCpuExitingHypervisorMode(@NonNull ITmfEvent event, @NonNull HostThread ht, @NonNull IKernelAnalysisEventLayout layout) {
        // Not used
        return null;
    }

    @Override
    public @Nullable VirtualCPU getVirtualCpu(@NonNull HostThread ht) {
        // Not used
        return null;
    }

    @Override
    public void handleEvent(@NonNull ITmfEvent event, IKernelAnalysisEventLayout layout) {

    }

    // private @Nullable KernelAnalysisModule getLttngKernelModuleFor(String
    // hostId) {
    // return TmfExperimentUtils.getAnalysisModuleOfClassForHost(fExperiment,
    // hostId, KernelAnalysisModule.class);
    // }

    /**
     * Return the number of known machines
     *
     * @return The number of known machines
     */
    public int numberOfKnownMachines() {
        return fKnownContainers.size();
    }

}
