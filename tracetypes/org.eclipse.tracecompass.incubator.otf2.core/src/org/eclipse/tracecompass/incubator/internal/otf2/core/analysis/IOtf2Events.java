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

/**
 * OTF2 events name
 *
 * @author Yoann Heitz
 */
public interface IOtf2Events {
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
}