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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.Activator;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.trace.SqliteReader.TableDescriptor;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * Trace type for SQLite 3 trace databases such as {@code array_ue_name.sqlite}.
 * <p>
 * These traces are single SQLite files. A {@code _meta_trace} table maps each
 * event table (e.g. {@code BFCNRMDBF_365}) to a dotted trace-point name (e.g.
 * {@code BFCNRMDBF.365}). Every event table shares a {@code time} column (ISO
 * timestamp with microsecond precision) plus event-specific columns. Events
 * from all tables are merged in timestamp order.
 *
 * @author Matthew Khouzam
 */
public class SQLiteTrace extends TmfTrace {

    private static final int BASE_CONFIDENCE = 5;
    private static final int META_CONFIDENCE = 20;
    private static final String META_TRACE_TABLE = "_meta_trace"; //$NON-NLS-1$
    private static final String TIME_COLUMN = "time"; //$NON-NLS-1$

    /** Parses "2024-07-23 16:30:04.623340" (optional fractional seconds). */
    private static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss") //$NON-NLS-1$
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter();

    private final List<@NonNull SqliteEvent> fEvents = new ArrayList<>();
    private final Map<@NonNull String, @NonNull TmfEventType> fEventTypes = new HashMap<>();
    private long fFileSize = 0;

