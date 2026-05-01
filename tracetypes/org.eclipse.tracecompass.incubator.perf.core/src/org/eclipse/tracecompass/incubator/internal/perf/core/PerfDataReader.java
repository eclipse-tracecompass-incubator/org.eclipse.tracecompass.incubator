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

package org.eclipse.tracecompass.incubator.internal.perf.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Random-access reader for {@code perf.data} files. Follows the layout
 * described in the project's {@code perf_data_format.md} reference.
 *
 * Responsibilities:
 * <ul>
 * <li>Detect normal vs piped headers and the byte order.</li>
 * <li>Parse the attrs section into {@link PerfEventAttr} objects, collecting
 * per-attr event IDs.</li>
 * <li>Iterate the data section one {@code perf_event_header}-framed record at
 * a time, decoding known bodies (MMAP, MMAP2, COMM, FORK, EXIT, SAMPLE, ...)
 * into a {@link PerfRecord}.</li>
 * <li>Load a subset of feature sections — enough to expose trace-level
 * metadata (hostname, arch, osrelease, cmdline, etc.).</li>
 * </ul>
 *
 * This reader does <em>not</em> attempt to be a fully-faithful perf
 * implementation: compressed record bodies (PERF_RECORD_COMPRESSED*) are
 * skipped, and features it does not understand are simply ignored.
 */
public final class PerfDataReader implements Closeable {

    /** Maximum piped record size (sanity bound). */
    private static final int MAX_RECORD_SIZE = 64 * 1024 * 1024;

    private final File fFile;
    private final RandomAccessFile fRaf;
    private final FileChannel fChannel;
    private final long fFileSize;

    private final PerfFileHeader fHeader;
    private final List<PerfEventAttr> fAttrs;
    private final PerfEventAttr fDefaultAttr;
    private final Map<Integer, byte[]> fFeatures;
    private final Map<String, String> fMetadata;

