/*******************************************************************************
 * Copyright (c) 2015, 2017 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.cct;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Abstract view to to be extended to display segment store statistics.
 *
 * @author Geneviève Bastien
 */
public class CallingContextTreeView extends TmfView {

    /**
     * The ID of this view
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.callstack.ui.views.cct"; //$NON-NLS-1$

    private @Nullable AbstractTmfTreeViewer fCctViewer = null;

    /**
     * Constructor
     */
    public CallingContextTreeView() {
        super("StatisticsView"); //$NON-NLS-1$
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        String analysisId = NonNullUtils.nullToEmptyString(getViewSite().getSecondaryId());
        AbstractTmfTreeViewer cctViewer = new CallingContextTreeViewer(parent, analysisId);
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            cctViewer.loadTrace(trace);
        }
        fCctViewer = cctViewer;
    }

    @Override
    public void setFocus() {
        AbstractTmfTreeViewer statsViewer = fCctViewer;
        if (statsViewer != null) {
            statsViewer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        AbstractTmfTreeViewer statsViewer = fCctViewer;
        if (statsViewer != null) {
            statsViewer.dispose();
        }
    }

}
