package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;

import org.eclipse.swt.widgets.Composite;
//import org.eclipse.tracecompass.internal.tmf.ui.viewers.eventdensity.EventDensityTreeViewer;
//import org.eclipse.tracecompass.internal.tmf.ui.viewers.eventdensity.EventDensityViewer;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

/**
 * Histogram View based on TmfChartView.
 */
public class EventDensityView extends TmfChartView {
    /** The view ID. */
    public static final String ID = "org.eclipse.overhead.incubator.ui.views.eventdensity"; //$NON-NLS-1$
    private static final String TITLE = "KVM Exit Density"; //$NON-NLS-1$
    private static final String X_AXIS_TITLE = "Time"; //$NON-NLS-1$
    private static final String Y_AXIS_TITLE = "Frequency"; //$NON-NLS-1$

    /**
     * Default Constructor
     */
    public EventDensityView() {
        super(ID);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        return new EventDensityViewer(parent, new TmfXYChartSettings(TITLE,
                X_AXIS_TITLE,
                Y_AXIS_TITLE, 1));
    }

    @Override
    public TmfViewer createLeftChildViewer(Composite parent) {
        EventDensityTreeViewer histogramTreeViewer = new EventDensityTreeViewer(parent);
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            histogramTreeViewer.traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
        return histogramTreeViewer;
    }
}