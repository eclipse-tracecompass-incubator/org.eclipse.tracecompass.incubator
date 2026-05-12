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

/**
 * Constants used for parsing {@code perf.data} files. These values come from
 * {@code tools/perf/util/header.h} and {@code include/uapi/linux/perf_event.h}
 * in the Linux kernel, as mirrored in
 * {@code src/quipper/kernel/perf_internals.h} and
 * {@code src/quipper/kernel/perf_event.h} of
 * <a href="https://github.com/google/perf_data_converter">perf_data_converter</a>.
 */
public final class PerfConstants {

    private PerfConstants() {
        // utility class
    }

    // ---------------------------------------------------------------------
    // Magic numbers
    // ---------------------------------------------------------------------

    /**
     * Little-endian "PERFILE2" magic (normal random-access file header).
     */
    public static final long PERF_MAGIC_LE = 0x32454c4946524550L;

    /**
     * Big-endian version of PERF_MAGIC_LE (byte-reversed). When this value is
     * read, the file was written by a big-endian host.
     */
    public static final long PERF_MAGIC_BE = 0x50455246494c4532L;

    /**
     * Little-endian "PERFFILE" (older PERFILE1 header). Kept for detection.
     */
    public static final long PERF_MAGIC_V1_LE = 0x454c4946524550L;

    // ---------------------------------------------------------------------
    // Feature bitmap
    // ---------------------------------------------------------------------

    /** Size of the {@code adds_features} bitmap, in bits. */
    public static final int HEADER_FEAT_BITS = 256;

    /** Reserved feature bit, always zero. */
    public static final int HEADER_RESERVED = 0;
    /** Tracing data (tracefs blob). */
    public static final int HEADER_TRACING_DATA = 1;
    /** Build-id array. */
    public static final int HEADER_BUILD_ID = 2;
    /** uname -n. */
    public static final int HEADER_HOSTNAME = 3;
    /** uname -r. */
    public static final int HEADER_OSRELEASE = 4;
    /** perf version string. */
    public static final int HEADER_VERSION = 5;
    /** uname -m. */
    public static final int HEADER_ARCH = 6;
    /** Number of CPUs. */
    public static final int HEADER_NRCPUS = 7;
    /** CPU description. */
    public static final int HEADER_CPUDESC = 8;
    /** CPU ID string. */
    public static final int HEADER_CPUID = 9;
    /** Total memory. */
    public static final int HEADER_TOTAL_MEM = 10;
    /** Command line used for perf record. */
    public static final int HEADER_CMDLINE = 11;
    /** Per-event descriptors. */
    public static final int HEADER_EVENT_DESC = 12;
    /** CPU topology. */
    public static final int HEADER_CPU_TOPOLOGY = 13;
    /** NUMA topology. */
    public static final int HEADER_NUMA_TOPOLOGY = 14;
    /** Branch stack availability marker. */
    public static final int HEADER_BRANCH_STACK = 15;
    /** PMU type-id to name mappings. */
    public static final int HEADER_PMU_MAPPINGS = 16;
    /** Event-group layout. */
    public static final int HEADER_GROUP_DESC = 17;
    /** AUX trace index. */
    public static final int HEADER_AUXTRACE = 18;
    /** perf stat recording marker. */
    public static final int HEADER_STAT = 19;
    /** CPU cache hierarchy. */
    public static final int HEADER_CACHE = 20;
    /** First/last sample timestamps. */
    public static final int HEADER_SAMPLE_TIME = 21;
    /** Memory topology. */
    public static final int HEADER_MEM_TOPOLOGY = 22;
    /** clockid used for timestamps. */
    public static final int HEADER_CLOCKID = 23;
    /** Directory-format recording marker. */
    public static final int HEADER_DIR_FORMAT = 24;
    /** BPF prog_info. */
    public static final int HEADER_BPF_PROG_INFO = 25;
    /** BPF BTF metadata. */
    public static final int HEADER_BPF_BTF = 26;
    /** Compression parameters. */
    public static final int HEADER_COMPRESSED = 27;
    /** Per-CPU-PMU capability strings. */
    public static final int HEADER_CPU_PMU_CAPS = 28;
    /** Wall-clock / monotonic reference pair. */
    public static final int HEADER_CLOCK_DATA = 29;
    /** Hybrid-core layout. */
    public static final int HEADER_HYBRID_TOPOLOGY = 30;

