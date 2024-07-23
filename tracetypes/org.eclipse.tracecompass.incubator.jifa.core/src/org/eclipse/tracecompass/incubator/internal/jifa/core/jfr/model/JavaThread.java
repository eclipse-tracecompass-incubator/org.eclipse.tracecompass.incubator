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

public class JavaThread extends Task {
    private long javaId;
    private long osId;

    /**
     * @return the javaId
     */
    public long getJavaId() {
        return javaId;
    }

    /**
     * @return the osId
     */
    public long getOsId() {
        return osId;
    }

    /**
     * @param javaId
     *            the javaId to set
     */
    public void setJavaId(long javaId) {
        this.javaId = javaId;
    }

    /**
     * @param osId
     *            the osId to set
     */
    public void setOsId(long osId) {
        this.osId = osId;
    }
}