    @Override
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file: " + path); //$NON-NLS-1$
        }
        // Cheaply check the magic header before opening the database.
        byte[] header = new byte[SqliteReader.MAGIC.length];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) { //$NON-NLS-1$
            if (raf.length() < header.length) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File too small"); //$NON-NLS-1$
            }
            raf.readFully(header);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot read file", e); //$NON-NLS-1$
        }
        if (!SqliteReader.hasMagic(header)) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a SQLite 3 database"); //$NON-NLS-1$
        }
        // It is a SQLite file. Boost confidence if it has a _meta_trace table.
        try (SqliteReader reader = new SqliteReader(path)) {
            for (TableDescriptor table : reader.readTables()) {
                if (META_TRACE_TABLE.equals(table.getName())) {
                    return new TraceValidationStatus(META_CONFIDENCE, Activator.class.getCanonicalName());
                }
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot read SQLite schema", e); //$NON-NLS-1$
        }
        // Valid SQLite, but not obviously one of our trace databases.
        return new TraceValidationStatus(BASE_CONFIDENCE, Activator.class.getCanonicalName());
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        File file = new File(path);
        fFileSize = file.length();
        try (SqliteReader reader = new SqliteReader(path)) {
            Map<@NonNull String, @NonNull String> tableToEventName = readMetaTrace(reader);
            for (TableDescriptor table : reader.readTables()) {
                String tableName = table.getName();
                if (tableName.startsWith("_meta") || tableName.startsWith("sqlite_")) { //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                }
                List<@NonNull String> columns = table.getColumns();
                if (!columns.contains(TIME_COLUMN)) {
                    // Not an event table (no timestamp column).
                    continue;
                }
                String eventName = tableToEventName.getOrDefault(tableName, tableName);
                fEventTypes.computeIfAbsent(eventName, name -> new TmfEventType(name, null));
                for (Map<@NonNull String, @Nullable Object> row : reader.readTableRows(table.getRootPage(), columns)) {
                    Object time = row.get(TIME_COLUMN);
                    if (!(time instanceof String)) {
                        continue;
                    }
                    long nanos = parseTimestamp((String) time);
                    fEvents.add(new SqliteEvent(nanos, eventName, row));
                }
            }
        } catch (IOException e) {
            throw new TmfTraceException("Error reading SQLite trace: " + e.getMessage(), e); //$NON-NLS-1$
        }
        // Stable sort by timestamp; events with equal timestamps keep their
        // per-table insertion order.
        fEvents.sort(Comparator.comparingLong(SqliteEvent::getTimestamp));
    }

    /**
     * Read {@code _meta_trace} to map each event table name to its dotted
     * trace-point name.
     */
    private static Map<@NonNull String, @NonNull String> readMetaTrace(SqliteReader reader) throws IOException {
        Map<@NonNull String, @NonNull String> map = new HashMap<>();
        for (TableDescriptor table : reader.readTables()) {
            if (!META_TRACE_TABLE.equals(table.getName())) {
                continue;
            }
            for (Map<@NonNull String, @Nullable Object> row : reader.readTableRows(table.getRootPage(), table.getColumns())) {
                Object tableName = row.get("table_name"); //$NON-NLS-1$
                Object name = row.get("name"); //$NON-NLS-1$
                if (tableName instanceof String && name instanceof String) {
                    map.put((String) tableName, (String) name);
                }
            }
        }
        return map;
    }

    /**
     * Parse an ISO timestamp "yyyy-MM-dd HH:mm:ss[.ffffff]" (assumed UTC) into
     * nanoseconds since the Unix epoch.
     */
    private static long parseTimestamp(String time) {
        LocalDateTime ldt = LocalDateTime.parse(time.trim(), TIME_FORMAT);
        long seconds = ldt.toEpochSecond(ZoneOffset.UTC);
        return seconds * 1_000_000_000L + ldt.getNano();
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return new TmfLongLocation(getNbEvents());
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        if (fEvents.isEmpty()) {
            return 0.0;
        }
        if (location instanceof TmfLongLocation) {
            long index = ((TmfLongLocation) location).getLocationInfo();
            return (double) index / fEvents.size();
        }
        return 0.0;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        long index = 0;
        if (location instanceof TmfLongLocation) {
            index = ((TmfLongLocation) location).getLocationInfo();
        }
        if (index < 0) {
            index = 0;
        }
        return new SqliteContext(fEvents, index);
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        long index = (long) Math.floor(ratio * fEvents.size());
        return seekEvent(new TmfLongLocation(index));
    }

    @Override
    public @Nullable ITmfEvent parseEvent(ITmfContext context) {
        if (!(context instanceof SqliteContext)) {
            return null;
        }
        SqliteContext sqliteContext = (SqliteContext) context;
        SqliteEvent event = sqliteContext.current();
        if (event == null) {
            return null;
        }
        ITmfTimestamp timestamp = TmfTimestamp.fromNanos(event.getTimestamp());
        TmfEventType eventType = fEventTypes.computeIfAbsent(event.getName(), name -> new TmfEventType(name, null));
        return new TmfEvent(this, sqliteContext.getIndex(), timestamp, eventType, buildContent(event));
    }

    @Override
    public synchronized @Nullable ITmfEvent getNext(ITmfContext context) {
        if (!(context instanceof SqliteContext)) {
            return null;
        }
        ITmfEvent event = super.getNext(context);
        if (event != null) {
            ((SqliteContext) context).advance();
        }
        return event;
    }

    private static ITmfEventField buildContent(SqliteEvent event) {
        List<ITmfEventField> fields = new ArrayList<>();
        for (Map.Entry<@NonNull String, @Nullable Object> entry : event.getFields().entrySet()) {
            Object value = entry.getValue();
            fields.add(new TmfEventField(entry.getKey(), value == null ? "" : value, null)); //$NON-NLS-1$
        }
        return new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields.toArray(new ITmfEventField[0]));
    }

    @Override
    public Iterable<@NonNull ITmfEventAspect<?>> getEventAspects() {
        List<@NonNull ITmfEventAspect<?>> aspects = new ArrayList<>();
        aspects.add(TmfBaseAspects.getTimestampAspect());
        aspects.add(TmfBaseAspects.getEventTypeAspect());
        aspects.add(new CellIdAspect());
        aspects.add(new TraceIdAspect());
        return Collections.unmodifiableList(aspects);
    }

    /** Aspect exposing the {@code cellid} column. */
    private static final class CellIdAspect implements ITmfEventAspect<Object> {
        @Override
        public String getName() {
            return "Cell ID"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return "The cell identifier of the trace event"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Object resolve(ITmfEvent event) {
            ITmfEventField field = event.getContent().getField("cellid"); //$NON-NLS-1$
            return field == null ? null : field.getValue();
        }
    }

    /** Aspect exposing the {@code traceid} column. */
    private static final class TraceIdAspect implements ITmfEventAspect<Object> {
        @Override
        public String getName() {
            return "Trace ID"; //$NON-NLS-1$
        }

        @Override
        public String getHelpText() {
            return "The trace point identifier of the trace event"; //$NON-NLS-1$
        }

        @Override
        public @Nullable Object resolve(ITmfEvent event) {
            ITmfEventField field = event.getContent().getField("traceid"); //$NON-NLS-1$
            return field == null ? null : field.getValue();
        }
    }

    /** @return the number of events read from the trace */
    long getEventCount() {
        return fEvents.size();
    }

    /** @return the trace file size in bytes */
    long getFileSize() {
        return fFileSize;
    }
}
