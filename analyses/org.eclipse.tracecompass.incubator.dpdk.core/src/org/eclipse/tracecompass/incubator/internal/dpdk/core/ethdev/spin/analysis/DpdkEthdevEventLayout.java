package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

/**
 * The event layout class to specify event names and their fields
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevEventLayout {

    /* Event names */
    private static final String ETH_DEV_RXQ_BURST_EMPTY = "lib.ethdev.rx.burst.empty"; //$NON-NLS-1$
    private static final String ETH_DEV_RXQ_BURST_NON_EMPTY = "lib.ethdev.rx.burst.nonempty"; //$NON-NLS-1$

    /* Event field names */
    private static final String PORT_ID = "port_id"; //$NON-NLS-1$
    private static final String QUEUE_ID = "queue_id"; //$NON-NLS-1$
    private static final String NB_RX = "nb_rx"; //$NON-NLS-1$
    private static final String THREAD_NAME = "context.name"; //$NON-NLS-1$
    private static final String CPU_ID = "context.cpu_id"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * This event is generated when a burst of packets is received
     *
     * @return The event name
     */
    public static String eventEthdevRxqBurstEmpty() {
        return ETH_DEV_RXQ_BURST_EMPTY;
    }

    /**
     * This event is generated when a burst of packets is received
     *
     * @return The event name
     */
    public static String eventEthdevRxqBurstNonEmpty() {
        return ETH_DEV_RXQ_BURST_NON_EMPTY;
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
     * @return The number of packets received
     */
    public static String fieldNbRxPkts() {
        return NB_RX;
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
