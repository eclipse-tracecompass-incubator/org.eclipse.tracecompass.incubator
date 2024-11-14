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

package org.eclipse.tracecompass.incubator.golang.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.DataInput;
import java.io.IOException;

import org.eclipse.tracecompass.incubator.internal.golang.core.trace.LEB128Util;
import org.junit.Test;

/**
 * LEB128 test cases
 *
 * @author Matthew Khouzam
 */
public class LEB128test {

    private static class ByteStream implements DataInput {

        public ByteStream(byte[] input) {
            data = input;
        }

        private final byte[] data;
        private int pos = 0;

        @Override
        public void readFully(byte[] b) throws IOException {
            fail();

        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            fail();

        }

        @Override
        public int skipBytes(int n) throws IOException {
            fail();
            return 0;
        }

        @Override
        public boolean readBoolean() throws IOException {
            fail();
            return false;
        }

        @Override
        public byte readByte() throws IOException {
            try {
                byte value = data[pos++];
                return value;
            } catch (IndexOutOfBoundsException e) {
                // OK we're in a test
            }
            return 0;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            fail();
            return 0;
        }

        @Override
        public short readShort() throws IOException {
            fail();
            return 0;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            fail();
            return 0;
        }

        @Override
        public char readChar() throws IOException {
            fail();
            return 0;
        }

        @Override
        public int readInt() throws IOException {
            fail();
            return 0;
        }

        @Override
        public long readLong() throws IOException {
            fail();
            return 0;
        }

        @Override
        public float readFloat() throws IOException {
            fail();
            return 0;
        }

        @Override
        public double readDouble() throws IOException {
            fail();
            return 0;
        }

        @Override
        public String readLine() throws IOException {
            fail();
            return null;
        }

        @Override
        public String readUTF() throws IOException {
            fail();
            return null;
        }
    }

    /**
     * Test the value 0
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testZero() throws IOException {
        byte[] test = new byte[1];
        test[0] = 0;
        long expected = 0;
        assertEquals(expected, LEB128Util.read(new ByteStream(test)));
    }

    /**
     * Test the value 1
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testOne() throws IOException {
        byte[] test = new byte[1];
        test[0] = 1;
        long expected = 1;
        assertEquals(expected, LEB128Util.read(new ByteStream(test)));
    }

    /**
     * Test the value 624485
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testBig() throws IOException {
        byte[] test = new byte[3];
        test[0] = (byte) 0xe5;
        test[1] = (byte) 0x8e;
        test[2] = 0x26;
        long expected = 624485;
        assertEquals(expected, LEB128Util.read(new ByteStream(test)));
    }

    /**
     * Test the value 167815081739229L
     *
     * @throws IOException
     *             won't happen
     */
    @Test
    public void testVeryBig() throws IOException {
        byte[] test = new byte[7];
        test[0] = (byte) 0xDD;
        test[1] = (byte) 0x87;
        test[2] = (byte) 0xD7;
        test[3] = (byte) 0xF2;
        test[4] = (byte) 0x87;
        test[5] = (byte) 0x94;
        test[6] = 0x26;
        long expected = 167815081739229L;
        assertEquals(expected, LEB128Util.read(new ByteStream(test)));
    }
}
