package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;


/**
 * The event layout class to specify event names and their fields
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevEventLayout {

    /* Event names */
    private static final String ETH_DEV_CONFIGURE = "lib.ethdev.configure"; //$NON-NLS-1$
    private static final String PROFILE_ETH_DEV_TX_BURST = "lib.ethdev.tx.burst.extended"; //$NON-NLS-1$
    private static final String PROFILE_ETH_DEV_RX_BURST = "lib.ethdev.rx.burst.extended"; //$NON-NLS-1$

    /* Event field names */
    private static final String PORT_ID = "port_id"; //$NON-NLS-1$
    private static final String QUEUE_ID = "queue_id"; //$NON-NLS-1$
    private static final String NB_RX_Q = "nb_rx_q";//$NON-NLS-1$
    private static final String NB_TX_Q = "nb_tx_q";//$NON-NLS-1$
    private static final String NB_RX = "nb_rx"; //$NON-NLS-1$
    private static final String SIZE = "size"; //$NON-NLS-1$
    private static final String NB_TX = "nb_tx"; //$NON-NLS-1$
    private static final String RC = "rc"; //$NON-NLS-1$


    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * This event is triggered when the app finishes configuring the Ethernet device.
     *
     * @return The event name
     */
    public static String eventEthdevConfigure() {
        return ETH_DEV_CONFIGURE;
    }

    /**
     * This event is generated when a burst of packets is received
     *
     * @return The event name
     */
    public static String eventProfileEthdevRxBurst() {
        return PROFILE_ETH_DEV_RX_BURST;
    }

    /**
     * This event is generated when a burst of packets is sent
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
     * @return Number identifying a NIC port
     */
    public static String fieldPortId() {
        return PORT_ID;
    }

    /**
     * @return Number identifying a queue that is associated to a NIC port
     */
    public static String fieldQueueId() {
        return QUEUE_ID;
    }

    /**
     * @return The number of configured Rx queues
     */
    public static String fieldNbRxQ() {
        return NB_RX_Q;
    }

    /**
     * @return The number of configured Tx queues
     */
    public static String fieldNbTxQ() {
        return NB_TX_Q;
    }

    /**
     * @return The number of packets received
     */
    public static String fieldNbRxPkts() {
        return NB_RX;
    }

    /**
     * @return The number of packets received
     */
    public static String fieldNbTxPkts() {
        return NB_TX;
    }

    /**
     * @return The number of packets received
     */
    public static String fieldSize() {
        return SIZE;
    }

    /**
     * @return Code indicating whether the operation was successfull or not
     */
    public static String fieldRc() {
        return RC;
    }
}
