/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.LogicalCore.LogicalCoreRole;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.LogicalCore.LogicalCoreStatus;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to handle core related events
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkLogicalCoreEventHandler implements IDpdkEventHandler {

    private DpdkLogicalCoreEventLayout fLayout;

    DpdkLogicalCoreEventHandler(DpdkLogicalCoreEventLayout layout) {
        fLayout = layout;
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        Integer lcoreId = event.getContent().getFieldValue(Integer.class, fLayout.fieldLcoreId());
        long ts = event.getTimestamp().getValue();
        String eventName = event.getName();

        if (eventName.equals(fLayout.eventLcoreStateChange())) {
            Integer lcoreRole = event.getContent().getFieldValue(Integer.class, fLayout.fieldLcoreState());
            LogicalCore.setRole(ssb, Objects.requireNonNull(lcoreRole), Objects.requireNonNull(lcoreId), ts);
        } else if (eventName.equals(fLayout.eventServiceLcoreStart())
                || eventName.equals(fLayout.eventServiceLcoreStop())
                || eventName.equals(fLayout.eventThreadLcoreStopped())) {
            LogicalCore.setStatus(ssb, LogicalCoreStatus.IDLE, Objects.requireNonNull(lcoreId), ts);
            LogicalCore.setFunction(ssb, 0L, Objects.requireNonNull(lcoreId), ts);
        } else if (eventName.equals(fLayout.eventThreadLcoreRunning())) {
            LogicalCore.setStatus(ssb, LogicalCoreStatus.RUNNING, Objects.requireNonNull(lcoreId), ts);
            Long lcoreFunction = event.getContent().getFieldValue(Long.class, fLayout.fieldF());
            LogicalCore.setFunction(ssb, Objects.requireNonNull(lcoreFunction), lcoreId, ts);
        } else if (eventName.equals(fLayout.eventThreadLcoreReady())) {
            LogicalCore.setRole(ssb, LogicalCoreRole.ROLE_RTE, Objects.requireNonNull(lcoreId), ts);
            LogicalCore.setFunction(ssb, 0L, Objects.requireNonNull(lcoreId), ts);
        }
    }
}
