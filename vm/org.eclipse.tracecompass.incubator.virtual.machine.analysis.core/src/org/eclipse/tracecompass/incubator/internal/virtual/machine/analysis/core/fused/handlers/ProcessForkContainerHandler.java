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
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
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
        ITmfStateValue value = TmfStateValue.newValueInt(parentTid);
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);

        /* Set the new process' exec_name */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.EXEC_NAME);
        value = TmfStateValue.newValueString(childProcessName);
        ss.modifyAttribute(timestamp, value, quark);

        /* Set the new process' status */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.STATUS);
        value = StateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Set the process' syscall name, to be the same as the parent's */
        quark = ss.getQuarkRelativeAndAdd(parentTidNode, FusedAttributes.SYSTEM_CALL);
        value = ss.queryOngoingState(quark);
        if (!value.isNull()) {
            quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.SYSTEM_CALL);
            ss.modifyAttribute(timestamp, value, quark);
        }

        Integer level = 0;
        Integer maxLevel = childVTIDs.length;

        /*
         * Set the max level. It is useful if we want to know the depth of the
         * hierarchy
         */
        quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_MAX_LEVEL);
        value = TmfStateValue.newValueInt(maxLevel);
        ss.modifyAttribute(timestamp, value, quark);

        for (long vtid : childVTIDs) {
            if (vtid == childTid) {
                /* Set the namespace level */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_LEVEL);
                value = TmfStateValue.newValueInt(level);
                ss.modifyAttribute(timestamp, value, quark);

                /* Set the namespace ID */
                quark = ss.optQuarkRelative(parentTidNode, FusedAttributes.NS_INUM);
                if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    continue;
                }

                value = ss.queryOngoingState(quark);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                ss.modifyAttribute(timestamp, value, quark);

                /* Save the tid */
                quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(value.unboxLong()));
                quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                ss.modifyAttribute(timestamp, TmfStateValue.newValueInt((int) vtid), quark);

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
                value = TmfStateValue.newValueInt((int) vtid);
                ss.modifyAttribute(timestamp, value, childTidNode);

                /* Set the VPPID attribute for the child */
                parentTidNode = ss.getQuarkRelativeAndAdd(parentTidNode, FusedAttributes.VTID);
                // When a process is forked but the parent was not state dumped,
                // we do not know the vppid
                value = ss.queryOngoingState(parentTidNode);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VPPID);
                ss.modifyAttribute(timestamp, value, quark);

                /* Set the ns_inum attribute for the child */
                quark = ss.optQuarkRelative(parentTidNode, FusedAttributes.NS_INUM);
                // We do not have namespace information for the parent, we only
                // know the last of the child
                if (quark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    value = TmfStateValue.nullValue();
                    if (level == maxLevel - 1) {
                        value = TmfStateValue.newValueLong(childNSInum);

                        /* Save the tid */
                        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(value.unboxLong()));
                        quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                        ss.modifyAttribute(timestamp, TmfStateValue.newValueInt((int) vtid), quark);
                    }
                    quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                    ss.modifyAttribute(timestamp, value, quark);
                } else {
                    value = ss.queryOngoingState(quark);
                    quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                    ss.modifyAttribute(timestamp, value, quark);

                    /* Save the tid */
                    quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(value.unboxLong()));
                    quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quark, childTid);
                    ss.modifyAttribute(timestamp, TmfStateValue.newValueInt((int) vtid), quark);
                }

            } else {
                /* Last level and new namespace */

                /* Create a new level for the current vtid */
                childTidNode = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VTID);
                value = TmfStateValue.newValueInt((int) vtid);
                ss.modifyAttribute(timestamp, value, childTidNode);

                /* Set the VPPID attribute for the child */
                value = TmfStateValue.newValueInt(0);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.VPPID);
                ss.modifyAttribute(timestamp, value, quark);

                /* Set the ns_inum attribute for the child */
                value = TmfStateValue.newValueLong(childNSInum);
                quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_INUM);
                ss.modifyAttribute(timestamp, value, quark);

                /* Save the tid */
                int quarkContainer = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getMachinesNode(ss), machineHost, FusedAttributes.CONTAINERS, Long.toString(childNSInum));
                quark = FusedVMEventHandlerUtils.saveContainerThreadID(ss, quarkContainer, childTid);
                ss.modifyAttribute(timestamp, TmfStateValue.newValueInt((int) vtid), quark);

                /* Save the parent's namespace ID */
                quark = ss.getQuarkRelativeAndAdd(quarkContainer, FusedAttributes.PARENT);
                if (ss.queryOngoingState(quark).isNull() && parentNSInum != null) {
                    ss.modifyAttribute(ss.getStartTime(), TmfStateValue.newValueLong(parentNSInum), quark);
                }
            }

            /* Set the ns_level attribute for the child */
            quark = ss.getQuarkRelativeAndAdd(childTidNode, FusedAttributes.NS_LEVEL);
            value = TmfStateValue.newValueInt(level);
            ss.modifyAttribute(timestamp, value, quark);
        }

    }

}
