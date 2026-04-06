/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.KvmExitAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;

/**
 * A data provider for KVM Exit rate visualization.
 * This provider shows the rate of KVM exits per second for each CPU over time,
 * providing a much more meaningful visualization than simple counts.
 *
 * The key improvement over the original histogram approach is that this provider:
 * 1. Calculates actual rates (exits/second) rather than raw counts
 * 2. Uses proper time interval handling for smooth visualizations
 * 3. Leverages the proven rate calculation pattern from DPDK provider
 *
 * @author Francois Belias
 */
@NonNullByDefault
public class KvmExitRateDataProvider extends AbstractRateDataProvider<KvmExitAnalysisModule, TmfTreeDataModel> {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.incubator.overhead.xy.rate.dataprovider"; //$NON-NLS-1$

    private static final String TITLE = "KVM Exit Rate"; //$NON-NLS-1$
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION =
            new TmfXYAxisDescription(Objects.requireNonNull("KVM Exits per Second"), " exits/s", DataType.NUMBER); //$NON-NLS-1$ //$NON-NLS-2$

    // Cache the tree model since CPU topology doesn't change during trace analysis
    private @Nullable TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fCached = null;

    /**
     * Constructor
     *
     * @param trace The trace this data provider analyzes
     * @param analysisModule The KVM exit analysis module
     */
    public KvmExitRateDataProvider(ITmfTrace trace, KvmExitAnalysisModule analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected TmfXYAxisDescription getYAxisDescription() {
        return Y_AXIS_DESCRIPTION;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters,
            @Nullable IProgressMonitor monitor) {

        // Return cached tree if available since CPU topology is static
        if (fCached != null) {
            TmfTreeModel<TmfTreeDataModel> cachedModel = fCached.getModel();
            if (cachedModel != null) {
                return cachedModel;
            }
        }

        List<TmfTreeDataModel> nodes = new ArrayList<>();

        // Create root node representing the trace
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1,
                Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        // Add individual CPU nodes
        // Each CPU gets its own data series for KVM exit rates
        for (Integer vcpuQuark : ss.getQuarks("VCPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            String cpuName = ss.getAttributeName(vcpuQuark);

            // Validate that this is actually a CPU number
            try {
                Integer.parseInt(cpuName);
            } catch (NumberFormatException e) {
                continue; // Skip non-numeric entries
            }

            // Check if this VCPU has KVM exit data
            int exitCountQuark = ss.optQuarkRelative(vcpuQuark, "kvm_exits"); //$NON-NLS-1$
            if (exitCountQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                long vcpuNodeId = getId(exitCountQuark);
                nodes.add(new TmfTreeDataModel(vcpuNodeId, rootId,
                        Collections.singletonList("VCPU " + cpuName), true, null)); //$NON-NLS-1$

                // Check also if this VCPU has exit reasons data
                int reasonQuark = ss.optQuarkRelative(vcpuQuark, "exit_reasons"); //$NON-NLS-1$
                if (reasonQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                    // iterate through every reason
                    for (Integer exitReasonQuark : ss.getSubAttributes(reasonQuark, false)) {
                        String reasonName = ss.getAttributeName(exitReasonQuark);
                        long reasonNodeId = getId(exitReasonQuark);

                        nodes.add(new TmfTreeDataModel(reasonNodeId, vcpuNodeId,
                                Collections.singletonList(reasonName), true, null));
                    }
                }
            }
        }

        // Add an "All CPUs" aggregated view
        // This provides a system-wide perspective on KVM exit activity
        long allCpusId = getId(-1); // Use -1 as a special marker for aggregated data
        nodes.add(new TmfTreeDataModel(allCpusId, rootId,
                Collections.singletonList("All VCPUs (Aggregated)"), true, null)); //$NON-NLS-1$

        TmfTreeModel<TmfTreeDataModel> treeModel = new TmfTreeModel<>(Collections.emptyList(), nodes);
        if (ss.waitUntilBuilt(0)) {
            fCached = new TmfModelResponse<>(treeModel,
                    org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status.COMPLETED, ""); //$NON-NLS-1$
        }
        return treeModel;
    }

    @Override
    protected Map<Integer, RateSeriesBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        Map<Integer, RateSeriesBuilder> builderMap = new HashMap<>();
        int length = filter.getTimesRequested().length;

        Map<Long, Integer> selectedEntries = getSelectedEntries(filter);

        // First pass: identify which quarks are exit reasons
        Set<Integer> exitReasonQuarks = new HashSet<>();
        Set<Integer> parentVcpuQuarks = new HashSet<>();

        for (Integer quark : selectedEntries.values()) {
            if (quark == -1) {
                continue; // Skip aggregated view marker
            }

            try {
                String attributeName = ss.getAttributeName(quark);

                // Check if this is an exit reason
                if (!"kvm_exits".equals(attributeName)) { //$NON-NLS-1$
                    int parentQuark = ss.getParentAttributeQuark(quark);
                    String parentName = ss.getAttributeName(parentQuark);

                    if ("exit_reasons".equals(parentName)) { //$NON-NLS-1$
                        exitReasonQuarks.add(quark);
                        // Track which VCPU this exit reason belongs to
                        int vcpuQuark = ss.getParentAttributeQuark(parentQuark);
                        int vcpuExitQuark = ss.optQuarkRelative(vcpuQuark, "kvm_exits"); //$NON-NLS-1$
                        if (vcpuExitQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            parentVcpuQuarks.add(vcpuExitQuark);
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                // Skip invalid quarks
            }
        }

        // Process each selected entry, but skip VCPU parents if their exit reasons are selected
        for (Map.Entry<Long, Integer> entry : selectedEntries.entrySet()) {
            Long selectedId = entry.getKey();
            Integer quark = entry.getValue();

            // Handle the special case of aggregated "All CPUs" data
            if (quark == -1) {
                String name = getTrace().getName() + "/All VCPUs (Aggregated)"; //$NON-NLS-1$
                builderMap.put(-1, new AggregatedRateSeriesBuilder(selectedId, -1, name, length, ss));
                continue;
            }

            try {
                String attributeName = ss.getAttributeName(quark);

                if ("kvm_exits".equals(attributeName)) { //$NON-NLS-1$
                    // Skip this VCPU if any of its exit reasons are selected
                    if (parentVcpuQuarks.contains(quark)) {
                        continue;
                    }

                    // Handle main KVM exit counter for a VCPU
                    int cpuQuark = ss.getParentAttributeQuark(quark);
                    String cpuName = ss.getAttributeName(cpuQuark);
                    String seriesName = getTrace().getName() + "/VCPU " + cpuName; //$NON-NLS-1$
                    builderMap.put(quark, new RateSeriesBuilder(selectedId, quark, seriesName, length));

                } else {
                    // Check if this is an exit reason quark
                    int parentQuark = ss.getParentAttributeQuark(quark);
                    String parentName = ss.getAttributeName(parentQuark);

                    if ("exit_reasons".equals(parentName)) { //$NON-NLS-1$
                        String reasonName = attributeName;
                        int vcpuQuark = ss.getParentAttributeQuark(parentQuark);
                        String cpuName = ss.getAttributeName(vcpuQuark);
                        String seriesName = getTrace().getName() + "/VCPU " + cpuName + "/" + reasonName; //$NON-NLS-1$ //$NON-NLS-2$
                        builderMap.put(quark, new RateSeriesBuilder(selectedId, quark, seriesName, length));
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                // Skip invalid quarks
                continue;
            }
        }

        return builderMap;
    }

    /**
     * Specialized builder for aggregated CPU data.
     * This builder sums the rates from all individual CPUs to provide
     * a system-wide view of KVM exit activity.
     */
    private class AggregatedRateSeriesBuilder extends RateSeriesBuilder {
        private final ITmfStateSystem fStateSystem;
        private final Map<Integer, Long> fPrevCpuCounts = new HashMap<>();
        private final Map<Integer, Integer> fCpuExitQuarks = new HashMap<>();

        public AggregatedRateSeriesBuilder(long id, int quark, String name, int length, ITmfStateSystem ss) {
            super(id, quark, name, length);
            fStateSystem = ss;

            for (Integer cpuQuark : ss.getQuarks("VCPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
                String cpuName = ss.getAttributeName(cpuQuark);
                try {
                    int cpuId = Integer.parseInt(cpuName);
                    int exitQuark = ss.optQuarkRelative(cpuQuark, "kvm_exits"); //$NON-NLS-1$
                    if (exitQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                        fCpuExitQuarks.put(cpuId, exitQuark);
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric CPU entries
                }
            }
        }

        @Override
        public void updateValue(int pos, long currentCount, long currentTime) {
            // For aggregated data, we need to query all CPUs and sum their rates
            double totalRate = 0;

            for (Map.Entry<Integer, Integer> entry : fCpuExitQuarks.entrySet()) {
                int cpuId = entry.getKey();
                int exitQuark = entry.getValue();

                try {
                    // Get current count for this CPU
                    ITmfStateInterval interval = fStateSystem.querySingleState(currentTime, exitQuark);
                    long currentCpuCount = extractCountFromValue(interval.getValue());

                    // Calculate rate for this CPU
                    Long prevCount = fPrevCpuCounts.get(cpuId);
                    if (prevCount != null) {
                        long deltaCount = currentCpuCount - prevCount;
                        if (deltaCount > 0) {
                            // Use the same time delta as the main calculation
                            long deltaTime = currentTime - super.getPrevTime();
                            if (deltaTime > 0) {
                                double cpuRate = deltaCount / (deltaTime * SECONDS_PER_NANOSECOND);
                                totalRate += cpuRate;
                            }
                        }
                    }

                    // Update previous count for next iteration
                    fPrevCpuCounts.put(cpuId, currentCpuCount);

                } catch (StateSystemDisposedException e) {
                    // Skip this CPU if there's an error
                    continue;
                }
            }

            // Set the aggregated rate value
            fValues[pos] = totalRate;
            setPrevTime(currentTime);
        }

        @Override
        public void setPrevObservation(long initialCount, long timestamp) {
            // Initialize previous counts for all CPUs
            super.setPrevObservation(0, timestamp); // We don't use the count parameter for aggregated data

            for (Map.Entry<Integer, Integer> entry : fCpuExitQuarks.entrySet()) {
                int cpuId = entry.getKey();
                int exitQuark = entry.getValue();

                try {
                    ITmfStateInterval interval = fStateSystem.querySingleState(timestamp, exitQuark);
                    long initialCpuCount = extractCountFromValue(interval.getValue());
                    fPrevCpuCounts.put(cpuId, initialCpuCount);
                } catch (Exception e) {
                    // Initialize to 0 if there's an error
                    fPrevCpuCounts.put(cpuId, 0L);
                }
            }
        }
    }
}