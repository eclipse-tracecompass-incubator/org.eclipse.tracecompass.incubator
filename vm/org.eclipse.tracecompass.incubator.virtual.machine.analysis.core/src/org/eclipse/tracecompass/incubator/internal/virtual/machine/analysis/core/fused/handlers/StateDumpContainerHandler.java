/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.LinuxValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Handler for the process statedump event. It initializes the processes'
 * namespaces and inserts the process in their proper namespace(s)
 *
 * @author Cédric Biancheri
 */
public class StateDumpContainerHandler extends VMKernelEventHandler {

    private static final String TID_FIELD = "tid"; //$NON-NLS-1$
    private static final String VTID_FIELD = "vtid"; //$NON-NLS-1$
    private static final String PID_FIELD = "pid"; //$NON-NLS-1$
    private static final String VPID_FIELD = "vpid"; //$NON-NLS-1$
    private static final String PPID_FIELD = "ppid"; //$NON-NLS-1$
    private static final String VPPID_FIELD = "vppid"; //$NON-NLS-1$
    private static final String NAME_FIELD = "name"; //$NON-NLS-1$
    private static final String STATUS_FIELD = "status"; //$NON-NLS-1$
    private static final String NS_LEVEL_FIELD = "ns_level"; //$NON-NLS-1$
    private static final String NS_INUM_FIELD = "ns_inum"; //$NON-NLS-1$

