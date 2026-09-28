/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

/** Server-compatible provider for the active TraceX time base. */
public final class TraceXDataProvider implements ITimeGraphDataProvider<TimeGraphEntryModel> {

    private static final long TRACE_ENTRY_ID = 1;

    private final TraceXTrace fTrace;

    /**
     * Construct a provider for one trace instance.
     *
     * @param trace
     *            initialized TraceX trace
     */
    public TraceXDataProvider(TraceXTrace trace) {
        fTrace = trace;
    }

    @Override
    public String getId() {
        return TraceXDataProviderFactory.ID;
    }

    @Override
    public TmfModelResponse<TmfTreeModel<TimeGraphEntryModel>> fetchTree(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        long start = fTrace.getStartTime().toNanos();
        long end = fTrace.getEndTime().toNanos();
        TimeGraphEntryModel entry = new TimeGraphEntryModel(TRACE_ENTRY_ID, -1,
                fTrace.getName(), start, end);
        TmfTreeModel<TimeGraphEntryModel> model = new TmfTreeModel<>(
                Collections.singletonList("Trace"), //$NON-NLS-1$
                Collections.singletonList(entry));
        return completed(model);
    }

    @Override
    public TmfModelResponse<TimeGraphModel> fetchRowModel(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        return completed(new TimeGraphModel(Collections.emptyList()));
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        return completed(Collections.emptyList());
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        Map<String, String> tooltip = new LinkedHashMap<>();
        tooltip.put("Time unit", Objects.requireNonNull( //$NON-NLS-1$
                fTrace.getProperties().get("time_unit"))); //$NON-NLS-1$
        tooltip.put("Ticks per second", //$NON-NLS-1$
                Objects.requireNonNull(fTrace.getProperties().get(
                        "ticks_per_second"))); //$NON-NLS-1$
        return completed(Collections.unmodifiableMap(tooltip));
    }

    private static <T> TmfModelResponse<T> completed(T model) {
        return new TmfModelResponse<>(model, Status.COMPLETED,
                CommonStatusMessage.COMPLETED);
    }
}
