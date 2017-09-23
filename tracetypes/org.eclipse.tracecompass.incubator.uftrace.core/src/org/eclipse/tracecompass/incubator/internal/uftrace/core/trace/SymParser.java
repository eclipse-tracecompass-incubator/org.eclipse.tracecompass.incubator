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
    private static final Pattern REGEX = Pattern.compile("^([a-fA-F\\d]+)\\s+([PTptw])\\s*(.*)$"); //$NON-NLS-1$

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
        LineIterator iter = FileUtils.lineIterator(file);
        SymParser sp = new SymParser();
        while (iter.hasNext()) {
            String line = iter.next();
            Matcher match = REGEX.matcher(line);
            if (!match.matches()) {
                throw new IllegalArgumentException("invalid " + line); //$NON-NLS-1$
            }
            long range = Long.parseUnsignedLong(match.group(1), 16);
            char c = match.group(2).charAt(0);
            String name = (match.groupCount() < 3) ? "Anonymous" : match.group(3);
            Symbol sym = new Symbol(c, name);
            sp.fMap.put(range, sym);
        }
        return sp;
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