    // ---------------------------------------------------------------------
    // Record types — kernel (perf_event_type)
    // ---------------------------------------------------------------------

    /** Memory map. */
    public static final int PERF_RECORD_MMAP = 1;
    /** Lost samples count. */
    public static final int PERF_RECORD_LOST = 2;
    /** Thread comm. */
    public static final int PERF_RECORD_COMM = 3;
    /** Thread exit. */
    public static final int PERF_RECORD_EXIT = 4;
    /** Throttle. */
    public static final int PERF_RECORD_THROTTLE = 5;
    /** Unthrottle. */
    public static final int PERF_RECORD_UNTHROTTLE = 6;
    /** Fork. */
    public static final int PERF_RECORD_FORK = 7;
    /** Counter read. */
    public static final int PERF_RECORD_READ = 8;
    /** Sample. */
    public static final int PERF_RECORD_SAMPLE = 9;
    /** Memory map with maj/min/ino or build-id. */
    public static final int PERF_RECORD_MMAP2 = 10;
    /** AUX buffer data. */
    public static final int PERF_RECORD_AUX = 11;
    /** Instruction trace start. */
    public static final int PERF_RECORD_ITRACE_START = 12;
    /** Lost samples number. */
    public static final int PERF_RECORD_LOST_SAMPLES = 13;
    /** Context switch. */
    public static final int PERF_RECORD_SWITCH = 14;
    /** CPU-wide context switch. */
    public static final int PERF_RECORD_SWITCH_CPU_WIDE = 15;
    /** Namespaces. */
    public static final int PERF_RECORD_NAMESPACES = 16;
    /** Cgroup path. */
    public static final int PERF_RECORD_CGROUP = 17;
    /** Kernel symbol load/unload. */
    public static final int PERF_RECORD_KSYMBOL = 18;
    /** BPF prog load/unload. */
    public static final int PERF_RECORD_BPF_EVENT = 19;
    /** Kernel text patch. */
    public static final int PERF_RECORD_TEXT_POKE = 20;
    /** AUX output HW id mapping. */
    public static final int PERF_RECORD_AUX_OUTPUT_HW_ID = 21;

    // ---------------------------------------------------------------------
    // Record types — user (perf_user_event_type, values 64+)
    // ---------------------------------------------------------------------

    /** Attr + IDs (piped mode). */
    public static final int PERF_RECORD_HEADER_ATTR = 64;
    /** Legacy event type (deprecated). */
    public static final int PERF_RECORD_HEADER_EVENT_TYPE = 65;
    /** tracefs blob. */
    public static final int PERF_RECORD_HEADER_TRACING_DATA = 66;
    /** Build-id for an mmapped binary. */
    public static final int PERF_RECORD_HEADER_BUILD_ID = 67;
    /** Round finished marker. */
    public static final int PERF_RECORD_FINISHED_ROUND = 68;
    /** Sample ID index. */
    public static final int PERF_RECORD_ID_INDEX = 69;
    /** AUX trace info. */
    public static final int PERF_RECORD_AUXTRACE_INFO = 70;
    /** AUX trace chunk. */
    public static final int PERF_RECORD_AUXTRACE = 71;
    /** AUX decoding error. */
    public static final int PERF_RECORD_AUXTRACE_ERROR = 72;
    /** Thread map. */
    public static final int PERF_RECORD_THREAD_MAP = 73;
    /** CPU map. */
    public static final int PERF_RECORD_CPU_MAP = 74;
    /** perf stat aggregation config. */
    public static final int PERF_RECORD_STAT_CONFIG = 75;
    /** perf stat counter sample. */
    public static final int PERF_RECORD_STAT = 76;
    /** perf stat round boundary. */
    public static final int PERF_RECORD_STAT_ROUND = 77;
    /** Event update. */
    public static final int PERF_RECORD_EVENT_UPDATE = 78;
    /** TSC-to-ns constants. */
    public static final int PERF_RECORD_TIME_CONV = 79;
    /** Feature section content (piped). */
    public static final int PERF_RECORD_HEADER_FEATURE = 80;
    /** Zstd-compressed block. */
    public static final int PERF_RECORD_COMPRESSED = 81;
    /** Synthesized init events finished. */
    public static final int PERF_RECORD_FINISHED_INIT = 82;
    /** Newer compressed block. */
    public static final int PERF_RECORD_COMPRESSED2 = 83;
    /** BPF metadata blob. */
    public static final int PERF_RECORD_BPF_METADATA = 84;

