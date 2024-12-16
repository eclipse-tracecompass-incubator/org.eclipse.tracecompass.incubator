/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler for the DPDK events required for the
 * {@link DpdkEthdevSpinAnalysisModule} analysis
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinEventHandler implements IDpdkEventHandler {

    private final DpdkEthdevEventLayout fLayout;

    DpdkEthdevSpinEventHandler(DpdkEthdevEventLayout layout) {
        fLayout = layout;
    }

    /**
     * Update the count of received or transmitted packets on the state system
     *
     * @param ssb
     *            State system builder
     * @param queueQuark
     *            Quark of the the Ethernet device queue
     * @param nbPkts
     *            Number of packets received or transmitted
     * @param ts
     *            Timestamp to use for state change
     */
    public void updateCounts(ITmfStateSystemBuilder ssb, int queueQuark, Integer nbPkts, long ts) {
        if (nbPkts <= 0) {
            return;
        }
        try {
            StateSystemBuilderUtils.incrementAttributeLong(ssb, ts, queueQuark, nbPkts);
        } catch (StateValueTypeException e) {
            Activator.getInstance().logWarning("Problem accessing the state of a NIC queue (Quark = " + queueQuark + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        long ts = event.getTimestamp().getValue();
        String eventName = event.getName();

        Integer portId = event.getContent().getFieldValue(Integer.class, fLayout.fieldPortId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, fLayout.fieldQueueId());
        String threadName = event.getContent().getFieldValue(String.class, fLayout.fieldThreadName());
        Integer cpuId = event.getContent().getFieldValue(Integer.class, fLayout.fieldCpuId());

        int threadQuark = ssb.getQuarkAbsoluteAndAdd(DpdkEthdevSpinAttributes.POLL_THREADS, threadName + "/" + cpuId); //$NON-NLS-1$
        int queueQark = ssb.getQuarkRelativeAndAdd(threadQuark, "P" + Objects.requireNonNull(portId).toString() + "/Q" + Objects.requireNonNull(queueId).toString()); //$NON-NLS-1$ //$NON-NLS-2$

        if (eventName.equals(fLayout.eventEthdevRxBurstEmpty())) {
            ssb.modifyAttribute(ts, DpdkEthdevSpinAttributes.SPIN_STATUS, queueQark);
        } else if (eventName.equals(fLayout.eventEthdevRxBurstNonEmpty())) {
            ssb.modifyAttribute(ts, DpdkEthdevSpinAttributes.ACTIVE_STATUS, queueQark);
        }
    }
}
