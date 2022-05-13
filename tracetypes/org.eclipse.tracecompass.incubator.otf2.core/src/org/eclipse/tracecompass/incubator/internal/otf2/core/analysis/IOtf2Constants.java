/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis;

import java.util.regex.Pattern;

/**
 * Constants for the OTF2 format
 *
 * @author Yoann Heitz
 */
public interface IOtf2Constants {
    /**
     * Pattern to match an OTF2 event name (<event type>_<event name>) like:
     * GlobalDef_Region
     */
    Pattern OTF2_EVENT_NAME_PATTERN = Pattern.compile("^(?<type>.*)_(?<name>.*)"); //$NON-NLS-1$

    /**
     * Edges
     */
    String EDGES = "Edges"; //$NON-NLS-1$

    /**
     * Unknown string
     */
    String UNKNOWN_STRING = "UNKNOWN"; //$NON-NLS-1$

    /**
     * type group in {@link IOtf2Constants#OTF2_EVENT_NAME_PATTERN}
     */
    String OTF2_TYPE_GROUP = "type"; //$NON-NLS-1$

    /**
     * name group in {@link IOtf2Constants#OTF2_EVENT_NAME_PATTERN}
     */
    String OTF2_NAME_GROUP = "name"; //$NON-NLS-1$

    /**
     * Global definition event type
     */
    String OTF2_GLOBAL_DEFINITION = "GlobalDef"; //$NON-NLS-1$

    /**
     * Event event type
     */
    String OTF2_EVENT = "Event"; //$NON-NLS-1$

    /**
     * Enum for collective MPI operations
     */
    enum CollectiveOperation {
        /**
         * MPI_Barrier operation
         */
        BARRIER,

        /**
         * MPI_Bcast operation
         */
        BCAST,

        /**
         * MPI_Gather operation
         */
        GATHER,

        /**
         * MPI_Gatherv operation
         */
        GATHERV,

        /**
         * MPI_Scatter operation
         */
        SCATTER,

        /**
         * MPI_Scatterv operation
         */
        SCATTERV,

        /**
         * MPI_Allgather operation
         */
        ALLGATHER,

        /**
         * MPI_Allgatherv operation
         */
        ALLGATHERV,

        /**
         * MPI_Alltoall operation
         */
        ALLTOALL,

        /**
         * MPI_Alltoallv operation
         */
        ALLTOALLV,

        /**
         * MPI_Alltoallw operation
         */
        ALLTOALLW,

        /**
         * MPI_Allreduce operation
         */
        ALLREDUCE,

        /**
         * MPI_Reduce operation
         */
        REDUCE,

        /**
         * MPI_Reducescatter operation
         */
        REDUCE_SCATTER,

        /**
         * MPI_Scan operation
         */
        SCAN,

        /**
         * MPI_Exscan operation
         */
        EXSCAN,

        /**
         * MPI_Reduce_scatter_block operation
         */
        REDUCE_SCATTER_BLOCK,

        /**
         * MPI create handle operation
         */
        CREATE_HANDLE,

        /**
         * MPI destroy handle operation
         */
        DESTROY_HANDLE,

        /**
         * MPI allocate operation
         */
        ALLOCATE,

        /**
         * MPI deallocate operation
         */
        DEALLOCATE,

        /**
         * MPI create handle and allocate operation
         */
        CREATE_HANDLE_AND_ALLOCATE,

        /**
         * MPI destroy handle and deallocate operation
         */
        DESTROY_HANDLE_AND_DEALLOCATE,

        /**
         * Unknown operation
         */
        UNKNOWN_OPERATION;
    }

    /**
     * @param operationCode
     *            The code for the collective operation
     * @return the associated operation member from CollectiveOperation
     */
    static CollectiveOperation getOperation(int operationCode) {
        switch (operationCode) {
        case 0:
            return CollectiveOperation.BARRIER;
        case 1:
            return CollectiveOperation.BCAST;
        case 2:
            return CollectiveOperation.GATHER;
        case 3:
            return CollectiveOperation.GATHERV;
        case 4:
            return CollectiveOperation.SCATTER;
        case 5:
            return CollectiveOperation.SCATTERV;
        case 6:
            return CollectiveOperation.ALLGATHER;
        case 7:
            return CollectiveOperation.ALLGATHERV;
        case 8:
            return CollectiveOperation.ALLTOALL;
        case 9:
            return CollectiveOperation.ALLTOALLV;
        case 10:
            return CollectiveOperation.ALLTOALLW;
        case 11:
            return CollectiveOperation.ALLREDUCE;
        case 12:
            return CollectiveOperation.REDUCE;
        case 13:
            return CollectiveOperation.REDUCE_SCATTER;
        case 14:
            return CollectiveOperation.SCAN;
        case 15:
            return CollectiveOperation.EXSCAN;
        case 16:
            return CollectiveOperation.REDUCE_SCATTER_BLOCK;
        case 17:
            return CollectiveOperation.CREATE_HANDLE;
        case 18:
            return CollectiveOperation.DESTROY_HANDLE;
        case 19:
            return CollectiveOperation.ALLOCATE;
        case 20:
            return CollectiveOperation.DEALLOCATE;
        case 21:
            return CollectiveOperation.CREATE_HANDLE_AND_ALLOCATE;
        case 22:
            return CollectiveOperation.DESTROY_HANDLE_AND_DEALLOCATE;
        default:
            return CollectiveOperation.UNKNOWN_OPERATION;
        }
    }

