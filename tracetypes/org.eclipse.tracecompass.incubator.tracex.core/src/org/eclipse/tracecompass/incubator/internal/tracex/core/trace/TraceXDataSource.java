/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Position-independent input used by the TraceX header parser.
 */
public interface TraceXDataSource extends AutoCloseable {

    /**
     * Query the source size.
     *
     * @return size in bytes
     * @throws IOException
     *             if the source cannot be queried
     */
    long size() throws IOException;

    /**
     * Read at an absolute position without changing shared cursor state.
     *
     * @param destination
     *            destination buffer
     * @param position
     *            absolute byte position
     * @return number of bytes read, zero, or -1 at end of input
     * @throws IOException
     *             if the source cannot be read
     */
    int read(ByteBuffer destination, long position) throws IOException;

    /** @return whether the source remains open */
    boolean isOpen();

    @Override
    void close() throws IOException;
}
