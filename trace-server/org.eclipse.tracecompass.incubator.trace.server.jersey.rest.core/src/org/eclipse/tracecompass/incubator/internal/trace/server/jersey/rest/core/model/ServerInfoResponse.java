/**********************************************************************
 * Copyright (c) 2024 Ericsson
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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 *
 * @author Vlad Arama
 */
@Schema(description = "System Information Response")
public interface ServerInfoResponse {

    /**
     * @return Version in the format Major.Minor.Micro
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Version in the format Major.Minor.Micro")
    String getVersion();

    /**
     * @return Build time or qualifier of the server version, if available
     */
    @Schema(description = "Build time or qualifier of the server version, if available")
    String getBuildTime();

    /**
     * @return Operating system name
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Operating system name")
    String getOs();

    /**
     * @return Architecture of the operating system
     */
    @Schema(description = "Architecture of the operating system")
    String getOsArch();

    /**
     * @return Operating system version
     */
    @Schema(description = "Operating system version")
    String getOsVersion();

    /**
     * @return Number of CPUs available
     */
    @Schema(description = "Number of CPUs available")
    int getCpuCount();

    /**
     * @return Maximum memory available to the JVM in bytes
     */
    @Schema(description = "Maximum memory available to the JVM in bytes")
    long getMaxMemory();

    /**
     * @return Name of the launcher used, if available
     */
    @Schema(description = "Name of the launcher used, if available")
    String getLauncherName();

    /**
     * @return Product identifier for the trace server
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Product identifier for the trace server")
    String getProductId();

    /**
     * @return supported TSP version
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The TSP version that the trace server supports")
    String getTspVersion();

}
