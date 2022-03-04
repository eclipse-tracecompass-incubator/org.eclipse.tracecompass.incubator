/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.format.DecimalUnitFormat;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.SequentialPaletteProvider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * A row model for the timegraph flows views.
 *
 * @author Yoann Heitz
 */
final class FlowsRowModel extends TimeGraphRowModel {

    /**
     * The styles for the row depends on the mean and standard deviation of the
     * states. This class allows to store a simple version of the states with
     * only the value stored as double, the start and the duration of the state.
     *
     * @author Yoann Heitz
     */
    private static class FlowsState {
        private long fStart;
        private long fDuration;
        private double fValue;

        /**
         * Constructor for this class
         *
         * @param start
         *            the start timestamp of the state
         * @param duration
         *            the duration of the state
         * @param value
         *            the value of the state
         */
        public FlowsState(long start, long duration, double value) {
            fStart = start;
            fDuration = duration;
            fValue = value;
        }

        /**
         * Gets the start of this state
         *
         * @return the start of this state
         */
        public long getStart() {
            return fStart;
        }

        /**
         * Gets the duration of this state
         *
         * @return the duration of this state
         */
        public long getDuration() {
            return fDuration;
        }

        /**
         * Gets the value of this state
         *
         * @return the value of this state
         */
        public double getValue() {
            return fValue;
        }
    }

    private static final String COLOR = "#ee0000cc"; //$NON-NLS-1$
    private static final int COLORS_NUMBER = 20;
    private static final float ZSCORE_FACTOR = (float) (COLORS_NUMBER / 2 * 0.7);
    private static final double ZERO_THRESHOLD = 1E-10;

    /**
     * This object allows to limit the number of units that are displayed.
     * However it needs to be improved.
     */
    private DecimalUnitFormat fFormat = new DecimalUnitFormat(1E9);
    private List<RGBAColor> fColors;
    private double fMean;
    private double fStandardDeviation;
    private TreeMap<Long, Double> fTimestampFlowChangeMap = new TreeMap<>();
    private @Nullable FlowsRowModel fParentModel;

    /**
     * Tests if a double is equal to zero
     *
     * @param value
     *            the double to test
     * @return true of the double is zero
     */
    private static boolean isZero(double value) {
        return Math.abs(value) <= ZERO_THRESHOLD;
    }

    /**
     * Constructor for this class
     *
     * @param entryID
     *            the ID of the entry associated to this row
     * @param states
     *            the list of states for this row
     * @param parentModel
     *            the row of the parent of the entry associated with this row.
     *            It can be null if this row entry is a root
     */
    public FlowsRowModel(long entryID, List<ITimeGraphState> states, @Nullable FlowsRowModel parentModel) {
        super(entryID, states);
        fParentModel = parentModel;
        RGBAColor baseColor = RGBAColor.fromString(COLOR);
        @Nullable IPaletteProvider paletteProvider = baseColor != null ? SequentialPaletteProvider.create(baseColor, COLORS_NUMBER) : null;
        List<RGBAColor> colors = paletteProvider != null ? paletteProvider.get() : new ArrayList<>();
        fColors = colors;
    }