    // ---------------------------------------------------------------------
    // sample_type bitmap (PERF_SAMPLE_*)
    // ---------------------------------------------------------------------

    /** Instruction pointer. */
    public static final long PERF_SAMPLE_IP = 1L << 0;
    /** pid / tid pair. */
    public static final long PERF_SAMPLE_TID = 1L << 1;
    /** Timestamp. */
    public static final long PERF_SAMPLE_TIME = 1L << 2;
    /** Data address. */
    public static final long PERF_SAMPLE_ADDR = 1L << 3;
    /** Counter read. */
    public static final long PERF_SAMPLE_READ = 1L << 4;
    /** Callchain. */
    public static final long PERF_SAMPLE_CALLCHAIN = 1L << 5;
    /** Event ID. */
    public static final long PERF_SAMPLE_ID = 1L << 6;
    /** CPU. */
    public static final long PERF_SAMPLE_CPU = 1L << 7;
    /** Sampling period. */
    public static final long PERF_SAMPLE_PERIOD = 1L << 8;
    /** Stream ID. */
    public static final long PERF_SAMPLE_STREAM_ID = 1L << 9;
    /** Raw blob. */
    public static final long PERF_SAMPLE_RAW = 1L << 10;
    /** Branch stack. */
    public static final long PERF_SAMPLE_BRANCH_STACK = 1L << 11;
    /** User registers. */
    public static final long PERF_SAMPLE_REGS_USER = 1L << 12;
    /** User stack. */
    public static final long PERF_SAMPLE_STACK_USER = 1L << 13;
    /** Weight. */
    public static final long PERF_SAMPLE_WEIGHT = 1L << 14;
    /** Data source. */
    public static final long PERF_SAMPLE_DATA_SRC = 1L << 15;
    /** Identifier (duplicates ID at a fixed position). */
    public static final long PERF_SAMPLE_IDENTIFIER = 1L << 16;
    /** Transaction. */
    public static final long PERF_SAMPLE_TRANSACTION = 1L << 17;
    /** Interrupt-time registers. */
    public static final long PERF_SAMPLE_REGS_INTR = 1L << 18;
    /** Physical address. */
    public static final long PERF_SAMPLE_PHYS_ADDR = 1L << 19;
    /** AUX data blob. */
    public static final long PERF_SAMPLE_AUX = 1L << 20;
    /** Cgroup id. */
    public static final long PERF_SAMPLE_CGROUP = 1L << 21;
    /** Data page size. */
    public static final long PERF_SAMPLE_DATA_PAGE_SIZE = 1L << 22;
    /** Code page size. */
    public static final long PERF_SAMPLE_CODE_PAGE_SIZE = 1L << 23;
    /** Weight struct variant. */
    public static final long PERF_SAMPLE_WEIGHT_STRUCT = 1L << 24;

    // ---------------------------------------------------------------------
    // misc flag masks (for perf_event_header.misc)
    // ---------------------------------------------------------------------

    /** CPU mode mask. */
    public static final int PERF_RECORD_MISC_CPUMODE_MASK = 7;
    /** Unknown CPU mode. */
    public static final int PERF_RECORD_MISC_CPUMODE_UNKNOWN = 0;
    /** Kernel mode. */
    public static final int PERF_RECORD_MISC_KERNEL = 1;
    /** User mode. */
    public static final int PERF_RECORD_MISC_USER = 2;
    /** Hypervisor mode. */
    public static final int PERF_RECORD_MISC_HYPERVISOR = 3;
    /** Guest kernel. */
    public static final int PERF_RECORD_MISC_GUEST_KERNEL = 4;
    /** Guest user. */
    public static final int PERF_RECORD_MISC_GUEST_USER = 5;
    /** MMAP2 carries a build-id. */
    public static final int PERF_RECORD_MISC_MMAP_BUILD_ID = 1 << 14;
    /** COMM_EXEC / MMAP_DATA / SWITCH_OUT shared bit. */
    public static final int PERF_RECORD_MISC_COMM_EXEC = 1 << 13;
    /** Context switch out. */
    public static final int PERF_RECORD_MISC_SWITCH_OUT = 1 << 13;

    /** Size of the kernel {@code perf_event_header} struct. */
    public static final int PERF_EVENT_HEADER_SIZE = 8; // u32 type, u16 misc, u16 size
}
