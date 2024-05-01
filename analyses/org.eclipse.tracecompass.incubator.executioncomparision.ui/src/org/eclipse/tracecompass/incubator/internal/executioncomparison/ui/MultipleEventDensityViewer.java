/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
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
 * MultipleEventDensityViewer extends EventDensityViewer to override handleCheckStateChangedEvent and reflect
 * changes to differential flame graph
 *  @author Fateme Faraji Daneshgar

 */
@SuppressWarnings("restriction")
public class MultipleEventDensityViewer extends EventDensityViewer {
    List<ITmfTreeViewerEntry> fWholeTraceList = new ArrayList<>();

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
    public void selectionRangeUpdated(@Nullable TmfSelectionRangeUpdatedSignal signal) {
       if (signal == null) {
            return;
        }

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
       if (signal.getSource()==this) {
           final ITmfTrace trace = getTrace();
           if (trace != null) {
               long selectedTime = signal.getBeginTime().toNanos();
               long selectedEndTime = signal.getEndTime().toNanos();
               setSelectionRange(selectedTime, selectedEndTime);
           }
          }
    }
   /**
    * handles check state of tree viewer and keeps the list of whole trace list
    * and dispatch TmfCheckboxChangedSignal signal to update the differential flame graph
    * @param entries
    * list of entries that should be checked
    */

    @SuppressWarnings("unchecked")
    @Override
    public void handleCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        super.handleCheckStateChangedEvent(entries);
        updateTraceList(entries);
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
    /*
     * Keeps fWholeTraceList updated to include all entries for experiment. it will be used when
     * checkedboxtree is reset.
     */
    private void updateTraceList(Collection<ITmfTreeViewerEntry> entries) {
        for(ITmfTreeViewerEntry entry:entries) {
            if (!fWholeTraceList.contains(entry)) {
                fWholeTraceList.add(entry);
            }
        }

    }


    /**
     * get WholeCheckedItems which is the checked item in the tree viewer
     * @return
     * fWholeTraceList list of checked Items in tree viewer
     */
    public List<ITmfTreeViewerEntry> getWholeCheckedItems(){
        return fWholeTraceList;
    }

    /**
     * just handles check state of the treeviewer, used in updating tree viewers with query updating
     * @param entries
     * list of entries that should be checked
     */
    public void UpdateCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        super.handleCheckStateChangedEvent(entries);
    }

}