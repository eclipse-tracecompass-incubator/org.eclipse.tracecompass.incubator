/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.golang.core.trace;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * LEB128 reader
 *
 * @author Matthew Khouzam
 *
 */
public class LEB128Util {
    /**
     * Read leb128... we're really doing 64 here
     *
     * @param in
     *            leb128
     * @return decoded long
     */
    public static long read(ByteBuffer in) {
        long result = 0;
        long shift = 0;
        byte current = 0;
        do {
            current = in.get();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0);
        return result;
    }

    /**
     * Read leb128... we're really doing 64 here
     *
     * @param in
     *            leb128
     * @return decoded long
     * @throws IOException
     *             if the file ends
     */
    public static long read(DataInput in) throws IOException {
        long result = 0;
        long shift = 0;
        byte current = 0;
        do {
            current = in.readByte();
            result |= ((long) (current & 0x7f)) << shift;
            shift += 7;
        } while ((current & 0x80) != 0);

        return result;
    }
}
