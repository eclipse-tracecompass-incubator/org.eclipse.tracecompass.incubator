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

public class TaskAllocatedMemory extends TaskSum {
    public TaskAllocatedMemory() {
        super(null);
    }

    private long allocatedMemory;

    /**
     * @return the allocatedMemory
     */
    public long getAllocatedMemory() {
        return allocatedMemory;
    }

    /**
     * @param allocatedMemory the allocatedMemory to set
     */
    public void setAllocatedMemory(long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }
}
