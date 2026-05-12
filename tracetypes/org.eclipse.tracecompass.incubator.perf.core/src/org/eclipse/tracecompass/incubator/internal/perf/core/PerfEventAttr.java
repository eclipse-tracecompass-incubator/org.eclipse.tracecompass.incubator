/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core;

import java.util.List;

/**
 * Parsed form of a {@code perf_file_attr} entry: the kernel
 * {@code perf_event_attr} plus the list of u64 event IDs that were emitted
 * for this configuration.
 *
 * Only the fields that are consequential for data-stream parsing are kept
 * strongly-typed. The raw attr bytes are kept so that consumers that care
 * about the full description can access it.
 */
public final class PerfEventAttr {

    private final int fType;
    private final int fSize;
    private final long fConfig;
    private final long fSamplePeriod;
    private final long fSampleType;
    private final long fReadFormat;
    private final long fFlags;
    private final long fBranchSampleType;
    private final List<Long> fIds;

    /**
     * Constructor
     *
     * @param type
     *            event type (PERF_TYPE_*)
     * @param size
     *            struct size, from perf_event_attr
     * @param config
     *            type-specific configuration
     * @param samplePeriod
     *            sample_period or sample_freq
     * @param sampleType
     *            PERF_SAMPLE_* bitmap
     * @param readFormat
     *            layout of PERF_FORMAT_READ
     * @param flags
     *            packed bit fields (disabled, inherit, ..., sample_id_all,
     *            ...). Only {@code sample_id_all} is interpreted.
     * @param branchSampleType
     *            branch-stack layout flags
     * @param ids
     *            list of event IDs seen in the data stream for this attr
     */
    public PerfEventAttr(int type, int size, long config, long samplePeriod,
            long sampleType, long readFormat, long flags, long branchSampleType,
            List<Long> ids) {
        fType = type;
        fSize = size;
        fConfig = config;
        fSamplePeriod = samplePeriod;
        fSampleType = sampleType;
        fReadFormat = readFormat;
        fFlags = flags;
        fBranchSampleType = branchSampleType;
        fIds = List.copyOf(ids);
    }

    /**
     * @return the PERF_TYPE_* value
     */
    public int getType() {
        return fType;
    }

    /**
     * @return the perf_event_attr.size value
     */
    public int getSize() {
        return fSize;
    }

    /**
     * @return the event config
     */
    public long getConfig() {
        return fConfig;
    }

    /**
     * @return the sample period (or frequency)
     */
    public long getSamplePeriod() {
        return fSamplePeriod;
    }

    /**
     * @return the PERF_SAMPLE_* bitmap controlling sample layout
     */
    public long getSampleType() {
        return fSampleType;
    }

    /**
     * @return the PERF_FORMAT_* bitmap controlling read_format layout
     */
    public long getReadFormat() {
        return fReadFormat;
    }

    /**
     * @return the packed flags qword (including sample_id_all)
     */
    public long getFlags() {
        return fFlags;
    }

    /**
     * @return true if sample_id_all is set, meaning non-SAMPLE records carry
     *         a trailing sample_id block
     */
    public boolean isSampleIdAll() {
        // sample_id_all is bit 18 in the packed flags qword (after disabled,
        // inherit, pinned, exclusive, exclude_user, exclude_kernel, exclude_hv,
        // exclude_idle, mmap, comm, freq, inherit_stat, enable_on_exec, task,
        // watermark, precise_ip:2, mmap_data, sample_id_all).
        return ((fFlags >>> 18) & 1L) != 0L;
    }

    /**
     * @return the branch_sample_type bitmap
     */
    public long getBranchSampleType() {
        return fBranchSampleType;
    }

    /**
     * @return the event IDs pointing to this attr
     */
    public List<Long> getIds() {
        return fIds;
    }
}
