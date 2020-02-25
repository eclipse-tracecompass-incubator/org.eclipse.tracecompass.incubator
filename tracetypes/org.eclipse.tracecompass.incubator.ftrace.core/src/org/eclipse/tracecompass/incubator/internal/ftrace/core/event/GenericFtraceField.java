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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.event;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.LinuxValues;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ftrace field class
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
@NonNullByDefault
public class GenericFtraceField {

    private static final Pattern KEYVAL_PATTERN = Pattern.compile("(?<key>[^\\s=\\[\\],]+)(=|:)\\s*(?<val>[^\\s=\\[\\],]+)"); //$NON-NLS-1$
    private static final String KEYVAL_KEY_GROUP = "key"; //$NON-NLS-1$
    private static final String KEYVAL_VAL_GROUP = "val"; //$NON-NLS-1$

    private static final double SECONDS_TO_NANO = 1000000000.0;
    private static final Map<Character, @NonNull Long> PREV_STATE_LUT;

    static {
        ImmutableMap.Builder<Character, @NonNull Long> builder = new ImmutableMap.Builder<>();

        builder.put('R', (long) LinuxValues.TASK_STATE_RUNNING);
        builder.put('S', (long) LinuxValues.TASK_INTERRUPTIBLE);
        builder.put('D', (long) LinuxValues.TASK_UNINTERRUPTIBLE);
        builder.put('T', (long) LinuxValues.TASK_STOPPED__);
        builder.put('t', (long) LinuxValues.TASK_TRACED__);
        builder.put('X', (long) LinuxValues.EXIT_ZOMBIE);
        builder.put('x', (long) LinuxValues.EXIT_ZOMBIE);
        builder.put('Z', (long) LinuxValues.EXIT_DEAD);
        builder.put('P', (long) LinuxValues.TASK_DEAD);
        builder.put('I', (long) LinuxValues.TASK_WAKEKILL);
        PREV_STATE_LUT = builder.build();
    }

    private final Long fTs;
    private String fName;
    private final Integer fCpu;
    private @Nullable Integer fTid;
    private @Nullable Integer fPid;
    private ITmfEventField fContent;

    /**
     * Constructor
     *
     * @param name   event name
     * @param cpu    the cpu number
     * @param ts     the timestamp in ns
     * @param pid    the process id
     * @param tid    the threadId
     * @param fields event fields (arguments)
     */
    public GenericFtraceField(String name, Integer cpu, Long ts, @Nullable Integer pid, @Nullable Integer tid, Map<String, Object> fields) {
        fName = name;
        fCpu = cpu;
        fPid = pid;
        fTid = tid;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fTs = ts;
    }

    /**
     * Parse a line from an ftrace ouput file
     *
     * @param line The string to parse
     * @return An event field
     */
    public static @Nullable GenericFtraceField parseLine(String line) {
        Matcher matcher = IGenericFtraceConstants.FTRACE_PATTERN.matcher(line);
        if (matcher.matches()) {
            Integer pid = Integer.parseInt(matcher.group(IGenericFtraceConstants.FTRACE_PID_GROUP));
            Integer tid = pid;
            Integer cpu = Integer.parseInt(matcher.group(IGenericFtraceConstants.FTRACE_CPU_GROUP));
            Double timestampInSec = Double.parseDouble(matcher.group(IGenericFtraceConstants.FTRACE_TIMESTAMP_GROUP));
            Long timestampInNano = (long) (timestampInSec * SECONDS_TO_NANO);

            String name = matcher.group(IGenericFtraceConstants.FTRACE_NAME_GROUP);
            name = name.trim();

            String separator = matcher.group(IGenericFtraceConstants.FTRACE_SEPARATOR_GROUP);
            separator = separator.trim();

            String attributes = matcher.group(IGenericFtraceConstants.FTRACE_DATA_GROUP);

            name = eventNameRewrite(name, separator);

            /*
             * There's no distinction between pid and tid in scheduling events. However,when there's a mismatch
             * between the tgid and the pid, we know the event happened on a thread and that
             * the tgid is the actual pid, and the pid the tid.
             */
            String tgid = matcher.group(IGenericFtraceConstants.FTRACE_TGID_GROUP);
            if (tgid != null) {
                Integer tgidNumeric = Integer.parseInt(tgid);
                if (!tgidNumeric.equals(pid)) {
                    pid = tgidNumeric;
                }
            }

            Map<@NonNull String, @NonNull Object> fields = new HashMap<>();

            Matcher keyvalMatcher = KEYVAL_PATTERN.matcher(attributes);
            while (keyvalMatcher.find()) {
                String key = keyvalMatcher.group(KEYVAL_KEY_GROUP);
                String value = keyvalMatcher.group(KEYVAL_VAL_GROUP);
                if (value != null) {
                    // This is a temporary solution. Refactor suggestions are welcome.
                    if (key.equals("prev_state")) { //$NON-NLS-1$
                        fields.put(key, parsePrevStateValue(value));
                    } else if (StringUtils.isNumeric(value)) {
                        if (key.equals("parent_pid") && name.equals("sched_process_fork")) {//$NON-NLS-1$ //$NON-NLS-2$
                            key = "pid"; //$NON-NLS-1$
                        }
                        fields.put(key, Long.parseUnsignedLong(value));
                    } else {
                        fields.put(key, decodeString(value));
                    }
                }
            }

            /*
             * If anything else fails, but we have discovered sort of a valid event
             * attributes lets just add the unparsed attributes with key "data".
             */
            if (fields.isEmpty() && attributes != null && !attributes.isEmpty()) {
                String key = "data"; //$NON-NLS-1$
                if (name.equals(IGenericFtraceConstants.FTRACE_EXIT_SYSCALL)) {
                    key = "ret"; //$NON-NLS-1$
                }
                fields.put(key, decodeString(attributes));
            }

            return new GenericFtraceField(name, cpu, timestampInNano, pid, tid, fields);
        }
        return null;
    }

