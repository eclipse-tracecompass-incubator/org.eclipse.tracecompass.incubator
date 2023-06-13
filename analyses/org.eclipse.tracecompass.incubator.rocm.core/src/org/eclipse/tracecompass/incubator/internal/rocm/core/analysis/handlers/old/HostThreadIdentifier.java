/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.handlers.old;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.rocm.core.trace.old.RocmTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * This class is used to identify the correct lane when defining dependencies.
 *
 * @author Arnaud Fiorini
 */
public class HostThreadIdentifier {
    /**
     * Each kernel is executed in a stream (logical separation) and a queue
     * (hardware separation) We create for each activity two state intervals:
     * one for the queue and one for the stream
     *
     * @author Arnaud Fiorini
     */
    public enum KERNEL_CATEGORY {
        /**
         * Activity mapped to the hardware queue designated by the ROCm runtime.
         */
        QUEUE,
        /**
         * Stream chosen by the user to execute its compute kernel.
         */
        STREAM
    }

    /**
     * ROCm categories used to differentiate memory transfers and API calls
     *
     * @author Arnaud Fiorini
     */
    public enum ROCM_CATEGORY {
        /**
         * API calls
         */
        SYSTEM,
        /**
         * Memory transfers
         */
        MEMORY
    }

    private final int fApiId; // Api type, Queue id, Stream id
    private final int fThreadId; // Tid, Queue type, Stream type
    private final int fCategoryId; // System, Memory, GPU id

    private HostThreadIdentifier(int apiId, int threadId, int categoryId) {
        fApiId = apiId;
        fThreadId = threadId;
        fCategoryId = categoryId;
    }

    /**
     * Constructor for Memory transfer events, as there is only one call stack
     * for this, there is no parameters
     *
     */
    public HostThreadIdentifier() {
        this(0, 0, ROCM_CATEGORY.MEMORY.ordinal());
    }

    /**
     * Constructor for GPU events
     *
     * @param categoryId
     *            stream id or queue id
     * @param category
     *            Either queues or streams
     * @param gpuId
     *            the id of the GPU
     */
    public HostThreadIdentifier(int categoryId, KERNEL_CATEGORY category, int gpuId) {
        // There are other categories (system, memory), this will separate the
        // GPU categories.
        this(categoryId, category.ordinal(), gpuId + ROCM_CATEGORY.values().length);
    }

    /**
     * Constructor for API events
     *
     * @param event
     *            the event from which we create the HostThreadIdentifier
     * @param tid
     *            the tid of this event
     */
    public HostThreadIdentifier(ITmfEvent event, int tid) {
        this(((RocmTrace) event.getTrace()).getApiId(event.getName()), tid, ROCM_CATEGORY.SYSTEM.ordinal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fApiId, fThreadId, fCategoryId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        HostThreadIdentifier that = (HostThreadIdentifier) other;
        return (fApiId == that.fApiId) && (fThreadId == that.fThreadId) && (fCategoryId == that.fCategoryId);
    }
}
