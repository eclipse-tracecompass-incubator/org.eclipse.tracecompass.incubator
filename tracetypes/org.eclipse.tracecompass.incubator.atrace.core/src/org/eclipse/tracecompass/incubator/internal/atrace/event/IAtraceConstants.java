/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.atrace.event;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Constants for the atrace format
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
@NonNullByDefault
public interface IAtraceConstants {

    /** Process dump pattern */
    Pattern PROCESS_DUMP_PATTERN = Pattern
            .compile("^\\s*(?<user>.*?)\\s+(?<pid>\\d+)\\s+(?<ppid>\\d+)\\s+(?<vsz>\\d+)\\s+(?<rss>\\d+)\\s+(?<wchan>.*?)\\s+(?<pc>.*?)\\s+(?<s>.*?)\\s+(?<name>.*?)?"); //$NON-NLS-1$

    /** Trace event pattern */
    Pattern TRACE_EVENT_PATTERN = Pattern.compile("(?<phase>\\w)(\\|(?<tid>\\d+)\\|(?<content>[^\\|]+))?"); //$NON-NLS-1$

}
