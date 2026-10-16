/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.agent.trace.core.event;

/**
 * Constants for Agent Trace event field names.
 *
 * @author Eya Matkho
 */
public interface IAgentTraceConstants {

    /** Field name for the event type */
    String TYPE = "type"; //$NON-NLS-1$

    /** Field name for the timestamp */
    String TIMESTAMP = "timestamp"; //$NON-NLS-1$

    /** Field name for the event ID */
    String EVENT_ID = "event_id"; //$NON-NLS-1$

    /** Field name for the parent ID */
    String PARENT_ID = "parent_id"; //$NON-NLS-1$

    /** Field name for the metadata object */
    String METADATA = "metadata"; //$NON-NLS-1$

    /** Field name for the data object */
    String DATA = "data"; //$NON-NLS-1$
}
