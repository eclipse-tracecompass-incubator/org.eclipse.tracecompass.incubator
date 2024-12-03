/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Implementation of the ServerInfoResponse interface
 *
 * @author Vlad Arama
 */
public class ServerInfoResponseImpl {
    private String version;
    @JsonInclude(JsonInclude.Include.NON_NULL) // Makes build time optional
    private String buildTime;
    private String os;
    private String osArch;
    private String osVersion;
    private int cpuCount;
    private long maxMemory;
    @JsonInclude(JsonInclude.Include.NON_NULL) // Makes launcher name optional
    private String launcherName;
    private String productId;
    private String tspVersion;
    private String jvmVersion;

    /**
     * @return Version in the format Major.Minor.Micro
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return Build time or qualifier of the server version, if available
     */
    public String getBuildTime() {
        return buildTime;
    }

    /**
     * @return Operating system name
     */
    public String getOs() {
        return os;
    }

    /**
     * @return Architecture of the operating system
     */
    public String getOsArch() {
        return osArch;
    }

    /**
     * @return Operating system version
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * @return Number of CPUs available
     */
    public int getCpuCount() {
        return cpuCount;
    }

    /**
     * @return Maximum memory available to the JVM in bytes
     */
    public long getMaxMemory() {
        return maxMemory;
    }

    /**
     * @return Name of the launcher used, if available
     */
    public String getLauncherName() {
        return launcherName;
    }

    /**
     * @return Product identifier for the software
     */
    public String getProductId() {
        return productId;
    }

    /**
     * @return Version in the format Major.Minor.Micro
     */
    public String getTspVersion() {
        return tspVersion;
    }

    /**
     * Set Version
     *
     * @param version
     *            Version of the server
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Set Build Time
     *
     * @param buildTime
     *            Time of build
     */
    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    /**
     * Set OS
     *
     * @param os
     *            Operating System
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * Set OS Architecture
     *
     * @param osArch
     *            Architecture of the OS
     */
    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    /**
     * Set OS Version
     *
     * @param osVersion
     *            Version of the OS
     */
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * Set CPU Count
     *
     * @param cpuCount
     *            Number of CPUs
     */
    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    /**
     * Set Max Memory
     *
     * @param maxMemory
     *            Maximum memory available
     */
    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    /**
     * Set Launcher Name
     *
     * @param launcherName
     *            Name of the launcher
     */
    public void setLauncherName(String launcherName) {
        this.launcherName = launcherName;
    }

    /**
     * Set Product ID
     *
     * @param productId
     *            Product ID
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Set the TSP version
     *
     * @param version
     *            the TSP version to set
     */
    public void setTspVersion(String version) {
        tspVersion = version;
    }

    /**
     * Set JVM Version
     *
     * @param jvmVersion jvm version
     */
    public void setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    /**
     * @return the jvm Version
     */
    public String getJvmVersion() {
        return jvmVersion;
    }

}
