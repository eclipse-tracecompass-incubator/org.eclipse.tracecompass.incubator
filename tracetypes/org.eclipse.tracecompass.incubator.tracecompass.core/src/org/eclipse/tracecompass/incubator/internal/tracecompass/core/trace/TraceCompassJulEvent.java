/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracecompass.core.trace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Geneviève Bastien
 */
public class TraceCompassJulEvent extends CtfTmfEvent {

    /** Lazy-loaded field for the event name */
    private transient @Nullable String fEventName;

    /** Lazy-loaded field containing the event's payload */
    private transient @Nullable ITmfEventField fContent;

    /**
     * Constructor
     *
     * @param trace
     *            The trace to which this event belongs
     * @param rank
     *            The rank of the event
     * @param timestamp
     *            The timestamp
     * @param channel
     *            The CTF channel of this event
     * @param cpu
     *            The event's CPU
     * @param declaration
     *            The event declaration
     * @param eventDefinition
     *            The event definition
     */
    public TraceCompassJulEvent(CtfTmfTrace trace, long rank, ITmfTimestamp timestamp,
            String channel, int cpu, IEventDeclaration declaration, IEventDefinition eventDefinition) {
        super(trace, rank, timestamp, channel, cpu, declaration, eventDefinition);
    }

    @Override
    public @NonNull String getName() {
        String eventName = fEventName;
        if (eventName == null) {
            getContent();
            eventName = fEventName;
            if (eventName == null) {
                fEventName = super.getName();
                eventName = fEventName;
            }
        }
        return eventName;
    }

    @Override
    public synchronized @NonNull ITmfEventField getContent() {
        ITmfEventField content = fContent;
        if (content == null) {
            ITmfEventField baseContent = super.getContent();
            ITmfEventField field = baseContent.getField("msg"); //$NON-NLS-1$
            content = baseContent;
            if (field == null) {
                fContent = baseContent;
                return baseContent;
            }

            String msg = (String) field.getValue();
            List<ITmfEventField> fields = new ArrayList<>();
            baseContent.getFields().stream().forEach(t -> fields.add(t));
            try {
                JSONObject root = new JSONObject(msg);
                char phase = root.optString("ph", "i").charAt(0); //$NON-NLS-1$
                fields.add(new TmfEventField("ph", String.valueOf(phase), null)); //$NON-NLS-1$
                fEventName = root.optString("name"); //$NON-NLS-1$
                fields.add(new TmfEventField("evName", fEventName, null)); //$NON-NLS-1$
                String id = root.optString("id"); //$NON-NLS-1$
                if (id != null) {
                    fields.add(new TmfEventField("id", id, null)); //$NON-NLS-1$
                }
                id = root.optString("cat"); //$NON-NLS-1$
                if (id != null) {
                    fields.add(new TmfEventField("cat", id, null)); //$NON-NLS-1$
                }
                String ts = root.optString("ts");
                if (ts != null) {
                    fields.add(new TmfEventField("ts", ts, null));
                }
                JSONObject args = root.optJSONObject("args");
                if (args != null) {
                    Iterator<?> keys = args.keys();
                    while (keys.hasNext()) {
                        String key = String.valueOf(keys.next());
                        String value = args.optString(key);
                        fields.add(new TmfEventField("args." + key, value, null));
                    }
                }
            } catch (JSONException e1) {
                // invalid, return null and it will fail
            }
            content = new TmfEventField(
                    ITmfEventField.ROOT_FIELD_ID, null, fields.toArray(new @NonNull TmfEventField[fields.size()]));
            fContent = content;

        }
        return content;
    }

}