    /**
     * int constant representing OTF2 undefined uint8 value
     */
    int OTF2_UNDEFINED_UINT8 = (1 << 8) - 1;

    /**
     * long constant representing OTF2 undefined uint32 value
     */
    long OTF2_UNDEFINED_UINT32 = (1L << 32) - 1;

    /**
     * long constant representing OTF2 undefined uint64 value
     */
    long OTF2_UNDEFINED_UINT64 = ~(0L);

    /**
     * long constant representing OTF2 undefined int64 value
     */
    long OTF2_UNDEFINED_INT64 = ~(OTF2_UNDEFINED_UINT64 >>> 1);

    /**
     * In the following lines, undefined and unknown constants are defined for
     * the different OTF2 references.
     *
     * The undefined constants should be used when no issues occurred while
     * reading a reference field but when the reference value is the undefined
     * value in the OTF2 standards.
     *
     * The unknown constants should be used when an issue occurred while reading
     * a reference but a default value should be used to continue to process the
     * trace.
     */

    /**
     * Constant representing an unknown string reference.
     */
    long OTF2_UNKNOWN_STRING = OTF2_UNDEFINED_UINT32;

    /**
     * Constant representing an undefined system tree node reference.
     */
    long OTF2_UNDEFINED_SYSTEM_TREE_NODE = OTF2_UNDEFINED_UINT32;

    /**
     * Constant representing an unknown system tree node reference.
     */
    long OTF2_UNKNOWN_SYSTEM_TREE_NODE = OTF2_UNDEFINED_UINT32;

    /**
     * Constant representing an unknown location group reference.
     */
    long OTF2_UNKNOWN_LOCATION_GROUP = OTF2_UNDEFINED_UINT32;

    /**
     * Constant representing an unknown location reference.
     */
    long OTF2_UNKNOWN_LOCATION = OTF2_UNDEFINED_UINT64;

    /**
     * Constant representing an unknown location group type.
     */
    int OTF2_UNKNOWN_LOCATION_GROUP_TYPE = OTF2_UNDEFINED_UINT8;

    /**
     * Constant representing an unknown metric member reference.
     */
    long OTF2_UNKNOWN_METRIC_MEMBER = OTF2_UNDEFINED_UINT32;

    /**
     * Constant representing an unknown metric type.
     */
    int OTF2_UNKNOWN_METRIC_TYPE = OTF2_UNDEFINED_UINT8;

    /**
     * Constant representing an unknown metric mode.
     */
    int OTF2_UNKNOWN_METRIC_MODE = OTF2_UNDEFINED_UINT8;

    /**
     * Constant representing an unknown value type.
     */
    int OTF2_UNKNOWN_VALUE_TYPE = OTF2_UNDEFINED_UINT8;

    /**
     * Constant representing an unknown base.
     */
    int OTF2_UNKNOWN_BASE = OTF2_UNDEFINED_UINT8;

    /**
     * Constant representing an unknown exponent.
     */
    long OTF2_UNKNOWN_EXPONENT = OTF2_UNDEFINED_INT64;

    /**
     * Constant representing an unknown metric class reference.
     */
    long OTF2_UNKNOWN_METRIC_CLASS = OTF2_UNDEFINED_UINT32;

    /**
     * Value of the "base" field for a binary metric
     */
    int BINARY_BASE_CODE = 0;

    /**
     * Value of the "base" field for a decimal metric
     */
    int DECIMAL_BASE_CODE = 1;

    /**
     * Mask to get the mode of a metric
     */
    int METRIC_MODE_MASK = ((1 << 4) - 1);

    /**
     * OTF2_METRIC_ABSOLUTE_POINT code
     */
    int OTF2_METRIC_ABSOLUTE_POINT = 4;

    /**
     * OTF2_METRIC_ABSOLUTE_NEXT code
     */
    int OTF2_METRIC_ABSOLUTE_NEXT = 6;

    /**
     * OTF2_TYPE_INT64 value
     */
    int OTF2_TYPE_INT64 = 4;

    /**
     * OTF2_TYPE_UINT64 value
     */
    int OTF2_TYPE_UINT64 = 8;

    /**
     * OTF2_TYPE_DOUBLE value
     */
    int OTF2_TYPE_DOUBLE = 10;
}
