/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.uftrace.core.Activator;

/**
 * Metadata:
 * <ul>
 * <li>The metadata starts with a 8-byte magic string which is 0x46 0x74 0x72
 * 0x61 0x63 0x65 0x21 0x00 or "Ftrace!".</li>
 * <li>It's followed by a 4-byte number of file version and the current version
 * is 4.</li>
 * <li>And then there's a 2-byte number of header (metadata) size and the
 * current value is 40 (or 0x28).</li>
 * <li>The next byte identifies a byte-order (endian) in the data files. The
 * value is same as the ELF format (EI_DATA: 1 is for the little-endian and 2 is
 * for the big-endian).</li>
 * <li>The next byte tells the size of address or long int type also same as the
 * ELF format (EI_CLASS: 1 is for 32-bit and 2 is for 64-bit).</li>
 * <li>Then 64-bit bit mask (feat_mask) of enabled features comes after it. The
 * bit 0 is for PLT (library call) hooking, the bit 1 is for task and session
 * info, the bit 2 is for kernel tracing, the bit 3 is for function arguments,
 * the bit 4 is for function return value, the bit 5 is for whether symbol file
 * contains relative offset or absolute address, and the bit 6 is for max
 * (function) stack depth.</li>
 * <li>The next 64-bit mask (info_mask) is for which kind of process and system
 * information was saved after the metadata.</li>
 * <li>And then it followed by a 2-byte number of maximum function call (stack)
 * depth given by user.</li>
 * <li>The rest 6-byte is reserved for future use and should be filled with
 * zero.</li>
 * </ul>
 * After the metadata, info string follows in a "key:value" form.
 *
 * @author Matthew Khouzam
 *
 */
public class InfoParser {
    /**
     * Magic + version + header size
     */
    private static final int ABSOLUTE_MINIMUM_SIZE = 16;
    /**
     * Max map size, typical size = 1k
     */
    private static final int MAX_SIZE = (int) 1e6;
    // TODO fill me
    private static final byte[] MAGIC = { 0x46, 0x74, 0x72,
            0x61, 0x63, 0x65, 0x21, 0x00 };
    private final int fVersion;
    private final ByteOrder fByteOrder;
    private final int fAddressSize;
    private final long fFeatures;
    private final short fMaxDepth;
    private final Map<String, String> fData = new LinkedHashMap<>();
    private final Map<String, String> fSafeData = Collections.unmodifiableMap(fData);

    /**
     * @return the version
     */
    public int getVersion() {
        return fVersion;
    }

    /**
     * @return the byteOrder
     */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /**
     * @return the addressSize
     */
    public int getAddressSize() {
        return fAddressSize;
    }

    /**
     * @return the features
     */
    public long getFeatures() {
        return fFeatures;
    }

    /**
     * @return the maxDepth
     */
    public int getMaxDepth() {
        return fMaxDepth;
    }

    /**
     * @return the data
     */
    public Map<String, String> getData() {
        return fSafeData;
    }

    private InfoParser(int version, ByteOrder bo, int addressSize, long features, short maxDepth) {
        fVersion = version;
        fByteOrder = bo;
        fAddressSize = addressSize;
        fFeatures = features;
        fMaxDepth = maxDepth;

    }

    /**
     * Parse an info file
     *
     * @param file
     *            an info file
     * @return an {@link InfoParser} or null if invalid
     */
    public static InfoParser parse(File file) {
        // hack, info didn't have as much info before v4
        ByteOrder bo = ByteOrder.nativeOrder();
        try (FileInputStream fis = new FileInputStream(file);
                FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer bb = fc.map(MapMode.READ_ONLY, 0, Math.min(fc.size(), MAX_SIZE));
            bb.order(ByteOrder.LITTLE_ENDIAN);
            if (bb.remaining() < ABSOLUTE_MINIMUM_SIZE) {
                return null;
            }
            byte[] magic = new byte[8];
            bb.get(magic);
            if (!Arrays.equals(magic, MAGIC)) {
                return null;
            }
            bb.mark();
            int version = bb.getInt();
            short size = bb.getShort();

            int byteOrderByte = bb.get();
            if (byteOrderByte == 1) {
                bo = ByteOrder.LITTLE_ENDIAN;
            } else if (byteOrderByte == 2) {
                bo = ByteOrder.BIG_ENDIAN;
                // reparse the beginning
                bb.reset();
                version = bb.getInt();
                size = bb.getShort();
                bb.get();
            } else {
                return null;
            }

            if (size > bb.remaining() || size < 40) {
                return null;
            }

            byte sizeByte = bb.get();

            int addressSize = -1;
            if (sizeByte == 1) {
                addressSize = 32;
            } else if (sizeByte == 2) {
                addressSize = 64;
            }
            long features = bb.getLong();
            short maxDepth = bb.getShort();
            for (int i = 0; i < 6; i++) {
                if (bb.get() != 0) {
                    return null;
                }
            }
            bb.position(size);
            InfoParser ip = new InfoParser(version, bo, addressSize, features, maxDepth);

            try (InputStreamReader in = new InputStreamReader(fis);) {
                in.skip(bb.position());
                try (BufferedReader br = new BufferedReader(in)) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        String[] entry = line.split(":", 2); //$NON-NLS-1$
                        if (entry.length > 1) {
                            String current = ip.fData.get(entry[0]);
                            if (current == null) {
                                ip.fData.put(entry[0], entry[1]);
                            } else {
                                ip.fData.put(entry[0], current + ';' + entry[1]);
                            }
                        }
                    }
                    return ip;
                }
            }
        } catch (IOException e) {
            Activator.getInstance().logWarning(e.getMessage(), e);
        }
        return null;
    }
}