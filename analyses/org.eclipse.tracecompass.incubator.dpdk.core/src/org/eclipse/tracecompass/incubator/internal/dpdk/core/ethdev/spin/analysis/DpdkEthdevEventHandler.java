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
public class DpdkEthdevEventHandler implements IDpdkEventHandler {

    /* Attribute names */
    private static final String POLL_THREADS = Objects.requireNonNull(Attributes.POLL_THREADS);

    DpdkEthdevEventHandler() {
        // Nothing here
    }

    /**
     * Update the count of received or transmitted packets on the state system
     *
     * @param ssb
     *            state system builder
     * @param queueQuark
     *            quark of the the Ethernet device queue
     * @param nbPkts
     *            number of packets received or transmitted
     * @param ts
     *            time to use for state change
     */
    public void updateCounts(ITmfStateSystemBuilder ssb, int queueQuark, Integer nbPkts, long ts) {
        if (nbPkts <= 0) {
            return;
        }
        try {
            StateSystemBuilderUtils.incrementAttributeLong(ssb, ts, queueQuark, nbPkts);
        } catch (StateValueTypeException e) {
            Activator.getInstance().logWarning(getClass().getName() + ": problem accessing the state of a NIC queue (Quark =" + String.valueOf(queueQuark) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        long ts = event.getTimestamp().getValue();
        String eventName = event.getName();

        Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
        String threadName = event.getContent().getFieldValue(String.class, DpdkEthdevEventLayout.fieldThreadName());
        Integer cpuId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldCpuId());

        int threadQuark = ssb.getQuarkAbsoluteAndAdd(POLL_THREADS, threadName + "/" + cpuId); //$NON-NLS-1$
        int queueQark = ssb.getQuarkRelativeAndAdd(threadQuark, "P" + Objects.requireNonNull(portId).toString() + "/Q" + Objects.requireNonNull(queueId).toString()); //$NON-NLS-1$ //$NON-NLS-2$

        if (eventName.equals(DpdkEthdevEventLayout.eventEthdevRxqBurstEmpty())) {
            ssb.modifyAttribute(ts, Attributes.SPIN_STATUS, queueQark);
        } else if (eventName.equals(DpdkEthdevEventLayout.eventEthdevRxqBurstNonEmpty())) {
            ssb.modifyAttribute(ts, Attributes.ACTIVE_STATUS, queueQark);
        }
    }
}
