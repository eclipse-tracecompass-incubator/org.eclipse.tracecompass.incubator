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
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * @author Cédric Biancheri
 */
public class ProcessForkContainerHandler extends VMKernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            The event layout
     * @param sp
     *            The state provider
     */
    public ProcessForkContainerHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField content = event.getContent();
        ITmfEventField field;
        String machineHost = event.getTrace().getHostId();
        String childProcessName = (String) content.getField(getLayout().fieldChildComm()).getValue();
        long childVTIDs[] = { };
        field = content.getField("vtids"); //$NON-NLS-1$
        if (field != null) {
            childVTIDs = (long[]) field.getValue();
        }
        Long childNSInum = content.getFieldValue(Long.class, "child_ns_inum"); //$NON-NLS-1$
        if (childNSInum == null) {
            childNSInum = -1L;
        } else {
            /* Save the namespace id somewhere so it can be reused */
            ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(childNSInum));
        }
        Long parentNSInum = content.getFieldValue(Long.class, "parent_ns_inum"); //$NON-NLS-1$

        Integer parentTid = ((Long) content.getField(getLayout().fieldParentTid()).getValue()).intValue();
        Integer childTid = ((Long) content.getField(getLayout().fieldChildTid()).getValue()).intValue();

        Integer parentTidNode = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), machineHost, parentTid.toString());
        Integer childTidNode = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), machineHost, childTid.toString());

        /* Assign the PPID to the new process */
        int quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.PPID);
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, parentTid, quark);

        /* Set the new process' exec_name */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.EXEC_NAME);
        ss.modifyAttribute(timestamp, childProcessName, quark);

        /* Set the new process' status */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.STATUS);
        ss.modifyAttribute(timestamp, StateValues.PROCESS_STATUS_WAIT_FOR_CPU, quark);

        /* Set the process' syscall name, to be the same as the parent's */
        quark = ss.getQuarkRelativeAndAdd(parentTidNode, FusedAttributes.SYSTEM_CALL);
        Object syscall = ss.queryOngoing(quark);
        if (syscall != null) {
            quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.SYSTEM_CALL);
            ss.modifyAttribute(timestamp, syscall, quark);
        }

        Integer level = 0;
        Integer maxLevel = childVTIDs.length;

        /*
         * Set the max level. It is useful if we want to know the depth of the
         * hierarchy
         */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_MAX_LEVEL);
        ss.modifyAttribute(timestamp, maxLevel, quark);

        for (long vtid : childVTIDs) {
            if (vtid == childTid) {
                /* Set the namespace level */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_LEVEL);
                ss.modifyAttribute(timestamp, level, quark);

                /* Set the namespace ID */
                quark = ss.optQuarkRelative(parentTidNode, FusedAttributes.NS_INUM);
                if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    continue;
                }

                Object nsInum = ss.queryOngoing(quark);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                ss.modifyAttribute(timestamp, nsInum, quark);

                /* Save the tid */
                if (nsInum instanceof Long) {
                    quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, String.valueOf((long) nsInum));
                    quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                    ss.modifyAttribute(timestamp, (int) vtid, quark);
                }

                /* Nothing else to do at the level 0 */
                continue;
            }
            /* Entering an other level */
            level++;

            if (level != maxLevel - 1 || childNSInum.equals(parentNSInum)) {
                /*
                 * We are not at the last level or we are still in the namespace
                 * of the parent
                 */

                /* Create a new level for the current vtid */
                childTidNode = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VTID);
                ss.modifyAttribute(timestamp, (int) vtid, childTidNode);

                /* Set the VPPID attribute for the child */
                parentTidNode = ss.getQuarkRelativeAndAdd(parentTidNode, FusedAttributes.VTID);
                // When a process is forked but the parent was not state dumped,
                // we do not know the vppid
                Object parentVtid = ss.queryOngoing(parentTidNode);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VPPID);
                ss.modifyAttribute(timestamp, parentVtid, quark);

                /* Set the ns_inum attribute for the child */
                quark = ss.optQuarkRelative(parentTidNode, FusedAttributes.NS_INUM);
                // We do not have namespace information for the parent, we only
                // know the last of the child
                if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    Object value = null;
                    if (level == maxLevel - 1) {
                        value = childNSInum;

                        /* Save the tid */
                        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString((long) value));
                        quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                        ss.modifyAttribute(timestamp, (int) vtid, quark);
                    }
                    quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                    ss.modifyAttribute(timestamp, value, quark);
                } else {
                    Object value = ss.queryOngoing(quark);
                    quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                    ss.modifyAttribute(timestamp, value, quark);

                    /* Save the tid */
                    if (value instanceof Long) {
                        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString((long) value));
                        quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                        ss.modifyAttribute(timestamp, (int) vtid, quark);
                    }
                }

            } else {
                /* Last level and new namespace */

                /* Create a new level for the current vtid */
                childTidNode = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VTID);
                ss.modifyAttribute(timestamp, (int) vtid, childTidNode);

                /* Set the VPPID attribute for the child */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VPPID);
                ss.modifyAttribute(timestamp, 0, quark);

                /* Set the ns_inum attribute for the child */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                ss.modifyAttribute(timestamp, childNSInum, quark);

                /* Save the tid */
                int quarkContainer = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(childNSInum));
                quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quarkContainer, childTid);
                ss.modifyAttribute(timestamp, (int) vtid, quark);

                /* Save the parent's namespace ID */
                quark = ss.getQuarkRelativeAndAdd(quarkContainer, FusedAttributes.PARENT);
                if (ss.queryOngoingState(quark).isNull() && parentNSInum != null) {
                    ss.modifyAttribute(ss.getStartTime(), parentNSInum, quark);
                }
            }

            /* Set the ns_level attribute for the child */
            quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_LEVEL);
            ss.modifyAttribute(timestamp, level, quark);
        }

    }

}
