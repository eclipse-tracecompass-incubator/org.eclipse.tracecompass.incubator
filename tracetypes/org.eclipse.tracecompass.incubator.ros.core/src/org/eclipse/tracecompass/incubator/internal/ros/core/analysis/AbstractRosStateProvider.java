/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout.IRosEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.Maps;

/**
 * Base state provider for the ROS analyses
 *
 * @author Christophe Bedard
 */
public abstract class AbstractRosStateProvider extends AbstractTmfStateProvider implements IRosStateProviderInstantiator {

    /** Timer scheduled */
    public static final String TIMER_SCHEDULED = "scheduled"; //$NON-NLS-1$
    /** Node subscribers */
    public static final String SUBSCRIBERS_LIST = "Subscribers"; //$NON-NLS-1$
    /** Node publishers */
    public static final String PUBLISHERS_LIST = "Publishers"; //$NON-NLS-1$
    /** Node callbacks */
    public static final String CALLBACKS = "callbacks"; //$NON-NLS-1$
    /** Subscriber callback for message processing */
    public static final String SUBSCRIBER_MESSAGE_PROCESSING = "message processing"; //$NON-NLS-1$
    /** Topic pub/sub Queue */
    public static final String QUEUE = "queue"; //$NON-NLS-1$
    /** Queue drops */
    public static final String DROPS = "drops"; //$NON-NLS-1$
    /** Topic name prefix */
    public static final String TOPIC_PREFIX = "/"; //$NON-NLS-1$

    private static final int UNKNOWN = -1;
    private static final String UNKNOWN_NODE_NAME = "UNKNOWN_NODE"; //$NON-NLS-1$

    /** The event layout */
    protected final IRosEventLayout fLayout;

    /**
     * Map for pid -> (node_type, node_instance_name)
     */
    private Map<Long, Pair<String, String>> fNodesNamesPid = Maps.newHashMap();

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param id
     *            the ID of the corresponding analysis
     */
    public AbstractRosStateProvider(ITmfTrace trace, @NonNull String id) {
        super(checkNotNull(trace), id);
        fLayout = IRosEventLayout.getDefault();
    }

    @Override
    abstract public int getVersion();

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return getNewRosStateProviderInstance(this.getClass(), getTrace());
    }

    /**
     * Basic check to figure out if further processing should be done with an
     * event
     *
     * @param event
     *            the event
     * @return true if the event should be handled, false otherwise
     */
    protected static boolean considerEvent(@NonNull ITmfEvent event) {
        // Consider if the provider name matches
        return event.getName().startsWith(IRosEventLayout.PROVIDER_NAME);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        // Get node name association from init_node event
        if (isEvent(event, fLayout.eventInitNode())) {
            // Add info to map for later
            putNodeName(event);
        }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /**
     * Check if an event is of a given type
     *
     * @param event
     *            the event
     * @param eventName
     *            the event name to check for
     * @return true if the event is of the given type, false otherwise
     */
    protected static boolean isEvent(@NonNull ITmfEvent event, @NonNull String eventName) {
        return event.getName().equals(eventName);
    }

    /**
     * Get field value from an event
     *
     * @param event
     *            the event
     * @param fieldName
     *            the field name to get
     * @return the value of the given field name
     */
    protected static Object getField(@NonNull ITmfEvent event, @NonNull String fieldName) {
        Object val = event.getContent().getFieldValue(Object.class, fieldName);
        if (val == null) {
            System.out.println("woops"); //$NON-NLS-1$
        }
        return val;
    }

    /**
     * Add a pid -> (node_type, node_instance_name) association to the map for
     * later use. This makes it possible to identify a node based on its PID.
     *
     * @param event
     *            the node_init event
     */
    private void putNodeName(@NonNull ITmfEvent initEvent) {
        /**
         * For this event, the procname is the node type, and the instance name
         * is available from the node_name field.
         */
        fNodesNamesPid.put((Long) getField(initEvent, fLayout.contextVpid()),
                new Pair<>((String) getField(initEvent, fLayout.contextProcname()),
                        (String) getField(initEvent, fLayout.fieldNodeName())));
    }

    /**
     * Get the full node name an event is associated to
     *
     * @param event
     *            the event
     * @return the full node name (format: node_type/node_instance_name)
     */
    protected String getNodeName(@NonNull ITmfEvent event) {
        @Nullable Pair<String, String> nodeNames = fNodesNamesPid.get(getField(event, fLayout.contextVpid()));
        if (nodeNames == null) {
            return UNKNOWN_NODE_NAME;
        }
        return nodeNames.getFirst() + nodeNames.getSecond();
    }

    /**
     * Extract generic task name from full task name field
     * <p>
     * This is simply done by truncating everything starting from the first
     * underscore found, since the procname/node name is appended to the generic
     * task name with an underscore in ros::trace::task_init().
     *
     * @param fullTaskName
     *            the full task name field value
     * @return the generic task name
     */
    protected static String extractGenericTaskName(String fullTaskName) {
        return StringUtils.substringBefore(fullTaskName, "_"); //$NON-NLS-1$
    }

    /**
     * Get timer_added period time in nanoseconds
     *
     * @param event
     *            the event
     * @return the period time in nanoseconds
     */
    protected @NonNull Long getTimerPeriodInNs(@NonNull ITmfEvent event) {
        Long sec = (Long) event.getContent().getField(fLayout.fieldPeriodSec()).getValue();
        Long nSec = (Long) event.getContent().getField(fLayout.fieldPeriodNsec()).getValue();
        return nSec + (sec * (long) Math.pow(10, 9));
    }

    /**
     * @param event
     *            the event
     * @return the procname
     */
    protected String getProcname(@NonNull ITmfEvent event) {
        return (String) event.getContent().getField(fLayout.contextProcname()).getValue();
    }

    /**
     * @param event
     *            the event
     * @return the vpid
     */
    protected Long getProcessId(@NonNull ITmfEvent event) {
        Long vpid = (Long) event.getContent().getField(fLayout.contextVpid()).getValue();
        if (vpid == null) {
            return (long) UNKNOWN;
        }
        return vpid;
    }

    /**
     * Dec number to hex string format
     *
     * @param dec
     *            the base ten number
     * @return the hex number as a string
     */
    protected static String formatLongDecToHex(long dec) {
        return "0x" + Long.toHexString(dec); //$NON-NLS-1$
    }
}
