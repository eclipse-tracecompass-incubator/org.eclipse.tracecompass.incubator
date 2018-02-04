/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Trace Event fields. Used as a quick wrapper for Trace Event log data.
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class TraceEventField {
    private static final double MICRO_TO_NANO = 1000.0;

    private final long fTs;
    private final char fPhase;
    private final String fName;
    private ITmfEventField fContent;
    private final @Nullable Map<String, Object> fArgs;
    private final @Nullable Integer fTid;
    private final @Nullable String fCategory;
    private final @Nullable String fId;
    private final @Nullable Long fDuration;
    private final @Nullable Object fPid;

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @return an event field
     */
    public static @Nullable TraceEventField parseJson(String fieldsString) {
        // looks like this
        // {"ts":94824347413117,"phase":"B","tid":39,"name":"TimeGraphView:BuildThread","args"={"trace":"django-httpd"}}
        JSONObject root;
        Map<@NonNull String, @NonNull Object> argsMap = new HashMap<>();
        try {
            root = new JSONObject(fieldsString);
            long ts = 0;

            Double tso = optDouble(root, ITraceEventConstants.TIMESTAMP);
            if (Double.isFinite(tso)) {
                ts = (long) (tso * MICRO_TO_NANO);
            }
            String phase = optString(root, ITraceEventConstants.PHASE, "I"); //$NON-NLS-1$
            if (phase == null) {
                // FIXME: Easy way to avoid null warning
                phase = "I"; //$NON-NLS-1$
            }
            String name = String.valueOf(optString(root,ITraceEventConstants.NAME, "E".equals(phase) ? "exit" : "unknown")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Integer tid = optInt(root, ITraceEventConstants.TID);
            if (tid == Integer.MIN_VALUE) {
                tid = null;
            }
            Object pid = root.opt(ITraceEventConstants.PID);
            Double duration = optDouble(root, ITraceEventConstants.DURATION);
            if (Double.isFinite(duration)) {
                duration = (duration * MICRO_TO_NANO);
            }
            String category = optString(root, ITraceEventConstants.CATEGORY);
            String id = optString(root, ITraceEventConstants.ID);
            JSONObject args = optJSONObject(root, ITraceEventConstants.ARGS);
            if (args != null) {
                Iterator<?> keys = args.keys();
                while (keys.hasNext()) {
                    String key = String.valueOf(keys.next());
                    String value = args.optString(key);
                    argsMap.put("arg/" + key, String.valueOf(value)); //$NON-NLS-1$
                }
            }
            argsMap.put(ITraceEventConstants.TIMESTAMP, ts);
            argsMap.put(ITraceEventConstants.PHASE, phase);
            argsMap.put(ITraceEventConstants.NAME, name);
            if (tid != null) {
                argsMap.put(ITraceEventConstants.TID, tid);
            }
            if (pid != null) {
                argsMap.put(ITraceEventConstants.PID, pid);
            }
            if (Double.isFinite(duration)) {
                argsMap.put(ITraceEventConstants.DURATION, duration);
            }
            if (category != null) {
                argsMap.put(ITraceEventConstants.CATEGORY, category);
            }
            if (id != null) {
                argsMap.put(ITraceEventConstants.ID, id);
            }
            return new TraceEventField(name, ts, phase, pid, tid, category, id, duration, argsMap);
        } catch (JSONException e1) {
            // invalid, return null and it will fail
        }
        return null;
    }

    private static double optDouble(JSONObject root, String key) {
        return root.has(key) ? root.optDouble(key) : Double.NaN;
    }

    private static int optInt(JSONObject root, String key) {
        return root.has(key) ? root.optInt(key) : Integer.MIN_VALUE;
    }

    private static @Nullable JSONObject optJSONObject(JSONObject root, String key){
        return root.has(key) ? root.optJSONObject(key) : null;
    }

    private static @Nullable String optString(JSONObject root, String key, @Nullable String defaultValue) {
        return root.has(key) ? root.optString(key) : defaultValue;
    }

    private static @Nullable String optString(JSONObject root, String key) {
        return optString(root, key, null);
    }

    /**
     * Constructor
     *
     * @param name
     *            event name
     * @param ts
     *            the timestamp in ns
     * @param phase
     *            the phase of the event
     * @param pid
     *            the process id
     * @param tid
     *            the threadId
     * @param category
     *            the category
     * @param id
     *            the ID of the event stream
     * @param duration
     *            the duration in ns
     * @param fields
     *            event fields (arguments)
     */
    protected TraceEventField(String name, long ts, String phase, @Nullable Object pid, @Nullable Integer tid, @Nullable String category, @Nullable String id, @Nullable Double duration, Map<String, Object> fields) {
        fName = name;
        fPid = pid;
        fTid = tid;
        fCategory = category;
        fId = id;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fTs = ts;
        fDuration = duration == null ? null : Double.isFinite(duration) ? duration.longValue() : null;
        fPhase = phase.charAt(0);
        @SuppressWarnings("null")
        Map<@NonNull String, @NonNull Object> args = fields.entrySet().stream()
                .filter(entry -> {
                    return entry.getKey().startsWith("arg/"); //$NON-NLS-1$
                })
                .collect(Collectors.toMap(entry -> entry.getKey().substring(4), Entry::getValue));
        fArgs = args.isEmpty() ? null : args;

    }

    /**
     * Get the event category
     *
     * @return the event category
     */
    public @Nullable String getCategory() {
        return fCategory;
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
     * Get the event ID
     *
     * @return the event ID
     */
    public @Nullable String getId() {
        return fId;
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
     * Get the phase of the event
     *
     * @return the event phase
     */
    public char getPhase() {
        return fPhase;
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
    public long getTs() {
        return fTs;
    }

    /**
     * Get the event duration if applicable
     *
     * @return the duration or null in ns
     */
    @Nullable
    public Long getDuration() {
        return fDuration;
    }

    /**
     * Get pid
     *
     * @return the process ID
     */
    @Nullable
    public Object getPid() {
        return fPid;
    }

    /**
     * Get the arguments passed
     *
     * @return a map of the arguments and their field names
     */
    @Nullable
    public Map<String, Object> getArgs() {
        return fArgs;
    }
}
