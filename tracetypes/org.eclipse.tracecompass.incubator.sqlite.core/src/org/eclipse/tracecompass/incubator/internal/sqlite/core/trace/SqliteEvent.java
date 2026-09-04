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

package org.eclipse.tracecompass.incubator.internal.sqlite.core.trace;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A single trace event read from a SQLite trace table row.
 * <p>
 * Each event carries a timestamp (in nanoseconds since the Unix epoch), an
 * event name (the dotted trace point name from {@code _meta_trace.name}, e.g.
 * {@code sensor.alpha}) and the row's column values.
 *
 * @author Matthew Khouzam
 */
public class SqliteEvent {

    private final long fTimestamp;
    private final String fName;
    private final Map<@NonNull String, @Nullable Object> fFields;
    private final SqliteSchema.@Nullable TableSchema fSchema;

    /**
     * Constructor.
     *
     * @param timestamp
     *            timestamp in nanoseconds since the Unix epoch
     * @param name
     *            the event (trace point) name
     * @param fields
     *            the column name to value mapping for the row
     * @param schema
     *            the external schema entry for this event's table, or
     *            {@code null} if the trace has no schema table or the table is
     *            not declared in it
     */
    public SqliteEvent(long timestamp, String name, Map<@NonNull String, @Nullable Object> fields, SqliteSchema.@Nullable TableSchema schema) {
        fTimestamp = timestamp;
        fName = name;
        fFields = fields;
        fSchema = schema;
    }

    /** @return the external schema entry, or {@code null} if none */
    public SqliteSchema.@Nullable TableSchema getSchema() {
        return fSchema;
    }

    /** @return the timestamp in nanoseconds since the Unix epoch */
    public long getTimestamp() {
        return fTimestamp;
    }

    /** @return the event (trace point) name */
    public String getName() {
        return fName;
    }

    /** @return the column name to value mapping for the row */
    public Map<@NonNull String, @Nullable Object> getFields() {
        return fFields;
    }
}