    private static Object decodeString(String val) {
        try {
            if (val.startsWith("0x") || val.startsWith("0X")) { //$NON-NLS-1$ //$NON-NLS-2$
                // Chances are this is an hexadecimal string. Parse the value
                return Long.parseUnsignedLong(val.substring(2), 16);
            }
        } catch (NumberFormatException e) {
            // Fall back to returning the string
        }
        return val;
    }

    /**
     * Get the event content
     *
     * @return the event content
     */
    public ITmfEventField getContent() {
        return fContent;
    }

    /**
     * Set the event's content
     *
     * @param fields Map of field values
     */
    public void setContent(Map<String, Object> fields) {
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
    }

    /**
     * Get the name of the event
     *
     * @return the event name
     */
    public String getName() {
        return fName;
    }

    /**
     * Set the event's name
     *
     * @param name New name of the event
     */
    public void setName(String name) {
        fName = name;
    }

    /**
     * Get the TID of the event
     *
     * @return the event TID
     */
    public @Nullable Integer getTid() {
        return fTid;
    }

    /**
     * Set the TID of the event
     *
     * @param tid The new tid
     */
    public void setTid(Integer tid) {
        fTid = tid;
    }

    /**
     * Get the timestamp
     *
     * @return the timestamp in ns
     */
    public Long getTs() {
        return fTs;
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
     * @param pid The new pid
     */
    public void setPid(Integer pid) {
        fPid = pid;
    }

    /**
     * Get the cpu number
     *
     * @return the cpu number
     */
    public Integer getCpu() {
        return fCpu;
    }

    /**
     * Parse the prev_state field on sched_switch event depending on whether it is a number or a character.
     *
     *
     * @return the state as a Long
     */
    private static Long parsePrevStateValue(String value) {
        Long state = 0L;
        if (StringUtils.isNumeric(value)) {
            state = Long.parseUnsignedLong(value);
        } else {
            state = PREV_STATE_LUT.getOrDefault(value.charAt(0), 0L);
        }
        return state;
    }

    /**
     * Searches for certain event names and rewrites them in order for different analysis to work.
     *
     *
     * @return the new or original event name
     */
    private static String eventNameRewrite(@Nullable String name, @Nullable String separator) {
        if (name == null) {
            return ""; //$NON-NLS-1$
        }

        /*
         * Rewrite syscall exit events to conform to syscall analysis.
         */
        if ((name.startsWith(IGenericFtraceConstants.FTRACE_SYSCALL_PREFIX) && separator != null && separator.equals(IGenericFtraceConstants.FTRACE_EXIT_SYSCALL_SEPARATOR)) ||
             name.startsWith(IGenericFtraceConstants.FTRACE_SYSCALL_EXIT_TRACECMD_PREFIX)
           ) {
            return IGenericFtraceConstants.FTRACE_EXIT_SYSCALL;
        }

        /*
         * Rewrite syscall enter from trace-cmd traces to conform to syscall analysis.
         */
        if (name.startsWith(IGenericFtraceConstants.FTRACE_SYSCALL_ENTER_TRACECMD_PREFIX)) {
            String newname = name.replaceFirst(IGenericFtraceConstants.FTRACE_SYSCALL_ENTER_TRACECMD_PREFIX, IGenericFtraceConstants.FTRACE_SYSCALL_PREFIX);
            if (newname != null) {
                return newname;
            }
        }

        return name;
    }
}
