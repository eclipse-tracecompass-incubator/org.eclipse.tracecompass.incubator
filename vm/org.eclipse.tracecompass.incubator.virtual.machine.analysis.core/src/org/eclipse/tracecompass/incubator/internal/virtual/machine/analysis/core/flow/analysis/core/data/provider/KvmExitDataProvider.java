/*******************************************************************************
 * KVM Exit Density Data Provider
 * This data provider analyzes KVM exit events per CPU
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.KvmExitAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.TmfXyResponseFactory;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXyTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

/**
 * A data provider for KVM Exit events histogram.
 * This provider gives a visual representation of KVM exits evolution
 * for each CPU over time.
 *
 * Based on the pattern of HistogramDataProvider to improve visualization.
 *
 * @author Francois Belias
 */
@NonNullByDefault
public class KvmExitDataProvider extends AbstractTmfTraceDataProvider implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.incubator.overhead.xy.dataprovider"; //$NON-NLS-1$
    private static final String TITLE = "KVM Exits Density"; //$NON-NLS-1$
    private static final AtomicLong TRACE_IDS = new AtomicLong();

    private final KvmExitAnalysisModule fmodule;
    private @Nullable TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fCached = null;
    private final long fTraceId = TRACE_IDS.getAndIncrement();
    private final Map<Integer, Long> fCpuIdToSeriesId = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace this data provider is for
     * @param analysisModule
     *            The analysis module
     */
    public KvmExitDataProvider(ITmfTrace trace, KvmExitAnalysisModule analysisModule) {
        super(trace);
        fmodule = analysisModule;
    }

    /**
     * Create the KVM exit data provider
     *
     * @param trace
     *            The trace for which to create the data provider
     * @return The data provider
     */
    /*public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        KvmExitAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KvmExitAnalysisModule.class, KvmExitAnalysisModule.ID);
        return module != null ? new KvmExitDataProvider(trace, module) : null;
    }*/

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (this.fCached != null) {
            return this.fCached;
        }

        fmodule.waitForInitialization();
        ITmfStateSystem ss = fmodule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        Builder<TmfTreeDataModel> builder = ImmutableList.builder();

        // Add the trace as root
        builder.add(new TmfTreeDataModel(fTraceId, -1, Collections.singletonList(getTrace().getName())));

        // Get all the CPUs with KVM exit data
        for (Integer cpuQuark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            // Get the CPU ID from the attribute name
            String cpuName = ss.getAttributeName(cpuQuark);
            int cpuId;

            try {
                cpuId = Integer.parseInt(cpuName);
            } catch (NumberFormatException e ) {
                continue;  // Just in case an entry is not a CPU number;
            }

            // Get or Create the exit count quark for this CPU
            int exitCountQuark = ss.optQuarkRelative(cpuQuark, "kvm_exits"); //$NON-NLS-1$
            if (exitCountQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                // create an unique ID for this CPU's exit events
                Long seriesId = TRACE_IDS.getAndIncrement();
                this.fCpuIdToSeriesId.put(cpuId, seriesId);
                builder.add(new TmfXyTreeDataModel(seriesId, fTraceId,
                        Collections.singletonList("CPU " + cpuName), true, null, true)); //$NON-NLS-1$

            }
        }

        // TODO see other dataprovider

        // Create an "All CPUs" entry to show aggregated data
        Long allCpusId = TRACE_IDS.getAndIncrement();
        this.fCpuIdToSeriesId.put(-1, allCpusId);  // I use -1 as Id to represent "All CPUs"
        builder.add(new TmfXyTreeDataModel(allCpusId, fTraceId,
                Collections.singletonList("All CPUs"), true, null, true)); //$NON-NLS-1$


        if (ss.waitUntilBuilt(0)) {
            TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> response = new TmfModelResponse<>(
                    new TmfTreeModel<>(Collections.emptyList(), builder.build()),
                    ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            fCached = response;
            return response;
        }

        return new TmfModelResponse<>(
                new TmfTreeModel<>(Collections.emptyList(), builder.build()),
                ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);

    }


    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fmodule.waitForInitialization();
        ITmfStateSystem ss = fmodule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
            //return TmfXyResponseFactory.createFailedResponse(CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        long[] xValues = new long[0];
        if (filter == null) {
            return TmfXyResponseFactory.create(TITLE, xValues, Collections.emptyList(), true);
        }
        xValues = filter.getTimesRequested();  // times

        Collection<Long> selected = filter.getSelectedItems();  // CPU selected
        if (selected.isEmpty()) {
         // If nothing is selected, return empty series
            return TmfXyResponseFactory.create(TITLE, xValues, Collections.emptyList(), true);
        }

        int numPoints = xValues.length;
        ImmutableList.Builder<IYModel> builder = ImmutableList.builder();

        try {
            // Process each selected CPU
            for (Map.Entry<Integer, Long> entry: this.fCpuIdToSeriesId.entrySet()) {
                int cpuId = entry.getKey();
                long seriesId = entry.getValue();

                if (!selected.contains(seriesId)) {
                    continue;
                }

                double[] values = new double[numPoints];
                Arrays.fill(values, 0.0);

                if (cpuId == -1) {
                    // "All CPUs" *  aggregate data from all CPUs
                    aggregateAllCpuData(ss, xValues, values);
                } else {
                    // individual CPU
                    fillCpuExitData(ss, cpuId, xValues, values);
                }

                String name;
                if (cpuId == -1) {
                    name = getTrace().getName() + "/All CPUs"; //$NON-NLS-1$
                } else {
                    name = getTrace().getName() + "/CPU " + cpuId; //$NON-NLS-1$
                }

                builder.add(new YModel(seriesId, name, values));
            }
        } catch (StateSystemDisposedException e) {
            return TmfXyResponseFactory.createFailedResponse(CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean completed = ss.waitUntilBuilt(0) || ss.getCurrentEndTime() >= filter.getEnd();
        return TmfXyResponseFactory.create(TITLE, xValues, builder.build(), completed);
    }

    /**
     * Fill the values array with KVM exit counts for a specific CPU
     *
     * @param ss The state system
     * @param cpuId The CPU ID
     * @param times The time points
     * @param values The array to fill with values
     * @throws StateSystemDisposedException If the state system is disposed
     */
    private static void fillCpuExitData(ITmfStateSystem ss, int cpuId, long[] times, double[] values) throws StateSystemDisposedException {
        // Find the quark for this CPU's KVM exits
        int cpuQuark = ss.optQuarkAbsolute("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$
        if (cpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }

        int exitQuark = ss.optQuarkRelative(cpuQuark, "kvm_exits"); //$NON-NLS-1$
        if (exitQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }

        // Get all intervals for this CPU's KVM exits
        List<ITmfStateInterval> intervals = Lists.newArrayList(
                ss.query2D(Collections.singleton(exitQuark), times[0], times[times.length - 1]));
        intervals.sort(Comparator.comparing(ITmfStateInterval::getStartTime));

        // Fill the values array
        for (int i = 0; i < times.length - 1; i++) {
            long start = times[i];
            long end = times[i + 1];
            double count = countExitsInRange(intervals, start, end);

            // Normalize by time to get exits per second
            long duration = end - start;
            if (duration > 0) {
                values[i] = count * 1_000_000_000 / duration; // exits per second
            }
        }

        // Copy last point for continuity
        if (values.length > 1) {
            values[values.length - 1] = values[values.length - 2];
        }
    }

    /**
     * Aggregate KVM exit data from all CPUs
     *
     * @param ss The state system
     * @param times The time points
     * @param values The array to fill with aggregated values
     * @throws StateSystemDisposedException If the state system is disposed
     */
    private static void aggregateAllCpuData(ITmfStateSystem ss, long[] times, double[] values) throws StateSystemDisposedException {
        // Get all CPUs with KVM exit data
        for (Integer cpuQuark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            String cpuName = ss.getAttributeName(cpuQuark);
            int cpuId;
            try {
                cpuId = Integer.parseInt(cpuName);

                // Create a temporary array for this CPU's data
                double[] cpuValues = new double[values.length];
                Arrays.fill(cpuValues, 0.0);

                // Get this CPU's data
                fillCpuExitData(ss, cpuId, times, cpuValues);

                // Add to the aggregate values
                for (int i = 0; i < values.length; i++) {
                    values[i] += cpuValues[i];
                }

            } catch (NumberFormatException e) {
                continue; // Skip entries that aren't CPU numbers
            }
        }
    }

    /**
     * Count the number of KVM exits in a specific time range
     *
     * @param intervals The list of state intervals
     * @param start The start time
     * @param end The end time
     * @return The count of KVM exits in the range
     */
    private static double countExitsInRange(List<ITmfStateInterval> intervals, long start, long end) {
        double count = 0;

        for (ITmfStateInterval interval : intervals) {
            // Check if the interval intersects with our time range
            if (interval.getEndTime() < start || interval.getStartTime() > end) {
                continue;
            }

            // Calculate the portion of the interval that's within our range
            long intervalStart = Math.max(interval.getStartTime(), start);
            long intervalEnd = Math.min(interval.getEndTime(), end);

            // Get the exit count from the interval
            Object value = interval.getValue();
            if (value instanceof Number) {
                double exitCount = ((Number) value).doubleValue();

                // Scale the count based on how much of the interval is in our range
                double portion = (double) (intervalEnd - intervalStart) / (interval.getEndTime() - interval.getStartTime());
                count += exitCount * portion;
            }
        }

        return count;
    }
}