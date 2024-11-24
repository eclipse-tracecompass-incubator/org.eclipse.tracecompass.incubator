/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis;

/**
 * Type for the {@link Ros2ObjectTimeGraphEntryModel}.
 *
 * @author Christophe Bedard
 */
public enum Ros2ObjectTimeGraphEntryModelType {
    /** Trace */
    TRACE,
    /** Node */
    NODE,
    /** Publisher */
    PUBLISHER,
    /** Subscription */
    SUBSCRIPTION,
    /** Client */
    CLIENT,
    /** Service */
    SERVICE,
    /** Timer */
    TIMER;
}
