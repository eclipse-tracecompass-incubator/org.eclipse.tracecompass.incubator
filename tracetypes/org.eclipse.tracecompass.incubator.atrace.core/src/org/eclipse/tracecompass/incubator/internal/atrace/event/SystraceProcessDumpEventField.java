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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.IGenericFtraceConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Systrace process dump event field.
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
@NonNullByDefault
public class SystraceProcessDumpEventField extends TmfEventField {

    private @Nullable Integer fPpid;
    private @Nullable Integer fPid;

    /**
     * Constructor
     *
     * @param name
     *            event name
     * @param pid
     *            the process id
     * @param pPid
     *            the parent process id
     * @param fields
     *            event fields (arguments)
     */
    public SystraceProcessDumpEventField(String name, @Nullable Integer pid, @Nullable Integer pPid, Map<String, Object> fields) {
        super(name, fields, fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new));
        fPid = pid;
        fPpid = pPid;
    }

    /**
     * Parse a line from an process dump in Systrace Output
     *
     * @param line
     *            The string to parse
     * @return An event field
     */
    public static @Nullable SystraceProcessDumpEventField parseLine(@Nullable String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = IAtraceConstants.PROCESS_DUMP_PATTERN.matcher(line);
        if (matcher.matches()) {
            String processName = matcher.group("name"); //$NON-NLS-1$
            Integer fPid = Integer.parseInt(matcher.group("pid")); //$NON-NLS-1$
            Integer fPpid = Integer.parseInt(matcher.group("ppid")); //$NON-NLS-1$

            Map<@NonNull String, @NonNull Object> fields = new HashMap<>();
            fields.put("name", String.valueOf(processName)); //$NON-NLS-1$
            fields.put("pid", (long) fPid); //$NON-NLS-1$
            fields.put("ppid", (long) fPpid); //$NON-NLS-1$
            fields.put("tid", (long) fPid); //$NON-NLS-1$
            fields.put("status", (long) 2); //$NON-NLS-1$

            return new SystraceProcessDumpEventField(IGenericFtraceConstants.PROCESS_DUMP_EVENT_NAME, fPid, fPpid, fields);
        }
        return null;
    }

    /**
     * Get the PPid of the event
     *
     * @return the parent process ID
     */
    @Nullable
    public Integer getPpid() {
        return fPpid;
    }

    /**
     * Set the PPID of the event
     *
     * @param Ppid
     *            the new parent process ID
     */
    public void setPpid(Integer Ppid) {
        this.fPpid = Ppid;
    }

    /**
     * Get pid
     *
     * @return the process ID
     */
    @Nullable
    public Integer getPid() {
        return fPid;
    }

    /**
     * Set the PID of the event
     *
     * @param pid
     *            The new pid
     */
    public void setPid(Integer pid) {
        this.fPid = pid;
    }

}
