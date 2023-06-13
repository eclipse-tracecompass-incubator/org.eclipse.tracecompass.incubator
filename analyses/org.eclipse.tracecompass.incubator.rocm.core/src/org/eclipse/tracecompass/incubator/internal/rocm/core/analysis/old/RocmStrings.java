/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old;

/**
 * Strings used in the ROCm module
 *
 * @author Arnaud Fiorini
 * @noimplement This interface is not intended to be implemented by clients.
 */
@SuppressWarnings({ "javadoc", "nls" })
public interface RocmStrings {
    /* Event types */
    String GPU_KERNEL = "compute_kernels_hsa";
    String HSA_API = "hsa_api";
    String HIP_API = "hip_api";
    String HCC_OPS = "hcc_ops";
    String ROCTX = "roctx";
    String ASYNC_COPY = "async_copy";
    String HIP_ACTIVITY = "hip_activity";
    String HSA_ACTIVITY = "hsa_activity";
    String KERNEL_EVENT = "kernel_event";
    String HIP_FUNCTION_NAME = "hip_function_name";
    String HSA_FUNCTION_NAME = "hsa_function_name";
    String METRIC_DOUBLE = "metric_double";
    String METRIC_INTEGER = "metric_uint64";
    String METRIC_NAME = "metric_name";
    String METRIC_NAME_END = "metric_name_end";

    /* Field names */
    String NAME = "name";
    String ARGS = "args";
    String KERNEL_NAME = "kernel_name";
    String KERNEL_DISPATCH_ID = "kernel_dispatch_id";
    String GPU_ID = "gpu_id";
    String CORRELATION_ID = "correlation_id";
    String DEVICE_ID = "device_id";
    String QUEUE_ID = "queue_id";
    String QUEUE_INDEX = "queue_index";
    String STREAM_ID = "stream_id";
    /** Function call id, is used to identify the function name */
    String CID = "cid";
    String TID = "tid";
    String PID = "pid";
    String ID = "id";
    String END = "end";
    String MESSAGE = "message";
    String VALUE = "value";

    /* State categories */
    String EDGES = "Edges";
    String GPU_ACTIVITY = "GPU Activity";
    String GPU = "GPU ";
    String GAP_ANALYSIS = "Gap Analysis";
    String THREAD = "Thread ";
    String SYSTEM = "System";
    String MEMORY = "Memory";
    String MEMORY_TRANSFERS = "Memory Transfers";
    String STREAMS = "HIP Streams";
    String STREAM = "Stream ";
    String QUEUES = "Queues";
    String QUEUE = "Queue ";
    String GPU_KERNELS = "GPU Kernels";

    /* Event Names */
    String KERNEL_EXECUTION = "KernelExecution";
    String KERNEL_LAUNCH = "hipLaunchKernel";
    String HIP_DEVICE_SYNCHRONIZE = "hipDeviceSynchronize";
    String COPY = "Copy";

    /* State values */
    String IDLE = "Idle";
}
