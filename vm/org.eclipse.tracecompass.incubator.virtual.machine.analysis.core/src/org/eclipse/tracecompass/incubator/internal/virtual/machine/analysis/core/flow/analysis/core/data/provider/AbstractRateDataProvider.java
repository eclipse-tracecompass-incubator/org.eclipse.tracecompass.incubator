package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * Abstract base class for data providers that calculate rates of events over time.
 * This class provides the common infrastructure for computing rates by analyzing
 * changes in cumulative counters between time intervals.
 *
 * The key insight is that rate calculation requires:
 * 1. A "builder" pattern to accumulate rate calculations for each data series
 * 2. Proper time interval handling to center measurements on interval midpoints
 * 3. Efficient querying of state system data across multiple time points
 *
 * @param <T> The analysis module type
 * @param <M> The tree data model type
 *
 * @author Francois Belias (Generated from DPDK pattern
 */
public abstract class AbstractRateDataProvider<T extends @NonNull TmfStateSystemAnalysisModule, M extends @NonNull TmfTreeDataModel>
        extends AbstractTreeCommonXDataProvider<T, M> {

    static final double SECONDS_PER_NANOSECOND = 1E-9;

    /**
     * Builder class for constructing rate data series.
     * This encapsulates the logic for calculating rates between consecutive observations.
     */
    protected class RateSeriesBuilder {
        private final long fId;
        private final int fMetricQuark;
        private final String fName;
        /**
         * fvalues
         */
        protected final double[] fValues;

        private long fPrevCount;
        private long fPrevTime;

        /**
         * Constructor for rate series builder
         *
         * @param id The unique series identifier
         * @param metricQuark The state system quark for this metric
         * @param name The display name for this series
         * @param length The number of data points in the series
         */
        public RateSeriesBuilder(long id, int metricQuark, String name, int length) {
            fId = id;
            fMetricQuark = metricQuark;
            fName = name;
            fValues = new double[length];
        }

        /**
         * Update the rate value at the specified position.
         * This calculates the rate as (change in count) / (change in time).
         *
         * @param pos The array position to update
         * @param currentCount The current cumulative count
         * @param currentTime The current timestamp
         */
        public void updateValue(int pos, long currentCount, long currentTime) {
            long deltaCount = currentCount - fPrevCount;
            long deltaTime = currentTime - getPrevTime();

            double rate = 0;
            if (deltaCount > 0 && deltaTime > 0) {
                // Convert nanoseconds to seconds for the rate calculation
                rate = deltaCount / (deltaTime * SECONDS_PER_NANOSECOND);
            }

            fValues[pos] = rate;
            fPrevCount = currentCount;
            setPrevTime(currentTime);
        }

        /**
         * Set the initial observation for rate calculation
         *
         * @param initialCount The starting count value
         * @param timestamp The starting timestamp
         */
        public void setPrevObservation(long initialCount, long timestamp) {
            fPrevCount = initialCount;
            setPrevTime(timestamp);
        }

        /**
         * Build the final Y-axis model from the accumulated data
         *
         * @param yAxisDescription Description for the Y axis
         * @return The completed IYModel
         */
        public IYModel build(TmfXYAxisDescription yAxisDescription) {
            return new YModel(fId, fName, fValues, yAxisDescription);
        }

        /**
         * Get the quark associated with this builder
         * @return the metric quark
         */
        public int getQuark() {
            return fMetricQuark;
        }

        public long getPrevTime() {
            return fPrevTime;
        }

        public void setPrevTime(long prevTime) {
            fPrevTime = prevTime;
        }
    }

    /**
     * Constructor
     *
     * @param trace The trace this provider analyzes
     * @param module The analysis module
     */
    public AbstractRateDataProvider(ITmfTrace trace, T module) {
        super(trace, module);
    }

    @Override
    protected @Nullable Collection<@NonNull IYModel> getYSeriesModels(ITmfStateSystem ss,
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return null;
        }

        long[] xValues = filter.getTimesRequested();
        if (xValues.length <= 1) {
            return Collections.emptyList();
        }

        // Initialize builders for each selected data series
        Map<Integer, RateSeriesBuilder> builderByQuark = initBuilders(ss, filter);
        if (builderByQuark.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate time step and start time for proper interval handling
        long halfStepSize = (xValues[1] - xValues[0]) / 2;
        long startTime = Math.max(filter.getStart() - halfStepSize, ss.getStartTime());

        // Seed each builder with the initial observation
        // This is crucial for rate calculation - we need a baseline
        for (RateSeriesBuilder builder : builderByQuark.values()) {
            ITmfStateInterval iv = ss.querySingleState(startTime, builder.getQuark());
            Object v = iv.getValue();
            long count = extractCountFromValue(v);
            builder.setPrevObservation(count, startTime);
        }

        // Adjust query times to center on interval midpoints
        // This provides more accurate rate calculations
        int n = xValues.length;
        List<Long> adjustedQueryTimes = new ArrayList<>(n);
        for (int i = 1; i < n; i++) {
            adjustedQueryTimes.add(xValues[i] - halfStepSize);
        }
        adjustedQueryTimes.add(Math.min(xValues[n - 1] + halfStepSize, ss.getCurrentEndTime()));

        // Query all intervals for all metrics in one efficient operation
        List<ITmfStateInterval> intervals = StreamSupport.stream(
                ss.query2D(builderByQuark.keySet(), adjustedQueryTimes).spliterator(), false)
                .sorted(Comparator.comparingLong(ITmfStateInterval::getStartTime))
                .collect(Collectors.toList());

        // Process each interval and update the corresponding builders
        for (ITmfStateInterval interval : intervals) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }

            // Find which time indices this interval affects
            int from = Collections.binarySearch(adjustedQueryTimes, interval.getStartTime());
            from = (from >= 0) ? from : -1 - from;

            int to = Collections.binarySearch(adjustedQueryTimes, interval.getEndTime());
            to = (to >= 0) ? to + 1 : -1 - to;

            if (from < to) {
                Object value = interval.getValue();
                long count = extractCountFromValue(value);
                RateSeriesBuilder builder = builderByQuark.get(interval.getAttribute());
                if (builder != null) {
                    // Update all affected time points
                    for (int j = from; j < to; j++) {
                        builder.updateValue(j, count, adjustedQueryTimes.get(j));
                    }
                }
            }
        }

        // Build and return all the Y-axis models
        return ImmutableList.copyOf(
                builderByQuark.values().stream()
                        .map(builder -> builder.build(getYAxisDescription()))
                        .collect(Collectors.toList()));
    }

    /**
     * Extract a count value from a state system value object.
     * Subclasses can override this if they need special handling for their value types.
     *
     * @param value The value from the state system
     * @return The count as a long value
     */
    protected long extractCountFromValue(Object value) {
        return (value instanceof Number) ? ((Number) value).longValue() : 0L;
    }

    /**
     * Initialize the rate series builders based on the selection filter.
     * This method must be implemented by subclasses to create builders
     * for their specific data structures.
     *
     * @param ss The state system
     * @param filter The selection and time filter
     * @return Map of quark to builder for selected data series
     */
    protected abstract @NonNull Map<Integer, RateSeriesBuilder> initBuilders(@NonNull ITmfStateSystem ss, @NonNull SelectionTimeQueryFilter filter);

    /**
     * Get the Y-axis description for this data provider.
     * Subclasses should return appropriate axis information.
     *
     * @return The Y-axis description
     */
    protected abstract TmfXYAxisDescription getYAxisDescription();
}