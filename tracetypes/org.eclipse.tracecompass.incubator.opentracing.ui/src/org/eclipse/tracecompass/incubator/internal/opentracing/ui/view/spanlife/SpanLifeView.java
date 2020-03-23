/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeDataProvider;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeEntryModel;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeEntryModel.LogEvent;
import org.eclipse.tracecompass.incubator.internal.opentracing.ui.Activator;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Simple gantt chart to see the life of the spans
 *
 * @author Katherine Nadeau
 */
public class SpanLifeView extends BaseDataProviderTimeGraphView {

    /**
     * Span life view Id
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.opentracing.ui.view.life.spanlife.view"; //$NON-NLS-1$
    private static final RGBA MARKER_COLOR = new RGBA(200, 0, 0, 150);

    private static final Image ERROR_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/delete_button.gif"); //$NON-NLS-1$


    private static class SpanTreeLabelProvider extends TreeLabelProvider {

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel entryModel = entry.getEntryModel();
                if ((entryModel instanceof SpanLifeEntryModel) && ((SpanLifeEntryModel) entryModel).getErrorTag()) {
                    return ERROR_IMAGE;
                }
            }
            return null;
        }
    }

    /**
     * Constructor
     */
    public SpanLifeView() {
        this(ID, new SpanLifePresentationProvider(), SpanLifeAnalysis.ID + SpanLifeDataProvider.SUFFIX);
        setTreeLabelProvider(new SpanTreeLabelProvider());
    }

    /**
     * Extendable constructor
     *
     * @param id
     *            the view ID
     * @param pres
     *            the presentation provider
     * @param dpID
     *            the dataprovider ID
     */
    public SpanLifeView(String id, TimeGraphPresentationProvider pres, String dpID) {
        super(id, pres, dpID);
    }

    @Override
    protected @NonNull List<IMarkerEvent> getViewMarkerList(long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        ITimeGraphEntry[] expandedElements = getTimeGraphViewer().getExpandedElements();
        List<IMarkerEvent> markers = new ArrayList<>();
        for (ITimeGraphEntry element : expandedElements) {
            ITmfTreeDataModel entryModel = ((TimeGraphEntry) element).getEntryModel();
            if (entryModel instanceof SpanLifeEntryModel) {
                SpanLifeEntryModel model = (SpanLifeEntryModel) entryModel;
                for (LogEvent log : model.getLogs()) {
                    markers.add(new SpanMarkerEvent(element, log.getTime(), MARKER_COLOR, log.getType()));
                }
            }
        }
        return markers;
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        super.buildEntryList((parentTrace instanceof TmfExperiment) ? parentTrace : trace, parentTrace, monitor);
    }

}
