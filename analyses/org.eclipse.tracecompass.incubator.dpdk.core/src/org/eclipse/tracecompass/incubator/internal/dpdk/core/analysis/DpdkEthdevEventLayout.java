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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis;

/**
 * This class specifies the names of events required for the analysis of
 * Ethdev-based applications, and their fields.
 *
 * To start using an Ethernet port, a Dpdk application must perform the
 * following steps:
 *
 * 1. **Configure the Ethernet port** by calling the API function
 * `rte_eth_dev_configure()`. This function requires specifying the number of RX
 * and TX queues to enable, along with other parameters that determine the
 * features and capabilities of the port (e.g., RSS).
 *
 * 2. **Set up the receive and transmit queues** by calling the API functions
 * `rte_eth_rx_queue_setup()` and `rte_eth_tx_queue_setup()`. The main
 * parameters for these functions are the number of descriptors and the memory
 * pool from which to allocate the `rte_mbuf` network memory buffers.
 *
 * 3. **Start the device** by calling the API function `rte_eth_dev_start()`.
 * From this point onward, the device becomes operational, and enqueue and
 * dequeue operations can be performed on its queues.
 *
 * 4. **Use the port queues** by polling the RX queues for incoming packets
 * using `rte_eth_rx_burst()` and transmit packets by sending them to the TX
 * queues using `rte_eth_tx_burst()`.
 *
 * 5. **Stop the Ethernet port** by calling `rte_eth_dev_stop()`.
 *
 * 6. **Close the Ethernet port** by calling `rte_eth_dev_close()`.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevEventLayout {
    /* Event names */
    private static final String ETH_DEV_CONFIGURE = "lib.ethdev.configure"; //$NON-NLS-1$
    private static final String ETH_DEV_RX_BURST_EMPTY = "lib.ethdev.rx.burst.empty"; //$NON-NLS-1$
    private static final String ETH_DEV_RX_BURST_NON_EMPTY = "lib.ethdev.rx.burst.nonempty"; //$NON-NLS-1$
    private static final String ETH_DEV_TX_BURST = "lib.ethdev.tx.burst"; //$NON-NLS-1$
    private static final String PROFILE_ETH_DEV_RX_BURST = "lib.ethdev.rx.burst.extended"; //$NON-NLS-1$
    private static final String PROFILE_ETH_DEV_TX_BURST = "lib.ethdev.tx.burst.extended"; //$NON-NLS-1$

    /* Event field names */
    private static final String PORT_ID = "port_id"; //$NON-NLS-1$
    private static final String QUEUE_ID = "queue_id"; //$NON-NLS-1$
    private static final String NB_RX_Q = "nb_rx_q";//$NON-NLS-1$
    private static final String NB_TX_Q = "nb_tx_q";//$NON-NLS-1$
    private static final String NB_RX = "nb_rx"; //$NON-NLS-1$
    private static final String NB_TX = "nb_tx"; //$NON-NLS-1$
    private static final String NB_PKTS = "nb_pkts"; //$NON-NLS-1$
    private static final String RC = "rc"; //$NON-NLS-1$
    private static final String SIZE = "size"; //$NON-NLS-1$
    private static final String THREAD_NAME = "context.name"; //$NON-NLS-1$
    private static final String CPU_ID = "context.cpu_id"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * This event is triggered when the configuration of the Ethernet port is
     * completed.
     *
     * @return The event name.
     */
    public static String eventEthdevConfigure() {
        return ETH_DEV_CONFIGURE;
    }

    /**
     * This event is generated when an empty burst of packets is received
     *
     * @return The event name
     */
    public static String eventEthdevRxBurstEmpty() {
        return ETH_DEV_RX_BURST_EMPTY;
    }

    /**
     * This event is generated when a burst of one or more packets is received
     *
     * @return The event name
     */
    public static String eventEthdevRxBurstNonEmpty() {
        return ETH_DEV_RX_BURST_NON_EMPTY;
    }

    /**
     * This event is generated when a burst of packets is sent
     *
     * @return The event name
     */
    public static String eventEthdevTxBurst() {
        return ETH_DEV_TX_BURST;
    }

    /**
     * This event is emitted by the Ethdev profiling library when a non empty
     * burst of packets is received
     *
     * @return The event name
     */
    public static String eventProfileEthdevRxBurst() {
        return PROFILE_ETH_DEV_RX_BURST;
    }

    /**
     * This event is emitted by the Ethdev profiling library when a burst of
     * packets is sent
     *
     * @return The event name
     */
    public static String eventProfileEthdevTxBurst() {
        return PROFILE_ETH_DEV_TX_BURST;
    }
    // ------------------------------------------------------------------------
    // Event field names
    // ------------------------------------------------------------------------

    /**
     * @return The name of the field specifying the NIC port identifier
     */
    public static String fieldPortId() {
        return PORT_ID;
    }

    /**
     * @return The name of the field indicating the id of a queue attached to a
     *         port
     */
    public static String fieldQueueId() {
        return QUEUE_ID;
    }

    /**
     * @return The name of the field indicating the number of RX queues
     *         supported by a port
     */
    public static String fieldNbRxQ() {
        return NB_RX_Q;
    }

    /**
     * @return The name of the field indicating the number of TX queues
     *         supported by a port
     */
    public static String fieldNbTxQ() {
        return NB_TX_Q;
    }

    /**
     * @return The name of the field specifying the number of packets received
     *         in a burst
     */
    public static String fieldNbRxPkts() {
        return NB_RX;
    }

    /**
     * @return The field name specifying the number of packets transmitted as a
     *         burst
     */
    public static String fieldNbPkts() {
        return NB_PKTS;
    }

    /**
     * @return The field name specifying the number of packets transmitted as a
     *         burst in the profiling event
     */
    public static String fieldNbTxPkts() {
        return NB_TX;
    }

    /**
     * @return The field name containing a code value representing the success
     *         or the failure of the configuration operation
     */
    public static String fieldRc() {
        return RC;
    }

    /**
     * @return The name of the field specifying the number of bytes denoting the
     *         size of the received or transmitted burst
     */
    public static String fieldSize() {
        return SIZE;
    }

    /**
     * @return The name of the thread issuing the DPDK event
     */
    public static String fieldThreadName() {
        return THREAD_NAME;
    }

    /**
     * @return The identifier of the CPU on which the DPDK event was recorded
     */
    public static String fieldCpuId() {
        return CPU_ID;
    }
}
