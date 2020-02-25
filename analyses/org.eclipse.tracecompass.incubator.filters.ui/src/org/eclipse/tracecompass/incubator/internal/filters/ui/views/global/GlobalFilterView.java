/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.filters.ui.views.global;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TmfFilterAppliedSignal;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * @author Geneviève Bastien
 *
 */
@SuppressWarnings("restriction")
public class GlobalFilterView extends TmfView {

    private @Nullable GlobalFilterViewer fViewer = null;

    /**
     * Constructor
     */
    public GlobalFilterView() {
        super("Global Filters View"); //$NON-NLS-1$
    }

    @Override
    public void setFocus() {
        if (fViewer != null) {
            fViewer.setFocus();
        }
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);

        if (parent == null) {
            return;
        }
        fViewer = new GlobalFilterViewer(this, parent, SWT.NONE);
        getViewSite().getActionBars().getToolBarManager().add(new Action() {
            @Override
            public String getDescription() {
                return "Delete the currently selected filters"; //$NON-NLS-1$
            }

            @Override
            public String getText() {
                return "Delete"; //$NON-NLS-1$
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_REMOVE_BOOKMARK);
            }

            @Override
            public void run() {
                if (fViewer != null) {
                    fViewer.deleteSelected();
                }
            }
        });
    }

    /**
     * An event filter was applied. The signal will be ignored if coming from
     * this view
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void eventSearchApplied(TmfFilterAppliedSignal signal) {
        if (signal.getSource() == this) {
            return;
        }
        applySearchOrFilter(signal.getFilter().getRegexes());
    }

    /**
     * Remove current filters and apply the trace's current filters
     *
     * @param signal
     *            The trace selection signal
     */
    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            applySearchOrFilter(Collections.emptyList());
            return;
        }
        TraceCompassFilter filter = TraceCompassFilter.getFilterForTrace(trace);
        applySearchOrFilter(filter == null ? Collections.emptyList() : filter.getRegexes());
    }

    private void applySearchOrFilter(Collection<String> regexes) {
        GlobalFilterViewer viewer = fViewer;
        if (viewer == null) {
            // Not set yet
            return;
        }

        viewer.eventFilterApplied(regexes);
    }

}
