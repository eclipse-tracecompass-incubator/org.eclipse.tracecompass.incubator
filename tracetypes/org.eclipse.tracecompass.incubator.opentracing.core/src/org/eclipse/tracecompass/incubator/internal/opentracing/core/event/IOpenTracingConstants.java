/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.event;

/**
 * Constants for the open tracing format
 *
 * @author Katherine Nadeau
 */
public interface IOpenTracingConstants {

    /**
     * traceID field name
     */
    String TRACE_ID = "traceID"; //$NON-NLS-1$
    /**
     * spanID field name
     */
    String SPAN_ID = "spanID"; //$NON-NLS-1$
    /**
     * flags field name (specific to jaeger tracer)
     */
    String FLAGS = "flags"; //$NON-NLS-1$
    /**
     * operationName field name
     */
    String OPERATION_NAME = "operationName"; //$NON-NLS-1$
    /**
     * references field name
     */
    String REFERENCES = "references"; //$NON-NLS-1$
    /**
     * reference type field name
     */
    String REFERENCE_TYPE = "refType"; //$NON-NLS-1$
    /**
     * startTime field name
     */
    String START_TIME = "startTime"; //$NON-NLS-1$
    /**
     * duration field name
     */
    String DURATION = "duration"; //$NON-NLS-1$
    /**
     * tags field name
     */
    String TAGS = "tags"; //$NON-NLS-1$
    /**
     * logs field name
     */
    String LOGS = "logs"; //$NON-NLS-1$
    /**
     * processID field name
     */
    String PROCESS_ID = "processID"; //$NON-NLS-1$
    /**
     * processID field name
     */
    String PROCESS_NAME = "processName"; //$NON-NLS-1$
    /**
     * service field name
     */
    String SERVICE_NAME = "serviceName"; //$NON-NLS-1$
    /**
     * process tags field name
     */
    String PROCESS_TAGS = "processTags"; //$NON-NLS-1$
    /**
     * key field name
     */
    String KEY = "key"; //$NON-NLS-1$
    /**
     * value field name
     */
    String VALUE = "value"; //$NON-NLS-1$
    /**
     * timestamp field name
     */
    String TIMESTAMP = "timestamp"; //$NON-NLS-1$
    /**
     * fields field name
     */
    String FIELDS = "fields"; //$NON-NLS-1$

}
