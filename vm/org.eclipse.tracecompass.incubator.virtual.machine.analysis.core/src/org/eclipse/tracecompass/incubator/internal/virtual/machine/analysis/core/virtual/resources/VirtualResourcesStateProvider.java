/*******************************************************************************
 * Copyright (c) 2014, 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mohamad Gebai - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.VcpuStateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.handlers.QemuKvmEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.handlers.SchedSwitchEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * This is the state provider which translates the virtual machine experiment
 * events to a state system.
 *
 * Attribute tree:
 *
 * <pre>
 * |- Virtual Machines
 * |  |- <Guest Host ID> -> Friendly name (trace name)
 * |  |  |- <VCPU number>
 * |  |  |  |- Status -> <Status value>
 * </pre>
 *
 * The status value of the VCPUs are either {@link VcpuStateValues#VCPU_IDLE},
 * {@link VcpuStateValues#VCPU_UNKNOWN} or {@link VcpuStateValues#VCPU_RUNNING}.
 * Those three values are ORed with flags {@link VcpuStateValues#VCPU_VMM}
 * and/or {@link VcpuStateValues#VCPU_PREEMPT} to indicate respectively whether
 * they are in hypervisor mode or preempted on the host.
 *
 * @author Mohamad Gebai
 */
public class VirtualResourcesStateProvider extends AbstractTmfStateProvider {

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 1;

    private final IVirtualEnvironmentModel fModel;
    private final Multimap<String, IVirtualMachineEventHandler> fEventNames = HashMultimap.create();
    private final Collection<IVirtualMachineEventHandler> fHandlers;
    private final Map<ITmfTrace, IKernelAnalysisEventLayout> fLayouts;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param experiment
     *            The virtual machine experiment
     * @param model
     *            The virtual environment model
     */
    public VirtualResourcesStateProvider(TmfExperiment experiment, IVirtualEnvironmentModel model) {
        super(experiment, "Virtual Machine State Provider"); //$NON-NLS-1$

        fModel = model;
        fLayouts = new HashMap<>();
        fHandlers = ImmutableSet.of(new SchedSwitchEventHandler(), new QemuKvmEventHandler());
    }

    // ------------------------------------------------------------------------
    // Event names management
    // ------------------------------------------------------------------------

    private void buildEventNames(ITmfTrace trace) {
        IKernelAnalysisEventLayout layout;
        if (trace instanceof IKernelTrace) {
            layout = ((IKernelTrace) trace).getKernelEventLayout();
        } else {
            /* Fall-back to the base LttngEventLayout */
            layout = DefaultEventLayout.getInstance();
        }
        fLayouts.put(trace, layout);
        fHandlers.forEach(handler -> {
            handler.getRequiredEvents(layout).forEach(event -> {
                fEventNames.put(event, handler);
            });
        });
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
        throw new IllegalStateException("VirtualMachineStateProvider: The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public VirtualResourcesStateProvider getNewInstance() {
        TmfExperiment trace = getTrace();
        return new VirtualResourcesStateProvider(trace, fModel);
    }

    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        /* Is the event managed by this analysis */
        final String eventName = event.getName();
        IKernelAnalysisEventLayout eventLayout = fLayouts.get(event.getTrace());
        if (eventLayout == null) {
            buildEventNames(event.getTrace());
            eventLayout = fLayouts.get(event.getTrace());
            if (eventLayout == null) {
                return;
            }
        }

        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());


        Collection<IVirtualMachineEventHandler> handlers = fEventNames.get(eventName);
        if (handlers.isEmpty()) {
            return;
        }
        IVirtualEnvironmentModel virtEnv = fModel;
        for (IVirtualMachineEventHandler handler : handlers) {
            handler.handleEvent(ss, event, virtEnv, eventLayout);
        }

    }

}
