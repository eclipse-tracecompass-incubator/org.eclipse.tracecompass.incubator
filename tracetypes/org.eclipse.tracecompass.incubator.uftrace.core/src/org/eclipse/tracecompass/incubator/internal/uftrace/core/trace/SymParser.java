/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 *
 * The uftrace saves the symbol table of the traced program so that it can
 * resolve the symbol from address easily. The symbol file contains only
 * function symbols and its format is almost identical to the output of nm(1)
 * command. The difference is that it also saves PLT entries which is used to
 * call library functions and it has 'P' type.
 *
 *
 * @author Matthew Khouzam
 *
 */
public class SymParser {
    private static final Pattern REGEX = Pattern.compile("^([a-fA-F\\d]+)\\s+([a-fA-F\\d]*)\\s*([ABbCcDdGgiNPpRrSsTtUuVvWw\\-\\?])\\s*(.*)$"); //$NON-NLS-1$

    /**
     * Symbol for
     *
     * @author Matthew Khouzam
     *
     */
    public static final class Symbol {
        private final char fType;
        private final String fName;

        private Symbol(char c, String name) {
            fType = c;
            fName = name;
        }

        /**
         * @return the type
         */
        public char getType() {
            return fType;
        }

        /**
         * @return the name
         */
        public String getName() {
            return fName;
        }

    }

    private final NavigableMap<Long, Symbol> fMap = new TreeMap<>();

    /**
     * Parse a file to get a symbol
     *
     * @param file
     *            the symbol file
     * @return the parser
     * @throws IOException
     *             the file is not able to be read.
     */
    public static SymParser parse(File file) throws IOException {

        try (LineIterator iter = FileUtils.lineIterator(file)) {
            SymParser sp = new SymParser();
            while (iter.hasNext()) {
                String line = iter.next();
                if (line.startsWith("#")) { //$NON-NLS-1$
                    continue;
                }
                Matcher match = REGEX.matcher(line);
                if (!match.matches()) {
                    throw new IllegalArgumentException("Symbol Parser: invalid line: " + line); //$NON-NLS-1$
                }
                long range = Long.parseUnsignedLong(match.group(1), 16);
                char c = match.group(3).charAt(0);
                String name = (match.groupCount() < 4) ? "Anonymous" : match.group(4); //$NON-NLS-1$
                Symbol sym = new Symbol(c, name);
                sp.fMap.put(range, sym);
            }
            return sp;
        }
    }

    /**
     * Get the map of symbols for addresses.
     *
     * @return the symbol for an address
     */
    public NavigableMap<Long, Symbol> getMap() {
        return fMap;
    }
}
