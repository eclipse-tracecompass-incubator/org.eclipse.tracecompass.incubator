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
package org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.event;

import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.diagnoser.GlobalDiagnoseInfo;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.model.GCModel;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.vo.GCEventVO;

public class ThreadEvent extends GCEvent {
    private String threadName;

    public ThreadEvent() {
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    protected void appendClassSpecificInfo(StringBuilder sb) {
        sb.append(threadName);
    }

    @Override
    protected void fillInfoToVO(GCModel model, GCEventVO vo, GlobalDiagnoseInfo diagnose) {
        super.fillInfoToVO(model, vo, diagnose);
        vo.saveInfo("threadName", threadName);
    }
}
