/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event.eventInfo;

import static org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.util.Constant.UNKNOWN_DOUBLE;

public class CpuTime {
    // unit is ms
    private double user = UNKNOWN_DOUBLE;
    private double sys = UNKNOWN_DOUBLE;
    private double real = UNKNOWN_DOUBLE;

    @Override
    public String toString() {
        return String.format("User=%.2fs Sys=%.2fs Real=%.2fs", getUser() / 1000, getSys() / 1000, getReal() / 1000); //$NON-NLS-1$
    }

    public double getValue(String type) {
        switch (type) {
        case "USER": //$NON-NLS-1$
            return getUser();
        case "SYS": //$NON-NLS-1$
            return getSys();
        case "REAL": //$NON-NLS-1$
            return getReal();
        default:
            return 0;
        }
    }

    public double getUser() {
        return user;
    }

    public void setUser(double user) {
        this.user = user;
    }

    public double getSys() {
        return sys;
    }

    public void setSys(double sys) {
        this.sys = sys;
    }

    public double getReal() {
        return real;
    }

    public void setReal(double real) {
        this.real = real;
    }
}
