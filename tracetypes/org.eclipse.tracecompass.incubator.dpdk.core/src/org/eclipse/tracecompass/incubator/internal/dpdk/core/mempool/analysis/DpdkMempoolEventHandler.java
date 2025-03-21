/**********************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis;

import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.IDpdkEventHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Event handler to process the DPDK events required for the
 * {@link DpdkMempoolAnalysisModule} analysis
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolEventHandler implements IDpdkEventHandler {

    DpdkMempoolEventHandler() {
        // do nothing
    }

    private static void updateThreadCount(ITmfStateSystemBuilder ssb, Long mempoolId, String threadName, String metricLabel, int nbObjs, long ts) {
        int mempoolQuark = ssb.optQuarkAbsolute(DpdkMempoolAttributes.MEMPOOLS, mempoolId.toString());

        if (mempoolQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
            int threadsQuark = ITmfStateSystem.INVALID_ATTRIBUTE;
            try {
                threadsQuark = ssb.getQuarkRelativeAndAdd(mempoolQuark, DpdkMempoolAttributes.THREADS);
            } catch (IndexOutOfBoundsException e) {
                Activator.getInstance().logError(e.getMessage());
            }

            if (threadsQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                int threadQuark = ssb.getQuarkRelativeAndAdd(threadsQuark, threadName);
                int metricQuark = ssb.getQuarkRelativeAndAdd(threadQuark, metricLabel);
                StateSystemBuilderUtils.incrementAttributeLong(ssb, ts, metricQuark, nbObjs);
            }
        }
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        String eventName = event.getName();
        long ts = event.getTimestamp().getValue();
        Long mempoolId = event.getContent().getFieldValue(Long.class, DpdkMempoolEventLayout.fieldMempoolId());
        Objects.requireNonNull(mempoolId);

        if (eventName.equals(DpdkMempoolEventLayout.eventMempoolCreate()) ||
                eventName.equals(DpdkMempoolEventLayout.eventMempoolCreateEmpty())) {
            String mempoolName = event.getContent().getFieldValue(String.class, DpdkMempoolEventLayout.fieldMempoolName());
            if (mempoolName != null) {
                int mempoolQuark = ssb.getQuarkAbsoluteAndAdd(DpdkMempoolAttributes.MEMPOOLS, String.valueOf(mempoolId));
                int nameQuark = ssb.getQuarkRelativeAndAdd(mempoolQuark, DpdkMempoolAttributes.MEMPOOL_NAME);
                ssb.modifyAttribute(ts, mempoolName, nameQuark);
            }
        } else if (eventName.equals(DpdkMempoolEventLayout.eventMempoolFree())) {
            int mempoolQuark = ssb.optQuarkAbsolute(DpdkMempoolAttributes.MEMPOOLS, mempoolId.toString());
            if (mempoolQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                for (int quark : ssb.getSubAttributes(mempoolQuark, true)) {
                    ssb.modifyAttribute(ts, null, quark);
                }
            }
        } else if (eventName.equals(DpdkMempoolEventLayout.eventMempoolGenericPut())) {
            String threadName = event.getContent().getFieldValue(String.class, DpdkMempoolEventLayout.fieldThreadName());
            Integer nbObjs = event.getContent().getFieldValue(Integer.class, DpdkMempoolEventLayout.fieldMempoolNbObjs());

            if (threadName != null && nbObjs != null) {
                updateThreadCount(ssb, mempoolId, threadName, DpdkMempoolAttributes.THREAD_OBJ_FREE, nbObjs, ts);
            }
        } else if (eventName.equals(DpdkMempoolEventLayout.eventMempoolGenericGet())) {
            String threadName = event.getContent().getFieldValue(String.class, DpdkMempoolEventLayout.fieldThreadName());
            Integer nbObjs = event.getContent().getFieldValue(Integer.class, DpdkMempoolEventLayout.fieldMempoolNbObjs());

            if (threadName != null && nbObjs != null) {
                updateThreadCount(ssb, mempoolId, threadName, DpdkMempoolAttributes.THREAD_OBJ_ALLOC, nbObjs, ts);
            }
        } else {
            Activator.getInstance().logError(eventName + " is an unexpected event!"); //$NON-NLS-1$
        }
    }
}
