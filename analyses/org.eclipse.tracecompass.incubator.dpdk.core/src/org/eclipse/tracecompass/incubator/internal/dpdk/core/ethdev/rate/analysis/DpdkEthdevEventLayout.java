package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.rate.analysis;


/**
 * The event layout class to specify event names and their fields
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkEthdevEventLayout {

    /* Event names */
    private static final String ETH_DEV_CONFIGURE = "lib.ethdev.configure"; //$NON-NLS-1$
    private static final String ETH_DEV_RXQ_BURST_NON_EMPTY = "lib.ethdev.rx.burst.nonempty"; //$NON-NLS-1$
    private static final String ETH_DEV_TXQ_BURST = "lib.ethdev.tx.burst"; //$NON-NLS-1$

    /* Event field names */
    private static final String PORT_ID = "port_id"; //$NON-NLS-1$
    private static final String QUEUE_ID = "queue_id"; //$NON-NLS-1$
    private static final String NB_RX_Q = "nb_rx_q";//$NON-NLS-1$
    private static final String NB_TX_Q = "nb_tx_q";//$NON-NLS-1$
    private static final String NB_RX = "nb_rx"; //$NON-NLS-1$
    private static final String NB_PKTS = "nb_pkts"; //$NON-NLS-1$
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
    public static String eventEthdevRxqBurstNonEmpty() {
        return ETH_DEV_RXQ_BURST_NON_EMPTY;
    }

    /**
     * This event is generated when a burst of packets is sent
     *
     * @return The event name
     */
    public static String eventEthdevTxqBurst() {
        return ETH_DEV_TXQ_BURST;
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
     * @return The number of packets transmitted
     */
    public static String fieldNbPkts() {
        return NB_PKTS;
    }

    /**
     * @return Code indicating whether the operation was successfull or not
     */
    public static String fieldRc() {
        return RC;
    }
}
