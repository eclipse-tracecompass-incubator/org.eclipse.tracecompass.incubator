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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.trace.SqliteReader.TableDescriptor;

/**
 * The (optional) external schema of a SQLite trace database.
 * <p>
 * A trace database may declare the meaning of its event tables in a separate
 * <em>schema table</em> whose name ends in {@code trace} (for example
 * {@code _meta_trace}). Each row of that table maps an event {@code table_name}
 * to a human-readable trace-point {@code name} and, when present, additional
 * metadata such as {@code severity}, {@code source} and the printf-style
 * {@code full_text} format string.
 * <p>
 * The schema table is optional: when it is absent (or a given event table is
 * not listed in it), the event table name itself is used as the event name and
 * no extra metadata is attached.
 *
 * @author Matthew Khouzam
 */
public class SqliteSchema {

    /** Column of the schema table holding the event table name. */
    private static final String COL_TABLE_NAME = "table_name"; //$NON-NLS-1$
    /** Column of the schema table holding the dotted trace-point name. */
    private static final String COL_NAME = "name"; //$NON-NLS-1$
    private static final String COL_SEVERITY = "severity"; //$NON-NLS-1$
    private static final String COL_SOURCE = "source"; //$NON-NLS-1$
    private static final String COL_FULL_TEXT = "full_text"; //$NON-NLS-1$

    /** Per-event-table metadata read from the schema table. */
    public static class TableSchema {
        private final String fName;
        private final @Nullable String fSeverity;
        private final @Nullable String fSource;
        private final @Nullable String fFullText;

        TableSchema(String name, @Nullable String severity, @Nullable String source, @Nullable String fullText) {
            fName = name;
            fSeverity = severity;
            fSource = source;
            fFullText = fullText;
        }

        /** @return the dotted trace-point name (e.g. {@code sensor.alpha}) */
        public String getName() {
            return fName;
        }

        /** @return the severity level, or {@code null} if not declared */
        public @Nullable String getSeverity() {
            return fSeverity;
        }

        /** @return the source location, or {@code null} if not declared */
        public @Nullable String getSource() {
            return fSource;
        }

        /** @return the printf-style format string, or {@code null} */
        public @Nullable String getFullText() {
            return fFullText;
        }
    }

    private final Map<@NonNull String, @NonNull TableSchema> fByTableName;

    private SqliteSchema(Map<@NonNull String, @NonNull TableSchema> byTableName) {
        fByTableName = byTableName;
    }

    /**
     * A table is a schema table if its (lower-cased) name ends in {@code trace}.
     *
     * @param tableName
     *            the table name to test
     * @return {@code true} if the table declares the external schema
     */
    public static boolean isSchemaTable(String tableName) {
        return tableName.toLowerCase(Locale.ROOT).endsWith("trace"); //$NON-NLS-1$
    }

    /**
     * Read the external schema by scanning for a table whose name ends in
     * {@code trace}. If none exists, an empty schema is returned.
     *
     * @param reader
     *            the open SQLite reader
     * @return the schema (possibly empty, never {@code null})
     * @throws IOException
     *             on read error
     */
    public static SqliteSchema read(SqliteReader reader) throws IOException {
        Map<@NonNull String, @NonNull TableSchema> byTableName = new HashMap<>();
        for (TableDescriptor table : reader.readTables()) {
            if (!isSchemaTable(table.getName())) {
                continue;
            }
            for (Map<@NonNull String, @Nullable Object> row : reader.readTableRows(table.getRootPage(), table.getColumns())) {
                Object tableName = row.get(COL_TABLE_NAME);
                Object name = row.get(COL_NAME);
                if (!(tableName instanceof String) || !(name instanceof String)) {
                    continue;
                }
                byTableName.put((String) tableName, new TableSchema(
                        (String) name,
                        asString(row.get(COL_SEVERITY)),
                        asString(row.get(COL_SOURCE)),
                        asString(row.get(COL_FULL_TEXT))));
            }
        }
        return new SqliteSchema(byTableName);
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String ? (String) value : null;
    }

    /**
     * Get the schema entry for an event table, if declared.
     *
     * @param tableName
     *            the event table name
     * @return the entry, or {@code null} if the table is not in the schema
     */
    public @Nullable TableSchema getTableSchema(String tableName) {
        return fByTableName.get(tableName);
    }

    /**
     * Resolve the event name for an event table: the declared trace-point name
     * if the table is in the schema, otherwise the table name itself.
     *
     * @param tableName
     *            the event table name
     * @return the event name to use
     */
    public String getEventName(String tableName) {
        TableSchema schema = fByTableName.get(tableName);
        return schema != null ? schema.getName() : tableName;
    }

    /** @return {@code true} if no external schema was found */
    public boolean isEmpty() {
        return fByTableName.isEmpty();
    }
}
