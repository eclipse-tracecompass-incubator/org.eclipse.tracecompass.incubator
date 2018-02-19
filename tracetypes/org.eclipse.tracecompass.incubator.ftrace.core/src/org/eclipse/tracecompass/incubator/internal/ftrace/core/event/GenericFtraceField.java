/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    private static final Pattern KEYVAL_PATTERN = Pattern.compile("(?<key>[^\\s=\\[\\]]+)=(?<val>[^\\s=\\[\\]]+)"); //$NON-NLS-1$
    private static final String KEYVAL_KEY_GROUP = "key"; //$NON-NLS-1$
    private static final String KEYVAL_VAL_GROUP = "val"; //$NON-NLS-1$

    private static final double SECONDS_TO_NANO = 1000000000.0;
    private static final Map<Character, @NonNull Long> PREV_STATE_LUT;

    static {
        ImmutableMap.Builder<Character, @NonNull Long> builder = new ImmutableMap.Builder<>();

        builder.put('R', (long) LinuxValues.TASK_STATE_RUNNING);
        builder.put('S', (long) LinuxValues.TASK_INTERRUPTIBLE);
        builder.put('D', (long) LinuxValues.TASK_INTERRUPTIBLE);
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
    private final String fName;
    private final Integer fCpu;
    private final @Nullable Integer fTid;
    private final @Nullable Integer fPid;
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
            name = name.substring(0, name.length() - 1);

            String attributes = matcher.group(IGenericFtraceConstants.FTRACE_DATA_GROUP);

            Map<@NonNull String, @NonNull Object> fields = new HashMap<>();
            fields.put(IGenericFtraceConstants.TIMESTAMP, timestampInNano);
            fields.put(IGenericFtraceConstants.NAME, name);

            Matcher keyvalMatcher = KEYVAL_PATTERN.matcher(attributes);
            while (keyvalMatcher.find()) {
                String key = keyvalMatcher.group(KEYVAL_KEY_GROUP);
                String value = keyvalMatcher.group(KEYVAL_VAL_GROUP);
                if (value != null) {
                    // This is a temporary solution. Refactor suggestions are welcome.
                    if (key.equals("prev_state")) { //$NON-NLS-1$
                        fields.put(key, PREV_STATE_LUT.getOrDefault(value.charAt(0), 0L));
                    } else if (StringUtils.isNumeric(value)) {
                        fields.put(key, Long.parseLong(value));
                    } else {
                        fields.put(key, value);
                    }
                }
            }
            return new GenericFtraceField(name, cpu, timestampInNano, pid, tid, fields);
        }
        return null;
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
     * Get the name of the event
     *
     * @return the event name
     */
    public String getName() {
        return fName;
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
     * Get the cpu number
     *
     * @return the cpu number
     */
    public Integer getCpu() {
        return fCpu;
    }
}
