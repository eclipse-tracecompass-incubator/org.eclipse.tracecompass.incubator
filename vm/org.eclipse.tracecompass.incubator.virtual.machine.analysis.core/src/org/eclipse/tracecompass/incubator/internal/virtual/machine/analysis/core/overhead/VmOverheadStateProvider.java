/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.handlers.QemuKvmEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.handlers.SchedSwitchEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * State provider for VM overhead analysis. At the first level is the status of
 * a thread from a guest perspective, level 2 detail when the thread is in VMM
 * mode or its VCPU is preempted by the host and level 3 gives reasons or status
 * on that preemption.
 *
 * @author Geneviève Bastien
 */
public class VmOverheadStateProvider extends AbstractTmfStateProvider {

    /**
     * The threads attribute in the state system
     */
    public static final String TRACES = "Traces"; //$NON-NLS-1$
    /**
     * The threads attribute in the state system
     */
    public static final String THREADS = "Threads"; //$NON-NLS-1$
    /**
     * First level of overhead
     */
    public static final String LEVEL_1 = "1"; //$NON-NLS-1$
    /**
     * Second level of overhead
     */
    public static final String LEVEL_2 = "2"; //$NON-NLS-1$
    /**
     * Third level of overhead
     */
    public static final String LEVEL_3 = "3"; //$NON-NLS-1$
    /**
     * The status string for a preempted VCPU
     */
    public static final String STATUS_VCPU_PREEMPTED = "VCPU Preempted"; //$NON-NLS-1$
    /**
     * The status string of a VMM mode
     */
    public static final String STATUS_VMM_MODE = "VMM"; //$NON-NLS-1$
    /**
     * The status string for running
     */
    public static final String STATUS_RUNNING = "Running"; //$NON-NLS-1$

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 4;

    private final Multimap<String, IVirtualMachineEventHandler> fEventNames = HashMultimap.create();
    private final Collection<IVirtualMachineEventHandler> fHandlers;
    private final Map<ITmfTrace, IKernelAnalysisEventLayout> fLayouts;

    private final IVirtualEnvironmentModel fModel;

    // A map of currently running thread and their kernel statuses
    private final Map<HostThread, GuestKernelThreadStatuses> fGuestThreads = new HashMap<>();

    // This class will update the guest kernel statuses for the running threads, so
    // that the line corresponding to the guest thread contains the real thread
    // statuses
    private class GuestKernelThreadStatuses {

        private final ITmfStateSystemBuilder fSs;
        private final int fQuark;
        private final Iterator<ProcessStatusInterval> fStatuses;
        private ProcessStatusInterval fCurrentStatus;

        public GuestKernelThreadStatuses(ITmfStateSystemBuilder ss, long start, Iterator<ProcessStatusInterval> statuses, int quark) {
            fSs = ss;
            fQuark = quark;
            fStatuses = statuses;
            fCurrentStatus = statuses.next();
            fSs.modifyAttribute(start, fCurrentStatus.getProcessStatus().name(), quark);
        }

        public boolean update(long start) {
            if (start <= fCurrentStatus.getEnd()) {
                return true;
            }
            if (fStatuses.hasNext()) {
                fCurrentStatus = fStatuses.next();
                fSs.modifyAttribute(fCurrentStatus.getStart(), fCurrentStatus.getProcessStatus().name(), fQuark);
                return true;
            }
            fSs.removeAttribute(fCurrentStatus.getEnd(), fQuark);
            return false;
        }

    }

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
    public VmOverheadStateProvider(TmfExperiment experiment, IVirtualEnvironmentModel model) {
        super(experiment, "Vm Overhead State Provider"); //$NON-NLS-1$

        fModel = model;
        fHandlers = ImmutableSet.of(new SchedSwitchEventHandler(this), new QemuKvmEventHandler(this));
        fLayouts = new HashMap<>();
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

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public TmfExperiment getTrace() {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof TmfExperiment) {
            return (TmfExperiment) trace;
        }
        throw new IllegalStateException("VirtualMachineStateProvider: The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        TmfExperiment trace = getTrace();
        return new VmOverheadStateProvider(trace, fModel);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        Set<HostThread> toRemove = new HashSet<>();
        fGuestThreads.entrySet().forEach(t -> {
            if (!t.getValue().update(event.getTimestamp().toNanos())) {
                toRemove.add(t.getKey());
            }
        });
        toRemove.forEach(ht -> fGuestThreads.remove(ht));

        /* Is the event managed by this analysis */
        final String eventName = event.getName();
        @Nullable IKernelAnalysisEventLayout eventLayout = fLayouts.get(event.getTrace());
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

    /**
     * Create a class that will provider the guest thread status values
     *
     * @param ss
     *            The overhead analysis state system
     * @param ht
     *            The host thread for which to get the statuses
     * @param start
     *            The timestamp where to start getting the statuses
     * @param tidQuark
     *            The quark of the guest thread for which to update the status, in
     *            the overhead state system
     */
    public void createGuestThreadStatus(ITmfStateSystemBuilder ss, HostThread ht, long start, int tidQuark) {
        IHostModel model = ModelManager.getModelFor(ht.getHost());
        Iterable<ProcessStatusInterval> statuses = model.getThreadStatusIntervals(ht.getTid(), start, Long.MAX_VALUE, 1);
        Iterator<ProcessStatusInterval> iterator = statuses.iterator();
        if (iterator.hasNext()) {
            GuestKernelThreadStatuses threadStatus = new GuestKernelThreadStatuses(ss, start, iterator, tidQuark);
            fGuestThreads.put(ht, threadStatus);
        }
    }

    /**
     * Stop tracking the thread status for a process, usually when the thread is
     * scheduled out
     *
     * @param ht
     *            The host thread for which to stop tracking status
     */
    public void removeGuestThreadStatus(HostThread ht) {
        fGuestThreads.remove(ht);
    }

    /**
     * Method to build the thread attribute name. If the thread is 0, it will add an
     * underscore with the CPU id after
     *
     * @param threadId
     *            The thread ID
     * @param cpuId
     *            The CPU number
     * @return The string corresponding to the thread attribute
     */
    public static String buildThreadAttributeName(int threadId, int cpuId) {
        if (threadId == 0) {
            if (cpuId < 0) {
                return String.valueOf(threadId);
            }
            return String.valueOf(threadId) + '_' + String.valueOf(cpuId);
        }

        return String.valueOf(threadId);
    }
}
