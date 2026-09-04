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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Minimal, read-only reader for the SQLite 3 database file format.
 * <p>
 * This intentionally implements only the subset of the format needed to iterate
 * over the rows of ordinary (rowid) tables: the database header, the schema
 * table ({@code sqlite_master}) and table b-tree traversal (interior/leaf
 * pages) with record decoding for the NULL, integer, float, text and blob
 * serial types. It does not support WITHOUT ROWID tables, indexes, incremental
 * vacuum, or encryption. It is sufficient to read the small SQLite trace
 * databases this trace type targets.
 * <p>
 * See the <a href="https://www.sqlite.org/fileformat2.html">SQLite file
 * format</a> documentation for the on-disk layout.
 *
 * @author Matthew Khouzam
 */
public class SqliteReader implements AutoCloseable {

    /** The 16-byte magic header string, including the trailing NUL. */
    public static final byte[] MAGIC = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII); //$NON-NLS-1$

    private static final int HEADER_SIZE = 100;
    private static final int BTREE_INTERIOR_TABLE = 0x05;
    private static final int BTREE_LEAF_TABLE = 0x0d;

    /** Number of consecutive pages to fetch per disk read (sliding window). */
    private static final int WINDOW_PAGES = 16;

    // Matches: CREATE TABLE [IF NOT EXISTS] 'name'(col1 TYPE, col2 TYPE, ...)
    private static final Pattern COLUMN_DEF = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:'[^']*'|\"[^\"]*\"|\\S+)\\s*\\((.*)\\)\\s*$", //$NON-NLS-1$
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final RandomAccessFile fFile;
    private final int fPageSize;
    private final long fFileLength;

    /**
     * Sliding window of consecutive pages held in memory. {@code fWindowBuffer}
     * holds up to {@link #WINDOW_PAGES} pages starting at the 1-based page
     * number {@code fWindowFirstPage}; {@code fWindowCount} is the number of
     * valid pages currently loaded (fewer at end of file).
     */
    private final byte[] fWindowBuffer;
    private long fWindowFirstPage = -1;
    private int fWindowCount = 0;

    /**
     * A table declared in the schema, with its column names and root page.
     */
    public static class TableDescriptor {
        private final String fName;
        private final long fRootPage;
        private final List<@NonNull String> fColumns;

        TableDescriptor(String name, long rootPage, List<@NonNull String> columns) {
            fName = name;
            fRootPage = rootPage;
            fColumns = columns;
        }

        /** @return the table name */
        public String getName() {
            return fName;
        }

        /** @return the 1-based root page of the table b-tree */
        public long getRootPage() {
            return fRootPage;
        }

        /** @return the ordered column names */
        public List<@NonNull String> getColumns() {
            return fColumns;
        }
    }

    /**
     * Open a SQLite file for reading.
     *
     * @param path
     *            the file path
     * @throws IOException
     *             if the file cannot be read or is not a SQLite database
     */
    public SqliteReader(String path) throws IOException {
        fFile = new RandomAccessFile(path, "r"); //$NON-NLS-1$
        fFileLength = fFile.length();
        byte[] header = new byte[HEADER_SIZE];
        if (fFileLength < HEADER_SIZE) {
            fFile.close();
            throw new IOException("File too small to be a SQLite database"); //$NON-NLS-1$
        }
        fFile.seek(0);
        fFile.readFully(header);
        for (int i = 0; i < MAGIC.length; i++) {
            if (header[i] != MAGIC[i]) {
                fFile.close();
                throw new IOException("Not a SQLite 3 database (bad magic)"); //$NON-NLS-1$
            }
        }
        int rawPageSize = ((header[16] & 0xff) << 8) | (header[17] & 0xff);
        // A page size of 1 means 65536.
        fPageSize = rawPageSize == 1 ? 65536 : rawPageSize;
        if (fPageSize < 512 || (fPageSize & (fPageSize - 1)) != 0) {
            fFile.close();
            throw new IOException("Invalid SQLite page size: " + fPageSize); //$NON-NLS-1$
        }
        fWindowBuffer = new byte[fPageSize * WINDOW_PAGES];
    }

    /**
     * Verify that a file starts with the SQLite magic header, without fully
     * opening it.
     *
     * @param header
     *            the first bytes of the file (at least {@link #MAGIC} long)
     * @return {@code true} if the header matches
     */
    public static boolean hasMagic(byte[] header) {
        if (header.length < MAGIC.length) {
            return false;
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (header[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /** @return the total file length in bytes */
    public long getFileLength() {
        return fFileLength;
    }

    /**
     * Read the schema and return all ordinary tables declared in it.
     *
     * @return the table descriptors, in schema order
     * @throws IOException
     *             on read error
     */
    public List<@NonNull TableDescriptor> readTables() throws IOException {
        List<@NonNull TableDescriptor> tables = new ArrayList<>();
        for (Map<@NonNull String, @Nullable Object> row : readTableRows(1, List.of("type", "name", "tbl_name", "rootpage", "sql"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            Object type = row.get("type"); //$NON-NLS-1$
            Object name = row.get("name"); //$NON-NLS-1$
            Object rootPage = row.get("rootpage"); //$NON-NLS-1$
            Object sql = row.get("sql"); //$NON-NLS-1$
            if (!"table".equals(type) || !(name instanceof String) || !(rootPage instanceof Number)) { //$NON-NLS-1$
                continue;
            }
            long root = ((Number) rootPage).longValue();
            if (root <= 0) {
                continue;
            }
            List<@NonNull String> columns = parseColumns(sql instanceof String ? (String) sql : null);
            tables.add(new TableDescriptor((String) name, root, columns));
        }
        return tables;
    }

    /**
     * Iterate all rows of a table b-tree, mapping each row to its column values
     * keyed by the provided column names. The rowid is not included unless a
     * column is an {@code INTEGER PRIMARY KEY} alias, in which case SQLite
     * stores a NULL in the record and the value is taken from the rowid.
     *
     * @param rootPage
     *            the 1-based root page of the table b-tree
     * @param columns
     *            the ordered column names
     * @return the rows in b-tree (rowid) order
     * @throws IOException
     *             on read error
     */
    public List<@NonNull Map<@NonNull String, @Nullable Object>> readTableRows(long rootPage, List<@NonNull String> columns) throws IOException {
        List<@NonNull Map<@NonNull String, @Nullable Object>> rows = new ArrayList<>();
        collectRows(rootPage, columns, rows);
        return rows;
    }

    private void collectRows(long page, List<@NonNull String> columns, List<@NonNull Map<@NonNull String, @Nullable Object>> out) throws IOException {
        byte[] pageData = readPage(page);
        // Page 1 has the 100-byte database header before the b-tree header.
        int headerOffset = (page == 1) ? HEADER_SIZE : 0;
        int type = pageData[headerOffset] & 0xff;
        int numCells = ((pageData[headerOffset + 3] & 0xff) << 8) | (pageData[headerOffset + 4] & 0xff);
        int cellPointerArray = headerOffset + (isInterior(type) ? 12 : 8);

        if (type == BTREE_LEAF_TABLE) {
            for (int i = 0; i < numCells; i++) {
                int cellPtr = ((pageData[cellPointerArray + 2 * i] & 0xff) << 8) | (pageData[cellPointerArray + 2 * i + 1] & 0xff);
                out.add(readLeafCell(pageData, cellPtr, columns));
            }
        } else if (type == BTREE_INTERIOR_TABLE) {
            for (int i = 0; i < numCells; i++) {
                int cellPtr = ((pageData[cellPointerArray + 2 * i] & 0xff) << 8) | (pageData[cellPointerArray + 2 * i + 1] & 0xff);
                long childPage = readUint32(pageData, cellPtr);
                collectRows(childPage, columns, out);
            }
            // Right-most pointer, stored in the interior header at offset 8.
            long rightMost = readUint32(pageData, headerOffset + 8);
            collectRows(rightMost, columns, out);
        }
        // Other page types (index pages, overflow) are not table rows: ignore.
    }

    private static boolean isInterior(int type) {
        return type == BTREE_INTERIOR_TABLE || type == 0x02;
    }

    private static Map<@NonNull String, @Nullable Object> readLeafCell(byte[] page, int cellPtr, List<@NonNull String> columns) throws IOException {
        long[] cursor = { cellPtr };
        long payloadSize = readVarint(page, cursor);
        long rowid = readVarint(page, cursor);
        int payloadStart = (int) cursor[0];

        // Note: this reader does not follow overflow pages. For the small
        // trace records this targets, the payload always fits on the page.
        int localPayload = (int) Math.min(payloadSize, page.length - payloadStart);
        byte[] payload = new byte[localPayload];
        System.arraycopy(page, payloadStart, payload, 0, localPayload);

        return decodeRecord(payload, rowid, columns);
    }

    @SuppressWarnings("null")
    private static Map<@NonNull String, @Nullable Object> decodeRecord(byte[] payload, long rowid, List<@NonNull String> columns) throws IOException {
        Map<@NonNull String, @Nullable Object> row = new LinkedHashMap<>();
        long[] cursor = { 0 };
        long headerSize = readVarint(payload, cursor);
        int headerEnd = (int) headerSize;
        List<Long> serialTypes = new ArrayList<>();
        while (cursor[0] < headerEnd) {
            serialTypes.add(readVarint(payload, cursor));
        }
        int dataOffset = headerEnd;
        for (int i = 0; i < serialTypes.size(); i++) {
            long serial = serialTypes.get(i);
            int[] consumed = { 0 };
            Object value = decodeValue(payload, dataOffset, serial, consumed);
            dataOffset += consumed[0];
            String colName = i < columns.size() ? columns.get(i) : ("col" + i); //$NON-NLS-1$
            // INTEGER PRIMARY KEY columns are stored as NULL; use the rowid.
            if (value == null && i == 0) {
                value = Long.valueOf(rowid);
            }
            row.put(colName, value);
        }
        return row;
    }

    private static @Nullable Object decodeValue(byte[] data, int offset, long serial, int[] consumed) throws IOException {
        switch ((int) serial) {
        case 0: // NULL
            consumed[0] = 0;
            return null;
        case 1: // 8-bit int
            consumed[0] = 1;
            return Long.valueOf(data[offset]);
        case 2: // 16-bit int
            consumed[0] = 2;
            return Long.valueOf((short) (((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff)));
        case 3: // 24-bit int
            consumed[0] = 3;
            return Long.valueOf(signExtend(readBigEndian(data, offset, 3), 24));
        case 4: // 32-bit int
            consumed[0] = 4;
            return Long.valueOf(signExtend(readBigEndian(data, offset, 4), 32));
        case 5: // 48-bit int
            consumed[0] = 6;
            return Long.valueOf(signExtend(readBigEndian(data, offset, 6), 48));
        case 6: // 64-bit int
            consumed[0] = 8;
            return Long.valueOf(readBigEndian(data, offset, 8));
        case 7: // IEEE 754 double
            consumed[0] = 8;
            return Double.valueOf(Double.longBitsToDouble(readBigEndian(data, offset, 8)));
        case 8: // integer constant 0
            consumed[0] = 0;
            return Long.valueOf(0L);
        case 9: // integer constant 1
            consumed[0] = 0;
            return Long.valueOf(1L);
        default:
            if (serial >= 12 && serial % 2 == 0) { // BLOB
                int len = (int) ((serial - 12) / 2);
                consumed[0] = len;
                byte[] blob = new byte[len];
                System.arraycopy(data, offset, blob, 0, Math.min(len, data.length - offset));
                return blob;
            }
            if (serial >= 13 && serial % 2 == 1) { // TEXT (UTF-8)
                int len = (int) ((serial - 13) / 2);
                consumed[0] = len;
                int safeLen = Math.min(len, data.length - offset);
                return new String(data, offset, safeLen, StandardCharsets.UTF_8);
            }
            throw new IOException("Unknown serial type: " + serial); //$NON-NLS-1$
        }
    }

    private static long readBigEndian(byte[] data, int offset, int length) {
        long value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (data[offset + i] & 0xff);
        }
        return value;
    }

    private static long signExtend(long value, int bits) {
        long signBit = 1L << (bits - 1);
        return (value ^ signBit) - signBit;
    }

    /**
     * Read a SQLite variable-length integer (big-endian, 7 bits per byte, up to
     * 9 bytes). Updates {@code cursor[0]} past the varint.
     */
    private static long readVarint(byte[] data, long[] cursor) {
        int pos = (int) cursor[0];
        long result = 0;
        for (int i = 0; i < 8; i++) {
            int b = data[pos++] & 0xff;
            result = (result << 7) | (b & 0x7f);
            if ((b & 0x80) == 0) {
                cursor[0] = pos;
                return result;
            }
        }
        // 9th byte contributes all 8 bits.
        int b = data[pos++] & 0xff;
        result = (result << 8) | b;
        cursor[0] = pos;
        return result;
    }

    private static long readUint32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    /**
     * Return the bytes of the given 1-based page. Pages are fetched in blocks of
     * {@link #WINDOW_PAGES} consecutive pages into an in-memory sliding window;
     * a request for a page outside the current window triggers a single block
     * read starting at that page. A fresh copy of the page is returned so that
     * callers may retain it across recursive reads that slide the window.
     */
    private byte[] readPage(long page) throws IOException {
        if (!isPageInWindow(page)) {
            fillWindow(page);
        }
        int indexInWindow = (int) (page - fWindowFirstPage);
        byte[] data = new byte[fPageSize];
        System.arraycopy(fWindowBuffer, indexInWindow * fPageSize, data, 0, fPageSize);
        return data;
    }

    private boolean isPageInWindow(long page) {
        return fWindowFirstPage > 0 && page >= fWindowFirstPage && page < fWindowFirstPage + fWindowCount;
    }

    /**
     * Load a block of up to {@link #WINDOW_PAGES} consecutive pages, starting at
     * {@code firstPage}, into the sliding window with a single read.
     */
    private void fillWindow(long firstPage) throws IOException {
        long startOffset = (firstPage - 1) * fPageSize;
        int toRead = (int) Math.min((long) fPageSize * WINDOW_PAGES, fFileLength - startOffset);
        if (toRead <= 0) {
            throw new IOException("Requested page " + firstPage + " is past end of file"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        fFile.seek(startOffset);
        fFile.readFully(fWindowBuffer, 0, toRead);
        // Zero out any trailing partial page slot so stale bytes are not read.
        if (toRead < fWindowBuffer.length) {
            java.util.Arrays.fill(fWindowBuffer, toRead, fWindowBuffer.length, (byte) 0);
        }
        fWindowFirstPage = firstPage;
        fWindowCount = (toRead + fPageSize - 1) / fPageSize;
    }

    /**
     * Parse the ordered column names out of a {@code CREATE TABLE} statement.
     */
    private static List<@NonNull String> parseColumns(@Nullable String sql) {
        List<@NonNull String> columns = new ArrayList<>();
        if (sql == null) {
            return columns;
        }
        Matcher matcher = COLUMN_DEF.matcher(sql.trim());
        if (!matcher.find()) {
            return columns;
        }
        String body = matcher.group(1);
        for (String part : splitTopLevel(body)) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String upper = trimmed.toUpperCase(Locale.ROOT);
            // Skip table constraints (they are not columns).
            if (upper.startsWith("PRIMARY KEY") || upper.startsWith("UNIQUE") //$NON-NLS-1$ //$NON-NLS-2$
                    || upper.startsWith("CHECK") || upper.startsWith("FOREIGN KEY") //$NON-NLS-1$ //$NON-NLS-2$
                    || upper.startsWith("CONSTRAINT")) { //$NON-NLS-1$
                continue;
            }
            columns.add(extractColumnName(trimmed));
        }
        return columns;
    }

    private static String extractColumnName(String columnDef) {
        String def = columnDef.trim();
        char first = def.charAt(0);
        if (first == '\'' || first == '"' || first == '`') {
            int end = def.indexOf(first, 1);
            if (end > 0) {
                return def.substring(1, end);
            }
        }
        if (first == '[') {
            int end = def.indexOf(']', 1);
            if (end > 0) {
                return def.substring(1, end);
            }
        }
        int space = 0;
        while (space < def.length() && !Character.isWhitespace(def.charAt(space))) {
            space++;
        }
        return def.substring(0, space);
    }

    /**
     * Split a comma-separated list, ignoring commas inside parentheses or
     * quotes (needed because column type definitions can contain commas).
     */
    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quote != 0) {
                current.append(c);
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            switch (c) {
            case '\'':
            case '"':
            case '`':
                quote = c;
                current.append(c);
                break;
            case '(':
                depth++;
                current.append(c);
                break;
            case ')':
                depth--;
                current.append(c);
                break;
            case ',':
                if (depth == 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
                break;
            default:
                current.append(c);
                break;
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    @Override
    public void close() throws IOException {
        fFile.close();
    }
}
