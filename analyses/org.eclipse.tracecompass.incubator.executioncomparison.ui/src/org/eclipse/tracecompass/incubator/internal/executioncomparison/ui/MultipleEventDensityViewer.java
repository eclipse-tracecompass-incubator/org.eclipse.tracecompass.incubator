/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.executioncomparison.core.TmfCheckboxChangedSignal;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.eventdensity.EventDensityViewer;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

/**
 * MultipleEventDensityViewer extends EventDensityViewer to override
 * handleCheckStateChangedEvent in order to reflect changes to the differential
 * flame graph
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 *
 */
@SuppressWarnings("restriction")
public class MultipleEventDensityViewer extends EventDensityViewer {
    List<ITmfTreeViewerEntry> fWholeTraceList = new ArrayList<>();

    /**
     * Constructor
     *
     * @param parent
     *            Composite
     * @param settings
     *            ChartSetting
     */
    public MultipleEventDensityViewer(Composite parent, TmfXYChartSettings settings) {
        super(parent, settings);
    }

    /*
     * We override this method to avoid raising a signal for the second group of
     * the MultipleEventDensityViewer, when a signal is raised from the first
     * group.
     */
    @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(@Nullable TmfSelectionRangeUpdatedSignal signal) {
        if (signal == null) {
            return;
        }

        /*
         * Signal sent when changing the text boxes or changing the query from
         * the ExecutionComparisonView. The trace context gets updated.
         */
        if (!(signal.getSource() instanceof MultipleEventDensityViewer)) {
            final ITmfTrace trace = getTrace();
            if (trace != null) {
                ITmfTimestamp selectedTime = signal.getBeginTime();
                ITmfTimestamp selectedEndTime = signal.getEndTime();
                TmfTraceManager.getInstance().updateTraceContext(trace,
                        builder -> builder.setSelection(new TmfTimeRange(selectedTime, selectedEndTime)));

            }
            super.selectionRangeUpdated(signal);
        }
        /*
         * Signal being updated is itself, we set the selection range for the
         * MultipleEventDensityViewer.
         */
        if (signal.getSource() == this) {
            final ITmfTrace trace = getTrace();
            if (trace != null) {
                long selectedTime = signal.getBeginTime().toNanos();
                long selectedEndTime = signal.getEndTime().toNanos();
                setSelectionRange(selectedTime, selectedEndTime);
            }
        }
    }

    /**
     * Handles check state of tree viewer and keeps the list of whole trace
     * list. Dispatches TmfCheckboxChangedSignal signal to update the
     * differential flame graph
     *
     * @param entries
     *            list of entries that should be checked
     */

    @Override
    public void handleCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        super.handleCheckStateChangedEvent(entries);
        updateTraceList(entries);
        List<String> traceNames = new ArrayList<>();
        for (ITmfTreeViewerEntry entry : entries) {
            if (entry instanceof TmfGenericTreeEntry) {
                TmfGenericTreeEntry<?> genericEntry = (TmfGenericTreeEntry<?>) entry;
                if (genericEntry.getModel() instanceof TmfTreeDataModel) {
                    TmfTreeDataModel model = (TmfTreeDataModel) genericEntry.getModel();
                    if (model != null) {
                        String name = model.getName();
                        traceNames.add(name);
                    }
                }
            }
        }
        TmfSignalManager.dispatchSignal(new TmfCheckboxChangedSignal(this, traceNames));
    }

    /*
     * Keeps fWholeTraceList updated to include all entries for experiment. it
     * will be used when the checkedboxtree is reset.
     */
    private void updateTraceList(Collection<ITmfTreeViewerEntry> entries) {
        for (ITmfTreeViewerEntry entry : entries) {
            if (!fWholeTraceList.contains(entry)) {
                fWholeTraceList.add(entry);
            }
        }

    }

    /**
     * Get WholeCheckedItems which is the checked item in the tree viewer
     *
     * @return fWholeTraceList list of checked Items in tree viewer
     */
    public List<ITmfTreeViewerEntry> getWholeCheckedItems() {
        return fWholeTraceList;
    }

    /**
     * Handles check state of the treeviewer, used in updating tree viewers with
     * query updating
     *
     * @param entries
     *            list of entries that should be checked
     */
    public void updateCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        super.handleCheckStateChangedEvent(entries);
    }

}
