/**********************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Response that includes the status and a status message")
public interface GenericResponse {

    /**
     * The statuses.
     */
    @Schema(description = "All possible statuses for a server response")
    enum Status {
        RUNNING, COMPLETED, FAILED, CANCELLED
    }

    /**
     * @return The status.
     */
    Status getStatus();

    /**
     * @return The status message.
     */
    String getStatusMessage();
}
