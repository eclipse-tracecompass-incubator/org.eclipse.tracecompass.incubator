/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph.FlameGraphView;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * View to display the flame graph. This uses the flameGraphNode tree generated
 * by CallGraphAnalysisUI.
 *
 * @author Sonia Farrah
 */
@NonNullByDefault({})
public class FlameGraphSelView extends FlameGraphView {

    /**
     * ID of the view
     */
    public static final String SEL_ID = FlameGraphSelView.class.getPackage().getName() + ".flamegraphViewSel"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public FlameGraphSelView() {
        super(SEL_ID);
    }

    /**
     * Handles the update of the selection. If the selection is a range, then
     * get the call graph for this range and update the view, otherwise get
     * callgraph of the full range
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void handleSelectionChange(TmfSelectionRangeUpdatedSignal signal) {
        ITmfTimestamp beginTime = signal.getBeginTime();
        ITmfTimestamp endTime = signal.getEndTime();

        if (beginTime != endTime) {
            buildFlameGraph(getCallgraphModules(), beginTime, endTime);
        } else {
            buildFlameGraph(getCallgraphModules(), null, null);
        }

    }

}
