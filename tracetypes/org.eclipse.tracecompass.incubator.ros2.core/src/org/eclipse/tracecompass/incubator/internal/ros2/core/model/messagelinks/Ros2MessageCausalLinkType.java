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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks;

/**
 * Types of message causal links.
 *
 * @author Christophe Bedard
 */
public enum Ros2MessageCausalLinkType {
    /** Periodic asynchronous many-to-many */
    PERIODIC_ASYNC,
    /** Partially synchronous many-to-many */
    PARTIAL_SYNC
}
