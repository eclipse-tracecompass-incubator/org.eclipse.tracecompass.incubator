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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.rate.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to handle DPDK ethdev library events
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevEventHandler implements IDpdkEventHandler {

    /* Attribute names */
    private static final String ETH_NICS = Objects.requireNonNull(Attributes.NICS);
    private static final String RX_Q = Objects.requireNonNull(Attributes.RX_Q);
    private static final String TX_Q = Objects.requireNonNull(Attributes.TX_Q);

    DpdkEthdevEventHandler() {
        // Nothing here
    }

    /**
     * Create an Ethernet (RX or TX) queue on the state system
     *
     * @param ts
     *            time to use for state change
     */
    static private void createNicQueue(ITmfStateSystemBuilder ssb, int queueSetQuark, int nbQueues, long ts) {
        if (nbQueues <= 0) {
            return;
        }

        for (int i = 0; i < nbQueues; i++) {
            ssb.getQuarkRelativeAndAdd(queueSetQuark, String.valueOf(i));
        }
    }

    /**
     * Update the count of received or transmitted packets on the state system
     *
     * @param ssb
     *            State System builder
     * @param queueQuark
     *            Quark of the the Ethernet device queue
     * @param nbPkts
     *            Number of packets received or transmitted
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
        Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
        long ts = event.getTimestamp().getValue();
        String eventName = event.getName();

        if (eventName.equals(DpdkEthdevEventLayout.eventEthdevConfigure())) {
            Integer rc = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldRc());
            Integer nbRxQueues = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxQ());
            Integer nbTxQueues = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbTxQ());

            if (Objects.requireNonNull(rc) == 0) {
                // save the new Ethernet device on the satet system
                int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NICS, String.valueOf(portId));
                int rxQueueQark = ssb.getQuarkRelativeAndAdd(portQuark, RX_Q);
                createNicQueue(ssb, rxQueueQark, Objects.requireNonNull(nbRxQueues), ts);
                int txQueueQark = ssb.getQuarkRelativeAndAdd(portQuark, TX_Q);
                createNicQueue(ssb, txQueueQark, Objects.requireNonNull(nbTxQueues), ts);
            } else {
                Activator.getInstance().logWarning("The event " + DpdkEthdevEventLayout.eventEthdevConfigure() + " presents a Non-Null RC value"); //$NON-NLS-1$ //$NON-NLS-2$
            }

        } else if (eventName.equals(DpdkEthdevEventLayout.eventEthdevRxqBurstNonEmpty())) {
            Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
            Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());
            int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NICS, String.valueOf(portId));
            int rxQsQark = ssb.getQuarkRelativeAndAdd(portQuark, RX_Q);
            int rxQueueQark = ssb.getQuarkRelativeAndAdd(rxQsQark, Objects.requireNonNull(queueId).toString());
            updateCounts(ssb, rxQueueQark, Objects.requireNonNull(nbRxPkts), ts);

        } else if (eventName.equals(DpdkEthdevEventLayout.eventEthdevTxqBurst())) {
            Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
            Integer nbTxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbPkts());
            int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NICS, String.valueOf(portId));
            int txQsQark = ssb.getQuarkRelativeAndAdd(portQuark, TX_Q);
            int txQueueQark = ssb.getQuarkRelativeAndAdd(txQsQark, Objects.requireNonNull(queueId).toString());
            updateCounts(ssb, txQueueQark, Objects.requireNonNull(nbTxPkts), ts);
        }
    }
}
