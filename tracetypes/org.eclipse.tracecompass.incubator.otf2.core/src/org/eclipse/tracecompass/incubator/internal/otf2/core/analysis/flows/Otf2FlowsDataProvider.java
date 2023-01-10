/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.callstack.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Multimap;

/**
 * Data provider for the OTF2 flows view.
 *
 * @author Yoann Heitz
 */
@SuppressWarnings("restriction")
public class Otf2FlowsDataProvider extends AbstractTimeGraphDataProvider<Otf2FlowsAnalysis, TimeGraphEntryModel> implements IOutputStyleProvider {

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    /**
     * Gets the data provider ID
     *
     * @return the data provider ID
     */
    public static String getFullDataProviderId() {
        return Otf2FlowsAnalysis.getFullAnalysisId() + SUFFIX;
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Otf2FlowsDataProvider(ITmfTrace trace, Otf2FlowsAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private static final String TOTAL_FLOW = "Total flow : "; //$NON-NLS-1$
    private static final String FLOW_UNIT = "MB/s"; //$NON-NLS-1$

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        List<Long> requestedTimes = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        List<Long> requestedEntries = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (ss == null || requestedEntries == null || requestedEntries.size() != 1 || requestedTimes == null || requestedTimes.size() != 1) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        Map<Long, Integer> entryIdToQuarks = getSelectedEntries(requestedEntries);

        double totalFlow = 0;
        Integer quark = entryIdToQuarks.get(requestedEntries.get(0));
        if (quark == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        List<Integer> quarksToQuery = ss.getSubAttributes(quark, true);
        if (quarksToQuery.isEmpty()) {
            quarksToQuery.add(quark);
        }
        try {
            for (ITmfStateInterval interval : ss.query2D(quarksToQuery, requestedTimes)) {
                totalFlow += interval.getValueDouble();
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }

        Map<String, String> model = new HashMap<>();
        model.put(TOTAL_FLOW, String.valueOf(totalFlow * 1E3) + FLOW_UNIT);
        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private void fillLeafQuarks(ITmfStateSystem ss, Map<Integer, FlowsRowModel> quarkRowMap, List<Integer> leafQuarks, int quark) {
        List<Integer> subQuarks = ss.getSubAttributes(quark, false);
        if (subQuarks.isEmpty()) {
            leafQuarks.add(quark);
        }
        FlowsRowModel parentFlowsRowModel = quarkRowMap.get(quark);
        for (Integer subQuark : subQuarks) {
            quarkRowMap.put(subQuark, new FlowsRowModel(getId(subQuark), new ArrayList<>(), parentFlowsRowModel));
            fillLeafQuarks(ss, quarkRowMap, leafQuarks, subQuark);
        }
    }

    private void fillRowModels(ITmfStateSystem ss, Map<Integer, FlowsRowModel> quarkRowMap, int childQuark, Map<Integer, Predicate<Multimap<String, Object>>> predicates, @Nullable IProgressMonitor monitor) {
        Integer parentQuark = ss.getParentAttributeQuark(childQuark);
        if (parentQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
            FlowsRowModel parentFlowRowModel = quarkRowMap.get(childQuark);
            FlowsRowModel flowRowModel = quarkRowMap.get(parentQuark);
            if (parentFlowRowModel != null && flowRowModel != null) {
                flowRowModel.computeStatisticsAndStates(this, predicates, monitor);
                quarkRowMap.put(parentQuark, flowRowModel);
                fillRowModels(ss, quarkRowMap, parentQuark, predicates, monitor);
            }
        }
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        Multimap<Integer, String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());

        // Query the intervals for the leaf quarks (Threads) and create flows
        // row models.
        List<Integer> leafQuarks = new ArrayList<>();
        Map<Integer, FlowsRowModel> quarkRowMap = new HashMap<>();
        fillLeafQuarks(ss, quarkRowMap, leafQuarks, ITmfStateSystem.ROOT_ATTRIBUTE);

        // Add Flow from the state system
        for (ITmfStateInterval interval : ss.query2D(leafQuarks, times)) {
            int quark = interval.getAttribute();
            long startTime = interval.getStartTime();
            long endTime = interval.getEndTime();
            double flowValue = interval.getValueDouble();
            FlowsRowModel rowModel = quarkRowMap.get(quark);
            if (rowModel != null) {
                rowModel.addFlowChange(startTime, flowValue);
                rowModel.addFlowChange(endTime, -flowValue);
            }
        }
        // Calculate flow of the parents
        for (int leafQuark : leafQuarks) {
            fillRowModels(ss, quarkRowMap, leafQuark, predicates, monitor);
        }
        List<ITimeGraphRowModel> rows = new ArrayList<>();
        // Retrieve the resulting rows
        for (Map.Entry<Integer, FlowsRowModel> entry : quarkRowMap.entrySet()) {
            long entryId = getId(entry.getKey());
            List<ITimeGraphState> eventList = Objects.requireNonNull(entry.getValue()).getStates();
            rows.add(new TimeGraphRowModel(entryId, eventList));
        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Builder<TimeGraphEntryModel> builder = new Builder<>();
        long parentId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new TimeGraphEntryModel(parentId, -1, String.valueOf(getTrace().getName()), ss.getStartTime(), ss.getCurrentEndTime()));
        addChildren(ss, builder, ITmfStateSystem.ROOT_ATTRIBUTE, parentId);
        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    /**
     * Add children to the TmfTreeModel
     *
     * @param ss
     *            the state system
     * @param builder
     *            builder for TimeGraphEntryModel
     * @param quark
     *            the quark for which the children will be added
     * @param parentId
     *            the ID of the parent quark
     */
    private void addChildren(ITmfStateSystem ss, Builder<TimeGraphEntryModel> builder, int quark, long parentId) {
        for (Integer child : ss.getSubAttributes(quark, false)) {
            long childId = getId(child);
            String name = ss.getAttributeName(child);
            builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), true));
            addChildren(ss, builder, child, childId);
        }
    }
}
