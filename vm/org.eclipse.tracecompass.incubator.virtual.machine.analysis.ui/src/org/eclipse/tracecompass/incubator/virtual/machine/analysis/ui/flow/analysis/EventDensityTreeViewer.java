package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;

import java.util.Comparator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider.KvmExitRateDataProvider;
//import org.eclipse.tracecompass.internal.tmf.core.histogram.HistogramDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.histogram.Messages;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;

import com.google.common.collect.ImmutableList;

/**
 * Tree viewer to select which trace to display in the New Histogram chart
 * chart.
 */
public class EventDensityTreeViewer extends AbstractSelectTreeViewer2 {

    private class HistogramLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TmfGenericTreeEntry) {
                TmfGenericTreeEntry<TmfTreeDataModel> genericEntry = (TmfGenericTreeEntry<TmfTreeDataModel>) element;
                return genericEntry.getName();
            }
            return null;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 1 && element instanceof TmfGenericTreeEntry && isChecked(element)) {
                TmfGenericTreeEntry<TmfTreeDataModel> genericEntry = (TmfGenericTreeEntry<TmfTreeDataModel>) element;
                if (!genericEntry.hasChildren()) {
                    /*
                     * the trace level entry should not have a legend, the Total and lost event
                     * entries should.
                     */
                    return getLegendImage(genericEntry.getModel().getId());
                }
            }
            return null;
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            the parent {@link Composite}
     */
    public EventDensityTreeViewer(Composite parent) {
        super(parent, 1, KvmExitRateDataProvider.ID);
        setLabelProvider(new HistogramLabelProvider());
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return () -> {
            return ImmutableList.of(createColumn(Messages.NewHistogramTree_ColumnName, Comparator.comparing(TmfGenericTreeEntry::getName)),
                    new TmfTreeColumnData(Messages.NewHistogramTree_Legend));
        };
    }

}