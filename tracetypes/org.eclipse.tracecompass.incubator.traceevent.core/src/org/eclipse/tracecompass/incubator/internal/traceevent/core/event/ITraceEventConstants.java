/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Constants for the trace event format
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public interface ITraceEventConstants {

    /**
     * Timestamp field name
     */
    String TIMESTAMP = "ts"; //$NON-NLS-1$
    /**
     * Duration field name
     */
    String DURATION = "dur"; //$NON-NLS-1$
    /**
     * Name field name
     */
    String NAME = "name"; //$NON-NLS-1$
    /**
     * TID field name
     */
    String TID = "tid"; //$NON-NLS-1$
    /**
     * PID field name
     */
    String PID = "pid"; //$NON-NLS-1$
    /**
     * Phase field name
     */
    String PHASE = "ph"; //$NON-NLS-1$
    /**
     * Category field name
     */
    String CATEGORY = "cat"; //$NON-NLS-1$
    /**
     * Id field name
     */
    String ID = "id"; //$NON-NLS-1$
    /**
     * Arguments field name
     */
    String ARGS = "args"; //$NON-NLS-1$

}
