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
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to handle the events required for the
 * {@link DpdkEthdevThroughputAnalysisModule} analysis
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputEventHandler implements IDpdkEventHandler {

    /* Attribute names */
    private static final String ETH_NIC_PORTS = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PORTS);
    private static final String RX_Q = Objects.requireNonNull(DpdkEthdevThroughputAttributes.RX_Q);
    private static final String TX_Q = Objects.requireNonNull(DpdkEthdevThroughputAttributes.TX_Q);
    private static final String PKT_NB = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_COUNT);
    private static final String PKT_NB_P = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_COUNT_P);
    private static final String PKT_SIZE_P = Objects.requireNonNull(DpdkEthdevThroughputAttributes.PKT_SIZE_P);

    /* Events layout */
    private final DpdkEthdevEventLayout fLayout;

    DpdkEthdevThroughputEventHandler(DpdkEthdevEventLayout layout) {
        fLayout = layout;
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
        int portQuark = ssb.getQuarkAbsoluteAndAdd(ETH_NIC_PORTS, portId.toString());
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
            Activator.getInstance().logWarning("Problem accessing the state of a port queue (Quark = " + queueQuark + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        String eventName = event.getName();
        long timestamp = event.getTimestamp().getValue();
        Integer portId = event.getContent().getFieldValue(Integer.class, fLayout.fieldPortId());

        if (eventName.equals(fLayout.eventEthdevRxBurstNonEmpty()) ||
                eventName.equals(fLayout.eventProfileEthdevRxBurst())) {
            handleBurstEvent(ssb, event, Objects.requireNonNull(portId), RX_Q, timestamp);
        } else if (eventName.equals(fLayout.eventEthdevTxBurst()) ||
                eventName.equals(fLayout.eventProfileEthdevTxBurst())) {
            handleBurstEvent(ssb, event, Objects.requireNonNull(portId), TX_Q, timestamp);
        } else {
            Activator.getInstance().logError("Unknown event (" + eventName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void handleBurstEvent(ITmfStateSystemBuilder ssb, ITmfEvent event, Integer portId, String queueCategory, long timestamp) {
        Integer size = null;
        boolean isRxEvent = queueCategory.equals(RX_Q);
        Integer nbPkts = event.getContent().getFieldValue(Integer.class, isRxEvent ? fLayout.fieldNbRxPkts() : fLayout.fieldNbPkts());
        boolean isProfileEvent = event.getName().equals(fLayout.eventProfileEthdevTxBurst()) || event.getName().equals(fLayout.eventProfileEthdevRxBurst());

        if (isProfileEvent) {
            size = event.getContent().getFieldValue(Integer.class, fLayout.fieldSize());
            if (!isRxEvent) {
                nbPkts = event.getContent().getFieldValue(Integer.class, fLayout.fieldNbTxPkts());
            }
        }

        Integer queueId = event.getContent().getFieldValue(Integer.class, fLayout.fieldQueueId());
        updateCounts(ssb, portId, Objects.requireNonNull(queueId), queueCategory, isProfileEvent, Objects.requireNonNull(nbPkts), size, timestamp);
    }
}
