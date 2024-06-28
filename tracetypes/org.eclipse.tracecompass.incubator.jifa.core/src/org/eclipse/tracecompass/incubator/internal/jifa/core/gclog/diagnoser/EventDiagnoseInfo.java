/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser;

public class EventDiagnoseInfo {
    private EventAbnormalSet abnormals = new EventAbnormalSet();

    public EventDiagnoseInfo() {
    }

    /**
     * @return the abnormals
     */
    public EventAbnormalSet getAbnormals() {
        return abnormals;
    }

    /**
     * @param abnormals
     *            the abnormals to set
     */
    public void setAbnormals(EventAbnormalSet abnormals) {
        this.abnormals = abnormals;
    }
}
