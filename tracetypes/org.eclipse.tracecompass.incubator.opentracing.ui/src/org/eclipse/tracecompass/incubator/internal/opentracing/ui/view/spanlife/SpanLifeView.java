/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.view.spanlife;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeDataProvider;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeEntryModel;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeEntryModel.LogEvent;
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

    /**
     * Constructor
     */
    public SpanLifeView() {
        this(ID, new SpanLifePresentationProvider(), SpanLifeAnalysis.ID + SpanLifeDataProvider.SUFFIX);
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
            if (((TimeGraphEntry) element).getModel() instanceof SpanLifeEntryModel) {
                SpanLifeEntryModel model = (SpanLifeEntryModel) ((TimeGraphEntry) element).getModel();
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
