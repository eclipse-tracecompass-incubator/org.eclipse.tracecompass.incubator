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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import java.util.Objects;
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
    private static final String LCORES = "LCores"; //$NON-NLS-1$
    private static final String LCORE_FUNCTION = "LCORE_FUNCTION"; //$NON-NLS-1$
    private static final String LCORE_ROLE = "LCORE_ROLE"; //$NON-NLS-1$
    private static final String LCORE_STATUS = "LCORE_STATUS"; //$NON-NLS-1$

    /* Service attribute names */
    private static final String SERVICES = "Services"; //$NON-NLS-1$
    private static final String SERVICE_CORE = "service_core"; //$NON-NLS-1$
    private static final String SERVICE_NAME = "service_name"; //$NON-NLS-1$
    private static final String SERVICE_STATUS = "service_status"; //$NON-NLS-1$

    /**
     * Values for the different roles that a worker logical core can take
     */
    enum LogicalCoreRole {
        /** The logical core is running in the runtime environment */
        ROLE_RTE(String.valueOf(Messages.LCoreRole_rte)),
        /** The logical core is not running in DPDK */
        ROLE_OFF(String.valueOf(Messages.LCoreRole_off)),
        /** The logical core is running the service */
        ROLE_SERVICE(String.valueOf(Messages.LCoreRole_service)),
        /** The logical core is not running in the EAL */
        ROLE_NON_EAL(String.valueOf(Messages.LCoreRole_non_eal)),
        /** The role of the logical core is unknown */
        ROLE_UNKNOWN(String.valueOf(Messages.LCoreRole_unknown));

        private final String fLabel;

        /**
         * Constructor
         *
         * @param label
         *            String identifying the role of a LCore
         */
        LogicalCoreRole(String label) {
            this.fLabel = label;
        }

        /**
         * Get the label associated with this LCore role
         *
         * @return the label string
         */
        public String getLabel() {
            return fLabel;
        }

        /**
         * Convert to a LCore role from an integer
         *
         * @param value
         *            integer encoding the ordinal order of the LCore role
         * @return an {@link LogicalCoreRole}
         */
        public static LogicalCoreRole fromInt(int value) {
            LogicalCoreRole[] roles = LogicalCoreRole.values();
            if (value < 0 || value >= roles.length) {
                return ROLE_UNKNOWN;
            }
            return Objects.requireNonNull(roles[value]);
        }
    }

    enum LogicalCoreStatus {
        /** LCore is waiting for a new command */
        IDLE(String.valueOf(Messages.LCoreStatus_idle)),
        /** LCore is running */
        RUNNING(String.valueOf(Messages.LCoreStatus_running)),
        /** LCore status is unknown */
        UNKNOWN(String.valueOf(Messages.LCoreStatus_unknown));

        private final String fLabel;

        /**
         * Constructor
         *
         * @param label
         *            String identifying the status of a LCore
         */
        LogicalCoreStatus(String label) {
            this.fLabel = label;
        }

        /**
         * Get the label associated with this LCore status
         *
         * @return the label string
         */
        public String getLabel() {
            return fLabel;
        }

    }

    enum ServiceStatus {
        /** Service has been registered */
        REGISTERED(String.valueOf(Messages.ServiceStatus_registered)),
        /** Service is Disabled */
        DISABLED(String.valueOf(Messages.ServiceStatus_disabled)),
        /** Service is Enabled */
        ENABLED(String.valueOf(Messages.ServiceStatus_enabled)),
        /** Service is enabled but waiting to be executed */
        PENDING(String.valueOf(Messages.ServiceStatus_pending)),
        /** Service is running */
        RUNNING(String.valueOf(Messages.ServiceStatus_running)),
        /** Service status is Unknown */
        UNKNOWN(String.valueOf(Messages.ServiceStatus_unknown));

        private final String fLabel;

        /**
         * Constructor
         *
         * @param label
         *            String identifying the status of a service
         */
        ServiceStatus(String label) {
            this.fLabel = label;
        }

        /**
         * Get the label associated with this service status
         *
         * @return the label string
         */
        public String getLabel() {
            return fLabel;
        }
    }

    /**
     * Assign a role and a status to a logical core
     *
     * @param ssb
     *            State system builder
     * @param newRole
     *            Role to input in the state system
     * @param lcoreId
     *            identifier number for the logical core
     * @param timestamp
     *            time to use for state change
     */
    public static void setRole(ITmfStateSystemBuilder ssb, LogicalCoreRole newRole, int lcoreId, long timestamp) {

        // Change the status if the newRole is not service
        if (newRole != LogicalCoreRole.ROLE_SERVICE) {
            final LogicalCoreStatus newStatus;
            if (newRole == LogicalCoreRole.ROLE_RTE) {
                newStatus = LogicalCoreStatus.IDLE;
            } else if (newRole == LogicalCoreRole.ROLE_OFF
                    || newRole == LogicalCoreRole.ROLE_NON_EAL) {
                newStatus = LogicalCoreStatus.UNKNOWN;
            } else {
                Activator.getInstance().logWarning("LogicalCore setRole with unexpected role value!"); //$NON-NLS-1$
                newStatus = LogicalCoreStatus.UNKNOWN;
            }
            setStatus(ssb, newStatus, lcoreId, timestamp);
        }

        // Update the state system
        final int lcoreQuark = ssb.getQuarkAbsoluteAndAdd(LCORES, String.valueOf(lcoreId));
        final int lcoreRoleQuark = ssb.getQuarkRelativeAndAdd(lcoreQuark, LCORE_ROLE);
        ssb.modifyAttribute(timestamp, newRole.getLabel(), lcoreRoleQuark);
    }

    /**
     * Set the status of a logical core
     *
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
        ssb.modifyAttribute(timestamp, newStatus.getLabel(), lcoreRoleQuark);
    }

    /**
     * Record the function pointer executed by a logical core
     *
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
        ssb.modifyAttribute(timestamp, serviceStatus.toString(), serviceStatusQuark);
    }
}
