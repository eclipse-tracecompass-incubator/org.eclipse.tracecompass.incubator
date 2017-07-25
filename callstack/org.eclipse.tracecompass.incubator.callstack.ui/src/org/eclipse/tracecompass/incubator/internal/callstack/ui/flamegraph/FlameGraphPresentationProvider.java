/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

import com.google.common.collect.ImmutableMap;

/**
 * Presentation provider for the flame graph view, based on the generic TMF
 * presentation provider.
 *
 * @author Sonia Farrah
 */
public class FlameGraphPresentationProvider extends TimeGraphPresentationProvider {
    /** Number of colors used for flameGraph events */
    public static final int NUM_COLORS = 360;

    private static final Format FORMATTER = new SubSecondTimeWithUnitFormat();

    private @Nullable FlameGraphView fView;

    private @Nullable Integer fAverageCharWidth;

    private enum State {
        MULTIPLE(new RGB(100, 100, 100)), EXEC(new RGB(0, 200, 0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Constructor
     */
    public FlameGraphPresentationProvider() {
        // Do nothing
    }

    @Override
    public StateItem[] getStateTable() {
        final float saturation = 0.6f;
        final float brightness = 0.6f;
        StateItem[] stateTable = new StateItem[NUM_COLORS + 1];
        stateTable[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        for (int i = 0; i < NUM_COLORS; i++) {
            RGB rgb = new RGB(i, saturation, brightness);
            stateTable[i + 1] = new StateItem(rgb, State.EXEC.toString());
        }
        return stateTable;
    }

    @Override
    public boolean displayTimesInTooltip() {
        return false;
    }

    @Override
    public String getStateTypeName() {
        return Messages.FlameGraph_Depth;
    }

    @NonNullByDefault({})
    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
//        AggregatedCalledFunctionStatistics statistics = ((FlamegraphEvent) event).getStatistics();
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        long nb = ((FlamegraphEvent) event).getNumberOfCalls();
        builder.put(Messages.FlameGraph_NbCalls, NumberFormat.getIntegerInstance().format(nb)); // $NON-NLS-1$
        Map<String, String> tooltip = ((FlamegraphEvent) event).getTooltip(FORMATTER);
        builder.putAll(tooltip);
//        builder.put(String.valueOf(Messages.FlameGraph_Durations), ""); //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_Duration, FORMATTER.format(event.getDuration())); //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_AverageDuration, FORMATTER.format(statistics.getDurationStatistics().getMean())); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_MaxDuration, FORMATTER.format((statistics.getDurationStatistics().getMax()))); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_MinDuration, FORMATTER.format(statistics.getDurationStatistics().getMin())); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_Deviation, FORMATTER.format(statistics.getDurationStatistics().getStdDev())); //$NON-NLS-1$
//
//        IStatistics<@NonNull ICalledFunction> cpuStats = statistics.getCpuTimesStatistics();
//        // CPU times *if available)
//        if (cpuStats.getMax() != IHostModel.TIME_UNKNOWN) {
//            builder.put(Messages.FlameGraph_CpuTimes, ""); //$NON-NLS-1$
//            builder.put("\t" + Messages.FlameGraph_CpuTime, FORMATTER.format(((FlamegraphEvent) event).getCpuTime())); //$NON-NLS-1$
//            builder.put("\t" + Messages.FlameGraph_AverageCpuTime, FORMATTER.format(cpuStats.getMean())); // $NON-NLS-1$ //$NON-NLS-1$
//            builder.put("\t" + Messages.FlameGraph_MaxCpuTime, FORMATTER.format(cpuStats.getMax())); // $NON-NLS-1$ //$NON-NLS-1$
//            builder.put("\t" + Messages.FlameGraph_MinCpuTime, FORMATTER.format(cpuStats.getMin())); // $NON-NLS-1$ //$NON-NLS-1$
//            builder.put("\t" + Messages.FlameGraph_CpuTimeDeviation, FORMATTER.format(cpuStats.getStdDev())); //$NON-NLS-1$
//        }
//        // Self times
//        builder.put(Messages.FlameGraph_SelfTimes, ""); //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_SelfTime, FORMATTER.format(((FlamegraphEvent) event).getSelfTime())); //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_AverageSelfTime, FORMATTER.format(statistics.getSelfTimeStatistics().getMean())); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_MaxSelfTime, FORMATTER.format(statistics.getSelfTimeStatistics().getMax())); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_MinSelfTime, FORMATTER.format(statistics.getSelfTimeStatistics().getMin())); // $NON-NLS-1$ //$NON-NLS-1$
//        builder.put("\t" + Messages.FlameGraph_SelfTimeDeviation, FORMATTER.format(statistics.getSelfTimeStatistics().getStdDev())); //$NON-NLS-1$
        return builder.build();

    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event instanceof FlamegraphEvent) {
            FlamegraphEvent flameGraphEvent = (FlamegraphEvent) event;
            return flameGraphEvent.getValue() + 1;
        } else if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return State.MULTIPLE.ordinal();
    }

    /**
     * Get the event's symbol.It could be an address or a name.
     *
     * @param fGEvent
     *            An event
     * @param providers
     *            A symbol provider
     */
    private static String getFunctionSymbol(FlamegraphEvent event, Collection<ISymbolProvider> providers) {
        String funcSymbol = ""; //$NON-NLS-1$
        if (event.getSymbol() instanceof Long || event.getSymbol() instanceof Integer) {
            long longAddress = ((Long) event.getSymbol()).longValue();
            for (ISymbolProvider provider : providers) {
                funcSymbol = provider.getSymbolText(longAddress);
                if (funcSymbol != null) {
                    break;
                }
            }
            if (funcSymbol == null) {
                return "0x" + Long.toHexString(longAddress); //$NON-NLS-1$
            }
            // take time of max segment for time a query the symbol name
//            ICalledFunction maxObject = event.getStatistics().getDurationStatistics().getMaxObject();
//            if (maxObject == null) {
//                return "0x" + Long.toHexString(longAddress); //$NON-NLS-1$
//            }
//            long time = maxObject.getStart();
//            int pid = event.getProcessId();
//            if (pid > 0) {
//                String text = symbolProvider.getSymbolText(pid, time, longAddress);
//                if (text != null) {
//                    return text;
//                }
//            }
        } else {
            return String.valueOf(event.getSymbol());
        }
        return funcSymbol;
    }

    @Override
    public void postDrawEvent(@Nullable ITimeEvent event, @Nullable Rectangle bounds, @Nullable GC gc) {
        if (bounds == null || gc == null) {
            return;
        }
        Integer averageCharWidth = fAverageCharWidth;
        if (averageCharWidth == null) {
            averageCharWidth = gc.getFontMetrics().getAverageCharWidth();
            fAverageCharWidth = averageCharWidth;
        }
        if (bounds.width <= averageCharWidth) {
            return;
        }
        if (!(event instanceof FlamegraphEvent)) {
            return;
        }
        String funcSymbol = ""; //$NON-NLS-1$
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace != null) {
            FlamegraphEvent fgEvent = (FlamegraphEvent) event;
            funcSymbol = getFunctionSymbol(fgEvent, SymbolProviderManager.getInstance().getSymbolProviders(activeTrace));
        }
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        Utils.drawText(gc, funcSymbol, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
    }

    /**
     * The flame graph view
     *
     * @return The flame graph view
     */
    public @Nullable FlameGraphView getView() {
        return fView;
    }

    /**
     * The flame graph view
     *
     * @param view
     *            The flame graph view
     */
    public void setView(FlameGraphView view) {
        fView = view;
    }

}
