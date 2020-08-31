/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.io.latencies.stats;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentsStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentsStatisticsViewer;
import org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput.ExecQueueStatsDataProviderFactory;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;

/**
 * Statistics view for the disk requests' execution queue latencies
 *
 * @author Geneviève Bastien
 */
public class ExecQueueLatencyStatisticsView extends AbstractSegmentsStatisticsView {

    @Override
    protected @NonNull AbstractTmfTreeViewer createSegmentStoreStatisticsViewer(@NonNull Composite parent) {
        return new AbstractSegmentsStatisticsViewer(parent, ExecQueueStatsDataProviderFactory.ID) {

        };
    }

}
