/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

import java.util.List;

/**
 * In the CPU data section the data is divided by the number of CPUs. This
 * represent the data contained in each CPU section.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceFileCPU {
    private final long fOffset; // in bytes
    private final long fSectionSize; // in bytes
    private final int fCpu;
    private final List<BinaryFTraceCPUDataPage> fLstPages;

    /**
     * Constructor
     *
     * @param offset
     *            The starting offset (in bytes) of this CPU section in the
     *            trace file.
     * @param size
     *            The size (in bytes) CPU section in the trace file.
     * @param cpu
     *            The CPU number of this CPU section.
     * @param lstPages
     *            A list of {@link BinaryFTraceCPUDataPage} contains all the
     *            pages belongs to this CPU.
     */
    public BinaryFTraceFileCPU(long offset, long size, int cpu, List<BinaryFTraceCPUDataPage> lstPages) {
        fOffset = offset;
        fSectionSize = size;
        fCpu = cpu;
        fLstPages = lstPages;
    }

    /**
     * Get the starting offset (in bytes) where the data of this cpu starts in
     * the binary file.
     *
     * @return The starting offset of the data of this cpu
     */
    public long getOffset() {
        return fOffset;
    }

    /**
     * Get the size of the data (in bytes) of this cpu in the binary file in
     * bytes
     *
     * @return The size of the data in bytes
     */
    public long getSectionSize() {
        return fSectionSize;
    }

    /**
     * Get the number of this CPU, which is the same as the order that the cpu
     * data is written in the binary file
     *
     * @return the order of this CPU
     */
    public int getCpuNumber() {
        return fCpu;
    }

    /**
     * Get all the pages of this CPU
     *
     * @return A list of all data pages belongs to this CPU
     */
    public List<BinaryFTraceCPUDataPage> getPages() {
        return fLstPages;
    }
}
