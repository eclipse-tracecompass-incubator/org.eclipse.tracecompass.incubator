/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.io.File;
import java.io.IOException;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

/**
 * The session id a random 16-character string (or 8-byte hex number) and it's
 * used as a file name of the map file (e.g. sid-5951ceee0be7fb17.map). A
 * session contains memory mapping of tasks which provides base address of each
 * module (library or executable). It's actually a copy of a /proc/<TID>/maps
 * file.
 *
 * TODO: put in an in memory ht
 *
 * @author Matthew Khouzam
 *
 */
public class MapParser {
    private static final String SESSION_PATTERN_STRING = "sid\\-([a-fA-F0-9]+)\\.map"; //$NON-NLS-1$
    private static final Pattern SESSION_PATTERN = Pattern.compile(SESSION_PATTERN_STRING);
    private static final Pattern MAPFILE_PATTERN = Pattern.compile(
            "^\\s*([a-fA-F0-9]+)\\-([a-fA-F0-9]+)\\s+([rxwps-]+)\\s+([a-fA-F0-9]+)\\s+([a-fA-F0-9]+)\\:([a-fA-F0-9]+)\\s+([a-fA-F0-9]+)\\s*(\\S+)?$"); //$NON-NLS-1$
    private final long fSessionId;
    private final NavigableMap<Long, MapEntry> fData;

    /**
     * Map parser builder
     *
     * @param file
     *            the file to read
     * @return the map parser
     * @throws IOException
     *             error reading the file
     */
    public static MapParser create(File file) throws IOException {
        String name = file.getName();
        Matcher sessionName = SESSION_PATTERN.matcher(name);
        if (!sessionName.matches()) {
            return null;
        }
        long sessionId = Long.parseUnsignedLong(sessionName.group(1), 16);
        LineIterator iter = FileUtils.lineIterator(file);
        NavigableMap<Long, MapEntry> entries = new TreeMap<>();
        while (iter.hasNext()) {
            String line = iter.next();
            Matcher matcher = MAPFILE_PATTERN.matcher(line);
            matcher.matches();
            long addrLow = Long.parseUnsignedLong(matcher.group(1), 16);
            long addrHigh = Long.parseUnsignedLong(matcher.group(2), 16);
            Perms perms = Perms.create(matcher.group(3));
            long offset = Long.parseLong(matcher.group(4), 16);
            char deviceHigh = (char) Integer.parseInt(matcher.group(5), 16);
            char deviceLow = (char) Integer.parseInt(matcher.group(6), 16);
            long iNode = Long.parseLong(matcher.group(7), 16);
            String pathName = matcher.group(8);

            entries.put(addrLow,
                    new MapEntry(addrLow, addrHigh, perms, offset, deviceLow, deviceHigh, iNode, pathName));
        }
        return new MapParser(sessionId, entries);
    }

    private MapParser(long sessionId, NavigableMap<Long, MapEntry> data) {
        fSessionId = sessionId;
        fData = data;
    }

    /**
     * Get the map data
     *
     * @return the map data
     */
    public NavigableMap<Long, MapEntry> getData() {
        return fData;
    }

    /**
     * Get the session ID
     *
     * @return the session ID
     */
    public long getSessionId() {
        return fSessionId;
    }

}
