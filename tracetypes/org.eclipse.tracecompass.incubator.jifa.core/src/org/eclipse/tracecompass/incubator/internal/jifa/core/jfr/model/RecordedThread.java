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

import java.lang.reflect.Field;

import org.eclipse.tracecompass.internal.analysis.counters.core.Activator;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.unit.IQuantity;

public class RecordedThread {
    private long javaThreadId;
    /**
     * @return the javaName
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * @param javaName the javaName to set
     */
    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    /**
     * @return the javaThreadId
     */
    public long getJavaThreadId() {
        return javaThreadId;
    }

    /**
     * @param osThreadId the osThreadId to set
     */
    public void setOsThreadId(long osThreadId) {
        this.osThreadId = osThreadId;
    }

    private String javaName;
    private long osThreadId;

    public RecordedThread(String javaName, long javaThreadId, long osThreadId) {
        this.javaName = javaName;
        this.javaThreadId = javaThreadId;
        this.osThreadId = osThreadId;
    }

    public RecordedThread(IMCThread imcThread) {
        this.javaThreadId = imcThread.getThreadId();
        this.javaName = imcThread.getThreadName();
        try {
            Field f = imcThread.getClass().getDeclaredField("osThreadId");
            f.setAccessible(true);
            Object value = f.get(imcThread);
            if (value instanceof IQuantity) {
                this.osThreadId = ((IQuantity) value).longValue();
            }
        } catch (Exception e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        if (this.javaThreadId == 0 && this.osThreadId > 0) {
            this.javaThreadId = -this.osThreadId;
        }
    }

    public long getOSThreadId() {
        return osThreadId;
    }
}
