package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;


import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;

/**
 * Presentation provider for VM/Native comparison flame graph
 * Uses blue for Native syscalls and green for VM syscalls
 *
 * @author Your Name
 */
public class VMNativeFlameGraphPresentationProvider extends TimeGraphPresentationProvider {

    /** Native syscall color (blue) */
    private static final RGB NATIVE_COLOR = new RGB(70, 130, 180); // Steel blue

    /** VM syscall color (green) */
    private static final RGB VM_COLOR = new RGB(34, 139, 34); // Forest green

    /** Default color for unknown events */
    private static final RGB DEFAULT_COLOR = new RGB(128, 128, 128); // Gray

    /** Native state item */
    private static final StateItem NATIVE_STATE = new StateItem(NATIVE_COLOR, "Native"); //$NON-NLS-1$

    /** VM state item */
    private static final StateItem VM_STATE = new StateItem(VM_COLOR, "VM"); //$NON-NLS-1$

    /** Default state item */
    private static final StateItem DEFAULT_STATE = new StateItem(DEFAULT_COLOR, "Unknown"); //$NON-NLS-1$

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event == null) {
            return TRANSPARENT;
        }

        ITimeGraphEntry entry = event.getEntry();
        if (entry != null) {
            String entryName = entry.getName();
            if (entryName.contains("Native")) { //$NON-NLS-1$
                return 0; // Native state
            } else if (entryName.contains("VM")) { //$NON-NLS-1$
                return 1; // VM state
            }
        }

        return 2; // Default state
    }

    @Override
    public StateItem[] getStateTable() {
        return new StateItem[] {
            NATIVE_STATE,
            VM_STATE,
            DEFAULT_STATE
        };
    }

    @Override
    public String getEventName(@Nullable ITimeEvent event) {
        if (event instanceof NamedTimeEvent) {
            return ((NamedTimeEvent) event).getLabel();
        }
        return super.getEventName(event);
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(@Nullable ITimeEvent event) {
        Map<String, String> tooltipInfo = super.getEventHoverToolTipInfo(event);

        if (event instanceof NamedTimeEvent namedEvent) {
            tooltipInfo.put("Syscall", namedEvent.getLabel()); //$NON-NLS-1$

            ITimeGraphEntry entry = event.getEntry();
            if (entry != null) {
                String entryName = entry.getName();
                if (entryName.contains("Native")) { //$NON-NLS-1$
                    tooltipInfo.put("Environment", "Native"); //$NON-NLS-1$ //$NON-NLS-2$
                } else if (entryName.contains("VM")) { //$NON-NLS-1$
                    tooltipInfo.put("Environment", "Virtualized"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            tooltipInfo.put("Duration", Long.toString(event.getDuration())); //$NON-NLS-1$
            tooltipInfo.put("Position", Long.toString(event.getTime())); //$NON-NLS-1$
        }

        return tooltipInfo;
    }

    @Override
    public void postDrawEvent(@Nullable ITimeEvent event, @Nullable Rectangle bounds, @Nullable GC gc) {
        super.postDrawEvent(event, bounds, gc);

        if (event instanceof NamedTimeEvent namedEvent && bounds != null && gc != null) {
            // Draw the syscall name on top of the event rectangle
            String label = namedEvent.getLabel();
            if (!label.isEmpty()) {
                gc.setForeground(gc.getDevice().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));

                // Calculate text position to center it in the rectangle
                org.eclipse.swt.graphics.Point textExtent = gc.textExtent(label);
                int textX = bounds.x + (bounds.width - textExtent.x) / 2;
                int textY = bounds.y + (bounds.height - textExtent.y) / 2;

                // Only draw text if it fits reasonably in the rectangle
                if (textExtent.x <= bounds.width - 4 && textExtent.y <= bounds.height - 2) {
                    gc.drawText(label, textX, textY, true);
                }
            }
        }
    }

    @Override
    public String getStateTypeName(@Nullable ITimeGraphEntry entry) {
        if (entry != null) {
            String entryName = entry.getName();
            if (entryName.contains("Native")) { //$NON-NLS-1$
                return "Native Flow"; //$NON-NLS-1$
            } else if (entryName.contains("VM")) { //$NON-NLS-1$
                return "VM Flow"; //$NON-NLS-1$
            }
        }
        return "Execution Flow"; //$NON-NLS-1$
    }
}