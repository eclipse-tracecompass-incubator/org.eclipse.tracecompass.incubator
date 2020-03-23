/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.messageflow;

import java.util.Collections;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.RosMessageFlowAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.RosMessageFlowDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.RosMessageFlowSegmentEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment.SegmentType;
import org.eclipse.tracecompass.incubator.internal.ros.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosView;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for {@link RosMessageFlowAnalysis}
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowView extends AbstractRosView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "messageflow"; //$NON-NLS-1$

    private static final String[] TREE_COLUMNS = new String[] { "Node", "Type", "Topic", "Segment type" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /**
     * Constructor
     */
    public RosMessageFlowView() {
        super(ID_SUFFIX, new RosMessageFlowPresentationProvider(), RosMessageFlowDataProvider.getFullDataProviderId());
        setTreeColumns(TREE_COLUMNS);
        setTreeLabelProvider(new RosMessageFlowTreeLabelProvider());
        // Enable view filter dialog
        setFilterColumns(new String[] { StringUtils.EMPTY });
        setFilterLabelProvider(new RosViewFilterLabelProvider());
    }

    private static class RosMessageFlowTreeLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            TimeGraphEntry entry = (TimeGraphEntry) element;
            ITmfTreeDataModel model = entry.getEntryModel();
            if (model instanceof RosMessageFlowSegmentEntryModel) {
                RosMessageFlowSegmentEntryModel segmentModel = (RosMessageFlowSegmentEntryModel) model;
                RosMessageFlowSegment segment = segmentModel.getSegment();
                if (columnIndex >= 0 && columnIndex < TREE_COLUMNS.length) {
                    if (columnIndex == 0) {
                        return segment.getNodeName();
                    } else if (columnIndex == 1) {
                        return getQueueTypeName(segment.getType());
                    } else if (columnIndex == 2) {
                        return segment.getTopicName();
                    } else if (columnIndex == 3) {
                        return segment.getType().name();
                    }
                }
            } else if (columnIndex == 0) {
                // Experiment name
                return entry.getName();
            }

            return StringUtils.EMPTY;
        }
    }

    private static @Nullable String getQueueTypeName(SegmentType type) {
        switch (type) {
        case SUB_QUEUE: // Fallthrough
        case SUB_CALLBACK:
            return AbstractRosStateProvider.SUBSCRIBERS_LIST;
        case PUB_QUEUE:
            return AbstractRosStateProvider.PUBLISHERS_LIST;
        case INVALID:
        default:
            return null;
        }
    }

    private static class RosViewFilterLabelProvider extends TreeLabelProvider {
        // Empty; only used to enable view filter dialog
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        IDialogSettings settings = Objects.requireNonNull(Activator.getDefault()).getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideArrowsAction);

        // add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
    }

    /**
     * Signal handler for analysis started, we need to rebuilt the entry list
     * with updated statistics values for the current graph worker of the
     * critical path module.
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void analysisStarted(TmfStartAnalysisSignal signal) {
        IAnalysisModule analysis = signal.getAnalysisModule();
        if (analysis instanceof RosMessageFlowAnalysis) {
            rebuild();
        }
    }

    @Override
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        // The view only applies to the ROS experiment itself
        return (trace != null) ? Collections.singleton(trace) : Collections.emptyList();
    }

    /**
     * @return the full view ID
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
