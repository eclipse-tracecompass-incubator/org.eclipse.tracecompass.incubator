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
import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.LogicalCore.ServiceStatus;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to handle service related events
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkServiceEventHandler implements IDpdkEventHandler {

    private DpdkLogicalCoreEventLayout fLayout;

    DpdkServiceEventHandler(DpdkLogicalCoreEventLayout layout) {
        fLayout = layout;
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        long ts = event.getTimestamp().getValue();
        Integer serviceId = event.getContent().getFieldValue(Integer.class, fLayout.fieldId());
        String eventName = event.getName();
        if (serviceId == null) {
            return;
        }
        if (eventName.equals(fLayout.eventServiceComponentRegister())) {
            String serviceName = IDpdkEventHandler.getStringFieldValue(event, fLayout.fieldServiceName());
            LogicalCore.setServiceName(ssb, Objects.requireNonNull(serviceName), serviceId, ts);
            LogicalCore.setServiceStatus(ssb, ServiceStatus.REGISTERED, serviceId, ts);
        } else if (eventName.equals(fLayout.eventServiceMapLcore())) {
            Integer lcoreId = event.getContent().getFieldValue(Integer.class, fLayout.fieldLcoreId());
            if (lcoreId != null) {
                LogicalCore.setServiceLcore(ssb, lcoreId, serviceId, ts);
            }
            Integer enabled = event.getContent().getFieldValue(Integer.class, fLayout.fieldEnabled());
            if (enabled != null && enabled == 0) {
                LogicalCore.setServiceStatus(ssb, ServiceStatus.DISABLED, serviceId, ts);
            } else {
                LogicalCore.setServiceStatus(ssb, ServiceStatus.ENABLED, serviceId, ts);
            }
        } else if (eventName.equals(fLayout.eventServiceRunBegin())) {
            Integer lcoreId = event.getContent().getFieldValue(Integer.class, fLayout.fieldLcoreId());
            if (lcoreId != null) {
                LogicalCore.setServiceLcore(ssb, lcoreId, serviceId, ts);
            }
            LogicalCore.setServiceStatus(ssb, ServiceStatus.RUNNING, serviceId, ts);
        } else if (eventName.equals(fLayout.eventServiceRunEnd())) {
            Integer lcoreId = event.getContent().getFieldValue(Integer.class, fLayout.fieldLcoreId());
            if (lcoreId != null) {
                LogicalCore.setServiceLcore(ssb, lcoreId, serviceId, ts);
            }
            LogicalCore.setServiceStatus(ssb, ServiceStatus.PENDING, serviceId, ts);
        } else if (eventName.equals(fLayout.eventServiceRunStateSet())) {
            Integer runState = event.getContent().getFieldValue(Integer.class, fLayout.fieldRunState());
            LogicalCore.setServiceStatus(ssb, runState != null && runState == 1 ? ServiceStatus.ENABLED : ServiceStatus.DISABLED, serviceId, ts);
        }
    }
}
