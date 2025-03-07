/**********************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Adel Belkhiri
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.messages"; //$NON-NLS-1$
    /** Logical core is idle */
    public static @Nullable String LCoreStatus_idle = null;

    /** Logical core is running */
    public static @Nullable String LCoreStatus_running = null;

    /** Logical core status is unknown */
    public static @Nullable String LCoreStatus_unknown = null;

    /** Logical core is managed by EAL */
    public static @Nullable String LCoreRole_rte = null;

    /** Logical core is not used by the application */
    public static @Nullable String LCoreRole_off = null;

    /** Logical core is used as a service */
    public static @Nullable String LCoreRole_service = null;

    /** Logical core is not managed by EAL */
    public static @Nullable String LCoreRole_non_eal = null;

    /** Logical core role is unknown */
    public static @Nullable String LCoreRole_unknown = null;

    /** Service is registered */
    public static @Nullable String ServiceStatus_registered = null;

    /** Service is enabled */
    public static @Nullable String ServiceStatus_enabled = null;

    /** Service is waiting for execution */
    public static @Nullable String ServiceStatus_pending = null;

    /** Service is disabled */
    public static @Nullable String ServiceStatus_disabled = null;

    /** Service is running */
    public static @Nullable String ServiceStatus_running = null;

    /** Service status is unknown */
    public static @Nullable String ServiceStatus_unknown = null;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // do nothing
    }
}
