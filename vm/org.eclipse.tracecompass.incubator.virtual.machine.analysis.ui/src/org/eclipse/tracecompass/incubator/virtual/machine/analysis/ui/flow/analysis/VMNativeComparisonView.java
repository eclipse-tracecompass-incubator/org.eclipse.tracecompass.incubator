package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider.VMNativeTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

// public static final String ID = "org.eclipse.tracecompass.analysis.vmcomparison.ui.view"; //$NON-NLS-1$

/**
 * Time graph view for VM/Native analysis
 */
public class VMNativeComparisonView extends BaseDataProviderTimeGraphView {

    public static final String ID = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.vm.comparative.view"; //$NON-NLS-1$

    /**
     * Construction of the class
     */
    public VMNativeComparisonView() {
        super(ID, new VMNativePresentationProvider(), VMNativeTimeGraphDataProvider.ID);
    }

    @Override
    protected @NonNull String getNextText() {
        return "Next VM/Native Event"; //$NON-NLS-1$
    }

    @Override
    protected @NonNull String getNextTooltip() {
        return "Go to next VM/Native event"; //$NON-NLS-1$
    }

    @Override
    protected @NonNull String getPrevText() {
        return "Previous VM/Native Event"; //$NON-NLS-1$
    }

    @Override
    protected @NonNull String getPrevTooltip() {
        return "Go to previous VM/Native event"; //$NON-NLS-1$
    }

    /**
     * Presentation provider for VM/Native states
     */
    private static class VMNativePresentationProvider extends TimeGraphPresentationProvider {

        // Définir les couleurs pour chaque état
        private static final RGB IDLE_COLOR = new RGB(200, 200, 200);        // Gris clair
        private static final RGB NATIVE_COLOR = new RGB(0, 150, 0);          // Vert
        private static final RGB VM_COLOR = new RGB(0, 100, 200);            // Bleu
        private static final RGB HOST_OVERHEAD_COLOR = new RGB(255, 140, 0);  // Orange

        @Override
        public StateItem[] getStateTable() {
            return new StateItem[] {
                new StateItem(IDLE_COLOR, "Idle"), //$NON-NLS-1$
                new StateItem(NATIVE_COLOR, "Native"), //$NON-NLS-1$
                new StateItem(VM_COLOR, "VM Guest"), //$NON-NLS-1$
                new StateItem(HOST_OVERHEAD_COLOR, "Host Overhead") //$NON-NLS-1$
            };
        }

        @Override
        public int getStateTableIndex(ITimeEvent event) {
            if (event instanceof TimeEvent) {
                TimeEvent timeEvent = (TimeEvent) event;
                return timeEvent.getValue();
            }
            return TRANSPARENT;
        }

        @Override
        public String getEventName(ITimeEvent event) {
            if (event instanceof TimeEvent) {
                TimeEvent timeEvent = (TimeEvent) event;
                int value = timeEvent.getValue();

                switch (value) {
                case 0:
                    return "Idle"; //$NON-NLS-1$
                case 1:
                    return "Native"; //$NON-NLS-1$
                case 2:
                    return "VM Guest"; //$NON-NLS-1$
                case 3:
                    return "Host Overhead"; //$NON-NLS-1$
                default:
                    return "Unknown"; //$NON-NLS-1$
                }
            }
            return ""; //$NON-NLS-1$
        }

        @Override
        public String getStateTypeName() {
            return "VM/Native State"; //$NON-NLS-1$
        }
    }
}