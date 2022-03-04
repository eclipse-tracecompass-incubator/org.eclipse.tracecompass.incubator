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
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
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
        @Nullable List<Long> requestedTimes = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        @Nullable List<Long> requestedEntries = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
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
            e.printStackTrace();
        }

        Map<String, String> model = new HashMap<>();
        model.put(TOTAL_FLOW, String.valueOf(totalFlow * 1E3) + FLOW_UNIT);
        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Returns the depth of a quark in the state system
     *
     * @param ss
     *            the state system
     * @param quark
     *            the quark for which we want to know the depth
     * @return the depth of the quark in the state system
     */
    private int getQuarkDepth(ITmfStateSystem ss, int quark) {
        int parentQuark = ss.getParentAttributeQuark(quark);
        if (parentQuark == ITmfStateSystem.ROOT_ATTRIBUTE) {
            return 0;
        }
        return 1 + getQuarkDepth(ss, parentQuark);
    }

    private static final int MAX_DEPTH = 4;

    /**
     * Return a map associating depths to the list of quarks at this depth in
     * the state system
     *
     * @param ss
     *            the state system
     * @return a map associating depths to the list of quarks at this depth in
     *         the state system
     */
    private Map<Integer, List<Integer>> getDepthToQuarksMap(ITmfStateSystem ss) {
        List<Integer> allQuarks = ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, true);
        Map<Integer, List<Integer>> depthToQuarks = new HashMap<>();
        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            depthToQuarks.put(depth, new ArrayList<>());
        }
        for (int quark : allQuarks) {
            int depth = getQuarkDepth(ss, quark);
            List<Integer> quarksAtThisDepth = depthToQuarks.get(depth);
            if (quarksAtThisDepth != null) {
                quarksAtThisDepth.add(quark);
            }
        }
        return depthToQuarks;
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

        Map<Integer, List<Integer>> depthToQuark = getDepthToQuarksMap(ss);
        Map<Long, FlowsRowModel> idRowModelMap = new HashMap<>();

        //Initialize row models
        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<Integer> quarksAtThisDepth = depthToQuark.get(depth);
            if (quarksAtThisDepth == null) {
                return null;
            }
            for (int quark : quarksAtThisDepth) {
                long entryId = getId(quark);
                int parentQuark = ss.getParentAttributeQuark(quark);
                long parentEntryId = getId(parentQuark);
                FlowsRowModel parentRowModel = null;
                if (parentQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
                    parentRowModel = idRowModelMap.get(parentEntryId);
                }
                idRowModelMap.put(entryId, new FlowsRowModel(entryId, new ArrayList<>(), parentRowModel));
            }
        }

        // Query the intervals for the quarks at maximum depth (representing
        // threads) and fill the row models
        List<Integer> threadQuarks = depthToQuark.get(MAX_DEPTH);
        if (threadQuarks == null) {
            return null;
        }
        for (ITmfStateInterval interval : ss.query2D(threadQuarks, times)) {
            int quark = interval.getAttribute();
            long entryId = getId(quark);
            FlowsRowModel rowModel = idRowModelMap.get(entryId);
            long startTime = interval.getStartTime();
            long endTime = interval.getEndTime();
            double flowValue = interval.getValueDouble();
            if (rowModel != null) {
                rowModel.addFlowChange(startTime, flowValue);
                rowModel.addFlowChange(endTime, -flowValue);
            }
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (int depth = 0; depth <= MAX_DEPTH; depth++) {
            List<Integer> quarksAtThisDepth = depthToQuark.get(depth);
            if (quarksAtThisDepth == null) {
                return null;
            }
            for (int quark : quarksAtThisDepth) {
                long entryId = getId(quark);
                FlowsRowModel rowModel = idRowModelMap.get(entryId);
                if (rowModel != null) {
                    rowModel.computeStatisticsAndStates(this, predicates, monitor);
                    List<ITimeGraphState> eventList = rowModel.getStates();
                    rows.add(new TimeGraphRowModel(entryId, eventList));
                }
            }
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
