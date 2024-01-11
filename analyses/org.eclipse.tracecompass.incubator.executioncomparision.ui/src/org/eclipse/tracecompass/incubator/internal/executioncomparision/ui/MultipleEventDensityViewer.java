/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.executioncomparision.core.TmfCheckboxChangedSignal;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.eventdensity.EventDensityViewer;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

/**
 *
 */
public class MultipleEventDensityViewer extends EventDensityViewer {

    /**
     * @param parent
     *              Composite
     * @param settings
     *               ChartSetting
     */
    public MultipleEventDensityViewer(Composite parent, TmfXYChartSettings settings) {
        super(parent, settings);
    }

   @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(TmfSelectionRangeUpdatedSignal signal) {
        if (signal == null) {
            return;
        }

        final ITmfTrace trace = getTrace();
        if (trace != null) {
            TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(trace);
            long selectedTime = ctx.getSelectionRange().getStartTime().toNanos();
            long selectedEndTime = ctx.getSelectionRange().getEndTime().toNanos();
            setSelectionRange(selectedTime, selectedEndTime);
        }

    }

    @Override
    public void handleCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        super.handleCheckStateChangedEvent(entries);
            List<String> traceNames = new ArrayList<>();
            for (ITmfTreeViewerEntry entry : entries) {
                if (entry instanceof TmfGenericTreeEntry) {
                    TmfGenericTreeEntry<TmfTreeDataModel> genericEntry = (TmfGenericTreeEntry<TmfTreeDataModel>) entry;
                    String name = genericEntry.getModel().getName();
                    traceNames.add(name);
                }
            }
            TmfSignalManager.dispatchSignal(new TmfCheckboxChangedSignal(this, traceNames));
        }



}