    /**
     * Open a perf.data file for reading.
     *
     * @param file
     *            the file
     * @throws IOException
     *             on I/O or format error
     */
    public PerfDataReader(File file) throws IOException {
        fFile = file;
        fRaf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
        fChannel = fRaf.getChannel();
        fFileSize = fChannel.size();

        fHeader = parseHeader();
        if (fHeader.isPiped()) {
            fAttrs = List.of();
            Map.of();
            fDefaultAttr = null;
            fFeatures = Map.of();
            fMetadata = Map.of();
        } else {
            fAttrs = parseAttrs(fHeader);
            Map<Long, PerfEventAttr> byId = new LinkedHashMap<>();
            for (PerfEventAttr attr : fAttrs) {
                for (Long id : attr.getIds()) {
                    byId.put(id, attr);
                }
            }
            Collections.unmodifiableMap(byId);
            fDefaultAttr = fAttrs.isEmpty() ? null : fAttrs.get(0);
            fFeatures = parseFeatures(fHeader);
            fMetadata = extractMetadata(fFeatures, fHeader.getOrder());
        }
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * @return the parsed header
     */
    public PerfFileHeader getHeader() {
        return fHeader;
    }

    /**
     * @return the event attrs present in the file
     */
    public List<PerfEventAttr> getAttrs() {
        return fAttrs;
    }

    /**
     * @return the underlying file
     */
    public File getFile() {
        return fFile;
    }

    /**
     * @return the total file size in bytes
     */
    public long getFileSize() {
        return fFileSize;
    }

    /**
     * @return human-readable metadata extracted from feature sections
     *         (hostname, arch, osrelease, version, cpudesc, cmdline, ...)
     */
    public Map<String, String> getMetadata() {
        return fMetadata;
    }

    /**
     * @return the first byte offset of the data section
     */
    public long getDataOffset() {
        return fHeader.getData().getOffset();
    }

    /**
     * @return the total byte size of the data section
     */
    public long getDataSize() {
        return fHeader.getData().getSize();
    }

    /**
     * Read the next record at the given absolute file offset and decode it.
     *
     * @param offset
     *            the absolute file offset at which the record starts. It
     *            must point at a {@code perf_event_header}.
     * @return the decoded record, or {@code null} if {@code offset} is past
     *         the end of the data section
     * @throws IOException
     *             on I/O or format error
     */
    public @Nullable PerfRecord readRecordAt(long offset) throws IOException {
        long dataEnd = fHeader.getData().getOffset() + fHeader.getData().getSize();
        if (offset >= dataEnd) {
            return null;
        }
        if (offset + PerfConstants.PERF_EVENT_HEADER_SIZE > dataEnd) {
            return null;
        }

        ByteBuffer hdr = readBytes(offset, PerfConstants.PERF_EVENT_HEADER_SIZE).order(fHeader.getOrder());
        int type = hdr.getInt() & 0xffffffff;
        int misc = hdr.getShort() & 0xffff;
        int size = hdr.getShort() & 0xffff;
        if (size < PerfConstants.PERF_EVENT_HEADER_SIZE || size > MAX_RECORD_SIZE) {
            throw new IOException("Invalid record size " + size + " at offset " + offset); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (offset + size > dataEnd) {
            // truncated record; stop here
            return null;
        }
        int bodyLen = size - PerfConstants.PERF_EVENT_HEADER_SIZE;
        ByteBuffer body = readBytes(offset + PerfConstants.PERF_EVENT_HEADER_SIZE, bodyLen);
        Map<String, Object> fields = new LinkedHashMap<>();
        long timestamp = decodeBody(type, misc, body, fields);
        return new PerfRecord(type, misc, size, offset - fHeader.getData().getOffset(), timestamp, fields);
    }

    @Override
    public void close() throws IOException {
        fChannel.close();
        fRaf.close();
    }

    // ---------------------------------------------------------------------
    // Header parsing
    // ---------------------------------------------------------------------

    private PerfFileHeader parseHeader() throws IOException {
        if (fFileSize < 16) {
            throw new IOException("File too small to contain a perf.data header"); //$NON-NLS-1$
        }
        // Read tentatively in little-endian
        ByteBuffer buf = readBytes(0, 16);
        long magic = buf.getLong();
        long size = buf.getLong();
        ByteOrder order = ByteOrder.LITTLE_ENDIAN;

        if (magic != PerfConstants.PERF_MAGIC_LE && magic != PerfConstants.PERF_MAGIC_V1_LE) {
            // Try big-endian interpretation
            buf = readBytes(0, 16).order(ByteOrder.BIG_ENDIAN);
            long beMagic = buf.getLong();
            long beSize = buf.getLong();
            if (beMagic == PerfConstants.PERF_MAGIC_LE || beMagic == PerfConstants.PERF_MAGIC_V1_LE) {
                magic = beMagic;
                size = beSize;
                order = ByteOrder.BIG_ENDIAN;
            } else {
                throw new IOException("Not a perf.data file (bad magic: 0x" //$NON-NLS-1$
                        + Long.toHexString(magic) + ")"); //$NON-NLS-1$
            }
        }

        // Piped headers are exactly 16 bytes
        if (size == 16) {
            return new PerfFileHeader(magic, size, order);
        }

        if (size < 16 + 16 * 3 + (PerfConstants.HEADER_FEAT_BITS / 8)) {
            throw new IOException("Perf header size too small: " + size); //$NON-NLS-1$
        }

        ByteBuffer full = readBytes(0, (int) Math.min(size, Integer.MAX_VALUE)).order(order);
        full.position(16); // past magic + size
        long attrSize = full.getLong();
        PerfFileSection attrs = readFileSection(full);
        PerfFileSection data = readFileSection(full);
        PerfFileSection eventTypes = readFileSection(full);
        long[] features = new long[PerfConstants.HEADER_FEAT_BITS / 64];
        for (int i = 0; i < features.length; i++) {
            features[i] = full.getLong();
        }

        return new PerfFileHeader(magic, size, attrSize, attrs, data, eventTypes, features, order);
    }

    private static PerfFileSection readFileSection(ByteBuffer buf) {
        long off = buf.getLong();
        long sz = buf.getLong();
        return new PerfFileSection(off, sz);
    }

    // ---------------------------------------------------------------------
    // Attrs parsing
    // ---------------------------------------------------------------------

    private List<PerfEventAttr> parseAttrs(PerfFileHeader header) throws IOException {
        long attrSize = header.getAttrSize();
        PerfFileSection attrs = header.getAttrs();
        if (attrSize == 0 || attrs.getSize() == 0) {
            return List.of();
        }
        int count = (int) (attrs.getSize() / attrSize);
        List<PerfEventAttr> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long entryOff = attrs.getOffset() + i * attrSize;
            // Each entry is `perf_event_attr attr; perf_file_section ids;`
            // attrSize from the header already includes both, so we can read
            // both in one shot.
            int readLen = (int) Math.min(attrSize, Integer.MAX_VALUE);
            ByteBuffer buf = readBytes(entryOff, readLen).order(header.getOrder());
            PerfEventAttr attr = parseOneAttr(buf, header);
            result.add(attr);
        }
        return List.copyOf(result);
    }

    private PerfEventAttr parseOneAttr(ByteBuffer buf, PerfFileHeader header) throws IOException {
        // perf_event_attr layout (fixed prefix that we care about):
        //   u32 type
        //   u32 size
        //   u64 config
        //   u64 sample_period (or sample_freq)
        //   u64 sample_type
        //   u64 read_format
        //   u64 flags (packed bit fields)
        //   u32 wakeup_events (or wakeup_watermark)
        //   u32 bp_type
        //   u64 bp_addr (or config1)
        //   u64 bp_len (or config2)
        //   u64 branch_sample_type
        //   u64 sample_regs_user
        //   u32 sample_stack_user
        //   u32 __reserved_2
        //   ... newer fields ignored
        int startPos = buf.position();
        int type = buf.getInt();
        int attrStructSize = buf.getInt();
        long config = buf.getLong();
        long samplePeriod = buf.getLong();
        long sampleType = buf.getLong();
        long readFormat = buf.getLong();
        long flags = buf.getLong();
        buf.getInt(); // wakeup_events
        buf.getInt(); // bp_type
        buf.getLong(); // bp_addr / config1
        buf.getLong(); // bp_len / config2
        long branchSampleType = buf.getLong();

        // Skip to the end of the on-disk perf_event_attr, then read the ids
        // section. attrStructSize is the perf_event_attr.size field; the
        // perf_file_section for ids immediately follows this struct.
        int attrEnd = startPos + attrStructSize;
        if (attrEnd > buf.limit()) {
            // attrStructSize is larger than what we mapped; fall back to the
            // whole buffer boundaries.
            attrEnd = buf.limit() - PerfFileSection.SIZE;
        }
        buf.position(attrEnd);
        if (buf.remaining() < PerfFileSection.SIZE) {
            // Truncated: no ids section.
            return new PerfEventAttr(type, attrStructSize, config, samplePeriod,
                    sampleType, readFormat, flags, branchSampleType, List.of());
        }
        PerfFileSection ids = readFileSection(buf);

        List<Long> idList = readIds(ids, header.getOrder());
        return new PerfEventAttr(type, attrStructSize, config, samplePeriod,
                sampleType, readFormat, flags, branchSampleType, idList);
    }

    private List<Long> readIds(PerfFileSection ids, ByteOrder order) throws IOException {
        if (ids.getSize() == 0) {
            return List.of();
        }
        int count = (int) (ids.getSize() / 8);
        if (count == 0) {
            return List.of();
        }
        ByteBuffer buf = readBytes(ids.getOffset(), count * 8).order(order);
        List<Long> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(buf.getLong());
        }
        return List.copyOf(out);
    }