    /**
     * Clips a value between two values
     *
     * @param min
     *            the minimum value
     * @param max
     *            the maximum value
     * @param value
     *            the value to clip between min and max
     * @return the clipped value
     */
    private static double clip(double min, double max, double value) {
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Gets the style to display depending on the value of the state
     *
     * @param value
     *            the value of the state
     * @return the style to display
     */
    private OutputElementStyle getStateStyle(double value) {
        double zScore = fStandardDeviation != 0 ? (value - fMean) / fStandardDeviation : 0;
        int index = (int) clip(0, COLORS_NUMBER - 1, COLORS_NUMBER / 2 + ZSCORE_FACTOR * zScore);
        RGBAColor color = fColors.get(index);
        return new OutputElementStyle(null, ImmutableMap.of(StyleProperties.BORDER_STYLE, StyleProperties.BorderStyle.SOLID, StyleProperties.BACKGROUND_COLOR, color.toString().substring(0, 7)));
    }

    /**
     * Computes the mean and the standard deviation for the non zero states of
     * this row, then produces the styles and states to display on the row
     * depending on the previously computed values.
     *
     * @param dataProvider
     *            the data provider that called this method
     * @param predicates
     *            the predicates used to filter the timegraph state
     * @param monitor
     *            the progress monitor
     */
    public void computeStatisticsAndStates(Otf2FlowsDataProvider dataProvider, Map<Integer, Predicate<Multimap<String, Object>>> predicates, @Nullable IProgressMonitor monitor) {
        if (fTimestampFlowChangeMap.isEmpty()) {
            return;
        }
        long lastTimestamp = fTimestampFlowChangeMap.firstKey();
        ArrayList<FlowsState> flowsStates = new ArrayList<>();
        double currentFlowValue = 0;
        double cumulativeFlow = 0;
        double cumulativeSquaredFlow = 0;
        long numberOfStates = 0;

        // The flow changes are parsed to compute the current flow value,
        // compute statistics and create associated states
        for (Map.Entry<Long, Double> entry : fTimestampFlowChangeMap.entrySet()) {
            long timestamp = entry.getKey();
            double flowChange = entry.getValue();
            long durationSinceLastTimestamp = timestamp - lastTimestamp;

            // The statistics are only computed on non null flow states
            if (!isZero(currentFlowValue)) {
                if (!isZero(flowChange)) {
                    flowsStates.add(new FlowsState(lastTimestamp, durationSinceLastTimestamp, currentFlowValue));
                    numberOfStates += 1;
                    cumulativeFlow += currentFlowValue;
                    cumulativeSquaredFlow += Math.pow(currentFlowValue, 2);
                    currentFlowValue += flowChange;
                    lastTimestamp = timestamp;
                }
            } else {
                flowsStates.add(new FlowsState(lastTimestamp, durationSinceLastTimestamp, 0.0));
                currentFlowValue += flowChange;
                lastTimestamp = timestamp;
            }
        }
        fMean = cumulativeFlow / numberOfStates;
        double squaredDataMean = cumulativeSquaredFlow / numberOfStates;
        double squaredMean = Math.pow(fMean, 2);

        // We must test if squaredDataMean >= squaredMean. Theoretically it
        // should not happen, but with inaccuracy when dealing with floats it
        // could happen
        fStandardDeviation = squaredDataMean >= squaredMean ? Math.sqrt(squaredDataMean - squaredMean) : 0;

        // Once mean and standard deviation have been calculated, we can create
        // the states and their styles
        List<ITimeGraphState> states = getStates();
        for (FlowsState state : flowsStates) {
            double value = state.getValue();
            @Nullable OutputElementStyle style = isZero(value) ? null : getStateStyle(value);
            @Nullable String label = isZero(value) ? null : fFormat.format(value, new StringBuffer(), null).append("B/s").toString(); //$NON-NLS-1$
            TimeGraphState timeGraphState = new TimeGraphState(state.getStart(), state.getDuration(), label, style);
            dataProvider.applyFilterAndAddState(states, timeGraphState, getEntryID(), predicates, monitor);
        }
    }

    /**
     * Recursive function to fill a flow change in the TreeMap containing the
     * flow changes. It then calls the same method on the parent row unless this
     * row has no parent
     *
     * @param timestamp
     *            the timestamp when the flow change occurred
     * @param value
     *            the value of the flow change
     */
    public void addFlowChange(long timestamp, double value) {
        fTimestampFlowChangeMap.merge(timestamp, value, (oldValue, newValue) -> oldValue + newValue);
        if (fParentModel != null) {
            fParentModel.addFlowChange(timestamp, value);
        }
    }

    /**
     * Gets the mean of the non null states for this row
     *
     * @return the mean of the non null states for this row
     */
    public double getMean() {
        return fMean;
    }

    /**
     * Get the standard deviation of the non null states for this row
     *
     * @return the standard deviation of the non null states for this row
     */
    public double getStandardDeviation() {
        return fStandardDeviation;
    }
}
