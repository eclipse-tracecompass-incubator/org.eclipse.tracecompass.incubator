/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

/**
 * The event layout class to specify event names and their fields
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkLogicalCoreEventLayout {

    /* Event names */
    private static final String LCORE_STATE_CHANGE = "lib.eal.lcore.state.change"; //$NON-NLS-1$
    private static final String THREAD_LCORE_READY = "lib.eal.thread.lcore.ready"; //$NON-NLS-1$
    private static final String THREAD_LCORE_RUNNING = "lib.eal.thread.lcore.running"; //$NON-NLS-1$
    private static final String THREAD_LCORE_STOPPED = "lib.eal.thread.lcore.stopped"; //$NON-NLS-1$
    private static final String SERVICE_LCORE_START = "lib.eal.service.lcore.start"; //$NON-NLS-1$
    private static final String SERVICE_LCORE_STOP = "lib.eal.service.lcore.stop"; //$NON-NLS-1$
    private static final String SERVICE_RUN_BEGIN = "lib.eal.service.run.begin"; //$NON-NLS-1$
    private static final String SERVICE_RUN_STATE_SET = "lib.eal.service.run.state.set"; //$NON-NLS-1$
    private static final String SERVICE_RUN_END = "lib.eal.service.run.end"; //$NON-NLS-1$
    private static final String SERVICE_MAP_LCORE = "lib.eal.service.map.lcore"; //$NON-NLS-1$
    private static final String SERVICE_COMPONENT_REGISTER = "lib.eal.service.component.register"; //$NON-NLS-1$

    /* Event field names */
    private static final String ENABLED = "enabled"; //$NON-NLS-1$
    private static final String F = "f"; //$NON-NLS-1$
    private static final String ID = "id"; //$NON-NLS-1$
    private static final String LCORE_ID = "lcore_id"; //$NON-NLS-1$
    private static final String LCORE_STATE = "lcore_state"; //$NON-NLS-1$
    private static final String RUN_STATE = "run_state"; //$NON-NLS-1$
    private static final String SERVICE_NAME = "service_name"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /**
     * This event is generated at the end of the function that changes the state
     * of a logical core
     *
     * @return The event name
     */
    public String eventLcoreStateChange() {
        return LCORE_STATE_CHANGE;
    }

    /**
     * This event is generated after the initialization is finished in the
     * eal_thread_loop function which is the main loop of the threads
     *
     * @return The event name
     */
    public String eventThreadLcoreReady() {
        return THREAD_LCORE_READY;
    }

    /**
     * This event is generated in the main loop of the threads when a function
     * to execute is received. Then the tracepoint thread_lcore_stopped is hit
     *
     * @return The event name
     */
    public String eventThreadLcoreRunning() {
        return THREAD_LCORE_RUNNING;
    }

    /**
     * This event is generated after a function is executed in the main loop of
     * the working threads
     *
     * @return The event name
     */
    public String eventThreadLcoreStopped() {
        return THREAD_LCORE_STOPPED;
    }

    /**
     * This event is generated when a service is launched on one logical core
     *
     * @return The event name
     */
    public String eventServiceLcoreStart() {
        return SERVICE_LCORE_START;
    }

    /**
     * This event is generated when a service has finished its execution on one
     * logical core
     *
     * @return The event name
     */
    public String eventServiceLcoreStop() {
        return SERVICE_LCORE_STOP;
    }

    /**
     * This event is generated when a service begins running on one logical core
     *
     * @return The event name
     */
    public String eventServiceRunBegin() {
        return SERVICE_RUN_BEGIN;
    }

    /**
     * This event is generated when the runstate of the service is set. Each
     * service is either running or stopped Setting a non-zero runstate enables
     * the service to run, while setting runstate zero disables it.
     *
     * @return The event name
     */
    public String eventServiceRunStateSet() {
        return SERVICE_RUN_STATE_SET;
    }

    /**
     * This event is generated when a service has finished executing.
     *
     * @return The event name
     */
    public String eventServiceRunEnd() {
        return SERVICE_RUN_END;
    }

    /**
     * This event is generated when an lcore is mapped or unmapped to a service.
     *
     * Each core can be added or removed from running a specific service. If
     * multiple cores are enabled on a service, a lock is used to ensure that
     * only one core runs the service at a time.
     *
     * @return The event name
     */
    public String eventServiceMapLcore() {
        return SERVICE_MAP_LCORE;
    }

    /**
     * This event is generated when a new service is registered.
     *
     * A service represents a component that requires CPU time periodically to
     * achieve its purpose. For example the eventdev SW PMD requires CPU cycles
     * to perform its scheduling. This can be achieved by registering it as a
     * service, and the application can then assign CPU resources to that
     * service.
     *
     * @return The event name
     */
    public String eventServiceComponentRegister() {
        return SERVICE_COMPONENT_REGISTER;
    }

    // ------------------------------------------------------------------------
    // Event field names
    // ------------------------------------------------------------------------

    /**
     * @return The name of the field specifying if it is enabled
     */
    public String fieldEnabled() {
        return ENABLED;
    }

    /**
     * @return The name of the field specifying the function address
     */
    public String fieldF() {
        return F;
    }

    /**
     * @return The name of the field specifying the id number
     */
    public String fieldId() {
        return ID;
    }

    /**
     * @return The name of the field specifying the logical core id
     */
    public String fieldLcoreId() {
        return LCORE_ID;
    }

    /**
     * @return The name of the field specifying the logical core state
     */
    public String fieldLcoreState() {
        return LCORE_STATE;
    }

    /**
     * @return The name of the field specifying the run state
     */
    public String fieldRunState() {
        return RUN_STATE;
    }

    /**
     * @return The name of the field specifying the service name
     */
    public String fieldServiceName() {
        return SERVICE_NAME;
    }
}
