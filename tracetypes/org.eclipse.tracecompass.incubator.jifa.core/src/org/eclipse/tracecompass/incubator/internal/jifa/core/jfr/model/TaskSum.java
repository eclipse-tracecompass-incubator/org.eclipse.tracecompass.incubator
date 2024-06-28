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

public class TaskSum extends TaskResultBase {
    public TaskSum() {
        super(null);
    }

    public TaskSum(Task task) {
        super(task);
    }

    private long sum;

    /**
     * @return the sum
     */
    public long getSum() {
        return sum;
    }

    /**
     * @param sum the sum to set
     */
    public void setSum(long sum) {
        this.sum = sum;
    }
}