    /**
     * Constructor with a layout
     *
     * @param layout
     *            The event layout for this trace
     * @param sp
     *            The state provider
     */
    public StateDumpContainerHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        int layerNode = createLevels(ss, event);
        if (layerNode != ITmfStateSystem.INVALID_ATTRIBUTE) {
            fillLevel(ss, event, layerNode);
        }
    }

    /**
     * Create all the levels of containers for a process inside the state
     * system.
     *
     * @param ss
     *            The state system
     * @param event
     *            The statedump_process event
     * @return The quark of the deepest level
     */
    public static int createLevels(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        ITmfEventField content = event.getContent();
        Long tid = content.getFieldValue(Long.class, TID_FIELD);
        Long vtid = content.getFieldValue(Long.class, VTID_FIELD);
        Long nsLevel = content.getFieldValue(Long.class, NS_LEVEL_FIELD);
        if (tid == null || vtid == null || nsLevel == null) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }
        long ts = event.getTimestamp().getValue();
        String hostId = event.getTrace().getHostId();
        String threadAttributeName = FusedVMEventHandlerUtils.buildThreadAttributeName(tid.intValue(), cpu);
        if (threadAttributeName == null) {
            return ITmfStateSystem.INVALID_ATTRIBUTE;
        }
        int threadNode = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), hostId, threadAttributeName);
        int layerQuark = threadNode;
        for (int i = 0; i < nsLevel; i++) {
            /* While we can go deeper we create an other level */
            layerQuark = ss.getQuarkRelativeAndAdd(layerQuark, FusedAttributes.VTID);
            if (i + 1 == nsLevel) {
                /*
                 * If the next layer is the last we can add the info contained
                 * in the event
                 */
                ss.modifyAttribute(ts, vtid.intValue(), layerQuark);
            }
            ss.getQuarkRelativeAndAdd(layerQuark, FusedAttributes.VPPID);
            int quark = ss.getQuarkRelativeAndAdd(layerQuark, FusedAttributes.NS_LEVEL);
            if (ss.queryOngoingState(quark).isNull()) {
                /* If the value didn't exist previously, set it */
                ss.modifyAttribute(ts, i + 1, quark);
            }
        }
        return layerQuark;
    }

    /**
     * Fill the first and last level of a thread node
     *
     * @param ss
     *            The state system
     * @param event
     *            The statedump_process event
     * @param layerNode
     *            The quark of the last level
     */
    public static void fillLevel(ITmfStateSystemBuilder ss, ITmfEvent event, int layerNode) {
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        ITmfEventField content = event.getContent();
        long ts = event.getTimestamp().getValue();
        int quark;
        String hostId = event.getTrace().getHostId();
        Long tid = content.getFieldValue(Long.class, TID_FIELD);
        Long pid = content.getFieldValue(Long.class, PID_FIELD);
        Long ppid = content.getFieldValue(Long.class, PPID_FIELD);
        Long status = content.getFieldValue(Long.class, STATUS_FIELD);
        String name = content.getFieldValue(String.class, NAME_FIELD);
        Long vtid = content.getFieldValue(Long.class, VTID_FIELD);
        Long vpid = content.getFieldValue(Long.class, VPID_FIELD);
        Long vppid = content.getFieldValue(Long.class, VPPID_FIELD);
        Long nsLevel = content.getFieldValue(Long.class, NS_LEVEL_FIELD);
        Long nsInum = content.getFieldValue(Long.class, NS_INUM_FIELD);

        if (tid == null || pid == null || ppid == null || status == null
                || name == null || vtid == null || vpid == null
                || vppid == null || nsLevel == null || nsInum == null) {
            return;
        }

        String threadAttributeName = FusedVMEventHandlerUtils.buildThreadAttributeName(tid.intValue(), cpu);
        if (threadAttributeName == null) {
            return;
        }

        int threadNode = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), hostId, threadAttributeName);

        /*
         * Set the max level, only at level 0. This can be useful to know the
         * depth of the hierarchy.
         */
        quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.NS_MAX_LEVEL);
        if (ss.queryOngoingState(quark).isNull()) {
            /*
             * Events are coming from the deepest layers first so no need to
             * update the ns_max_level.
             */
            ss.modifyAttribute(ts, nsLevel.intValue() + 1, quark);
        }
        int maxLevel = ss.queryOngoingState(quark).unboxInt();

        /*
         * Set the process' status. Only for level 0.
         */
        quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.STATUS);
        int processStatus = StateValues.PROCESS_STATUS_UNKNOWN;
        if (ss.queryOngoing(quark) == null) {
            switch (status.intValue()) {
            case LinuxValues.STATEDUMP_PROCESS_STATUS_WAIT_CPU:
                processStatus = StateValues.PROCESS_STATUS_WAIT_FOR_CPU;
                break;
            case LinuxValues.STATEDUMP_PROCESS_STATUS_WAIT:
                /*
                 * We have no information on what the process is waiting on
                 * (unlike a sched_switch for example), so we will use the
                 * WAIT_UNKNOWN state instead of the "normal" WAIT_BLOCKED
                 * state.
                 */
                processStatus = StateValues.PROCESS_STATUS_WAIT_UNKNOWN;
                break;
            default:
                processStatus = StateValues.PROCESS_STATUS_UNKNOWN;
            }
            ss.modifyAttribute(ts, processStatus, quark);
        }

        /*
         * Set the process' name. Only for level 0.
         */
        quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.EXEC_NAME);
        if (ss.queryOngoing(quark) == null) {
            /* If the value didn't exist previously, set it */
            ss.modifyAttribute(ts, name, quark);
        }

        String attributePpid = FusedAttributes.PPID;
        /* Prepare the level if we are not in the root namespace */
        if (nsLevel != 0) {
            attributePpid = "VPPID"; //$NON-NLS-1$
        }

        /* Set the process' PPID */
        quark = ss.getQuarkRelativeAndAdd(layerNode, attributePpid);

        if (ss.queryOngoing(quark) == null) {
            int setPpid;
            int setVppid;
            if (vpid.equals(vtid)) {
                /* We have a process. Use the 'PPID' field. */
                setVppid = vppid.intValue();
                setPpid = ppid.intValue();
            } else {
                /*
                 * We have a thread, use the 'PID' field for the parent.
                 */
                setVppid = vpid.intValue();
                setPpid = pid.intValue();
            }
            ss.modifyAttribute(ts, setVppid, quark);
            if (nsLevel != 0) {
                /* Set also for the root layer */
                quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.PPID);
                if (ss.queryOngoing(quark) == null) {
                    ss.modifyAttribute(ts, setPpid, quark);
                }
            }
        }

        /* Set the namespace level */
        quark = ss.getQuarkRelativeAndAdd(layerNode, FusedAttributes.NS_LEVEL);
        if (ss.queryOngoingState(quark).isNull()) {
            /* If the value didn't exist previously, set it */
            ss.modifyAttribute(ts, nsLevel.intValue(), quark);
        }

        /* Set the namespace identification number */
        quark = ss.getQuarkRelativeAndAdd(layerNode, FusedAttributes.NS_INUM);
        if (ss.queryOngoingState(quark).isNull()) {
            /* If the value didn't exist previously, set it */
            ss.modifyAttribute(ts, nsInum, quark);
        }

        /* Save the namespace id somewhere so it can be reused */
        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), hostId, FusedAttributes.CONTAINERS, Long.toString(nsInum));

        /* Save the tid in the container. We also keep the vtid */
        quark = ss.getQuarkRelativeAndAdd(quark, FusedAttributes.THREADS, String.valueOf(tid));
        ss.modifyAttribute(ts, vtid.intValue(), quark);

        if (nsLevel != maxLevel - 1) {
            /*
             * We are not at the deepest level. So this namespace is the father
             * of the namespace one layer deeper. We are going to tell him we
             * found his father. That will make him happy.
             */
            quark = ss.getQuarkRelativeAndAdd(layerNode, FusedAttributes.VTID, FusedAttributes.NS_INUM);
            Long childNSInum = ss.queryOngoingState(quark).unboxLong();
            if (childNSInum > 0) {
                quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), hostId, FusedAttributes.CONTAINERS, Long.toString(childNSInum), FusedAttributes.PARENT);
                ss.modifyAttribute(ss.getStartTime(), nsInum, quark);
            }
        }

        if (nsLevel == 0) {
            /* Root namespace => no parent */
            quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), hostId, FusedAttributes.CONTAINERS, Long.toString(nsInum), FusedAttributes.PARENT);
            ss.modifyAttribute(ss.getStartTime(), -1L, quark);
        }

    }

}