    // ---------------------------------------------------------------------
    // Features parsing
    // ---------------------------------------------------------------------

    private Map<Integer, byte[]> parseFeatures(PerfFileHeader header) throws IOException {
        // The feature index is an array of perf_file_section entries located
        // at data.offset + data.size (+ event_types.size when present). The
        // entries come in ascending bit order over the features bitmap.
        long indexStart = header.getData().getOffset() + header.getData().getSize();
        long eventTypesEnd = header.getEventTypes().getOffset() + header.getEventTypes().getSize();
        if (eventTypesEnd > indexStart) {
            indexStart = eventTypesEnd;
        }
        if (indexStart >= fFileSize) {
            return Map.of();
        }

        List<Integer> bits = new ArrayList<>();
        for (int bit = 1; bit < PerfConstants.HEADER_FEAT_BITS; bit++) {
            if (header.hasFeature(bit)) {
                bits.add(bit);
            }
        }
        int count = bits.size();
        if (count == 0) {
            return Map.of();
        }
        long indexLen = (long) count * PerfFileSection.SIZE;
        if (indexStart + indexLen > fFileSize) {
            return Map.of();
        }
        ByteBuffer idx = readBytes(indexStart, (int) indexLen).order(header.getOrder());
        Map<Integer, byte[]> out = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            long off = idx.getLong();
            long sz = idx.getLong();
            int bit = bits.get(i);
            if (off < 0 || sz < 0 || sz > Integer.MAX_VALUE
                    || off + sz > fFileSize) {
                continue;
            }
            ByteBuffer payload = readBytes(off, (int) sz);
            byte[] bytes = new byte[(int) sz];
            payload.get(bytes);
            out.put(bit, bytes);
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, String> extractMetadata(Map<Integer, byte[]> features, ByteOrder order) {
        Map<String, String> out = new LinkedHashMap<>();
        putString(out, features, PerfConstants.HEADER_HOSTNAME, "hostname", order); //$NON-NLS-1$
        putString(out, features, PerfConstants.HEADER_OSRELEASE, "osrelease", order); //$NON-NLS-1$
        putString(out, features, PerfConstants.HEADER_VERSION, "version", order); //$NON-NLS-1$
        putString(out, features, PerfConstants.HEADER_ARCH, "arch", order); //$NON-NLS-1$
        putString(out, features, PerfConstants.HEADER_CPUDESC, "cpudesc", order); //$NON-NLS-1$
        putString(out, features, PerfConstants.HEADER_CPUID, "cpuid", order); //$NON-NLS-1$
        putCmdline(out, features, order);
        putNrCpus(out, features, order);
        return Collections.unmodifiableMap(out);
    }

    private static void putString(Map<String, String> out, Map<Integer, byte[]> features,
            int bit, String key, ByteOrder order) {
        byte[] data = features.get(bit);
        if (data == null || data.length < 4) {
            return;
        }
        // Each "string" feature is encoded as u32 len followed by the
        // NUL-terminated string padded to 8 bytes.
        ByteBuffer buf = ByteBuffer.wrap(data).order(order);
        int len = buf.getInt();
        if (len <= 0 || len > buf.remaining()) {
            return;
        }
        byte[] bytes = new byte[len];
        buf.get(bytes);
        out.put(key, trimNul(new String(bytes)));
    }

    private static void putCmdline(Map<String, String> out, Map<Integer, byte[]> features, ByteOrder order) {
        byte[] data = features.get(PerfConstants.HEADER_CMDLINE);
        if (data == null || data.length < 4) {
            return;
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(order);
        int nr = buf.getInt();
        if (nr <= 0 || nr > 1024) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nr && buf.remaining() >= 4; i++) {
            int len = buf.getInt();
            if (len <= 0 || len > buf.remaining()) {
                break;
            }
            byte[] bytes = new byte[len];
            buf.get(bytes);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(trimNul(new String(bytes)));
        }
        if (sb.length() > 0) {
            out.put("cmdline", sb.toString()); //$NON-NLS-1$
        }
    }

    private static void putNrCpus(Map<String, String> out, Map<Integer, byte[]> features, ByteOrder order) {
        byte[] data = features.get(PerfConstants.HEADER_NRCPUS);
        if (data == null || data.length < 8) {
            return;
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(order);
        // perf writes nr_cpus_available first, then nr_cpus_online (see
        // tools/perf/util/header.c and Documentation/perf.data-file-format.txt).
        int nrAvail = buf.getInt();
        int nrOnline = buf.getInt();
        out.put("nr_cpus_available", Integer.toString(nrAvail)); //$NON-NLS-1$
        out.put("nr_cpus_online", Integer.toString(nrOnline)); //$NON-NLS-1$
    }

    private static String trimNul(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '\0') {
            end--;
        }
        return s.substring(0, end);
    }

    // ---------------------------------------------------------------------
    // Record body decoding
    // ---------------------------------------------------------------------

    /**
     * Decode a record body, populating {@code fields}. Returns the timestamp
     * attached to the record, or 0 if none could be determined.
     */
    private long decodeBody(int type, int misc, ByteBuffer rawBody, Map<String, Object> fields) {
        ByteBuffer body = rawBody.order(fHeader.getOrder());
        switch (type) {
        case PerfConstants.PERF_RECORD_MMAP:
            return decodeMmap(body, misc, fields, false);
        case PerfConstants.PERF_RECORD_MMAP2:
            return decodeMmap(body, misc, fields, true);
        case PerfConstants.PERF_RECORD_COMM:
            return decodeComm(body, fields);
        case PerfConstants.PERF_RECORD_EXIT:
        case PerfConstants.PERF_RECORD_FORK:
            return decodeForkExit(body, fields);
        case PerfConstants.PERF_RECORD_LOST:
            return decodeLost(body, fields);
        case PerfConstants.PERF_RECORD_THROTTLE:
        case PerfConstants.PERF_RECORD_UNTHROTTLE:
            return decodeThrottle(body, fields);
        case PerfConstants.PERF_RECORD_SAMPLE:
            return decodeSample(body, fields);
        case PerfConstants.PERF_RECORD_SWITCH:
            return decodeSwitch(body, fields, false);
        case PerfConstants.PERF_RECORD_SWITCH_CPU_WIDE:
            return decodeSwitch(body, fields, true);
        case PerfConstants.PERF_RECORD_FINISHED_ROUND:
        case PerfConstants.PERF_RECORD_FINISHED_INIT:
            return 0L;
        default:
            // Unknown / not yet decoded: keep the raw bytes for downstream.
            byte[] raw = new byte[body.remaining()];
            body.get(raw);
            fields.put("raw", raw); //$NON-NLS-1$
            return 0L;
        }
    }

    private long decodeMmap(ByteBuffer body, int misc, Map<String, Object> fields, boolean mmap2) {
        int pid = body.getInt();
        int tid = body.getInt();
        long addr = body.getLong();
        long len = body.getLong();
        long pgoff = body.getLong();
        fields.put("pid", pid); //$NON-NLS-1$
        fields.put("tid", tid); //$NON-NLS-1$
        fields.put("addr", addr); //$NON-NLS-1$
        fields.put("len", len); //$NON-NLS-1$
        fields.put("pgoff", pgoff); //$NON-NLS-1$
        if (mmap2) {
            // The union is 24 bytes in both variants:
            //   { u32 maj; u32 min; u64 ino; u64 ino_generation; }  // 24 B
            //   { u8 build_id_size; u8 __res1; u16 __res2;
            //     u8 build_id[20]; }                                // 24 B
            // The build-id variant is indicated by the
            // PERF_RECORD_MISC_MMAP_BUILD_ID bit in the record's misc field.
            if ((misc & PerfConstants.PERF_RECORD_MISC_MMAP_BUILD_ID) != 0) {
                int buildIdSize = body.get() & 0xff;
                body.get(); // __reserved_1
                body.getShort(); // __reserved_2
                byte[] buildId = new byte[20];
                body.get(buildId);
                fields.put("build_id_size", buildIdSize); //$NON-NLS-1$
                fields.put("build_id", buildId); //$NON-NLS-1$
            } else {
                int maj = body.getInt();
                int min = body.getInt();
                long ino = body.getLong();
                long inoGen = body.getLong();
                fields.put("maj", maj); //$NON-NLS-1$
                fields.put("min", min); //$NON-NLS-1$
                fields.put("ino", ino); //$NON-NLS-1$
                fields.put("ino_generation", inoGen); //$NON-NLS-1$
            }
            int prot = body.getInt();
            int flags = body.getInt();
            fields.put("prot", prot); //$NON-NLS-1$
            fields.put("flags", flags); //$NON-NLS-1$
        }
        fields.put("filename", readCString(body)); //$NON-NLS-1$
        long ts = decodeTrailingSampleId(body, fields);
        return ts;
    }

    private long decodeComm(ByteBuffer body, Map<String, Object> fields) {
        int pid = body.getInt();
        int tid = body.getInt();
        fields.put("pid", pid); //$NON-NLS-1$
        fields.put("tid", tid); //$NON-NLS-1$
        fields.put("comm", readCString(body)); //$NON-NLS-1$
        return decodeTrailingSampleId(body, fields);
    }

    private long decodeForkExit(ByteBuffer body, Map<String, Object> fields) {
        int pid = body.getInt();
        int ppid = body.getInt();
        int tid = body.getInt();
        int ptid = body.getInt();
        long time = body.getLong();
        fields.put("pid", pid); //$NON-NLS-1$
        fields.put("ppid", ppid); //$NON-NLS-1$
        fields.put("tid", tid); //$NON-NLS-1$
        fields.put("ptid", ptid); //$NON-NLS-1$
        fields.put("time", time); //$NON-NLS-1$
        decodeTrailingSampleId(body, fields);
        return time;
    }

    private long decodeLost(ByteBuffer body, Map<String, Object> fields) {
        long id = body.getLong();
        long lost = body.getLong();
        fields.put("id", id); //$NON-NLS-1$
        fields.put("lost", lost); //$NON-NLS-1$
        return decodeTrailingSampleId(body, fields);
    }

    private long decodeThrottle(ByteBuffer body, Map<String, Object> fields) {
        long time = body.getLong();
        long id = body.getLong();
        long streamId = body.getLong();
        fields.put("time", time); //$NON-NLS-1$
        fields.put("id", id); //$NON-NLS-1$
        fields.put("stream_id", streamId); //$NON-NLS-1$
        decodeTrailingSampleId(body, fields);
        return time;
    }

    private long decodeSwitch(ByteBuffer body, Map<String, Object> fields, boolean cpuWide) {
        if (cpuWide) {
            int pid = body.getInt();
            int tid = body.getInt();
            fields.put("next_prev_pid", pid); //$NON-NLS-1$
            fields.put("next_prev_tid", tid); //$NON-NLS-1$
        }
        return decodeTrailingSampleId(body, fields);
    }

    /**
     * Decode a PERF_RECORD_SAMPLE body using the sample_type bitmap of the
     * default attr. The sample fields are laid out in the order dictated by
     * PERF_SAMPLE_* flags.
     */
    private long decodeSample(ByteBuffer body, Map<String, Object> fields) {
        PerfEventAttr attr = fDefaultAttr;
        if (attr == null) {
            byte[] raw = new byte[body.remaining()];
            body.get(raw);
            fields.put("raw", raw); //$NON-NLS-1$
            return 0L;
        }
        long sampleType = attr.getSampleType();
        long time = 0L;

        if ((sampleType & PerfConstants.PERF_SAMPLE_IDENTIFIER) != 0 && body.remaining() >= 8) {
            fields.put("identifier", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_IP) != 0 && body.remaining() >= 8) {
            fields.put("ip", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_TID) != 0 && body.remaining() >= 8) {
            fields.put("pid", body.getInt()); //$NON-NLS-1$
            fields.put("tid", body.getInt()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_TIME) != 0 && body.remaining() >= 8) {
            time = body.getLong();
            fields.put("time", time); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_ADDR) != 0 && body.remaining() >= 8) {
            fields.put("addr", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_ID) != 0 && body.remaining() >= 8) {
            fields.put("id", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_STREAM_ID) != 0 && body.remaining() >= 8) {
            fields.put("stream_id", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_CPU) != 0 && body.remaining() >= 8) {
            fields.put("cpu", body.getInt()); //$NON-NLS-1$
            body.getInt(); // reserved
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_PERIOD) != 0 && body.remaining() >= 8) {
            fields.put("period", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_READ) != 0) {
            // Minimal handling: swallow the read_format payload by size. We do
            // not know the exact layout without interpreting read_format
            // bitmap fields, so just record the offset and skip.
            fields.put("read_format_raw", true); //$NON-NLS-1$
            // We don't know the size; we'll bail out from here to avoid
            // decoding the wrong fields. Consumers that want read_format
            // support will need a richer decoder.
            byte[] raw = new byte[body.remaining()];
            body.get(raw);
            fields.put("raw_tail", raw); //$NON-NLS-1$
            return time;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_CALLCHAIN) != 0 && body.remaining() >= 8) {
            long nr = body.getLong();
            if (nr < 0 || nr > (long) body.remaining() / 8) {
                // malformed
                return time;
            }
            long[] ips = new long[(int) nr];
            for (int i = 0; i < nr; i++) {
                ips[i] = body.getLong();
            }
            fields.put("callchain", ips); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_RAW) != 0 && body.remaining() >= 4) {
            int sz = body.getInt();
            if (sz < 0 || sz > body.remaining()) {
                return time;
            }
            byte[] raw = new byte[sz];
            body.get(raw);
            fields.put("raw", raw); //$NON-NLS-1$
        }
        // Additional sample fields past PERF_SAMPLE_RAW are intentionally not
        // decoded here. They are relatively rare in typical recordings and can
        // be added later without breaking the current decoder.
        return time;
    }

    /**
     * When {@code sample_id_all} is set in the default attr, non-SAMPLE
     * records carry a trailing sample_id block at the end of their body,
     * whose layout is a restricted subset of sample_type:
     * {TID, TIME, ID, STREAM_ID, CPU, IDENTIFIER}.
     *
     * @return the extracted time (or 0 if none)
     */
    private long decodeTrailingSampleId(ByteBuffer body, Map<String, Object> fields) {
        PerfEventAttr attr = fDefaultAttr;
        if (attr == null || !attr.isSampleIdAll()) {
            return 0L;
        }
        long sampleType = attr.getSampleType();
        long time = 0L;

        // Compute the trailing block size to ensure there is enough room.
        int size = 0;
        if ((sampleType & PerfConstants.PERF_SAMPLE_TID) != 0) {
            size += 8;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_TIME) != 0) {
            size += 8;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_ID) != 0) {
            size += 8;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_STREAM_ID) != 0) {
            size += 8;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_CPU) != 0) {
            size += 8;
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_IDENTIFIER) != 0) {
            size += 8;
        }
        if (size == 0 || body.remaining() < size) {
            return 0L;
        }
        body.position(body.limit() - size);
        if ((sampleType & PerfConstants.PERF_SAMPLE_TID) != 0) {
            fields.put("sample_pid", body.getInt()); //$NON-NLS-1$
            fields.put("sample_tid", body.getInt()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_TIME) != 0) {
            time = body.getLong();
            fields.put("sample_time", time); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_ID) != 0) {
            fields.put("sample_id", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_STREAM_ID) != 0) {
            fields.put("sample_stream_id", body.getLong()); //$NON-NLS-1$
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_CPU) != 0) {
            fields.put("sample_cpu", body.getInt()); //$NON-NLS-1$
            body.getInt();
        }
        if ((sampleType & PerfConstants.PERF_SAMPLE_IDENTIFIER) != 0) {
            fields.put("sample_identifier", body.getLong()); //$NON-NLS-1$
        }
        return time;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ByteBuffer readBytes(long offset, int len) throws IOException {
        if (offset < 0 || len < 0 || offset + len > fFileSize) {
            throw new IOException("Read out of bounds: offset=" + offset //$NON-NLS-1$
                    + ", len=" + len + ", size=" + fFileSize); //$NON-NLS-1$ //$NON-NLS-2$
        }
        ByteBuffer buf = fChannel.map(MapMode.READ_ONLY, offset, len);
        // Default to little-endian; callers that care override via .order().
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }

    private static String readCString(ByteBuffer body) {
        int start = body.position();
        int end = start;
        while (end < body.limit() && body.get(end) != 0) {
            end++;
        }
        byte[] bytes = new byte[end - start];
        body.get(bytes);
        if (body.position() < body.limit()) {
            // consume the NUL. Padding follows up to 8-byte alignment; since
            // the whole record is already 8-byte aligned by perf, the
            // remaining bytes until `body.limit()` will be consumed by
            // decodeTrailingSampleId (or nothing, for records without it).
            body.get();
        }
        return new String(bytes);
    }
}
