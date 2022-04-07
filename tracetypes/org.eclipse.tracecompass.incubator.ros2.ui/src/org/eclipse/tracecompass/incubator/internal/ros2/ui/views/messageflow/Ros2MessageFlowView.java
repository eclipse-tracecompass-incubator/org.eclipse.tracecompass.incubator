/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.messageflow;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow.Ros2MessageFlowAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow.Ros2MessageFlowDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.AbstractRos2DataProviderTimeGraphView;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2HideInternalObjectsAction;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2ObjectTreeLabelProvider;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for ROS 2 message flow.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowView extends AbstractRos2DataProviderTimeGraphView {

    private static final String ID_SUFFIX = ".messageflow"; //$NON-NLS-1$

    private class Ros2MessageFlowTreeLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            return Ros2ObjectTreeLabelProvider.getColumnText(element, columnIndex);
        }
    }

    /**
     * Constructor
     */
    public Ros2MessageFlowView() {
        super(getFullViewId(), new Ros2MessageFlowPresentationProvider(), Ros2MessageFlowDataProvider.getFullDataProviderId());
        setTreeColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setTreeLabelProvider(new Ros2MessageFlowTreeLabelProvider());
        setFilterColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setFilterLabelProvider(new Ros2MessageFlowTreeLabelProvider());
    }

    /**
     * @return the full ID of this view
     */
    public static @NonNull String getFullViewId() {
        return AbstractRos2DataProviderTimeGraphView.getViewIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        IDialogSettings settings = Objects.requireNonNull(Activator.getDefault()).getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        // Hide arrows button
        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideArrowsAction);

        // Hide internal objects
        IAction hideInternalAction = new Ros2HideInternalObjectsAction(getTimeGraphViewer());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideInternalAction);

        // Add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
    }

    /**
     * Signal handler for analysis start. Every time the message flow analysis
     * is triggered, we need to rebuild the entry list once the analysis is
     * complete.
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void analysisStarted(TmfStartAnalysisSignal signal) {
        IAnalysisModule analysis = signal.getAnalysisModule();
        if (analysis instanceof Ros2MessageFlowAnalysis) {
            Ros2MessageFlowAnalysis messageFlowAnalysis = (Ros2MessageFlowAnalysis) analysis;
            new Thread() {
                @Override
                public void run() {
                    Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSetWithExperiment(getTrace());
                    if (traces.contains(messageFlowAnalysis.getTrace()) && messageFlowAnalysis.waitForCompletion()) {
                        rebuild();
                    }
                }
            }.start();
        }
    }
}
