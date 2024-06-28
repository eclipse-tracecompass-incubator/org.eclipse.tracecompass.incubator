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

import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;

/**
 * Immutable model object for Logical Core Analysis. Manages also the service
 * states
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class LogicalCore {

    /* Logical Core attribute names */
    private static final String LCORES = "LCORES"; //$NON-NLS-1$
    private static final String LCORE_FUNCTION = "LCORE_FUNCTION"; //$NON-NLS-1$
    private static final String LCORE_ROLE = "LCORE_ROLE"; //$NON-NLS-1$
    private static final String LCORE_STATUS = "LCORE_STATUS"; //$NON-NLS-1$

    /* Service attribute names */
    private static final String SERVICES = "services"; //$NON-NLS-1$
    private static final String SERVICE_CORE = "service_core"; //$NON-NLS-1$
    private static final String SERVICE_NAME = "service_name"; //$NON-NLS-1$
    private static final String SERVICE_STATUS = "service_status"; //$NON-NLS-1$

    /**
     * Values for the different roles that a logical core can take
     */
    public final class LogicalCoreRole {
        /** The logical core is running in the runtime environment */
        public static final int ROLE_RTE = 0;
        /** The logical core is not running in DPDK */
        public static final int ROLE_OFF = 1;
        /** The logical core is running the service */
        public static final int ROLE_SERVICE = 2;
        /** The logical core is not running in the EAL */
        public static final int ROLE_NON_EAL = 3;
    }

    enum LogicalCoreStatus {
        /** Lcore is waiting for a new command */
        IDLE,
        /** Lcore is running */
        RUNNING,
        /** Not yet ready to start scheduling services */
        DISABLED,
        /** Command is executed */
        OFF;
    }

    enum ServiceStatus {
        /** Unknown Status */
        UNKNOWN,
        /** Service has been registered */
        REGISTERED,
        /** Service is Disabled */
        DISABLED,
        /** Service is Enabled */
        ENABLED,
        /** Service is enabled but waiting to be executed */
        PENDING,
        /** Service is running */
        RUNNING;
    }

    /**
     * @param ssb
     *            State system builder
     * @param newRole
     *            Role integer value to input in the state system
     * @param lcoreId
     *            identifier number for the logical core
     * @param timestamp
     *            time to use for state change
     */
    public static void setRole(ITmfStateSystemBuilder ssb, Integer newRole, Integer lcoreId, long timestamp) {
        LogicalCoreStatus newStatus;
        if (newRole == LogicalCoreRole.ROLE_RTE) {
            newStatus = LogicalCoreStatus.IDLE;
        } else if (newRole == LogicalCoreRole.ROLE_OFF) {
            newStatus = LogicalCoreStatus.OFF;
        } else {
            Activator.getInstance().logWarning("LogicalCore setRole with unexpected role value " + newRole.toString()); //$NON-NLS-1$
            newStatus = LogicalCoreStatus.IDLE;
        }
        setStatus(ssb, newStatus, lcoreId, timestamp);
        // Update State system
        int lcoreQuark = ssb.getQuarkAbsoluteAndAdd(LCORES, String.valueOf(lcoreId));
        int lcoreRoleQuark = ssb.getQuarkRelativeAndAdd(lcoreQuark, LCORE_ROLE);
        ssb.modifyAttribute(timestamp, newRole, lcoreRoleQuark);
    }

    /**
     * @param ssb
     *            State System builder
     * @param newStatus
     *            Status enum value to update the state system
     * @param lcoreId
     *            identifier number for the logical core
     * @param timestamp
     *            time to use for state change
     */
    public static void setStatus(ITmfStateSystemBuilder ssb, LogicalCoreStatus newStatus, Integer lcoreId, long timestamp) {
        int lcoreQuark = ssb.getQuarkAbsoluteAndAdd(LCORES, String.valueOf(lcoreId));
        int lcoreRoleQuark = ssb.getQuarkRelativeAndAdd(lcoreQuark, LCORE_STATUS);
        ssb.modifyAttribute(timestamp, newStatus, lcoreRoleQuark);
    }

    /**
     * @param ssb
     *            State System builder
     * @param function
     *            the function address that is running on the logical core
     * @param lcoreId
     *            identifier number for the logical core
     * @param timestamp
     *            time to use for state change
     */
    public static void setFunction(ITmfStateSystemBuilder ssb, Long function, Integer lcoreId, long timestamp) {
        int lcoreQuark = ssb.getQuarkAbsoluteAndAdd(LCORES, String.valueOf(lcoreId));
        int lcoreFuncQuark = ssb.getQuarkRelativeAndAdd(lcoreQuark, LCORE_FUNCTION);
        ssb.modifyAttribute(timestamp, Long.toHexString(function), lcoreFuncQuark);
    }

    private static int getServiceQuark(ITmfStateSystemBuilder ssb, Integer serviceId) {
        return ssb.getQuarkAbsoluteAndAdd(SERVICES, String.valueOf(serviceId));
    }

    /**
     * @param ssb
     *            State System builder
     * @param lcoreId
     *            identifier number for the logical core
     * @param serviceId
     *            identifier number for the service
     * @param timestamp
     *            time to use for state change
     */
    public static void setServiceLcore(ITmfStateSystemBuilder ssb, Integer lcoreId, Integer serviceId, long timestamp) {
        int serviceLcoreQuark = ssb.getQuarkRelativeAndAdd(getServiceQuark(ssb, serviceId), SERVICE_CORE);
        ssb.modifyAttribute(timestamp, String.valueOf(lcoreId), serviceLcoreQuark);
    }

    /**
     * @param ssb
     *            State System builder
     * @param serviceName
     *            name of the service currently
     * @param serviceId
     *            identifier number for the service
     * @param timestamp
     *            time to use for state change
     */
    public static void setServiceName(ITmfStateSystemBuilder ssb, String serviceName, Integer serviceId, long timestamp) {
        int serviceNameQuark = ssb.getQuarkRelativeAndAdd(getServiceQuark(ssb, serviceId), SERVICE_NAME);
        ssb.modifyAttribute(timestamp, serviceName, serviceNameQuark);
    }

    /**
     * @param ssb
     *            State System builder
     * @param serviceStatus
     *            Status enum value to update the state system
     * @param serviceId
     *            identifier number for the service
     * @param timestamp
     *            time to use for state change
     */
    public static void setServiceStatus(ITmfStateSystemBuilder ssb, ServiceStatus serviceStatus, Integer serviceId, long timestamp) {
        int serviceStatusQuark = ssb.getQuarkRelativeAndAdd(getServiceQuark(ssb, serviceId), SERVICE_STATUS);
        ssb.modifyAttribute(timestamp, serviceStatus, serviceStatusQuark);
    }
}
