/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.event;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Constants for the ftrace format
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
@NonNullByDefault
public interface IGenericFtraceConstants {

    /**
     * Timestamp field name
     */
    String TIMESTAMP = "ts"; //$NON-NLS-1$
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
     * Pattern to match an ftrace event line
     */
    Pattern FTRACE_PATTERN = Pattern.compile("^\\s*(?<comm>.*)-(?<pid>\\d+)(?:\\s+\\(.*\\))?\\s+\\[(?<cpu>\\d+)\\](?:\\s+....)?\\s+(?<timestamp>[0-9]+(?<us>\\.[0-9]+)?): (?<name>\\w+:\\s+)+(?<data>.+)"); //$NON-NLS-1$
    /**
     * PID group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_PID_GROUP = "pid"; //$NON-NLS-1$
    /**
     * CPU group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_CPU_GROUP = "cpu"; //$NON-NLS-1$
    /**
     * Timestamp group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_TIMESTAMP_GROUP = "timestamp"; //$NON-NLS-1$
    /**
     * Name group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_NAME_GROUP = "name"; //$NON-NLS-1$
    /**
     * Data group in {@link IGenericFtraceConstants#FTRACE_PATTERN}
     */
    String FTRACE_DATA_GROUP = "data"; //$NON-NLS-1$
}
