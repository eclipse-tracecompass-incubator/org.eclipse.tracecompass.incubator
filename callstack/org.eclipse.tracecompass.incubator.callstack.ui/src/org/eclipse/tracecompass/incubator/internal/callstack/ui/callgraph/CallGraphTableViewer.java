/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.callgraph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Displays the Call Stack data in a column table
 *
 * @author Sonia Farrah
 */
public class CallGraphTableViewer extends AbstractSegmentStoreTableViewer {

//    private final String fAnalysisId;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param tableViewer
     *            The table viewer
     * @param analysisId The ID of the analysis for this view
     */
    public CallGraphTableViewer(@NonNull TableViewer tableViewer, String analysisId) {
        super(tableViewer);
//        fAnalysisId = analysisId;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
//        Iterable<CallGraphAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, CallGraphAnalysis.class);
//        return StreamUtils.getStream(modules)
//                .filter(m -> m.getId().equals(fAnalysisId))
//                .findFirst().orElse(null);
        return null;
        // TODO: re-implement this with callstack instead of callgraph
//        if (module == null) {
//            return null;
//        }
//        module.schedule();
//        Job job = new Job(Messages.CallGraphAnalysis) {
//
//            @Override
//            protected IStatus run(IProgressMonitor monitor) {
//                module.waitForCompletion(NonNullUtils.checkNotNull(monitor));
//                if (monitor.isCanceled()) {
//                    return Status.CANCEL_STATUS;
//                }
//                return Status.OK_STATUS;
//            }
//        };
//        job.schedule();
//        return module;
    }
}