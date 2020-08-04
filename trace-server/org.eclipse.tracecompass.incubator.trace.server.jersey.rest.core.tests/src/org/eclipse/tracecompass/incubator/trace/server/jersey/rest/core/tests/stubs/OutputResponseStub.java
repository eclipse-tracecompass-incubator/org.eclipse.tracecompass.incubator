/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableList;

/**
 * A Stub class for the response. It matches the trace server protocol's
 * <code>GenericResponse</code> schema
 *
 * @author Geneviève Bastien
 */
public abstract class OutputResponseStub implements Serializable {

    private static final long serialVersionUID = -3731497188853515695L;
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CAMCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final List<String> STATUSES = ImmutableList.of(STATUS_FAILED, STATUS_CANCELLED, STATUS_COMPLETED, STATUS_RUNNING);

    private final String fStatus;
    private final String fStatusMessage;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param status
     *            The status of this response: a choice of status
     * @param statusMessage
     *            The custom status message of the response
     */
    @JsonCreator
    public OutputResponseStub(String status, String statusMessage) {
        fStatus = status;
        fStatusMessage = statusMessage;
        assertTrue("Status: '" + status + "'", STATUSES.contains(status));
    }

    /**
     * Get whether this request is still running
     *
     * @return <code>true</code> if the request is running
     */
    public boolean isRunning() {
        return fStatus.equals(STATUS_RUNNING);
    }

    /**
     * Get whether this request is completed
     *
     * @return <code>true</code> if the request is completed
     */
    public boolean isCompleted() {
        return fStatus.equals(STATUS_COMPLETED);
    }

    /**
     * Get whether this request is failed
     *
     * @return <code>true</code> if the request is failed
     */
    public boolean isFailed() {
        return fStatus.equals(STATUS_FAILED);
    }

    /**
     * Get whether this request is cancelled
     *
     * @return <code>true</code> if the request is cancelled
     */
    public boolean isCancelled() {
        return fStatus.equals(STATUS_CANCELLED);
    }

    /**
     * Get the custom status message from this response
     *
     * @return The custom status message
     */
    public String getStatusMessage() {
        return fStatusMessage;
    }

    @Override
    public String toString() {
        return "Status: " + fStatus;
    }

}
