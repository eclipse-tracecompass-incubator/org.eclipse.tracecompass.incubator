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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to process the DPDK events required for the
 * {@link DpdkEthdevThroughputAnalysisModule} analysis
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputEventHandler implements IDpdkEventHandler {

    /* Attribute names */
    private static final String ETH_NICS = Objects.requireNonNull(DpdkEthdevThroughputAttributes.NICS);
    private static final String RX_Q = Objects.requireNonNull(DpdkEthdevThroughputAttributes.RX_Q);
    private static final String TX_Q = Objects.requireNonNull(DpdkEthdevThroughputAttributes.TX_Q);
    private static final String PKT_NB = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_COUNT);

    private static final String PKT_NB_P = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_COUNT_P);
    private static final String PKT_SIZE_P = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_SIZE_P);

    DpdkEthdevThroughputEventHandler() {
        // Nothing here
    }

    /**
     * Update the count of received or transmitted packets on the state system
     *
     * @param ts
     *            Timestamp to use for the state change
     */
    static private void createNicQueues(ITmfStateSystemBuilder ssb, int queueSetQuark, int nbQueues, long ts) {
        if (nbQueues <= 0) {
            return;
        }

        for (int i = 0; i < nbQueues; i++) {
            ssb.getQuarkRelativeAndAdd(queueSetQuark, String.valueOf(i));
        }
    }

    /**
     * Update the count of bytes received or transmitted on the state system
     *
     * @param ssb
     *            State system builder
     * @param portId
     *            Port identifier
     * @param queueId
     *            Queue identifier
     * @param queueCategoryAttribute
     *            Category of the target queue (RX or TX)
     * @param isProfileEvent
     *            The event is emitted through pre-loading the custom profiling
     *            library
     * @param nbPkts
     *            Number of packets received or transmitted
     * @param size
     *            Size of the burst in bytes
     * @param timestamp
     *            Time to use for the state change
     */
    public void updateCounts(ITmfStateSystemBuilder ssb, Integer portId, Integer queueId, String queueCategoryAttribute,
            boolean isProfileEvent, Integer nbPkts, @Nullable Integer size, long timestamp) {
        int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NICS, portId.toString());
        int queuesQuark = ssb.getQuarkRelativeAndAdd(portQuark, queueCategoryAttribute);
        int queueQuark = ssb.getQuarkRelativeAndAdd(queuesQuark, queueId.toString());

        try {
            if (isProfileEvent) {
                int pktSizeQuark = ssb.getQuarkRelativeAndAdd(queueQuark, PKT_SIZE_P);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, timestamp, pktSizeQuark, Objects.requireNonNull(size));

                int pktNumberQuark = ssb.getQuarkRelativeAndAdd(queueQuark, PKT_NB_P);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, timestamp, pktNumberQuark, nbPkts);
            } else {
                int pktNumberQuark = ssb.getQuarkRelativeAndAdd(queueQuark, PKT_NB);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, timestamp, pktNumberQuark, nbPkts);
            }
        } catch (StateValueTypeException e) {
            Activator.getInstance().logWarning("Problem accessing the state of a NIC queue (Quark = " + queueQuark + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        String eventName = event.getName();
        long timestamp = event.getTimestamp().getValue();
        Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
        Objects.requireNonNull(portId);

        if (eventName.equals(DpdkEthdevEventLayout.eventEthdevConfigure())) {
            handleConfigureEvent(ssb, event, portId, timestamp);
        } else if (eventName.equals(DpdkEthdevEventLayout.eventEthdevRxBurstNonEmpty()) ||
                eventName.equals(DpdkEthdevEventLayout.eventProfileEthdevRxBurst())) {
            handleRxBurstEvent(ssb, event, portId, timestamp, eventName);
        } else if (eventName.equals(DpdkEthdevEventLayout.eventEthdevTxBurst()) ||
                eventName.equals(DpdkEthdevEventLayout.eventProfileEthdevTxBurst())) {
            handleTxBurstEvent(ssb, event, portId, timestamp, eventName);
        } else {
            Activator.getInstance().logError("Unknown event (" + eventName + ") !!"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static void handleConfigureEvent(ITmfStateSystemBuilder ssb, ITmfEvent event, Integer portId, long timestamp) {
        Integer rc = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldRc());
        Integer nbRxQueues = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxQ());
        Integer nbTxQueues = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbTxQ());

        if (rc != null && rc == 0 && nbRxQueues != null && nbTxQueues != null) {
            int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NICS, portId.toString());

            int rxQueueQuark = ssb.getQuarkRelativeAndAdd(portQuark, RX_Q);
            createNicQueues(ssb, rxQueueQuark, nbRxQueues, timestamp);

            int txQueueQuark = ssb.getQuarkRelativeAndAdd(portQuark, TX_Q);
            createNicQueues(ssb, txQueueQuark, nbTxQueues, timestamp);
        } else {
            Activator.getInstance().logWarning(DpdkEthdevEventLayout.eventEthdevConfigure() + "event has invalid or missing fields"); //$NON-NLS-1$
        }
    }

    private void handleRxBurstEvent(ITmfStateSystemBuilder ssb, ITmfEvent event, Integer portId, long timestamp, String eventName) {
        Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
        Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());

        Integer size = null;

        boolean isProfileEvent = eventName.equals(DpdkEthdevEventLayout.eventProfileEthdevRxBurst());
        if (isProfileEvent) {
            size = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldSize());
        }

        updateCounts(ssb, portId, Objects.requireNonNull(queueId), RX_Q, isProfileEvent, Objects.requireNonNull(nbRxPkts), size, timestamp);
    }

    private void handleTxBurstEvent(ITmfStateSystemBuilder ssb, ITmfEvent event, Integer portId, long timestamp, String eventName) {
        Integer nbTxPkts;
        Integer size = null;
        Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());

        boolean isProfileEvent = eventName.equals(DpdkEthdevEventLayout.eventProfileEthdevTxBurst());
        if (isProfileEvent) {
            nbTxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbTxPkts());
            size = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldSize());
        } else {
            nbTxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbPkts());
        }

        updateCounts(ssb, portId, Objects.requireNonNull(queueId), TX_Q, isProfileEvent, Objects.requireNonNull(nbTxPkts), size, timestamp);
    }
}
