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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Abstract data provider for the OTF2 flows XY view.
 *
 * @author Yoann Heitz
 */
public class Otf2FlowsXYDataProvider extends AbstractTreeCommonXDataProvider<Otf2FlowsAnalysis, TmfTreeDataModel> implements IOutputStyleProvider {

    /** Data provider suffix ID */
    private static final String SUFFIX = ".xy.dataprovider"; //$NON-NLS-1$

    private static final String TITLE = "Flows XY dataprovider"; //$NON-NLS-1$

    /** Y axis description for this data provider */
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription("Perceived data flows", "B/s", DataType.NUMBER); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Otf2FlowsXYDataProvider(ITmfTrace trace, Otf2FlowsAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        ImmutableList.Builder<IYModel> ySeries = ImmutableList.builder();
        Map<Integer, double[]> quarkToValues = new HashMap<>();

        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selectedItems == null) {
            return null;
        }

        List<Long> times = DataProviderParameterUtils.extractTimeRequested(fetchParameters);
        if (times == null) {
            return null;
        }

        Map<Long, Integer> idsToQuarks = getSelectedEntries(selectedItems);
        for (Integer quark : idsToQuarks.values()) {
            quarkToValues.put(quark, new double[times.size()]);
        }

        // Query the state system to fill the arrays of values
        try {
            for (ITmfStateInterval interval : ss.query2D(quarkToValues.keySet(), times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return null;
                }
                double[] row = quarkToValues.get(interval.getAttribute());
                Object value = interval.getValue();
                if (row != null && (value instanceof Number)) {
                    Double dblValue = ((Number) value).doubleValue();
                    for (int i = 0; i < times.size(); i++) {
                        Long time = times.get(i);
                        if (interval.getStartTime() <= time && interval.getEndTime() >= time) {
                            // Convert from B/ns to B/s and fill the array
                            double valueInBPerSecond = dblValue * 1E9;
                            row[i] = valueInBPerSecond;

                            // Increment parents values
                            int parentQuark = ss.getParentAttributeQuark(interval.getAttribute());
                            while (parentQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
                                double[] parentRow = quarkToValues.get(parentQuark);
                                if (parentRow != null) {
                                    parentRow[i] += valueInBPerSecond;
                                }
                                parentQuark = ss.getParentAttributeQuark(parentQuark);
                            }
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            Activator.getInstance().logError(e.getMessage());
            return null;
        }

        for (Entry<Integer, double[]> values : quarkToValues.entrySet()) {
            ySeries.add(new YModel(getId(values.getKey()), ss.getFullAttributePath(values.getKey()), values.getValue(), Y_AXIS_DESCRIPTION));
        }
        return ySeries.build();
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Builder<TmfTreeDataModel> builder = new Builder<>();
        long parentId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new TmfTreeDataModel(parentId, -1, String.valueOf(getTrace().getName())));
        addChildren(ss, builder, ITmfStateSystem.ROOT_ATTRIBUTE, parentId);
        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    /**
     * Add children to the TmfTreeModel
     *
     * @param ss
     *            the state system
     * @param builder
     *            builder for TmfTreeDataModel
     * @param quark
     *            the quark for which the children will be added
     * @param parentId
     *            the ID of the parent quark
     */
    protected void addChildren(ITmfStateSystem ss, Builder<TmfTreeDataModel> builder, int quark, long parentId) {
        for (Integer child : ss.getSubAttributes(quark, false)) {
            long childId = getId(child);
            String name = ss.getFullAttributePath(child);
            builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), true));
            addChildren(ss, builder, child, childId);
        }
    }

    @Override
    protected String getTitle() {
        return TITLE;
    }

    /**
     * Gets the ID of this data provider
     *
     * @return the ID of this data provider
     */
    public static String getFullDataProviderId() {
        return Otf2FlowsAnalysis.getFullAnalysisId() + SUFFIX;
    }
}
