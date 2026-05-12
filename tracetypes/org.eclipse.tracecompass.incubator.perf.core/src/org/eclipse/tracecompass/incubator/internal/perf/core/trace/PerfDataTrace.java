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

package org.eclipse.tracecompass.incubator.internal.perf.core.trace;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.perf.core.Activator;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfConstants;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfDataReader;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfRecord;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.ImmutableList;

/**
 * A {@link TmfTrace} backed by the Linux {@code perf.data} binary format.
 *
 * The heavy lifting is done by {@link PerfDataReader}. This class wires the
 * parsed {@link PerfRecord} stream into {@link ITmfEvent} instances and
 * exposes seek-by-location, seek-by-ratio, progress, and aspects.
 */
public class PerfDataTrace extends TmfTrace
        implements ITmfPropertiesProvider, ITmfTraceKnownSize {

    /** Confidence returned when validation succeeds. */
    private static final int CONFIDENCE = 100;

    private @Nullable PerfDataReader fReader;
    private long fDataStart;
    private long fDataEnd;
    private long fFileSize;
    private @NonNull TmfLongLocation fCurrentLocation = new TmfLongLocation(0L);

    /**
     * No-arg constructor required by the extension-point framework.
     */
    public PerfDataTrace() {
        super();
    }

    // ---------------------------------------------------------------------
    // Aspects
    // ---------------------------------------------------------------------

    private static final Collection<ITmfEventAspect<?>> ASPECTS = ImmutableList.of(
            TmfBaseAspects.getTimestampAspect(),
            TmfBaseAspects.getEventTypeAspect(),
            new PerfFieldAspect("pid", "PID"), //$NON-NLS-1$ //$NON-NLS-2$
            new PerfFieldAspect("tid", "TID"), //$NON-NLS-1$ //$NON-NLS-2$
            new PerfCpuAspectImpl(),
            new PerfFieldAspect("ip", "IP"), //$NON-NLS-1$ //$NON-NLS-2$
            new PerfFieldAspect("addr", "Address"), //$NON-NLS-1$ //$NON-NLS-2$
            new PerfFieldAspect("comm", "Comm"), //$NON-NLS-1$ //$NON-NLS-2$
            new PerfFieldAspect("filename", "Filename"), //$NON-NLS-1$ //$NON-NLS-2$
            TmfBaseAspects.getContentsAspect());

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return ASPECTS;
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        if (path == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No path supplied"); //$NON-NLS-1$
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead() || file.length() < 16) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a readable file"); //$NON-NLS-1$
        }
        try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buf = ch.map(MapMode.READ_ONLY, 0, 16);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            long magic = buf.getLong();
            boolean ok = magic == PerfConstants.PERF_MAGIC_LE
                    || magic == PerfConstants.PERF_MAGIC_V1_LE;
            if (!ok) {
                buf.order(ByteOrder.BIG_ENDIAN);
                buf.rewind();
                long beMagic = buf.getLong();
                ok = beMagic == PerfConstants.PERF_MAGIC_LE
                        || beMagic == PerfConstants.PERF_MAGIC_V1_LE;
            }
            if (ok) {
                return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
            }
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a perf.data file"); //$NON-NLS-1$
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "I/O error validating perf.data", e); //$NON-NLS-1$
        }
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    @Override
    public void initTrace(@Nullable IResource resource, @Nullable String path,
            @Nullable Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        if (path == null) {
            throw new TmfTraceException("No path supplied"); //$NON-NLS-1$
        }
        File file = new File(path);
        try {
            PerfDataReader reader = new PerfDataReader(file);
            fReader = reader;
            fDataStart = reader.getDataOffset();
            fDataEnd = fDataStart + reader.getDataSize();
            fFileSize = reader.getFileSize();
            if (reader.getHeader().isPiped()) {
                // Piped streams are not supported yet; use a zero-length data
                // section so the iterator produces no events instead of
                // attempting to decode unknown framing.
                fDataStart = 0;
                fDataEnd = 0;
            }
            fCurrentLocation = new TmfLongLocation(fDataStart);
        } catch (IOException e) {
            throw new TmfTraceException("Failed to parse perf.data", e); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized void dispose() {
        PerfDataReader reader = fReader;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Activator.getInstance().logError("Failed to close perf.data reader", e); //$NON-NLS-1$
            }
            fReader = null;
        }
        super.dispose();
    }

    // ---------------------------------------------------------------------
    // Seek / parse
    // ---------------------------------------------------------------------

    @Override
    public @NonNull ITmfLocation getCurrentLocation() {
        return fCurrentLocation;
    }

    @Override
    public double getLocationRatio(@Nullable ITmfLocation location) {
        long span = fDataEnd - fDataStart;
        if (span <= 0) {
            return 0.0;
        }
        long pos;
        if (location instanceof TmfLongLocation) {
            Long info = ((TmfLongLocation) location).getLocationInfo();
            pos = info == null ? fDataStart : info.longValue();
        } else {
            pos = fDataStart;
        }
        return Math.max(0.0, Math.min(1.0, (double) (pos - fDataStart) / span));
    }

    @Override
    public ITmfContext seekEvent(@Nullable ITmfLocation location) {
        TmfContext ctx = new TmfContext();
        long offset = fDataStart;
        if (location instanceof TmfLongLocation) {
            Long info = ((TmfLongLocation) location).getLocationInfo();
            if (info != null) {
                offset = Math.max(fDataStart, Math.min(info.longValue(), fDataEnd));
            }
        }
        ctx.setLocation(new TmfLongLocation(offset));
        ctx.setRank(0);
        return ctx;
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        long span = fDataEnd - fDataStart;
        long offset = fDataStart + (long) Math.max(0.0, Math.min(1.0, ratio)) * span;
        return seekEvent(new TmfLongLocation(offset));
    }

    @Override
    public @Nullable ITmfEvent parseEvent(@Nullable ITmfContext ctx) {
        if (ctx == null) {
            return null;
        }
        ITmfLocation loc = ctx.getLocation();
        long offset;
        if (loc instanceof TmfLongLocation) {
            Long info = ((TmfLongLocation) loc).getLocationInfo();
            offset = info == null ? fDataStart : info.longValue();
        } else {
            offset = fDataStart;
        }
        if (offset < fDataStart) {
            offset = fDataStart;
        }
        if (offset >= fDataEnd) {
            return null;
        }
        PerfDataReader reader = fReader;
        if (reader == null) {
            return null;
        }
        try {
            PerfRecord record = reader.readRecordAt(offset);
            if (record == null) {
                return null;
            }
            long nextOffset = offset + record.getSize();
            ctx.setLocation(new TmfLongLocation(nextOffset));
            fCurrentLocation = new TmfLongLocation(nextOffset);
            long ts = record.getTimestamp();
            TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, record,
                    fieldsFromRecord(record));
            return new TmfEvent(this, ctx.getRank(), TmfTimestamp.fromNanos(ts),
                    PerfEventType.lookup(record.getType()), content);
        } catch (IOException e) {
            Activator.getInstance().logError("Failed to parse perf.data record at " + offset, e); //$NON-NLS-1$
            return null;
        }
    }

    private static TmfEventField[] fieldsFromRecord(PerfRecord record) {
        Map<String, Object> m = record.getFields();
        TmfEventField[] out = new TmfEventField[m.size()];
        int i = 0;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            // Preserve native array types (byte[], long[]) as-is so
            // analyses can consume them directly. The Contents aspect
            // calls toString() on non-primitive values; the primitive
            // array toString is ugly but acceptable for debugging.
            out[i++] = new TmfEventField(e.getKey(), e.getValue(), null);
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Size / progress / properties
    // ---------------------------------------------------------------------

    @Override
    public int size() {
        long span = fDataEnd - fDataStart;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, span / 1024));
    }

    @Override
    public int progress() {
        Long pos = fCurrentLocation.getLocationInfo();
        long cur = pos == null ? fDataStart : pos.longValue();
        long done = Math.max(0L, cur - fDataStart);
        return (int) Math.min(Integer.MAX_VALUE, done / 1024);
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        PerfDataReader reader = fReader;
        if (reader == null) {
            return Collections.emptyMap();
        }
        Map<String, String> props = new LinkedHashMap<>();
        props.put("file", reader.getFile().getAbsolutePath()); //$NON-NLS-1$
        props.put("file_size", Long.toString(fFileSize)); //$NON-NLS-1$
        props.put("byte_order", reader.getHeader().getOrder().toString()); //$NON-NLS-1$
        props.put("piped", Boolean.toString(reader.getHeader().isPiped())); //$NON-NLS-1$
        props.put("data_offset", Long.toString(fDataStart)); //$NON-NLS-1$
        props.put("data_size", Long.toString(fDataEnd - fDataStart)); //$NON-NLS-1$
        props.put("num_attrs", Integer.toString(reader.getAttrs().size())); //$NON-NLS-1$
        if (!reader.getAttrs().isEmpty()) {
            List<Long> allIds = reader.getAttrs().get(0).getIds();
            props.put("attr0_sample_type", "0x" + Long.toHexString(reader.getAttrs().get(0).getSampleType())); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("attr0_num_ids", Integer.toString(allIds.size())); //$NON-NLS-1$
        }
        for (Map.Entry<String, String> e : reader.getMetadata().entrySet()) {
            props.put(e.getKey(), e.getValue());
        }
        return Collections.unmodifiableMap(props);
    }

    // ---------------------------------------------------------------------
    // Aspects used above
    // ---------------------------------------------------------------------

    /**
     * Generic aspect pulling a named field out of the embedded {@link PerfRecord}.
     */
    private static final class PerfFieldAspect implements ITmfEventAspect<Object> {

        private final String fField;
        private final String fName;

        PerfFieldAspect(String field, String name) {
            fField = field;
            fName = name;
        }

        @Override
        public @NonNull String getName() {
            return fName;
        }

        @Override
        public @NonNull String getHelpText() {
            return ""; //$NON-NLS-1$
        }

        @Override
        public @Nullable Object resolve(@NonNull ITmfEvent event) {
            Object val = event.getContent().getValue();
            if (val instanceof PerfRecord) {
                PerfRecord rec = (PerfRecord) val;
                Object v = rec.getField(fField);
                if (v == null) {
                    // Some fields live under the sample_id_all trailer
                    v = rec.getField("sample_" + fField); //$NON-NLS-1$
                }
                if (v instanceof Long) {
                    return "0x" + Long.toHexString((Long) v); //$NON-NLS-1$
                }
                return v;
            }
            return null;
        }
    }

    /**
     * CPU aspect for perf.data events.
     */
    public static final class PerfCpuAspectImpl extends TmfCpuAspect {
        @Override
        public @Nullable Integer resolve(@NonNull ITmfEvent event) {
            Object val = event.getContent().getValue();
            if (val instanceof PerfRecord) {
                PerfRecord rec = (PerfRecord) val;
                Object cpu = rec.getField("cpu"); //$NON-NLS-1$
                if (cpu == null) {
                    cpu = rec.getField("sample_cpu"); //$NON-NLS-1$
                }
                if (cpu instanceof Integer) {
                    return (Integer) cpu;
                }
            }
            return null;
        }
    }
}
