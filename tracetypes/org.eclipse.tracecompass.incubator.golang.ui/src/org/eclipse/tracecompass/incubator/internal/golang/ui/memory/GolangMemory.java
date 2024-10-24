package org.eclipse.tracecompass.incubator.internal.golang.ui.memory;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.golang.core.analysis.memory.GoMemoryAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.memory.MemoryUsageView2;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

public class GolangMemory extends MemoryUsageView2 {


    /** ID string */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.golang.ui.memoryUsage"; //$NON-NLS-1$

    /**
     * Constructor
     */
    @SuppressWarnings("restriction")
    public GolangMemory() {
        super("Heap Status", GoMemoryAnalysisModule.ID,
                new TmfXYChartSettings("Heap Status", "Time", "Memory Usage", 1));
    }

}
