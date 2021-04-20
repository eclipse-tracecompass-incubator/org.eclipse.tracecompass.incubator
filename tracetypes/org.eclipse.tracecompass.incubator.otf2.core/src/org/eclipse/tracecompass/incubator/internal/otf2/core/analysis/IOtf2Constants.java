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
     * String definition name
     */
    String OTF2_STRING = "String"; //$NON-NLS-1$

    /**
     * Region definition name
     */
    String OTF2_REGION = "Region"; //$NON-NLS-1$

    /**
     * LocationGroup definition name
     */
    String OTF2_LOCATION_GROUP = "LocationGroup"; //$NON-NLS-1$

    /**
     * Location definition name
     */
    String OTF2_LOCATION = "Location"; //$NON-NLS-1$

    /**
     * System tree node definition name
     */
    String OTF2_SYSTEM_TREE_NODE = "SystemTreeNode"; //$NON-NLS-1$

    /**
     * Communicator definition name
     */
    String OTF2_COMM = "Comm"; //$NON-NLS-1$

    /**
     * Group definition name
     */
    String OTF2_GROUP = "Group"; //$NON-NLS-1$

    /**
     * Group member name
     */
    String OTF2_GROUP_MEMBER = "GroupMember"; //$NON-NLS-1$

    /**
     * Enter event name
     */
    String OTF2_ENTER = "Enter"; //$NON-NLS-1$

    /**
     * Leave event name
     */
    String OTF2_LEAVE = "Leave"; //$NON-NLS-1$

    /**
     * MPI Send event name
     */
    String OTF2_MPI_SEND = "MpiSend"; //$NON-NLS-1$

    /**
     * MPI Isend event name
     */
    String OTF2_MPI_ISEND = "MpiIsend"; //$NON-NLS-1$

    /**
     * MPI Recv event name
     */
    String OTF2_MPI_RECV = "MpiRecv"; //$NON-NLS-1$

    /**
     * MPI Irecv event name
     */
    String OTF2_MPI_IRECV = "MpiIrecv"; //$NON-NLS-1$

    /**
     * MPI CollectiveBegin event name
     */
    String OTF2_MPI_COLLECTIVE_BEGIN = "MpiCollectiveBegin"; //$NON-NLS-1$

    /**
     * MPI CollectiveEnd event name
     */
    String OTF2_MPI_COLLECTIVE_END = "MpiCollectiveEnd"; //$NON-NLS-1$

    /**
     * String reference field name
     */
    String OTF2_STRING_REFERENCE = "stringRef"; //$NON-NLS-1$

    /**
     * String value field name
     */
    String OTF2_STRING_VALUE = "stringValue"; //$NON-NLS-1$

    /**
     * Region reference field name
     */
    String OTF2_REGION_REFERENCE = "regionRef"; //$NON-NLS-1$

    /**
     * Location reference field name
     */
    String OTF2_LOCATION_REFERENCE = "locationRef"; //$NON-NLS-1$

    /**
     * Location group reference field name
     */
    String OTF2_LOCATION_GROUP_REFERENCE = "locationGroupRef"; //$NON-NLS-1$

    /**
     * System tree node reference field name
     */
    String OTF2_SYSTEM_TREE_NODE_REFERENCE = "systemTreeNodeRef"; //$NON-NLS-1$

    /**
     * Group reference field name
     */
    String OTF2_GROUP_REFERENCE = "groupRef"; //$NON-NLS-1$

    /**
     * Number of members field name
     */
    String OTF2_NUMBER_OF_MEMBERS = "numberOfMembers"; //$NON-NLS-1$

    /**
     * Rank field name
     */
    String OTF2_RANK = "rank"; //$NON-NLS-1$

    /**
     * Members field name
     */
    String OTF2_MEMBERS = "members"; //$NON-NLS-1$

    /**
     * Communicator reference field name
     */
    String OTF2_COMMUNICATOR_REFERENCE = "commRef"; //$NON-NLS-1$

    /**
     * Name field name
     */
    String OTF2_NAME = "name"; //$NON-NLS-1$

    /**
     * Communicator field name
     */
    String OTF2_COMMUNICATOR = "communicator"; //$NON-NLS-1$

    /**
     * Sender field name
     */
    String OTF2_SENDER = "sender"; //$NON-NLS-1$

    /**
     * Receiver field name
     */
    String OTF2_RECEIVER = "receiver"; //$NON-NLS-1$

    /**
     * Message tag field name
     */
    String OTF2_MESSAGE_TAG = "msgTag"; //$NON-NLS-1$

    /**
     * Request ID field name
     */
    String OTF2_REQUEST_ID = "requestID"; //$NON-NLS-1$

    /**
     * Collective operation code field name
     */
    String OTF2_COLLECTIVE_OPERATION = "collectiveOp"; //$NON-NLS-1$

    /**
     * Root field name
     */
    String OTF2_ROOT = "root"; //$NON-NLS-1$

    /**
     * Enum for collective MPI operations
     */
    public enum CollectiveOperation {
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
    public static CollectiveOperation getOperation(int operationCode) {
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
}