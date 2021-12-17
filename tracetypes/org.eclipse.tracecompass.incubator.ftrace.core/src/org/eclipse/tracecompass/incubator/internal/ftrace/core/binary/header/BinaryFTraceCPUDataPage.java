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

/**
 * For the CPU Data section of the Binary FTrace data maybe paged. This object
 * represent a page in this section. Reference:
 * https://howtodoinjava.com/design-patterns/creational/builder-pattern-in-java/
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceCPUDataPage {
    private final long fPageStartingOffset;
    private final long fDataStartingOffset;

    private final long fTimeStamp;
    private final long fFlags;
    private final int fSize;
    private final int fCpu;

    // References to other objects to set up the iterator correctly
    private BinaryFTraceCPUDataPage nextPage; // Pointer to the next page

    /**
     * Constructor
     *
     * @param builder
     *            A {@link BinaryFTraceCPUDataPageBuilder} to pass the data to
     *            the BinaryFTraceCPUDataPage so that the object remains
     *            immutable while preventing too many parameters being passed to
     *            the constructor.
     */
    private BinaryFTraceCPUDataPage(BinaryFTraceCPUDataPageBuilder builder) {
        fPageStartingOffset = builder.fBuilderPageStartingOffset;
        fDataStartingOffset = builder.fBuilderDataStartingOffset;
        fTimeStamp = builder.fBuilderTimeStamp;
        fFlags = builder.fBuilderFlags;
        fCpu = builder.fBuilderCpu;
        fSize = builder.fBuilderSize;

        nextPage = builder.builderNextPage;
    }

    /**
     * Get the offset of the first byte of a cpu page
     *
     * @return the offset of the first byte
     */
    public long getPageStartingOffset() {
        return fPageStartingOffset;
    }

    /**
     * Get the size of the page in bytes
     *
     * @return the size of the page
     */
    public int getSize() {
        return fSize;
    }

    /**
     * Get the offset of the start of recorded events in a cpu page
     *
     * @return the offset of the start of data in a cpu page
     */
    public long getDataStartingOffset() {
        return fDataStartingOffset;
    }

    /**
     * Get the time stamp of this cpu page
     *
     * @return the time stamp of each cpu page
     */
    public long getTimeStamp() {
        return fTimeStamp;
    }

    /**
     * Get flags of this cpu page
     *
     * @return the flags of this cpu page
     */
    public long getFlags() {
        return fFlags;
    }

    /**
     * Get the CPU of the page.
     *
     * @return The CPU number of the page.
     */
    public int getCpu() {
        return fCpu;
    }

    /**
     * Get the next page after this page. Returns null if there is no more next
     * page.
     *
     * @return The next BinaryFTraceCPUDataPage of this page.
     */
    public BinaryFTraceCPUDataPage getNextPage() {
        return nextPage;
    }

    /**
     * Builder
     */
    public static class BinaryFTraceCPUDataPageBuilder {
        private long fBuilderPageStartingOffset;
        private long fBuilderDataStartingOffset;

        private long fBuilderTimeStamp;
        private long fBuilderFlags;
        private int fBuilderCpu;
        private int fBuilderSize;

        private BinaryFTraceCPUDataPage builderNextPage; // Pointer to the next
                                                         // page

        /**
         * Constructor
         *
         * Initialize all default values for a {@link BinaryFTraceCPUDataPage}
         */
        public BinaryFTraceCPUDataPageBuilder() {
            fBuilderPageStartingOffset = 0;
            fBuilderDataStartingOffset = 0;
            fBuilderTimeStamp = 0;
            fBuilderFlags = 0;
            fBuilderCpu = 0;
            fBuilderSize = 0;
            builderNextPage = null;
        }

        /**
         * Set the starting offset (in bytes) of the current page.
         *
         * @param pageStartingOffset
         *            The starting offset (in bytes) of the current page.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder pageStartingOffset(long pageStartingOffset) {
            fBuilderPageStartingOffset = pageStartingOffset;
            return this;
        }

        /**
         * Set the starting offset (in bytes) of the data section of the current
         * page.
         *
         * @param pageDataStartingOffset
         *            The starting offset (in bytes) of the data section of the
         *            current page.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder pageDataStartingOffset(long pageDataStartingOffset) {
            fBuilderDataStartingOffset = pageDataStartingOffset;
            return this;
        }

        /**
         * Set the time stamp of the current page.
         *
         * @param timeStamp The time stamp of the current page.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder timeStamp(long timeStamp) {
            fBuilderTimeStamp = timeStamp;
            return this;
        }

        /**
         * Set the flags value of the current page. The flag should be interpreted in binary.
         *
         * @param flags The flags value of the current page.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder flags(long flags) {
            fBuilderFlags = flags;
            return this;
        }

        /**
         * Set the cpu number of this page.
         *
         * @param cpu The cpu number of this page.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder cpu(int cpu) {
            fBuilderCpu = cpu;
            return this;
        }

        /**
         * Set a {@link BinaryFTraceCPUDataPage} as the following page of the current page in this CPU section.
         * Set to null if the the page is the last page in the CPU section.
         *
         * @param nextPage The page following this page in this CPU section.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder nextPage(BinaryFTraceCPUDataPage nextPage) {
            this.builderNextPage = nextPage;
            return this;
        }

        /**
         * Set the size of the page
         * @param size the size
         *
         * @param headerInfo The header information of the FTrace binary file.
         * @return A {@link BinaryFTraceCPUDataPageBuilder}
         */
        public BinaryFTraceCPUDataPageBuilder size(int size) {
            fBuilderSize = size;
            return this;
        }

        /**
         * Generate a new {@link BinaryFTraceCPUDataPage} using the current information stored
         * in the builder.
         *
         * @return a {@link BinaryFTraceCPUDataPage}
         */
        public BinaryFTraceCPUDataPage build() {
            return new BinaryFTraceCPUDataPage(this);
        }
    }
}
