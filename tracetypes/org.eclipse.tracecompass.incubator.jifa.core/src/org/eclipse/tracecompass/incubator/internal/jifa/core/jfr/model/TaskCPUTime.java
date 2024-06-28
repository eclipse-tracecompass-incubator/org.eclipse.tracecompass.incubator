/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

public class TaskCPUTime extends TaskResultBase {

    private long user;

    private long system;

    public TaskCPUTime() {
    }

    public long totalCPUTime() {
        return user + system;
    }

    public TaskCPUTime(Task task) {
        super(task);
    }

    /**
     * @return the system
     */
    public long getSystem() {
        return system;
    }

    /**
     * @param system the system to set
     */
    public void setSystem(long system) {
        this.system = system;
    }

    /**
     * @return the user
     */
    public long getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(long user) {
        this.user = user;
    }
}
